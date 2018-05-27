package com.github.antego;

import com.typesafe.config.Config;
import org.apache.zookeeper.*;
import org.apache.zookeeper.data.Stat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.*;


public class Coordinator implements AutoCloseable {
    private static final Logger logger = LoggerFactory.getLogger(Coordinator.class);

    private final String rootNodeName;
    private final String nodePrefix;
    private final Config config;
    private final ZookeeperWatcherFactory watcherFactory;
    private ZooKeeper zookeeper;

    private ClusterState clusterState;
    private String selfId;

    public Coordinator(Config config, ZookeeperWatcherFactory watcherFactory) {
        this.config = config;
        rootNodeName = config.getString(ConfigurationKey.ZOOKEEPER_ROOT_NODE_NAME);
        nodePrefix = config.getString(ConfigurationKey.ZOOKEEPER_NODE_PREFIX);
        this.watcherFactory = watcherFactory;
    }

    public void init() throws KeeperException, InterruptedException {
        createRootNodeIfNotExists();
    }

    @Override
    public void close() throws Exception {
        zookeeper.close();
    }

    public void setZookeeper(ZooKeeper zookeeper) {
        this.zookeeper = zookeeper;
    }

    private void createSelfNode(String id) throws KeeperException, InterruptedException {
        if (id == null) {
            id = UUID.randomUUID().toString();
        }
        String nodeName = nodePrefix + id;
        String selfHost = config.getString("host");
        int selfPort = config.getInt("port");
        byte[] nodeData = (selfHost + ":" + selfPort).getBytes(StandardCharsets.UTF_8);
        zookeeper.create(nodeName, nodeData, ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL);
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

    //todo synchronized?
    public void notifyClusterStateChanged() throws KeeperException, InterruptedException {
        List<String> newChildrenNodes = zookeeper.getChildren(rootNodeName, true);
        List<Node> nodes = new ArrayList<>();
        for (String path : newChildrenNodes) {
            if (!path.startsWith(nodePrefix)) {
                logger.error("Retrieved path [{}] is not starting with prefix [{}]", path, nodePrefix);
                continue;
            }
            String id = path.substring(nodePrefix.length(), path.length());
            byte[] data;
            try {
                data = zookeeper.getData(path, false, null);
            } catch (KeeperException.NoNodeException e) {
                logger.error("Can't retrieve data for node [{}]. Node not found.", e);
                continue;
            }
            String[] hostPort = new String(data, StandardCharsets.UTF_8).split(":");
            String host = hostPort[0];
            int port = Integer.valueOf(hostPort[1]);
            Node node = new Node(id, host, port);
            nodes.add(node);
        }
        clusterState = new ClusterState(nodes, selfId);
    }

    public boolean isMyKey(int i) {
        return i % clusterState.getNumberOfInstances() == clusterState.getSelfOrdinal();
    }

    public void advertiseSelf(String id) throws KeeperException, InterruptedException {
        createSelfNode(id);
    }

    public void advertiseSelf() throws KeeperException, InterruptedException {
        createSelfNode(null);
    }
}
