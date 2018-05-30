package com.github.antego.cluster;

import com.codahale.metrics.Timer;
import com.github.antego.util.ConfigurationKey;
import com.github.antego.util.MetricName;
import com.github.antego.util.Monitoring;
import com.typesafe.config.Config;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.ZooDefs;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.data.Stat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;


/**
 * Class is responsible for maintenance of up-to-date cluster state
 */
public class Coordinator implements AutoCloseable {
    private static final Logger logger = LoggerFactory.getLogger(Coordinator.class);

    private final String rootNodeName;
    private final String nodePrefix;
    private final Config config;
    private final ClusterWatcherFactory watcherFactory;
    private final ZooKeeper zookeeper;

    private volatile ClusterState clusterState;
    private volatile String selfId;

    public Coordinator(ZooKeeper zooKeeper, Config config, ClusterWatcherFactory watcherFactory) throws KeeperException, InterruptedException {
        logger.info("Creating Coordinator");
        this.config = config;
        rootNodeName = config.getString(ConfigurationKey.ZOOKEEPER_ROOT_NODE_NAME);
        nodePrefix = config.getString(ConfigurationKey.ZOOKEEPER_NODE_PREFIX);
        this.watcherFactory = watcherFactory;
        this.zookeeper = zooKeeper;

        logger.info("Initializing Coordinator");
        createRootNodeIfNotExists();
        refreshClusterState();
    }

    @Override
    public void close() throws Exception {
        logger.info("Closing Coordinator");
        zookeeper.close();
    }

    /**
     * Create node in a zookeeper thus notify other nodes of our presence.
     * Not called from init because we need a time to rebalance stale data on a restart.
     *
     *
     * @param id Id which will be advertised to other nodes
     * @throws KeeperException
     * @throws InterruptedException
     */
    public void advertiseSelf(String id) throws KeeperException, InterruptedException {
        logger.info("Notifying cluster about me");
        createSelfNode(id);
    }

    private void createSelfNode(String id) throws KeeperException, InterruptedException {
        if (id == null || config.getBoolean(ConfigurationKey.ZOOKEEPER_NODE_RANDOM_ID)) {
            logger.info("Generating self id");
            id = UUID.randomUUID().toString();
        }
        logger.info("My id is [{}]", id);
        String nodePath = rootNodeName + "/" + nodePrefix + id;
        String selfHost = config.getString(ConfigurationKey.ADVERTISE_HOST);
        int selfPort = config.getInt(ConfigurationKey.ADVERTISE_PORT);
        byte[] nodeData = (selfHost + ":" + selfPort).getBytes(StandardCharsets.UTF_8);
        zookeeper.create(nodePath, nodeData, ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL);
        selfId = id;
    }

    private void createRootNodeIfNotExists() throws KeeperException, InterruptedException {
        logger.info("Checking root path");
        Stat stat = zookeeper.exists(rootNodeName, false);
        if (stat != null) {
            logger.info("Root path already created. Skipping");
            return;
        }
        try {
            String resultPath = zookeeper.create(rootNodeName, null, ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
            logger.info("No root path found. Created zookeeper root path at [{}]", resultPath);
        } catch (KeeperException.NodeExistsException e) {
            logger.info("Tried to create root path. Root path have been already created");
        }
    }

    /*
     * This method is called from the zookeeper event thread.
     * No synchronization needed because methods of zookeeper client are thread-safe.
     * Assignment of ClusterState is done to the volatile variable.
     */
    public void refreshClusterState() throws KeeperException, InterruptedException {
        try (Timer.Context context = Monitoring.getTimerContext(MetricName.REFRESH_CLUSTER_STATE)) {
            logger.info("Refreshing cluster state");
            ClusterWatcher watcher = watcherFactory.createWatcher(this);
            List<String> newChildrenNodes = zookeeper.getChildren(rootNodeName, watcher);
            logger.info("Received cluster nodes {}", newChildrenNodes.toString());
            List<Node> nodes = new ArrayList<>();
            for (String path : newChildrenNodes) {
                String id = path.substring(nodePrefix.length(), path.length());
                byte[] data;
                try {
                    data = zookeeper.getData(rootNodeName + "/" + path, false, null);
                } catch (KeeperException.NoNodeException e) {
                    logger.error("Can't retrieve data for node [{}]. Node not found.", e);
                    continue;
                }
                Node node = Node.fromIdAndData(id, data);
                nodes.add(node);
            }
            clusterState = new ClusterState(nodes, selfId);
        }
    }

    public void removeSelf() throws Exception {
        String nodePath = rootNodeName + "/" + nodePrefix + selfId;
        logger.info("Removing own node [{}] from zookeeper", nodePath);
        zookeeper.delete(nodePath, -1);
    }

    public ClusterState getClusterState() {
        return clusterState;
    }
}
