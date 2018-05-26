package com.github.antego;

import com.typesafe.config.Config;
import org.apache.zookeeper.*;
import org.apache.zookeeper.data.Stat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;


public class Coordinator implements AutoCloseable, Watcher {
    private static final Logger logger = LoggerFactory.getLogger(Coordinator.class);

    private final String rootNodeName;
    private final String nodePrefix;
    private final Config config;
    private ZooKeeper zookeeper;

    public Coordinator(Config config) {
        this.config = config;
        rootNodeName = config.getString("zookeeper.root.node.name");
        nodePrefix = config.getString("zookeeper.node.prefix");
    }

    public void init() throws KeeperException, InterruptedException {
        createRootNodeIfNotExists();
        createSelfNode();
    }

    @Override
    public void close() throws Exception {
        zookeeper.close();
    }

    @Override
    public void process(WatchedEvent event) {

    }

    public void setZookeeper(ZooKeeper zookeeper) {
        this.zookeeper = zookeeper;
    }

    private void createSelfNode() throws KeeperException, InterruptedException {
        String selfHostPort = config.getString("host") + ":" + config.getInt("port");
        zookeeper.create(nodePrefix, selfHostPort.getBytes(StandardCharsets.UTF_8), ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL_SEQUENTIAL);
    }

    private void createRootNodeIfNotExists() throws KeeperException, InterruptedException {
        Stat stat = zookeeper.exists(rootNodeName, false);
        if (stat == null) {
            logger.info(zookeeper.create(rootNodeName, new byte[0], ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT));
        }
    }
}
