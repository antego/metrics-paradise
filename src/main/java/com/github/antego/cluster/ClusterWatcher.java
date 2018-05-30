package com.github.antego.cluster;

import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class watches for a new or deleted nodes. Watcher is registered on a root zookeeper node.
 */
public class ClusterWatcher implements Watcher {
    private final static Logger logger = LoggerFactory.getLogger(ClusterWatcher.class);
    private Coordinator coordinator;

    public ClusterWatcher(Coordinator coordinator) {
        this.coordinator = coordinator;
    }

    @Override
    public void process(WatchedEvent event) {
        logger.debug("Received new event [{}]", event.getType());
        if (event.getType() == Event.EventType.NodeChildrenChanged) {
            try {
                coordinator.refreshClusterState();
            } catch (KeeperException e) {
                logger.error("Error", e);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
}
