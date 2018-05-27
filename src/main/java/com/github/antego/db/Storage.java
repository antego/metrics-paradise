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
    private static final String metricTable = "create table if not exists metric (timestamp bigint, name varchar, value double)";
    private static final String metricPut = "insert into metric values (?, ?, ?)";
    private static final String metricGet =
            "select * from metric where timestamp >= ? and timestamp < ? and name = ? limit "
                    + config.getInt(ConfigurationKey.DB_RESULT_LIMIT);
    private final Connection connection;
    private final PreparedStatement putStmt;
    private final PreparedStatement getStmt;


    public Storage() throws SQLException {
        String url = config.getString(ConfigurationKey.DB_H2_URL);
        connection = DriverManager.getConnection(url);
        connection.createStatement().executeUpdate(metricTable);
        putStmt = connection.prepareStatement(metricPut);
        getStmt = connection.prepareStatement(metricGet);
    }


    public void put(long timestamp, String name, double value) throws SQLException {
        putStmt.setLong(1, timestamp);
        putStmt.setString(2, name);
        putStmt.setDouble(3, value);
        putStmt.execute();
    }

    public List<Metric> get(String name, long timeStartInclusive, long timeEndExclusive) throws SQLException {
        getStmt.setLong(1, timeStartInclusive);
        getStmt.setLong(2, timeEndExclusive);
        getStmt.setString(3, name);
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
