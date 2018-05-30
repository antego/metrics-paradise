package com.github.antego;

import com.github.antego.api.Endpoint;
import com.github.antego.cluster.ClusterWatcherFactory;
import com.github.antego.cluster.Coordinator;
import com.github.antego.core.LocalStorage;
import com.github.antego.api.RemoteNodeClient;
import com.github.antego.core.MetricRouter;
import com.github.antego.util.Utils;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.apache.zookeeper.ZooKeeper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;
import java.util.concurrent.CountDownLatch;


/**
 * Runner class. Represents a lifecycle of a node.
 * Lifecycle:
 * 1. Fire up coordinator and get up-to-date state of a cluster.
 * 2. Prepare {@code MetricRouter}.
 * 3. Call rebalance. In case of cold start it will do nothing.
 * If node is starting after restart than all data will be rebalanced from it to other nodes.
 * 4. Start API Endpoint for serving requests. Passed latch will stop the main thread from
 * proceeding to a shutdown sequence. Latch will be unlocked on a /shutdown api call.
 * 5. On a shutdown the sequence is opposite to a start sequence.
 * First current node deleted from the zookeeper and data is migrated to the other nodes.
 */
public class Runner {
    private static final Logger logger = LoggerFactory.getLogger(Runner.class);

    public static void main(String[] args) throws Exception {
        new Runner().start(ConfigFactory.load());
    }

    public void start(Config config) {
        logger.info("Starting Metrics Paradise Node");
        try {
            // Start phase
            ZooKeeper zooKeeper = Utils.createZookeeperClient(config);
            Coordinator coordinator = new Coordinator(zooKeeper, config, new ClusterWatcherFactory());

            MetricRouter metricRouter = new MetricRouter(new LocalStorage(), coordinator, new RemoteNodeClient(config));
            metricRouter.doRebalanceIfNeeded();

            CountDownLatch shutdown = new CountDownLatch(1);
            Endpoint endpoint = new Endpoint(metricRouter, shutdown, config);
            endpoint.start();

            // Normal work
            coordinator.advertiseSelf(UUID.randomUUID().toString());

            try {
                shutdown.await();
            } catch (InterruptedException ignore) {
            }

            // Shutdown phase
            coordinator.removeSelf();
            endpoint.stop();

            coordinator.refreshClusterState();
            metricRouter.doRebalanceIfNeeded();

            metricRouter.close();
            coordinator.close();
        } catch (Exception e) {
            logger.error("Exception in runner", e);
        }
    }
}
