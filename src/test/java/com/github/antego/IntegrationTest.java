package com.github.antego;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigValueFactory;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.testcontainers.containers.GenericContainer;

import java.io.IOException;

import static com.github.antego.Utils.createZookeeperClient;

public class IntegrationTest {
    private static GenericContainer zookeeperContainer;

    @BeforeClass
    public static void createTestVerifyClient() throws IOException {
        zookeeperContainer = new GenericContainer("bitnami/zookeeper:3.4.12")
                .withExposedPorts(2181)
                .withEnv("ALLOW_ANONYMOUS_LOGIN", "yes");
        zookeeperContainer.start();
    }

    @AfterClass
    public static void disconnectFromZookeeper() throws InterruptedException {
        zookeeperContainer.stop();
    }

    @Test
    public void shouldStartInstanceAndSaveMetrics() throws Exception {
        Config config = ConfigFactory.load();
        config = config.withValue(ConfigurationKey.ZOOKEEPER_PORT,
                ConfigValueFactory.fromAnyRef(zookeeperContainer.getMappedPort(2181)));

        new Runner().start(config);

    }
}
