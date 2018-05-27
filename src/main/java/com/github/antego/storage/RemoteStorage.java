package com.github.antego.storage;


import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.ContentProvider;
import org.eclipse.jetty.client.util.StringContentProvider;

import java.net.URI;
import java.sql.SQLException;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import static com.github.antego.Utils.dumpMetricToTsv;

public class RemoteStorage implements AutoCloseable {
    HttpClient httpClient = new HttpClient();

    public RemoteStorage() throws Exception {
        httpClient.start();
    }

    public void put(Metric metric, URI uri) throws InterruptedException, ExecutionException, TimeoutException {
        httpClient.POST(uri).content(new StringContentProvider(dumpMetricToTsv(metric)), "").path("/metrics").send();
    }

    public List<Metric> get(String metric, long startTime, long endTime) {
        return Collections.emptyList();
    }

    public double getMin(String name, long timeStartInclusive, long timeEndExclusive) throws SQLException {
        return 0;
    }

    @Override
    public void close() throws Exception {
        httpClient.stop();
    }
}
