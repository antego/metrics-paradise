package com.github.antego.db;


import java.sql.SQLException;
import java.util.Collections;
import java.util.List;

public class RemoteStorage implements Storage {
    public void put(Metric metric) {

    }

    public List<Metric> get(String metric, long startTime, long endTime) {
        return Collections.emptyList();
    }

    @Override
    public double getMin(String name, long timeStartInclusive, long timeEndExclusive) throws SQLException {
        return 0;
    }
}
