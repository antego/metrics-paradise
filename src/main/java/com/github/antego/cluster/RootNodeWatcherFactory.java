package com.github.antego.cluster;

public class RootNodeWatcherFactory {
    public RootNodeWatcher createWatcher(Coordinator coordinator) {
        return new RootNodeWatcher(coordinator);
    }
}
