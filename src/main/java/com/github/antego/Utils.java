package com.github.antego;

import com.github.antego.storage.Metric;
import com.typesafe.config.Config;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooKeeper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static com.github.antego.ConfigurationKey.ZOOKEEPER_CONNECT_TIMEOUT_SEC;

public class Utils {
    private static final Logger logger = LoggerFactory.getLogger(Utils.class);

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

    public static ZooKeeper createZookeeperClient(Config config) throws IOException {
        int port = config.getInt(ConfigurationKey.ZOOKEEPER_PORT);
        String host = config.getString(ConfigurationKey.ZOOKEEPER_HOST);
        CountDownLatch latch = new CountDownLatch(1);
        ZooKeeper zooKeeper = new ZooKeeper(host + ":" + port,
                config.getInt(ConfigurationKey.ZOOKEEPER_SESSION_TIMEOUT_MS), event -> {
            logger.info("New state {}", event.getState());
            if (event.getState() == Watcher.Event.KeeperState.SyncConnected) {
                latch.countDown();
            }
        });
        try {
            latch.await(config.getLong(ZOOKEEPER_CONNECT_TIMEOUT_SEC), TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return zooKeeper;
    }
}
