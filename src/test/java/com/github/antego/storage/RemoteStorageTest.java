package com.github.antego.storage;

import org.junit.Rule;
import org.junit.Test;
import org.mockserver.client.server.MockServerClient;
import org.mockserver.junit.MockServerRule;
import org.mockserver.model.HttpRequest;
import org.mockserver.verify.VerificationTimes;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import static org.junit.Assert.assertEquals;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

public class RemoteStorageTest {
    private RemoteStorage storage = new RemoteStorage();
    @Rule
    public MockServerRule mockServerRule = new MockServerRule(this);
    private MockServerClient mockServerClient;

    public RemoteStorageTest() throws Exception {
    }

    @Test
    public void shouldSendPut() throws Exception {
        HttpRequest request = request().withMethod("POST")
                .withPath("/metrics")
                .withBody("10\tmetric\t2.0\n", StandardCharsets.UTF_8);

        mockServerClient.when(request).respond(response().withStatusCode(201));

        Metric metric = new Metric(10, "metric", 2);
        storage.put(metric, URI.create("http://localhost:" + mockServerRule.getPort()));

        mockServerClient.verify(request, VerificationTimes.once());
    }

    @Test
    public void shouldSendGet() throws Exception {
        HttpRequest request = request().withMethod("GET")
                .withPath("/metrics");

        mockServerClient.when(request).respond(response().withBody("10\tmetric\t456456\n"));

        List<Metric> metrics = storage.get("metric", 0, 12,
                URI.create("http://localhost:" + mockServerRule.getPort()));

        assertEquals(456456, metrics.get(0).getValue(), .000001);
    }

}