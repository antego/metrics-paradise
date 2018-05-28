package com.github.antego.core;

import com.github.antego.api.RemoteNodeClient;
import com.github.antego.cluster.Coordinator;
import org.junit.Test;

import java.net.URI;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class MetricRouterTest {
    private LocalStorage localStorage = mock(LocalStorage.class);
    private Coordinator coordinator = mock(Coordinator.class);
    private RemoteNodeClient remoteNodeClient = mock(RemoteNodeClient.class);
    private MetricRouter router = new MetricRouter(localStorage, coordinator, remoteNodeClient);
    private URI uri = URI.create("http://uri");


    @Test
    public void shouldPutIfMetricMatch() throws Exception {
        Metric metric = new Metric(10, "name", 0);
        when(coordinator.isMetricOwnedByNode(anyInt())).thenReturn(true);

        router.put(metric);

        verify(localStorage).put(metric);
    }

    @Test
    public void shouldProxyNotSelfMetric() throws Exception {
        Metric metric = new Metric(10, "name", 0);
        when(coordinator.isMetricOwnedByNode(anyInt())).thenReturn(false);

        router.put(metric);

        verify(remoteNodeClient).put(eq(metric), any());
    }

    @Test
    public void shouldGetMetricIfMatch() throws Exception {
        when(coordinator.isMetricOwnedByNode(anyInt())).thenReturn(true);

        router.get("metric", 10, 20);

        verify(localStorage).get("metric", 10, 20);
    }

    @Test
    public void shouldGetMetricIfNoMatch() throws Exception {
        when(coordinator.isMetricOwnedByNode(anyInt())).thenReturn(false);
        when(coordinator.getUriOfMetricNode(anyString())).thenReturn(uri);

        router.get("metric", 10, 20);

        verify(remoteNodeClient).get("metric", 10, 20, uri);
    }
}