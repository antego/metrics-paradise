package com.github.antego.storage;

import java.sql.SQLException;
import java.util.List;

public interface Storage {
    List<Metric> get(String name, long timeStartInclusive, long timeEndExclusive) throws Exception;
    double getMin(String name, long timeStartInclusive, long timeEndExclusive) throws Exception;
    void put(Metric metric) throws Exception;
}
