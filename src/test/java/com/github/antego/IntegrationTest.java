package com.github.antego;

import com.github.antego.core.AggregationType;
import com.github.antego.core.Metric;
import com.github.antego.api.RemoteNodeClient;
import com.github.antego.util.ConfigurationKey;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigValueFactory;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.AuthenticationStore;
import org.eclipse.jetty.client.api.Response;
import org.eclipse.jetty.client.util.BasicAuthentication;
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
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static com.github.antego.api.MetricResource.CHECK_PATH;
import static com.github.antego.api.MetricResource.SHUTDOWN_PATH;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class IntegrationTest {
    private static GenericContainer zookeeperContainer;
    private RemoteNodeClient storage;
    private HttpClient client = new HttpClient();

    @BeforeClass
    public static void createTestVerifyClient() throws IOException {
        zookeeperContainer = new GenericContainer("bitnami/zookeeper:3.4.12")
                .withExposedPorts(2181)
                .withEnv("ALLOW_ANONYMOUS_LOGIN", "yes");
        zookeeperContainer.start();
    }

    @AfterClass
    public static void disconnectFromZookeeper() throws InterruptedException {
        if (zookeeperContainer != null) {
            zookeeperContainer.stop();
        }
    }

    @Before
    public void startClient() throws Exception {
        Config config = ConfigFactory.load();
        String user = config.getString(ConfigurationKey.JETTY_USER);
        String password = config.getString(ConfigurationKey.JETTY_PASSWORD);
        AuthenticationStore store = client.getAuthenticationStore();
        store.addAuthentication(new BasicAuthentication(URI.create("http://localhost:8080/"), "realm",
                user, password));
        store.addAuthentication(new BasicAuthentication(URI.create("http://localhost:8081/"), "realm",
                user, password));
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

        String host = "http://localhost:8080/";
        Instance instance1 = new Instance(host, config);
        instance1.waitAvailable();

        Metric metric = new Metric(20, "metric", 4);
        Metric metric2 = new Metric(19, "metric", 6);
        host = "http://localhost:8080/";
        storage = new RemoteNodeClient(config);
        storage.put(metric, URI.create(host));
        storage.put(metric2, URI.create(host));
        List<Metric> metrics = storage.get("metric", 10, 21, URI.create(host));
        double min = storage.getAggregated("metric", 10, 21, AggregationType.MIN, URI.create(host));
        double max = storage.getAggregated("metric", 10, 21, AggregationType.MAX, URI.create(host));
        double mean = storage.getAggregated("metric", 10, 21, AggregationType.MEAN, URI.create(host));

        assertEquals(metric.getValue(), metrics.get(0).getValue(), .00001);
        assertEquals(4, min, .00001);
        assertEquals(6, max, .00001);
        assertEquals(5, mean, .00001);
        instance1.shutdown();
    }

    @Test
    public void shouldWriteAndReadAcrossInstances() throws Exception {
        Config config = ConfigFactory.load().withValue(ConfigurationKey.ZOOKEEPER_PORT,
                ConfigValueFactory.fromAnyRef(zookeeperContainer.getMappedPort(2181)));
        config = config.withValue(ConfigurationKey.JETTY_SECURITY_ENABLED, ConfigValueFactory.fromAnyRef(true));

        Config config1 = config.withValue(ConfigurationKey.JETTY_PORT, ConfigValueFactory.fromAnyRef(8080))
                .withValue(ConfigurationKey.ADVERTISE_PORT, ConfigValueFactory.fromAnyRef(8080));
        Config config2 = config.withValue(ConfigurationKey.JETTY_PORT, ConfigValueFactory.fromAnyRef(8081))
                .withValue(ConfigurationKey.ADVERTISE_PORT, ConfigValueFactory.fromAnyRef(8081));

        String host1 = "http://localhost:8080";
        String host2 = "http://localhost:8081";
        Instance instance1 = new Instance(host1, config1);
        Instance instance2 = new Instance(host2, config2);
        instance1.waitAvailable();
        instance2.waitAvailable();

        Metric metric1 = new Metric(20, "metric1", 4);
        Metric metric2 = new Metric(20, "metric2", 4);
        storage = new RemoteNodeClient(config);
        storage.put(metric1, URI.create(host1));
        storage.put(metric2, URI.create(host2));
        List<Metric> metrics = storage.get("metric1", 10, 21, URI.create(host2));
        assertEquals(metric1.getValue(), metrics.get(0).getValue(), .00001);
        metrics = storage.get("metric2", 10, 21, URI.create(host1));
        assertEquals(metric2.getValue(), metrics.get(0).getValue(), .00001);

        instance1.shutdown();
        instance2.shutdown();
    }

    @Test
    public void shouldSendMetricsOnShutdown() throws Exception {
        Config config = ConfigFactory.load().withValue(ConfigurationKey.ZOOKEEPER_PORT,
                ConfigValueFactory.fromAnyRef(zookeeperContainer.getMappedPort(2181)));

        Config config1 = config.withValue(ConfigurationKey.JETTY_PORT, ConfigValueFactory.fromAnyRef(8080))
                .withValue(ConfigurationKey.ADVERTISE_PORT, ConfigValueFactory.fromAnyRef(8080));
        Config config2 = config.withValue(ConfigurationKey.JETTY_PORT, ConfigValueFactory.fromAnyRef(8081))
                .withValue(ConfigurationKey.ADVERTISE_PORT, ConfigValueFactory.fromAnyRef(8081));

        String host1 = "http://localhost:8080";
        String host2 = "http://localhost:8081";
        Instance instance1 = new Instance(host1, config1);
        Instance instance2 = new Instance(host2, config2);
        instance1.waitAvailable();
        instance2.waitAvailable();

        Metric metric1 = new Metric(20, "metric1", 4);
        Metric metric2 = new Metric(20, "metric2", 4);
        Metric metric3 = new Metric(20, "metric3", 4);
        Metric metric4 = new Metric(20, "metric4", 4);
        Metric metric5 = new Metric(20, "metric5", 4);
        storage = new RemoteNodeClient(config);
        storage.put(metric1, URI.create(host1));
        storage.put(metric2, URI.create(host1));
        storage.put(metric3, URI.create(host1));
        storage.put(metric4, URI.create(host1));
        storage.put(metric5, URI.create(host1));
        instance1.shutdown();

        List<Metric> metrics = storage.get("metric1", 10, 21, URI.create(host2));
        metrics.addAll(storage.get("metric2", 10, 21, URI.create(host2)));
        metrics.addAll(storage.get("metric3", 10, 21, URI.create(host2)));
        metrics.addAll(storage.get("metric4", 10, 21, URI.create(host2)));
        metrics.addAll(storage.get("metric5", 10, 21, URI.create(host2)));

        assertEquals(5, metrics.size());

        instance2.shutdown();
    }

    private class Instance {
        private final String host;
        private final CountDownLatch endpointLatch = new CountDownLatch(1);

        public Instance(String host, Config config) {
            this.host = host;
            new Thread(() -> {
                new Runner().start(config);
                endpointLatch.countDown();
            }).start();
        }

        public void waitAvailable() throws Exception {
            Response response = null;
            for (int i = 0; i < 5; i++) {
                try {
                    response = client.newRequest(host).path(CHECK_PATH).timeout(10, TimeUnit.SECONDS).method(HttpMethod.GET).send();
                } catch (ExecutionException e) {
                    Thread.sleep(5000);
                }
            }
            if (response == null) {
                fail();
            }
            assertEquals(200, response.getStatus());
        }

        public void shutdown() throws InterruptedException, ExecutionException, TimeoutException {
            client.newRequest(host).path(SHUTDOWN_PATH).method(HttpMethod.GET).send();
            endpointLatch.await();
        }
    }
}
