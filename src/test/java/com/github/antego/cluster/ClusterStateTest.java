package com.github.antego.cluster;

import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.*;

public class ClusterStateTest {
    @Test
    public void shouldReturnRightNode() {
        Node node1 = new Node("1", "h1", 1);
        Node node2 = new Node("2", "h2", 2);
        Node node3 = new Node("3", "h3", 3);

        ClusterState state = new ClusterState(Arrays.asList(node1, node2, node3), "1");

        assertEquals("h2", state.getNodeByMetric(4).getHost());
    }

}