package com.github.antego;

public class ClusterState {
    private int numberOfInstances;
    private int selfOrdinal;

    public ClusterState(int numberOfInstances, int selfOrdinal) {
        this.numberOfInstances = numberOfInstances;
        this.selfOrdinal = selfOrdinal;
    }

    public int getNumberOfInstances() {
        return numberOfInstances;
    }

    public int getSelfOrdinal() {
        return selfOrdinal;
    }
}
