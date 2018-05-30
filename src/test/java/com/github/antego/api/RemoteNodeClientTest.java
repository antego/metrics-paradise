package com.github.antego.api;

import com.github.antego.core.Metric;
import com.typesafe.config.Config;
import org.junit.Rule;
import org.junit.Test;
import org.mockserver.client.server.MockServerClient;
import org.mockserver.junit.MockServerRule;
import org.mockserver.model.HttpRequest;
import org.mockserver.verify.VerificationTimes;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static com.github.antego.api.MetricResource.METRICS_PATH;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

public class RemoteNodeClientTest {
    private RemoteNodeClient storage = new RemoteNodeClient(mock(Config.class));

    @Rule
    public MockServerRule mockServerRule = new MockServerRule(this);
    private MockServerClient mockServerClient;

    public RemoteNodeClientTest() throws Exception {
    }

    @Test
    public void shouldSendPut() throws Exception {
        HttpRequest request = request().withMethod("POST")
                .withPath(METRICS_PATH)
                .withBody("10\tmetric\t2.0\n", StandardCharsets.UTF_8);

        mockServerClient.when(request).respond(response().withStatusCode(201));

        Metric metric = new Metric(10, "metric", 2);
        storage.put(metric, URI.create("http://localhost:" + mockServerRule.getPort()));

        mockServerClient.verify(request, VerificationTimes.once());
    }

    @Test
    public void shouldSendGet() throws Exception {
        HttpRequest request = request().withMethod("GET")
                .withPath(METRICS_PATH);

        mockServerClient.when(request).respond(response().withBody("10\tmetric\t456456\n"));

        List<Metric> metrics = storage.get("metric", 0, 12,
                URI.create("http://localhost:" + mockServerRule.getPort()));

        assertEquals(456456, metrics.get(0).getValue(), .000001);
    }

}