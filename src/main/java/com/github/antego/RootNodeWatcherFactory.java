package com.github.antego;

public class RootNodeWatcherFactory {
    public RootNodeWatcher createWatcher(Coordinator coordinator) {
        return new RootNodeWatcher(coordinator);
    }
}
