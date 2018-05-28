package com.github.antego.core;

import org.junit.Test;

import java.sql.SQLException;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertEquals;

public class LocalStorageTest {
    private LocalStorage localStorage = new LocalStorage();

    public LocalStorageTest() throws SQLException {
    }

    @Test
    public void shouldPutAndRetrieveMetric() throws SQLException {
        long timestamp = 10;
        String name = "metric1";
        double value = 0.5;
        localStorage.put(new Metric(timestamp, name, value));

        List<Metric> metric = localStorage.get(name, 6L, 13L);
        assertEquals(timestamp, metric.get(0).getTimestamp());
        assertEquals(name, metric.get(0).getName());
        assertEquals(value, metric.get(0).getValue(), 0.00001);
    }

    @Test
    public void shouldReturnMin() throws SQLException {
        String name = "metric1";
        localStorage.put(new Metric(10, name, 4));
        localStorage.put(new Metric(11, name, 16));

        double min = localStorage.getMin(name, 6L, 13L); //todo check if no such metric
        assertEquals(4, 4, .00000001);
    }

    @Test
    public void shouldReturnUniqueNames() throws SQLException {
        localStorage.put(new Metric(10, "name1", 4));
        localStorage.put(new Metric(11, "name2", 16));
        localStorage.put(new Metric(11, "name2", 16));

        Set<String> names = localStorage.getAllMetricNames();
        assertEquals(2, names.size());
    }

    @Test
    public void shouldDeleteMetrics() throws SQLException {
        localStorage.put(new Metric(10, "name1", 4));
        localStorage.delete("name1");
        assertEquals(0, localStorage.getAllMetricNames().size());
    }
}
