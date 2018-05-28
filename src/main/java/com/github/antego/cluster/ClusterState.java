package com.github.antego.cluster;

import java.net.URI;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

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

    public Node getNodeByMetric(int metricHash) {
        int index = metricHash % numberOfInstances;
        return nodes.get(index);
    }

    public boolean isMetricOwnedByMe(int metricHashCode) {
        if (numberOfInstances == 0) {
            return true;
        }
        return metricHashCode % numberOfInstances == selfIndex;
    }

    public URI getUriOfMetricNode(String name) {
        Node node = getNodeByMetric(name.hashCode());
        String host = node.getHost();
        int port = node.getPort();
        return URI.create("http://" + host + ":" + port);
    }
}
