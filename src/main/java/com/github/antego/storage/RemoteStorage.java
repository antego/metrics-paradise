package com.github.antego.storage;


import com.github.antego.Utils;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.client.util.StringContentProvider;
import org.eclipse.jetty.http.HttpMethod;

import java.net.URI;
import java.sql.SQLException;
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
        httpClient.POST(uri).content(new StringContentProvider(dumpMetricToTsv(metric)), "")
                .path("/metrics").send();
    }

    public List<Metric> get(String metric, long startTime, long endTime, URI uri) throws Exception {
        ContentResponse response = httpClient.newRequest(uri)
                .method(HttpMethod.GET)
                .param("timestampstart", String.valueOf(startTime))
                .param("timestampend", String.valueOf(endTime))
                .param("metricname", metric)
                .path("/metrics").send();
        String tsv = response.getContentAsString();
        return Utils.parseTsv(tsv);
    }

    public double getMin(String name, long timeStartInclusive, long timeEndExclusive) throws SQLException {
        return 0;
    }

    @Override
    public void close() throws Exception {
        httpClient.stop();
    }
}
