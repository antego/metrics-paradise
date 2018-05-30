package com.github.antego.api;

import com.codahale.metrics.Timer;
import com.github.antego.core.AggregationType;
import com.github.antego.core.Metric;
import com.github.antego.core.MetricRouter;
import com.github.antego.util.MetricName;
import com.github.antego.util.Monitoring;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.stream.Collectors;

import static com.github.antego.util.Utils.dumpMetricsToTsv;
import static com.github.antego.util.Utils.parseTsv;

@Path("/")
public class MetricResource {
    private static final Logger logger = LoggerFactory.getLogger(MetricResource.class);

    public static final String TIMESTAMPSTART = "time_start_sec";
    public static final String TIMESTAMPEND = "time_end_sec";
    public static final String METRICNAME = "metric_name";
    public static final String AGGR = "aggr_type";
    public static final String METRICS_PATH = "/metrics";
    public static final String SHUTDOWN_PATH = "/shutdown";
    public static final String CHECK_PATH = "/check";

    private final MetricRouter metricRouter;
    private final CountDownLatch shutdown;

    @Inject
    public MetricResource(MetricRouter metricRouter, CountDownLatch shutdown) {
        this.metricRouter = metricRouter;
        this.shutdown = shutdown;
    }

    @GET
    @Path(METRICS_PATH)
    @Produces(MediaType.TEXT_PLAIN)
    public String getMetricsInTsv(@QueryParam(TIMESTAMPSTART) long timestampStart,
                              @QueryParam(TIMESTAMPEND) long timestampEnd,
                              @QueryParam(METRICNAME) List<String> metricNames,
                              @QueryParam(AGGR) String aggrRaw) throws Exception {
        Monitoring.mark(MetricName.GET_REQUEST);
        try (Timer.Context context = Monitoring.getTimerContext(MetricName.GET_REQUEST_TIME)) {
            logger.debug("Received query for metrics {}, [{}], [{}], [{}]", metricNames, timestampStart, timestampEnd, aggrRaw);
            if (aggrRaw != null && !aggrRaw.isEmpty()) {
                List<Double> results = new ArrayList<>();
                for (String metricName : metricNames) {
                    results.add(metricRouter.getAggregated(metricName,
                            timestampStart, timestampEnd, AggregationType.valueOf(aggrRaw.toUpperCase())));
                }
                return results.stream().map(Object::toString).collect(Collectors.joining("\t", "", "\n"));
            }
            StringBuilder results = new StringBuilder();
            for (String metricName : metricNames) {
                results.append(dumpMetricsToTsv(metricRouter.get(metricName, timestampStart, timestampEnd)));
            }
            return results.toString();
        }
    }

    @POST
    @Path(METRICS_PATH)
    public Response saveMetricsFromTsv(String tsv) throws Exception {
        Monitoring.mark(MetricName.POST_REQUEST);
        try (Timer.Context context = Monitoring.getTimerContext(MetricName.POST_REQUEST_TIME)) {
            logger.debug("Received metric [{}]", tsv);
            List<Metric> metrics;
            try {
                metrics = parseTsv(tsv);
            } catch (Exception e) {
                logger.error("Failed to parse TSV", e);
                return Response
                        .status(Response.Status.BAD_REQUEST)
                        .entity(e.getMessage())
                        .type(MediaType.TEXT_PLAIN)
                        .build();
            }
            for (Metric metric : metrics) {
                metricRouter.put(metric);
            }
            return Response.status(Response.Status.CREATED).build();
        }
    }

    @GET
    @Path(SHUTDOWN_PATH)
    public Response shutdown() {
        logger.info("Received shutdown command");
        shutdown.countDown();
        return Response.status(200).build();
    }

    @GET
    @Path(CHECK_PATH)
    public Response checkAvailable() {
        return Response.status(200).build();
    }

    @Provider
    public static class GeneralExceptionMapper implements ExceptionMapper<Exception> {
        @Override
        public Response toResponse(Exception exception) {
            Monitoring.mark(MetricName.ERROR);
            logger.error("Exception while serving request", exception);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }
}
