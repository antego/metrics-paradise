package com.github.antego.db;

import java.sql.SQLException;
import java.util.List;

public interface Storage {
    List<Metric> get(String name, long timeStartInclusive, long timeEndExclusive) throws SQLException;
    double getMin(String name, long timeStartInclusive, long timeEndExclusive) throws SQLException;
    void put(Metric metric) throws SQLException;
}
