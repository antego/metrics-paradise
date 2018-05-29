package com.github.antego.util;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.codahale.metrics.graphite.Graphite;
import com.codahale.metrics.graphite.GraphiteReporter;
import com.codahale.metrics.jvm.GarbageCollectorMetricSet;
import com.codahale.metrics.jvm.MemoryUsageGaugeSet;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

import java.util.concurrent.TimeUnit;

public class Monitoring {
    private static final MetricRegistry registry = new MetricRegistry();
    private static final Config config = ConfigFactory.load();

    static {
        if (config.getBoolean(ConfigurationKey.MONITORING_ENABLED)) {
            registry.register("jvm.memory", new MemoryUsageGaugeSet());
            registry.register("jvm.gc", new GarbageCollectorMetricSet());
            Graphite graphite = new Graphite(config.getString(ConfigurationKey.MONITORING_GRAPHITE_HOST),
                    config.getInt(ConfigurationKey.MONITORING_GRAPHITE_PORT));
            GraphiteReporter reporter = GraphiteReporter.forRegistry(registry)
                    .convertRatesTo(TimeUnit.SECONDS)
                    .convertDurationsTo(TimeUnit.MILLISECONDS)
                    .prefixedWith(config.getString(ConfigurationKey.MONITORING_PREFIX))
                    .build(graphite);
            reporter.start(5, TimeUnit.SECONDS);
        }
    }

    public static void mark(MetricName name) {
        registry.meter(name.toString()).mark();
    }

    public static Timer.Context getTimerContext(MetricName name) {
        return registry.timer(name.toString()).time();
    }
}
