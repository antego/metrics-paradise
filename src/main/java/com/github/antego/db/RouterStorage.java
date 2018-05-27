package com.github.antego.db;

import com.github.antego.cluster.Coordinator;

import java.sql.SQLException;
import java.util.List;
import java.util.Set;

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
    public List<Metric> get(String name, long timeStartInclusive, long timeEndExclusive) throws SQLException {
        doRebalanceIfNeeded();
        if (coordinator.isMetricOwnedByNode(name.hashCode())) {
            return localStorage.get(name, timeStartInclusive, timeEndExclusive);
        }
        return remoteStorage.get(name, timeStartInclusive, timeEndExclusive);
    }

    @Override
    public double getMin(String name, long timeStartInclusive, long timeEndExclusive) throws SQLException {
        doRebalanceIfNeeded();
        if (coordinator.isMetricOwnedByNode(name.hashCode())) {
            return localStorage.getMin(name, timeStartInclusive, timeEndExclusive);
        }
        return remoteStorage.getMin(name, timeStartInclusive, timeEndExclusive);
    }

    @Override
    public void put(Metric metric) throws SQLException {
        doRebalanceIfNeeded();
        if (coordinator.isMetricOwnedByNode(metric.getName().hashCode())) {
            localStorage.put(metric);
            return;
        }
        remoteStorage.put(metric);
    }

    private void doRebalanceIfNeeded() throws SQLException {
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

    private void rebalance() throws SQLException {
        Set<String> metricNames = localStorage.getAllMetricNames();
        for (String name : metricNames) {
            if (!coordinator.isMetricOwnedByNode(name.hashCode())) {
                List<Metric> metrics = localStorage.get(name, 0, Long.MAX_VALUE);
                metrics.forEach(remoteStorage::put);
            }
            localStorage.delete(name);
        }
    }
}
