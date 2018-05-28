package com.github.antego.cluster;

import com.github.antego.ConfigurationKey;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigValueFactory;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.ZooKeeper;
import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ClusterStateTest {
    @Test
    public void shouldReturnRightNode() {
        Node node1 = new Node("1", "h1", 1);
        Node node2 = new Node("2", "h2", 2);
        Node node3 = new Node("3", "h3", 3);

        ClusterState state = new ClusterState(Arrays.asList(node1, node2, node3), "1");

        assertEquals("h2", state.getNodeByMetric(4).getHost());
    }


    @Test
    public void shouldComputeIndex() {
        Node node1 = new Node("1", "h1", 1);
        Node node2 = new Node("2", "h2", 2);
        Node node3 = new Node("3", "h3", 3);
        Node node4 = new Node("4", "h4", 3);
        Node node5 = new Node("5", "h5", 3);
        ClusterState state = new ClusterState(Arrays.asList(node1, node2, node3), "3");

        assertTrue(state.isMetricOwnedByNode(2)); // 2 mod 3 = 2
        assertTrue(state.isMetricOwnedByNode(20)); // 20 mod 3 = 2

        state = new ClusterState(Arrays.asList(node2, node3, node4, node5), "3");
        assertFalse(state.isMetricOwnedByNode(18)); // 18 mod 4 = 2
        assertTrue(state.isMetricOwnedByNode(29)); // 29 mod 4 = 1
    }
}