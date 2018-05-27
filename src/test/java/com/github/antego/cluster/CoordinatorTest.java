package com.github.antego.cluster;

import com.github.antego.ConfigurationKey;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigValueFactory;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.data.Stat;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.testcontainers.containers.GenericContainer;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static com.github.antego.ConfigurationKey.ZOOKEEPER_ROOT_NODE_NAME;
import static com.github.antego.TestHelper.createPath;
import static com.github.antego.TestHelper.generateRandomNode;
import static com.github.antego.Utils.createZookeeperClient;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class CoordinatorTest {
    private static Config config = ConfigFactory.load();
    private static GenericContainer zookeeperContainer;
    private static int zookeeperPort;
    private static ZooKeeper zookeeperClient;
    private ClusterWatcherFactory factory = mock(ClusterWatcherFactory.class);

    @BeforeClass
    public static void createTestVerifyClient() throws IOException {
        zookeeperContainer = new GenericContainer("bitnami/zookeeper:3.4.12")
                .withExposedPorts(2181)
                .withEnv("ALLOW_ANONYMOUS_LOGIN", "yes");
        zookeeperContainer.start();
        zookeeperPort = zookeeperContainer.getMappedPort(2181);
        zookeeperClient = createZookeeperClient(config.withValue(ConfigurationKey.ZOOKEEPER_PORT,
                ConfigValueFactory.fromAnyRef(zookeeperPort)));
    }

    @AfterClass
    public static void disconnectFromZookeeper() throws InterruptedException {
        zookeeperClient.close();
        zookeeperContainer.stop();
    }

    @Before
    public void testInNewRootNode() {
        config = config.withValue(ZOOKEEPER_ROOT_NODE_NAME,
                ConfigValueFactory.fromAnyRef("/" + UUID.randomUUID().toString()));
    }

    @Test
    public void shouldCreateNodeOnStart() throws Exception {
        Coordinator coordinator = new Coordinator(config, factory);
        coordinator.setZookeeper(createZookeeperClient(config.withValue(ConfigurationKey.ZOOKEEPER_PORT,
                ConfigValueFactory.fromAnyRef(zookeeperPort))));
        coordinator.init();

        Stat stat = zookeeperClient.exists(config.getString(ZOOKEEPER_ROOT_NODE_NAME), false);
        assertTrue(stat != null);
    }

    @Test
    public void shouldDeleteNodeOnExit() throws Exception {
        Coordinator coordinator = new Coordinator(config, factory);
        coordinator.setZookeeper(createZookeeperClient(config.withValue(ConfigurationKey.ZOOKEEPER_PORT,
                ConfigValueFactory.fromAnyRef(zookeeperPort))));
        coordinator.init();
        coordinator.advertiseSelf("1");
        coordinator.close();

        Stat stat = zookeeperClient.exists(config.getString(ZOOKEEPER_ROOT_NODE_NAME) + "/1", false);
        assertTrue(stat == null);
    }

    @Test
    public void shouldSignalAboutChangedClusterState() throws KeeperException, InterruptedException {
        ZooKeeper zooKeeper = mock(ZooKeeper.class);
        when(zooKeeper.getChildren(any(), any()))
                .thenReturn(Arrays.asList("1", "2", "3"))
                .thenReturn(Arrays.asList("2", "3", "4", "7"));
        when(zooKeeper.getData(anyString(), anyBoolean(), any()))
                .thenReturn("host:80".getBytes(StandardCharsets.UTF_8));

        Config config = CoordinatorTest.config.withValue(ConfigurationKey.ZOOKEEPER_NODE_PREFIX,
                ConfigValueFactory.fromAnyRef(""));
        Coordinator coordinator = new Coordinator(config, factory);
        coordinator.setZookeeper(zooKeeper);
        coordinator.advertiseSelf("3");

        coordinator.notifyClusterStateChanged();
        assertTrue(coordinator.isMetricOwnedByNode(2)); // 2 mod 3 = 2
        assertTrue(coordinator.isMetricOwnedByNode(20)); // 20 mod 3 = 2

        coordinator.notifyClusterStateChanged();
        assertFalse(coordinator.isMetricOwnedByNode(18)); // 18 mod 4 = 2
        assertTrue(coordinator.isMetricOwnedByNode(29)); // 29 mod 4 = 1
    }

    @Test
    public void shouldAssignWatcherOnInit() throws Exception {
        CountDownLatch latch = new CountDownLatch(1); // need to wait till event comes back
        Coordinator coordinator = new Coordinator(config, factory);
        ClusterWatcher watcher = spy(new ClusterWatcher(coordinator) {
            @Override
            public void process(WatchedEvent event) {
                latch.countDown();
            }
        });
        when(factory.createWatcher(eq(coordinator))).thenReturn(watcher);
        coordinator.setZookeeper(createZookeeperClient(config.withValue(ConfigurationKey.ZOOKEEPER_PORT,
                ConfigValueFactory.fromAnyRef(zookeeperPort))));
        coordinator.init();

        createPath(zookeeperClient, generateRandomNode(config.getString(ZOOKEEPER_ROOT_NODE_NAME)));
        latch.await(10, TimeUnit.SECONDS);
        verify(watcher).process(any());
    }

    //todo fetch all nodes on start
    //todo test self id on advertise

}
