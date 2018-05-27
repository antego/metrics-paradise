package com.github.antego.cluster;

import com.github.antego.ConfigurationKey;
import com.github.antego.cluster.Coordinator;
import com.github.antego.cluster.ClusterWatcher;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigValueFactory;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.ZooKeeper;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.testcontainers.containers.GenericContainer;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import static com.github.antego.TestHelper.createPath;
import static com.github.antego.TestHelper.generateRandomNode;
import static com.github.antego.Utils.createZookeeperClient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class ClusterWatcherTest {
    private static final Config config = ConfigFactory.load();

    @ClassRule
    public static GenericContainer zookeeperContainer = new GenericContainer("bitnami/zookeeper:3.4.12")
            .withExposedPorts(2181)
            .withEnv("ALLOW_ANONYMOUS_LOGIN", "yes");

    private static ZooKeeper zookeeperClient;

    @BeforeClass
    public static void createTestVerifyClient() throws IOException {
        int zookeeperPort = zookeeperContainer.getMappedPort(2181);
        zookeeperClient = createZookeeperClient(config.withValue(ConfigurationKey.ZOOKEEPER_PORT,
                ConfigValueFactory.fromAnyRef(zookeeperPort)));
    }

    @AfterClass
    public static void disconnectFromZookeeper() throws InterruptedException {
        zookeeperClient.close();
    }

    @Test
    public void shouldNotifyAboutChildrenNodeChange() throws KeeperException, InterruptedException {
        Coordinator coordinator = mock(Coordinator.class);
        CountDownLatch latch = new CountDownLatch(1);
        ClusterWatcher watcher = new ClusterWatcher(coordinator) {
            @Override
            public void process(WatchedEvent event) {
                super.process(event);
                latch.countDown();
            }
        };

        createPath(zookeeperClient, config.getString(ConfigurationKey.ZOOKEEPER_ROOT_NODE_NAME));
        List<String> children = zookeeperClient.getChildren(config.getString(ConfigurationKey.ZOOKEEPER_ROOT_NODE_NAME), watcher);
        createPath(zookeeperClient, generateRandomNode(config.getString(ConfigurationKey.ZOOKEEPER_ROOT_NODE_NAME)));

        if (children.size() == 0) {
            latch.await();
            verify(coordinator, times(1)).notifyClusterStateChanged();
        }
    }
}