package com.github.antego.db;

public class Metric {
    private final long timestamp;
    private final String name;
    private final double value;

    public Metric(long timestamp, String name, double value) {
        this.timestamp = timestamp;
        this.name = name;
        this.value = value;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public String getName() {
        return name;
    }

    public double getValue() {
        return value;
    }
}
