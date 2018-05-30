package com.github.antego.core;

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

    @Override
    public String toString() {
        return "Metric{" +
                "timestamp=" + timestamp +
                ", name='" + name + '\'' +
                ", value=" + value +
                '}';
    }
}
