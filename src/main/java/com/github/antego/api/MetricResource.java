package com.github.antego.api;

import com.github.antego.core.Metric;
import com.github.antego.core.MetricRouter;
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
import java.util.List;
import java.util.concurrent.CountDownLatch;

import static com.github.antego.Utils.dumpMetricsToTsv;
import static com.github.antego.Utils.parseTsv;

@Path("/")
public class MetricResource {
    private static final Logger logger = LoggerFactory.getLogger(MetricResource.class);
    private final MetricRouter metricRouter;
    private final CountDownLatch shutdown;

    @Inject
    public MetricResource(MetricRouter metricRouter, CountDownLatch shutdown) {
        this.metricRouter = metricRouter;
        this.shutdown = shutdown;
    }

    @GET
    @Path("/metrics")
    @Produces(MediaType.TEXT_PLAIN)
    public String getMetricsInTsv(@QueryParam("timestampstart") long timestampStart,
                              @QueryParam("timestampend") long timestampEnd,
                              @QueryParam("metricname") String metricName) throws Exception {
        logger.debug("Received query for metrics [{}], [{}], [{}]", metricName, timestampStart, timestampEnd);
        return dumpMetricsToTsv(metricRouter.get(metricName, timestampStart, timestampEnd));
    }

    @POST
    @Path("/metrics")
    public Response saveMetricsFromTsv(String tsv) throws Exception {
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

    @GET
    @Path("/shutdown")
    public Response shutdown() {
        logger.info("Received shutdown command");
        shutdown.countDown();
        return Response.status(200).build();
    }

    @GET
    @Path("/check")
    public Response checkAvailable() {
        return Response.status(200).build();
    }
}
