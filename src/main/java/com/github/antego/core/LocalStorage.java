package com.github.antego.core;

import com.codahale.metrics.Timer;
import com.github.antego.util.ConfigurationKey;
import com.github.antego.util.MetricName;
import com.github.antego.util.Monitoring;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class LocalStorage {
    private static final Config config = ConfigFactory.load();

    private static final String METRIC_TABLE =
            "create table if not exists metric (timestamp bigint, name varchar, value double)";
    private static final String METRIC_PUT = "insert into metric values (?, ?, ?)";
    private static final String BASE_QUERY =
            "from metric where timestamp >= ? and timestamp < ? and name = ? limit "
            + config.getInt(ConfigurationKey.DB_RESULT_LIMIT);
    private static final String METRIC_GET = "select * " + BASE_QUERY;
    private static final String METRIC_GET_MIN = "select min(value) " + BASE_QUERY;
    private static final String METRIC_GET_MAX = "select max(value) " + BASE_QUERY;
    private static final String METRIC_GET_MEAN = "select avg(value) " + BASE_QUERY;
    private static final String GET_METRIC_NAMES = "select distinct name from metric";
    private static final String DELETE_METRICS = "delete from metric where name = ?";

    private final Connection connection;

    private final PreparedStatement putStmt;
    private final PreparedStatement getStmt;
    private final PreparedStatement getMinStmt;
    private final PreparedStatement getMaxStmt;
    private final PreparedStatement getMeanStmt;
    private final PreparedStatement getNamesStmt;
    private final PreparedStatement deleteStmt;

    public LocalStorage() throws SQLException {
        String url = config.getString(ConfigurationKey.DB_H2_URL);
        connection = DriverManager.getConnection(url);

        connection.createStatement().executeUpdate(METRIC_TABLE);

        putStmt = connection.prepareStatement(METRIC_PUT);
        getStmt = connection.prepareStatement(METRIC_GET);
        getMinStmt = connection.prepareStatement(METRIC_GET_MIN);
        getMaxStmt = connection.prepareStatement(METRIC_GET_MAX);
        getMeanStmt = connection.prepareStatement(METRIC_GET_MEAN);
        getNamesStmt = connection.prepareStatement(GET_METRIC_NAMES);
        deleteStmt = connection.prepareStatement(DELETE_METRICS);
    }

    //todo aggreagate queries
    public void put(Metric metric) throws SQLException {
        Monitoring.mark(MetricName.STORAGE_PUT);
        try (Timer.Context context = Monitoring.getTimerContext(MetricName.STORAGE_PUT_TIME)) {
            putStmt.setLong(1, metric.getTimestamp());
            putStmt.setString(2, metric.getName());
            putStmt.setDouble(3, metric.getValue());
            putStmt.execute();
        }
    }

    public List<Metric> get(String name, long timeStartInclusive, long timeEndExclusive) throws SQLException {
        Monitoring.mark(MetricName.STORAGE_GET);
        try (Timer.Context context = Monitoring.getTimerContext(MetricName.STORAGE_GET_TIME)) {
            populateBaseQuery(getStmt, timeStartInclusive, timeEndExclusive, name);
            ResultSet rs = getStmt.executeQuery();
            List<Metric> metrics = new ArrayList<>();
            while (rs.next()) {
                long timestamp = rs.getLong(1);
                String resultName = rs.getString(2);
                double value = rs.getDouble(3);
                metrics.add(new Metric(timestamp, resultName, value));
            }
            return metrics;
        }
    }

    public double getAggregated(String name, long timeStartInclusive, long timeEndExclusive, AggregationType type) throws SQLException {
        Monitoring.mark(MetricName.STORAGE_GET_AGGR);
        try (Timer.Context context = Monitoring.getTimerContext(MetricName.STORAGE_GET_AGGR_TIME)) {
            PreparedStatement stmt;
            if (type == AggregationType.MIN) {
                stmt = getMinStmt;
            } else if (type == AggregationType.MAX) {
                stmt = getMaxStmt;
            } else {
                stmt = getMeanStmt;
            }
            populateBaseQuery(stmt, timeStartInclusive, timeEndExclusive, name);
            ResultSet rs = stmt.executeQuery();
            rs.next();
            return rs.getDouble(1);
        }
    }

    private void populateBaseQuery(PreparedStatement stmt, long timeStartInclusive, long timeEndExclusive, String name) throws SQLException {
        stmt.setLong(1, timeStartInclusive);
        stmt.setLong(2, timeEndExclusive);
        stmt.setString(3, name);
    }

    public Set<String> getAllMetricNames() throws SQLException {
        ResultSet rs = getNamesStmt.executeQuery();
        Set<String> names = new HashSet<>();
        while (rs.next()) {
            names.add(rs.getString(1));
        }
        return names;
    }

    public void delete(String name) throws SQLException {
        deleteStmt.setString(1, name);
        deleteStmt.executeUpdate();
    }
}
