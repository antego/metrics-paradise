package com.github.antego.cluster;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class ClusterState {
    private int numberOfInstances;
    private int selfOrdinal;

    public ClusterState(List<Node> nodes, String selfId) {
        nodes.sort(Comparator.comparing(Node::getId));
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
