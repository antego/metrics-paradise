package com.github.antego;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.apache.zookeeper.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class TestHelper {
    private static final Logger logger = LoggerFactory.getLogger(TestHelper.class);
    private static final Config config = ConfigFactory.load();

    public static ZooKeeper createZookeeperClient(int port) throws IOException {
        CountDownLatch latch = new CountDownLatch(1);
        ZooKeeper zooKeeper = new ZooKeeper("localhost:" + port, 200000, event -> {
            logger.info("New state {}", event.getState());
            if (event.getState() == Watcher.Event.KeeperState.SyncConnected) {
                latch.countDown();
            }
        });
        try {
            latch.await(config.getLong("zookeeper.connect.timeout.sec"), TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return zooKeeper;
    }

    public static void createPath(ZooKeeper zooKeeper, String path) throws KeeperException, InterruptedException {
        zooKeeper.create(path, null, ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
    }

    public static String generateRandomNode(String root) {
        return root + "/" + UUID.randomUUID().toString();
    }
}
