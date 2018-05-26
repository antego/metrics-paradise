package com.github.antego;

import com.typesafe.config.Config;
import org.apache.zookeeper.*;
import org.apache.zookeeper.data.Stat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.UUID;


public class Coordinator implements AutoCloseable {
    private static final Logger logger = LoggerFactory.getLogger(Coordinator.class);

    private final String rootNodeName;
    private final String nodePrefix;
    private final Config config;
    private final ZookeeperWatcherFactory watcherFactory;
    private ZooKeeper zookeeper;

    private ClusterState clusterState;

    public Coordinator(Config config, ZookeeperWatcherFactory watcherFactory) {
        this.config = config;
        rootNodeName = config.getString("zookeeper.root.node.name");
        nodePrefix = config.getString("zookeeper.node.prefix");
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
    }

    private void createRootNodeIfNotExists() throws KeeperException, InterruptedException {
        Stat stat = zookeeper.exists(rootNodeName, false);
        if (stat != null) {
            return;
        }
        String resultPath = zookeeper.create(rootNodeName, new byte[0], ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);

    }

    public void notifyClusterStateChanged() {

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
