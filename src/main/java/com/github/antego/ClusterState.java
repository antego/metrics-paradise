package com.github.antego;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class ClusterState {
    private int numberOfInstances;
    private int selfOrdinal;
    private List<Node> nodes;

    public ClusterState(List<Node> nodes, String selfId) {
        nodes.sort(Comparator.comparing(Node::getId));
        this.nodes = nodes;
        numberOfInstances = nodes.size();
        selfOrdinal = Collections.binarySearch(nodes, new Node(selfId, null, 0), Comparator.comparing(Node::getId));
    }

    public int getNumberOfInstances() {
        return numberOfInstances;
    }

    public int getSelfOrdinal() {
        return selfOrdinal;
    }
}
