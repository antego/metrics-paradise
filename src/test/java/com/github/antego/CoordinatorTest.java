package com.github.antego;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.data.Stat;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertTrue;

public class CoordinatorTest {
    private static final Logger logger = LoggerFactory.getLogger(CoordinatorTest.class);
    private static final Config config = ConfigFactory.load();

    @ClassRule
    public static GenericContainer zookeeperContainer = new GenericContainer("bitnami/zookeeper:3.4.12")
            .withExposedPorts(2181)
            .withEnv("ALLOW_ANONYMOUS_LOGIN", "yes");

    private static ZooKeeper zookeeperClient;

    @BeforeClass
    public static void createTestVerifyClient() throws IOException {
        zookeeperClient = connectToZookeeper();
    }

    @AfterClass
    public static void disconnectFromZookeeper() throws InterruptedException {
        zookeeperClient.close();
    }

    @Test
    public void shouldCreateNodeOnStart() throws Exception {
        try (Coordinator coordinator = new Coordinator(config, connectToZookeeper())) {
            Stat stat = zookeeperClient.exists(config.getString("zookeeper.root.node.name"), false);

            assertTrue(stat != null);
        }
    }

    private static ZooKeeper connectToZookeeper() throws IOException {
        CountDownLatch latch = new CountDownLatch(1);
        ZooKeeper zooKeeper = new ZooKeeper("localhost:" + zookeeperContainer.getMappedPort(2181), 200000, event -> {
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
}
