package com.github.antego.api;

import com.github.antego.api.MetricResource;
import com.github.antego.storage.Metric;
import com.github.antego.storage.RouterStorage;
import org.glassfish.hk2.utilities.binding.AbstractBinder;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.Response;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.concurrent.CountDownLatch;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class MetricResourceTest extends JerseyTest {
    private RouterStorage routerStorage = mock(RouterStorage.class);

    @Override
    protected Application configure() {
        return new ResourceConfig(MetricResource.class).register(new StorageBinder());
    }

    @Test
    public void shouldWriteMetricsFromTsv() throws Exception {
        int status = target("metrics").request().post(Entity.text("123123\tmetric\t0.5")).getStatus();
        assertEquals(201, status);
        ArgumentCaptor<Metric> captor = ArgumentCaptor.forClass(Metric.class);
        verify(routerStorage).put(captor.capture());

        assertEquals(123123, captor.getValue().getTimestamp());
        assertEquals("metric", captor.getValue().getName());
        assertEquals(0.5, captor.getValue().getValue(), .000001);
    }

    @Test
    public void shouldRespondWithTsv() throws Exception {
        Metric metric1 = new Metric(1000, "metric1", 4);
        Metric metric2 = new Metric(1001, "metric2", 2);
        when(routerStorage.get(any(), anyLong(), anyLong())).thenReturn(Arrays.asList(metric1, metric2));

        Response response = target("metrics")
                .queryParam("timestampstart", 1000)
                .queryParam("timestampend", 1002)
                .queryParam("metricname")
                .request()
                .get();
        assertEquals(200, response.getStatus());

        assertEquals("1000\tmetric1\t4.0\n1001\tmetric2\t2.0\n", response.readEntity(String.class));
    }


    public class StorageBinder extends AbstractBinder {
        @Override
        public void configure() {
            bind(routerStorage).to(RouterStorage.class);
            bind(new CountDownLatch(0)).to(CountDownLatch.class);
        }
    }
}
