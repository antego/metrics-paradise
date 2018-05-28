package com.github.antego.core;

import com.github.antego.api.RemoteNodeClient;
import com.github.antego.cluster.ClusterState;
import com.github.antego.cluster.Coordinator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.List;
import java.util.Set;

/**
 * Thread-safe
 */
public class MetricRouter {
    private static final Logger logger = LoggerFactory.getLogger(MetricRouter.class);

    private final LocalStorage localStorage;
    private final Coordinator coordinator;
    private ClusterState state;
    private final RemoteNodeClient remoteNodeClient;
    private final Object lock = new Object();

    public MetricRouter(LocalStorage localStorage, Coordinator coordinator, RemoteNodeClient remoteNodeClient) {
        this.localStorage = localStorage;
        this.coordinator = coordinator;
        this.remoteNodeClient = remoteNodeClient;
    }

    public List<Metric> get(String name, long timeStartInclusive, long timeEndExclusive) throws Exception {
        URI targetUri;
        doRebalanceIfNeeded();
        synchronized (lock) {
            if (state.isMetricOwnedByMe(name.hashCode())) {
                return localStorage.get(name, timeStartInclusive, timeEndExclusive);
            }
            targetUri = state.getUriOfMetricNode(name);
        }
        return remoteNodeClient.get(name, timeStartInclusive, timeEndExclusive, targetUri);
    }

    public double getMin(String name, long timeStartInclusive, long timeEndExclusive) throws Exception {
        doRebalanceIfNeeded();
        synchronized (lock) {
            if (state.isMetricOwnedByMe(name.hashCode())) {
                return localStorage.getMin(name, timeStartInclusive, timeEndExclusive);
            }
        }
        return remoteNodeClient.getMin(name, timeStartInclusive, timeEndExclusive);
    }

    public void put(Metric metric) throws Exception {
        URI targetUri;
        doRebalanceIfNeeded();
        synchronized (lock) {
            if (state.isMetricOwnedByMe(metric.getName().hashCode())) {
                localStorage.put(metric);
                return;
            }
            targetUri = state.getUriOfMetricNode(metric.getName());
        }
        try {
            remoteNodeClient.put(metric, targetUri);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public void doRebalanceIfNeeded() throws Exception {
        synchronized (lock) {
            if (isClusterChanged()) {
                rebalance();
            } else {
                logger.debug("State not changed");
            }
        }
    }

    private boolean isClusterChanged() {
        ClusterState stateFromCoordinator = coordinator.getClusterState();
        if (stateFromCoordinator != state) {
            state = stateFromCoordinator;
            return true;
        }
        return false;
    }

    //todo rebalancer thread
    private void rebalance() throws Exception {
        logger.info("Rebalancing metrics");
        Set<String> metricNames = localStorage.getAllMetricNames();
        for (String name : metricNames) {
            if (!state.isMetricOwnedByMe(name.hashCode())) {
                URI targetUri = state.getUriOfMetricNode(name);
                logger.debug("Rebalancing metric with name [{}] to node [{}]", name, targetUri);
                List<Metric> metrics = localStorage.get(name, 0, Long.MAX_VALUE);
                for (Metric metric : metrics) {
                    remoteNodeClient.put(metric, targetUri);
                }
                localStorage.delete(name);
            }
        }
    }

    public void close() throws Exception {
        remoteNodeClient.close();
    }
}
