package com.github.antego;

public class ZookeeperWatcherFactory {
    public ZookeeperWatcher createWatcher(Coordinator coordinator) {
        return new ZookeeperWatcher(coordinator);
    }
}
