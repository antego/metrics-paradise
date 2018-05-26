package com.github.antego;

import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ZookeeperWatcher implements Watcher {
    private final static Logger logger = LoggerFactory.getLogger(ZookeeperWatcher.class);
    private Coordinator coordinator;


    public ZookeeperWatcher(Coordinator coordinator) {
        this.coordinator = coordinator;
    }

    @Override
    public void process(WatchedEvent event) {
        if (event.getType() == Event.EventType.NodeChildrenChanged) {
            logger.info(event.getState().toString());
            coordinator.notifyClusterStateChanged();
        }
    }
}
