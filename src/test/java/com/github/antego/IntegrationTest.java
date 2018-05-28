package com.github.antego;

import com.github.antego.core.Metric;
import com.github.antego.api.RemoteNodeClient;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigValueFactory;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.Response;
import org.eclipse.jetty.http.HttpMethod;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.testcontainers.containers.GenericContainer;

import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class IntegrationTest {
    private static GenericContainer zookeeperContainer;
    private RemoteNodeClient storage = new RemoteNodeClient();
    private HttpClient client = new HttpClient();

    public IntegrationTest() throws Exception {
    }

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

    @Before
    public void startClient() throws Exception {
        client.start();
    }

    @After
    public void stopClient() throws Exception {
        storage.close();
    }

    @Test
    public void shouldStartInstanceAndSaveMetrics() throws Exception {
        Config config = ConfigFactory.load().withValue(ConfigurationKey.ZOOKEEPER_PORT,
                ConfigValueFactory.fromAnyRef(zookeeperContainer.getMappedPort(2181)));

        new Thread(() -> new Runner().start(config)).start();

        String host = "http://localhost:8080";

        waitTillStart(host);

        Metric metric = new Metric(20, "metric", 4);
        storage.put(metric, URI.create(host));
        List<Metric> metrics = storage.get("metric", 10, 21, URI.create(host));

        assertEquals(metric.getValue(), metrics.get(0).getValue(), .00001);
        client.newRequest(host).path("/shutdown").method(HttpMethod.POST).send();
    }

    private void waitTillStart(String host) throws Exception {
        Response response = null;
        for (int i = 0; i < 5; i++) {
            try {
                response = client.newRequest(host).path("/check").timeout(10, TimeUnit.SECONDS).method(HttpMethod.GET).send();
            } catch (ExecutionException e) {
                Thread.sleep(5000);
            }
        }
        if (response == null) {
            fail();
        }
        assertEquals(200, response.getStatus());
    }




}
