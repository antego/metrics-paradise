package com.github.antego.db;

import com.github.antego.ConfigurationKey;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class Storage {
    private static final Config config = ConfigFactory.load();
    private static final String metricTable =
            "create table if not exists metric (timestamp bigint, name varchar, value double)";
    private static final String metricPut = "insert into metric values (?, ?, ?)";

    private static final String BASE_QUERY =
            "from metric where timestamp >= ? and timestamp < ? and name = ? limit "
            + config.getInt(ConfigurationKey.DB_RESULT_LIMIT);
    private static final String metricGet =
            "select * " + BASE_QUERY;
    private static final String metricGetMin =
            "select min(value) " + BASE_QUERY;
    private final Connection connection;
    private final PreparedStatement putStmt;
    private final PreparedStatement getStmt;
    private final PreparedStatement getMinStmt;


    public Storage() throws SQLException {
        String url = config.getString(ConfigurationKey.DB_H2_URL);
        connection = DriverManager.getConnection(url);
        connection.createStatement().executeUpdate(metricTable);
        putStmt = connection.prepareStatement(metricPut);
        getStmt = connection.prepareStatement(metricGet);
        getMinStmt = connection.prepareStatement(metricGetMin);
    }


    //todo synchronized?
    //todo index
    //todo aggreagate queries
    public void put(Metric metric) throws SQLException {
        putStmt.setLong(1, metric.getTimestamp());
        putStmt.setString(2, metric.getName());
        putStmt.setDouble(3, metric.getValue());
        putStmt.execute();
    }

    public List<Metric> get(String name, long timeStartInclusive, long timeEndExclusive) throws SQLException {
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

    public double getMin(String name, long timeStartInclusive, long timeEndExclusive) throws SQLException {
        populateBaseQuery(getMinStmt, timeStartInclusive, timeEndExclusive, name);
        ResultSet rs = getMinStmt.executeQuery();
        rs.next();
        return rs.getDouble(1);
    }

    private void populateBaseQuery(PreparedStatement stmt, long timeStartInclusive, long timeEndExclusive, String name) throws SQLException {
        stmt.setLong(1, timeStartInclusive);
        stmt.setLong(2, timeEndExclusive);
        stmt.setString(3, name);
    }
}
