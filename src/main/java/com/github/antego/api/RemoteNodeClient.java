package com.github.antego.api;


import com.github.antego.Utils;
import com.github.antego.core.Metric;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.client.api.Response;
import org.eclipse.jetty.client.util.StringContentProvider;
import org.eclipse.jetty.http.HttpMethod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.sql.SQLException;
import java.util.List;

import static com.github.antego.Utils.dumpMetricToTsv;

public class RemoteNodeClient implements AutoCloseable {
    private final static Logger logger = LoggerFactory.getLogger(RemoteNodeClient.class);
    private final HttpClient httpClient = new HttpClient();

    public RemoteNodeClient() throws Exception {
        logger.info("Creating remote node client");
        httpClient.start();
    }

    public void put(Metric metric, URI uri) throws Exception {
        Response response = httpClient.POST(uri).content(new StringContentProvider(dumpMetricToTsv(metric)))
                .path("/metrics").send();
        if (response.getStatus() != 201) {
            throw new Exception("Failed to write metric");
        }
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
        logger.info("Stopping remote node client");
        httpClient.stop();
    }
}