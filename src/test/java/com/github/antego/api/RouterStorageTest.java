package com.github.antego.api;

import com.github.antego.cluster.Coordinator;
import com.github.antego.db.RouterStorage;
import com.github.antego.db.LocalStorage;
import com.github.antego.db.Metric;
import com.github.antego.db.RemoteStorage;
import org.junit.Test;

import java.sql.SQLException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class RouterStorageTest {
    private LocalStorage localStorage = mock(LocalStorage.class);
    private Coordinator coordinator = mock(Coordinator.class);
    private RemoteStorage remoteStorage = mock(RemoteStorage.class);
    private RouterStorage dispatcher = new RouterStorage(localStorage, coordinator, remoteStorage);


    @Test
    public void shouldPutIfMetricMatch() throws SQLException {
        Metric metric = new Metric(10, "name", 0);
        when(coordinator.isMetricOwnedByNode(anyInt())).thenReturn(true);

        dispatcher.put(metric);

        verify(localStorage).put(metric);
    }

    @Test
    public void shouldProxyNotSelfMetric() throws SQLException {
        Metric metric = new Metric(10, "name", 0);
        when(coordinator.isMetricOwnedByNode(anyInt())).thenReturn(false);

        dispatcher.put(metric);

        verify(remoteStorage).put(metric);
    }

    @Test
    public void shouldGetMetricIfMatch() throws SQLException {
        when(coordinator.isMetricOwnedByNode(anyInt())).thenReturn(true);

        dispatcher.get("metric", 10, 20);

        verify(localStorage).get("metric", 10, 20);
    }

    @Test
    public void shouldGetMetricIfNoMatch() throws SQLException {
        when(coordinator.isMetricOwnedByNode(anyInt())).thenReturn(false);

        dispatcher.get("metric", 10, 20);

        verify(remoteStorage).get("metric", 10, 20);
    }
}