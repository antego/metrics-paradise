package com.github.antego;

import com.github.antego.api.Endpoint;
import com.github.antego.cluster.ClusterWatcherFactory;
import com.github.antego.cluster.Coordinator;
import com.github.antego.core.LocalStorage;
import com.github.antego.api.RemoteNodeClient;
import com.github.antego.core.MetricRouter;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.apache.zookeeper.ZooKeeper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;
import java.util.concurrent.CountDownLatch;

public class Runner {
    private static final Logger logger = LoggerFactory.getLogger(Runner.class);

    public static void main(String[] args) throws Exception {
        new Runner().start(ConfigFactory.load());
    }

    public void start(Config config) {
        try {
            ZooKeeper zooKeeper = Utils.createZookeeperClient(config);
            Coordinator coordinator = new Coordinator(zooKeeper, config, new ClusterWatcherFactory());
            coordinator.init();

            MetricRouter metricRouter = new MetricRouter(new LocalStorage(), coordinator, new RemoteNodeClient());
            metricRouter.doRebalanceIfNeeded();

            CountDownLatch shutdown = new CountDownLatch(1);
            Endpoint endpoint = new Endpoint(metricRouter, shutdown, config);
            endpoint.start();

            coordinator.advertiseSelf(UUID.randomUUID().toString());

            //todo handle interrupted exception
            shutdown.await();

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
