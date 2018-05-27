package com.github.antego.cluster;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class ClusterState {
    private final int numberOfInstances;
    private final int selfIndex;
    private final List<Node> nodes;

    public ClusterState(List<Node> nodes, String selfId) {
        nodes.sort(Comparator.comparing(Node::getId));
        numberOfInstances = nodes.size();
        selfIndex = Collections.binarySearch(nodes, new Node(selfId, null, 0), Comparator.comparing(Node::getId));
        this.nodes = nodes;
    }

    public int getNumberOfInstances() {
        return numberOfInstances;
    }

    public int getSelfIndex() {
        return selfIndex;
    }

    public Node getNodeByMetric(int metricHash) {
        int index = metricHash % numberOfInstances;
        return nodes.get(index);
    }
}
