package com.github.antego.api;

import com.github.antego.db.Metric;
import com.github.antego.db.Storage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

@Path("/metrics")
public class MetricResource {
    private static final Logger logger = LoggerFactory.getLogger(MetricResource.class);
    private final Storage storage;

    @Inject
    public MetricResource(Storage storage) {
        this.storage = storage;
    }

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String getMetricsInTsv(@QueryParam("timestampstart") long timestampStart,
                              @QueryParam("timestampend") long timestampEnd,
                              @QueryParam("metricname") String metricName) throws SQLException {

        return dumpMetricsToTsv(storage.get(metricName, timestampStart, timestampEnd));
    }

    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    public Response saveMetricsFromTsv(String tsv) throws SQLException {
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
            storage.put(metric);
        }
        return Response.status(Response.Status.CREATED).build();
    }

    private List<Metric> parseTsv(String tsv) {
        List<Metric> metrics = new ArrayList<>();
        String[] lines = tsv.split("\n");
        for (String line : lines) {
            String[] fields = line.split("\t");
            long timestamp = Long.valueOf(fields[0]);
            String name = fields[1];
            double value = Double.valueOf(fields[2]);
            Metric metric = new Metric(timestamp, name, value);
            metrics.add(metric);
        }
        return metrics;
    }

    private String dumpMetricsToTsv(List<Metric> metrics) {
        StringBuilder builder = new StringBuilder();
        for (Metric metric : metrics) {
            builder.append(metric.getTimestamp()).append("\t")
                    .append(metric.getName()).append("\t")
                    .append(metric.getValue()).append("\n");
        }
        return builder.toString();
    }


}
