package com.github.antego.core;

import com.github.antego.api.RemoteNodeClient;
import com.github.antego.cluster.Coordinator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.List;
import java.util.Set;

public class MetricRouter {
    private static final Logger logger = LoggerFactory.getLogger(MetricRouter.class);

    private final LocalStorage localStorage;
    private final Coordinator coordinator;
    private final RemoteNodeClient remoteNodeClient;
    private int clusterVersion = 0;

    public MetricRouter(LocalStorage localStorage, Coordinator coordinator, RemoteNodeClient remoteNodeClient) {
        this.localStorage = localStorage;
        this.coordinator = coordinator;
        this.remoteNodeClient = remoteNodeClient;
    }

    public List<Metric> get(String name, long timeStartInclusive, long timeEndExclusive) throws Exception {
        doRebalanceIfNeeded();
        if (coordinator.isMetricOwnedByNode(name.hashCode())) {
            return localStorage.get(name, timeStartInclusive, timeEndExclusive);
        }
        URI targetUri = coordinator.getUriOfMetricNode(name);
        return remoteNodeClient.get(name, timeStartInclusive, timeEndExclusive, targetUri);
    }

    public double getMin(String name, long timeStartInclusive, long timeEndExclusive) throws Exception {
        doRebalanceIfNeeded();
        if (coordinator.isMetricOwnedByNode(name.hashCode())) {
            return localStorage.getMin(name, timeStartInclusive, timeEndExclusive);
        }
        return remoteNodeClient.getMin(name, timeStartInclusive, timeEndExclusive);
    }

    public void put(Metric metric) throws Exception {
        doRebalanceIfNeeded();
        if (coordinator.isMetricOwnedByNode(metric.getName().hashCode())) {
            localStorage.put(metric);
            return;
        }
        URI targetUri = coordinator.getUriOfMetricNode(metric.getName());
        try {
            remoteNodeClient.put(metric, targetUri);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public void doRebalanceIfNeeded() throws Exception {
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
        logger.info("Rebalancing metrics");
        Set<String> metricNames = localStorage.getAllMetricNames();
        for (String name : metricNames) {
            if (!coordinator.isMetricOwnedByNode(name.hashCode())) {
                URI targetUri = coordinator.getUriOfMetricNode(name);
                List<Metric> metrics = localStorage.get(name, 0, Long.MAX_VALUE);
                for (Metric metric : metrics) {
                    remoteNodeClient.put(metric, targetUri);
                }
            }
            localStorage.delete(name);
        }
    }

    public void close() throws Exception {
        remoteNodeClient.close();
    }
}
