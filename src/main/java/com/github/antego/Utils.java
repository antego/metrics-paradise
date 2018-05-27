package com.github.antego;

import com.github.antego.storage.Metric;

import java.util.ArrayList;
import java.util.List;

public class Utils {
    public static List<Metric> parseTsv(String tsv) {
        List<Metric> metrics = new ArrayList<>();
        String[] lines = tsv.split("\n");
        for (String line : lines) {
            String[] fields = line.split("\t");
            long timestamp = Long.valueOf(fields[0]);
            String name = fields[1];
            double value = Double.valueOf(fields[2]);
            Metric metric = new Metric(timestamp, name, value);
            metrics.add(metric);
        }
        return metrics;
    }

    public static String dumpMetricsToTsv(List<Metric> metrics) {
        StringBuilder builder = new StringBuilder();
        for (Metric metric : metrics) {
            builder.append(dumpMetricToTsv(metric));
        }
        return builder.toString();
    }

    public static String dumpMetricToTsv(Metric metric) {
        StringBuilder builder = new StringBuilder();
        builder.append(metric.getTimestamp()).append("\t")
                .append(metric.getName()).append("\t")
                .append(metric.getValue()).append("\n");
        return builder.toString();
    }
}
