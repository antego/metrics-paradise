package com.github.antego.cluster;

public class ClusterWatcherFactory {
    public ClusterWatcher createWatcher(Coordinator coordinator) {
        return new ClusterWatcher(coordinator);
    }
}
