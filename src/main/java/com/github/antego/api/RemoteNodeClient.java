package com.github.antego.api;


import com.codahale.metrics.Timer;
import com.github.antego.core.AggregationType;
import com.github.antego.util.ConfigurationKey;
import com.github.antego.util.MetricName;
import com.github.antego.util.Monitoring;
import com.github.antego.util.Utils;
import com.github.antego.core.Metric;
import com.typesafe.config.Config;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.AuthenticationStore;
import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.client.api.Response;
import org.eclipse.jetty.client.util.BasicAuthentication;
import org.eclipse.jetty.client.util.StringContentProvider;
import org.eclipse.jetty.http.HttpMethod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.List;

import static com.github.antego.api.MetricResource.AGGR;
import static com.github.antego.api.MetricResource.METRICNAME;
import static com.github.antego.api.MetricResource.METRICS_PATH;
import static com.github.antego.api.MetricResource.TIMESTAMPEND;
import static com.github.antego.api.MetricResource.TIMESTAMPSTART;
import static com.github.antego.util.Utils.dumpMetricToTsv;

public class RemoteNodeClient implements AutoCloseable {
    private static final Logger logger = LoggerFactory.getLogger(RemoteNodeClient.class);

    private final HttpClient httpClient = new HttpClient();
    private final AuthenticationStore store;
    private final boolean securityEnabled;
    private final String user;
    private final String password;

    public RemoteNodeClient(Config config) throws Exception {
        logger.info("Creating remote node client");
        securityEnabled = config.getBoolean(ConfigurationKey.JETTY_SECURITY_ENABLED);
        if (securityEnabled) {
            user = config.getString(ConfigurationKey.JETTY_USER);
            password = config.getString(ConfigurationKey.JETTY_PASSWORD);
            store = httpClient.getAuthenticationStore();
        } else {
            user = null;
            password = null;
            store = null;
        }
        httpClient.start();
    }

    public void put(Metric metric, URI uri) throws Exception {
        Monitoring.mark(MetricName.REMOTE_POST);
        try (Timer.Context context = Monitoring.getTimerContext(MetricName.REMOTE_POST_TIME)) {
            secureIfNeeded(uri);
            Response response = httpClient.POST(uri)
                    .content(new StringContentProvider(dumpMetricToTsv(metric)))
                    .path(METRICS_PATH).send();
            if (response.getStatus() != 201) {
                throw new Exception("Failed to write metric");
            }
        }
    }

    public List<Metric> get(String metric, long startTime, long endTime, URI uri) throws Exception {
        Monitoring.mark(MetricName.REMOTE_GET);
        try (Timer.Context context = Monitoring.getTimerContext(MetricName.REMOTE_GET_TIME)) {
            secureIfNeeded(uri);
            ContentResponse response = httpClient.newRequest(uri)
                    .method(HttpMethod.GET)
                    .param(TIMESTAMPSTART, String.valueOf(startTime))
                    .param(TIMESTAMPEND, String.valueOf(endTime))
                    .param(METRICNAME, metric)
                    .accept("text/plain")
                    .path(METRICS_PATH).send();
            String tsv = response.getContentAsString();
            return Utils.parseTsv(tsv);
        }
    }

    public double getAggregated(String name, long startTime, long endTime, AggregationType type, URI uri) throws Exception {
        Monitoring.mark(MetricName.REMOTE_GET_AGGR);
        try (Timer.Context context = Monitoring.getTimerContext(MetricName.REMOTE_GET_AGGR_TIME)) {
            secureIfNeeded(uri);
            ContentResponse response = httpClient.newRequest(uri)
                    .method(HttpMethod.GET)
                    .param(TIMESTAMPSTART, String.valueOf(startTime))
                    .param(TIMESTAMPEND, String.valueOf(endTime))
                    .param(METRICNAME, name)
                    .param(AGGR, type.toString())
                    .accept("text/plain")
                    .path(METRICS_PATH).send();
            String value = response.getContentAsString();
            return Double.valueOf(value);
        }
    }

    @Override
    public void close() throws Exception {
        logger.info("Stopping remote node client");
        httpClient.stop();
    }

    private void secureIfNeeded(URI uri) {
        if (securityEnabled) {
            store.addAuthentication(new BasicAuthentication(uri, "realm", user, password));
        }
    }

}
