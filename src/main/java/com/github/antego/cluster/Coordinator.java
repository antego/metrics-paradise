package com.github.antego.cluster;

import com.github.antego.ConfigurationKey;
import com.typesafe.config.Config;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.ZooDefs;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.data.Stat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;


public class Coordinator implements AutoCloseable {
    private static final Logger logger = LoggerFactory.getLogger(Coordinator.class);

    private final String rootNodeName;
    private final String nodePrefix;
    private final Config config;
    private final ClusterWatcherFactory watcherFactory;
    private volatile ZooKeeper zookeeper;

    private volatile ClusterState clusterState;
    private AtomicInteger clusterStateVersion = new AtomicInteger();
    private String selfId;

    public Coordinator(Config config, ClusterWatcherFactory watcherFactory) {
        this.config = config;
        rootNodeName = config.getString(ConfigurationKey.ZOOKEEPER_ROOT_NODE_NAME);
        nodePrefix = config.getString(ConfigurationKey.ZOOKEEPER_NODE_PREFIX);
        this.watcherFactory = watcherFactory;
    }

    public void init() throws KeeperException, InterruptedException {
        createRootNodeIfNotExists();
        refreshClusterState();
    }

    @Override
    public void close() throws Exception {
        zookeeper.close();
    }

    public void setZookeeper(ZooKeeper zookeeper) {
        this.zookeeper = zookeeper;
    }

    public void advertiseSelf(String id) throws KeeperException, InterruptedException {
        createSelfNode(id);
    }

    private void createSelfNode(String id) throws KeeperException, InterruptedException {
        if (id == null) {
            id = UUID.randomUUID().toString();
        }
        String nodePath = rootNodeName + "/" + nodePrefix + id;
        String selfHost = config.getString(ConfigurationKey.ADVERTISE_HOST);
        int selfPort = config.getInt(ConfigurationKey.ADVERTISE_PORT);
        byte[] nodeData = (selfHost + ":" + selfPort).getBytes(StandardCharsets.UTF_8);
        zookeeper.create(nodePath, nodeData, ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL);
        selfId = id;
    }

    private void createRootNodeIfNotExists() throws KeeperException, InterruptedException {
        Stat stat = zookeeper.exists(rootNodeName, false);
        if (stat != null) {
            return;
        }
        String resultPath = zookeeper.create(rootNodeName, null, ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
        logger.info("Created zookeeper root path {}", resultPath);
    }

    public void notifyClusterStateChanged() throws KeeperException, InterruptedException {
        refreshClusterState();
    }

    /*
     * This method is called from the zookeeper event thread.
     * No synchronization needed because methods of zookeeper client are thread-safe.
     * Assignment of ClusterState is done to the volatile variable.
     */
    public void refreshClusterState() throws KeeperException, InterruptedException {
        ClusterWatcher watcher = watcherFactory.createWatcher(this);
        List<String> newChildrenNodes = zookeeper.getChildren(rootNodeName, watcher);
        List<Node> nodes = new ArrayList<>();
        for (String path : newChildrenNodes) {
            String id = path.substring(nodePrefix.length(), path.length());
            byte[] data;
            try {
                data = zookeeper.getData(path, false, null);
            } catch (KeeperException.NoNodeException e) {
                logger.error("Can't retrieve data for node [{}]. Node not found.", e);
                continue;
            }
            Node node = Node.fromIdAndData(id, data);
            nodes.add(node);
        }
        clusterState = new ClusterState(nodes, selfId);
        clusterStateVersion.incrementAndGet();
    }

    public boolean isMetricOwnedByNode(int metricHashCode) {
        return metricHashCode % clusterState.getNumberOfInstances() == clusterState.getSelfIndex();
    }

    public int getClusterStateVersion() {
        return clusterStateVersion.get();
    }

    public URI getUriOfMetricNode(String name) {
        Node node = clusterState.getNodeByMetric(name.hashCode());
        String host = node.getHost();
        int port = node.getPort();
        return URI.create("http://" + host + ":" + port);
    }
}
