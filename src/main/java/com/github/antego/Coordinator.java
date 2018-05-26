package com.github.antego;

import com.typesafe.config.Config;
import org.apache.zookeeper.*;
import org.apache.zookeeper.data.Stat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;


public class Coordinator implements AutoCloseable {
    private static final Logger logger = LoggerFactory.getLogger(Coordinator.class);

    private final ZooKeeper zookeeper;
    private final String rootNodeName;
    private final String nodePrefix;

    public Coordinator(Config config, ZooKeeper zookeeper) throws KeeperException, InterruptedException {
        this.zookeeper = zookeeper;
        rootNodeName = config.getString("zookeeper.root.node.name");
        nodePrefix = config.getString("zookeeper.node.prefix");

        String selfHostPort = config.getString("host") + ":" + config.getInt("port");

        createRootNodeIfNotExists();
        zookeeper.create(nodePrefix, selfHostPort.getBytes(StandardCharsets.UTF_8), ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL_SEQUENTIAL);
    }

    private void createRootNodeIfNotExists() throws KeeperException, InterruptedException {
        Stat stat = zookeeper.exists(rootNodeName, false);
        if (stat == null) {
            logger.info(zookeeper.create(rootNodeName, new byte[0], ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT));
        }
    }

    @Override
    public void close() throws Exception {
        zookeeper.close();
    }
}
