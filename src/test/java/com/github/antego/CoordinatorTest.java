package com.github.antego;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.apache.zookeeper.KeeperException;
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
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;

import static com.github.antego.ConfigurationKey.ZOOKEEPER_NODE_PREFIX;
import static com.github.antego.ConfigurationKey.ZOOKEEPER_ROOT_NODE_NAME;
import static com.github.antego.TestHelper.createZookeeperClient;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class CoordinatorTest {
    private static final Logger logger = LoggerFactory.getLogger(CoordinatorTest.class);
    private static final Config config = ConfigFactory.load();

    private static GenericContainer zookeeperContainer;
    private static int zookeeperPort;

    private static ZooKeeper zookeeperClient;

    @BeforeClass
    public static void createTestVerifyClient() throws IOException {
        zookeeperContainer = new GenericContainer("bitnami/zookeeper:3.4.12")
                .withExposedPorts(2181)
                .withEnv("ALLOW_ANONYMOUS_LOGIN", "yes");
        zookeeperContainer.start();
        zookeeperPort = zookeeperContainer.getMappedPort(2181);
        zookeeperClient = createZookeeperClient(zookeeperPort);
    }

    @AfterClass
    public static void disconnectFromZookeeper() throws InterruptedException {
        zookeeperClient.close();
        zookeeperContainer.stop();
    }

    @Test
    public void shouldCreateNodeOnStart() throws Exception {
        try (Coordinator coordinator = new Coordinator(config, mock(ZookeeperWatcherFactory.class))) {
            coordinator.setZookeeper(createZookeeperClient(zookeeperPort));
            coordinator.init();

            Stat stat = zookeeperClient.exists(config.getString(ZOOKEEPER_ROOT_NODE_NAME), false);
            assertTrue(stat != null);
        }
    }

    @Test
    public void shouldDeleteNodeOnExit() throws Exception {
        try (Coordinator coordinator = new Coordinator(config, mock(ZookeeperWatcherFactory.class))) {
            coordinator.setZookeeper(createZookeeperClient(zookeeperPort));
            coordinator.init();
        }
        Stat stat = zookeeperClient.exists(config.getString(ZOOKEEPER_NODE_PREFIX) + "0000000001", false);
        assertTrue(stat == null);
    }

    @Test
    public void shouldSignalAboutChangedClusterState() throws KeeperException, InterruptedException {
        ZookeeperWatcherFactory factory = mock(ZookeeperWatcherFactory.class);
        ZooKeeper zooKeeper = mock(ZooKeeper.class);
        when(zooKeeper.getChildren(any(), anyBoolean()))
                .thenReturn(Arrays.asList("1", "2", "3"))
                .thenReturn(Arrays.asList("2", "3", "4", "7"));
        when(zooKeeper.getData(anyString(), anyBoolean(), any()))
                .thenReturn("host:port".getBytes(StandardCharsets.UTF_8));

        Coordinator coordinator = new Coordinator(config, factory);
        coordinator.advertiseSelf();
        coordinator.notifyClusterStateChanged();

        assertTrue(coordinator.isMyKey(2)); // 2 mod 3 = 2
        assertTrue(coordinator.isMyKey(20)); // 20 mod 3 = 2

        coordinator.notifyClusterStateChanged();
        assertFalse(coordinator.isMyKey(18)); // 18 mod 4 = 2
        assertTrue(coordinator.isMyKey(29)); // 29 mod 4 = 1
    }

    //todo fetch all nodes on start
    //todo test fetching state on cluster change
    //todo test self id on advertise

}
