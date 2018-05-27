package com.github.antego;

import com.github.antego.db.Metric;
import com.github.antego.db.Storage;
import org.junit.Test;

import java.sql.SQLException;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class StorageTest {
    private Storage storage = new Storage();

    public StorageTest() throws SQLException {
    }

    @Test
    public void shouldPutAndRetrieveMetric() throws SQLException {
        long timestamp = 10;
        String name = "metric1";
        double value = 0.5;
        storage.put(new Metric(timestamp, name, value));

        List<Metric> metric = storage.get(name, 6L, 13L);
        assertEquals(timestamp, metric.get(0).getTimestamp());
        assertEquals(name, metric.get(0).getName());
        assertEquals(value, metric.get(0).getValue(), 0.00001);
    }
}
