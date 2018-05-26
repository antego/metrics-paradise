package com.github.antego;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.ZooDefs;
import org.apache.zookeeper.ZooKeeper;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;

import java.io.IOException;

import static com.github.antego.TestHelper.createZookeeperClient;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class ZookeeperWatcherTest {

    private static final Logger logger = LoggerFactory.getLogger(ZookeeperWatcherTest.class);
    private static final Config config = ConfigFactory.load();


    @ClassRule
    public static GenericContainer zookeeperContainer = new GenericContainer("bitnami/zookeeper:3.4.12")
            .withExposedPorts(2181)
            .withEnv("ALLOW_ANONYMOUS_LOGIN", "yes");
    private static int zookeeperPort;

    private static ZooKeeper zookeeperClient;

    @BeforeClass
    public static void createTestVerifyClient() throws IOException {
        zookeeperPort = zookeeperContainer.getMappedPort(2181);
        zookeeperClient = createZookeeperClient(zookeeperPort);
    }

    @AfterClass
    public static void disconnectFromZookeeper() throws InterruptedException {
        zookeeperClient.close();
    }

    @Test
    public void shouldNotifyAboutChildrenNodeChange() throws KeeperException, InterruptedException {
        Coordinator coordinator = mock(Coordinator.class);
        ZookeeperWatcher watcher = new ZookeeperWatcher(coordinator);

        zookeeperClient.create(config.getString(ConfigurationKey.ZOOKEEPER_ROOT_NODE_NAME), null, ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
        zookeeperClient.create(config.getString(ConfigurationKey.ZOOKEEPER_NODE_PREFIX), null, ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);

        zookeeperClient.getChildren(config.getString(ConfigurationKey.ZOOKEEPER_ROOT_NODE_NAME), watcher);
        zookeeperClient.create(config.getString(ConfigurationKey.ZOOKEEPER_NODE_PREFIX) + "1", null, ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
        zookeeperClient.getChildren(config.getString(ConfigurationKey.ZOOKEEPER_ROOT_NODE_NAME), watcher);

        verify(coordinator).notifyClusterStateChanged();
    }


}