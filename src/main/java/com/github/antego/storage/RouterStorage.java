package com.github.antego.storage;

import com.github.antego.cluster.Coordinator;

import java.net.URI;
import java.sql.SQLException;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

public class RouterStorage implements Storage {
    private LocalStorage localStorage;
    private Coordinator coordinator;
    private RemoteStorage remoteStorage;
    private int clusterVersion = 0;

    public RouterStorage(LocalStorage localStorage, Coordinator coordinator, RemoteStorage remoteStorage) {
        this.localStorage = localStorage;
        this.coordinator = coordinator;
        this.remoteStorage = remoteStorage;
    }

    @Override
    public List<Metric> get(String name, long timeStartInclusive, long timeEndExclusive) throws Exception {
        doRebalanceIfNeeded();
        if (coordinator.isMetricOwnedByNode(name.hashCode())) {
            return localStorage.get(name, timeStartInclusive, timeEndExclusive);
        }
        return remoteStorage.get(name, timeStartInclusive, timeEndExclusive);
    }

    @Override
    public double getMin(String name, long timeStartInclusive, long timeEndExclusive) throws Exception {
        doRebalanceIfNeeded();
        if (coordinator.isMetricOwnedByNode(name.hashCode())) {
            return localStorage.getMin(name, timeStartInclusive, timeEndExclusive);
        }
        return remoteStorage.getMin(name, timeStartInclusive, timeEndExclusive);
    }

    @Override
    public void put(Metric metric) throws Exception {
        doRebalanceIfNeeded();
        if (coordinator.isMetricOwnedByNode(metric.getName().hashCode())) {
            localStorage.put(metric);
            return;
        }
        URI targetUri = coordinator.getUriOfMetricNode(metric.getName());
        try {
            remoteStorage.put(metric, targetUri);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void doRebalanceIfNeeded() throws Exception {
        if (isClusterChanged()) {
            rebalance();
        }
    }

    private boolean isClusterChanged() {
        int newVersion = coordinator.getClusterStateVersion();
        boolean changed = clusterVersion != newVersion;
        clusterVersion = newVersion;
        return changed;
    }

    private void rebalance() throws Exception {
        Set<String> metricNames = localStorage.getAllMetricNames();
        for (String name : metricNames) {
            if (!coordinator.isMetricOwnedByNode(name.hashCode())) {
                URI targetUri = coordinator.getUriOfMetricNode(name);
                List<Metric> metrics = localStorage.get(name, 0, Long.MAX_VALUE);
                for (Metric metric : metrics) {
                    remoteStorage.put(metric, targetUri);
                }
            }
            localStorage.delete(name);
        }
    }
}
