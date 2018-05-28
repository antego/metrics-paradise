package com.github.antego.api;

import com.github.antego.ConfigurationKey;
import com.github.antego.core.MetricRouter;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.eclipse.jetty.server.Server;
import org.glassfish.hk2.api.Factory;
import org.glassfish.hk2.utilities.binding.AbstractBinder;
import org.glassfish.jersey.jetty.JettyHttpContainerFactory;
import org.glassfish.jersey.server.ResourceConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.util.concurrent.CountDownLatch;

public class Endpoint {
    private static final Logger logger = LoggerFactory.getLogger(Endpoint.class);
    private final Config config;
    private final MetricRouter metricRouter;
    private CountDownLatch shutdown;
    private Server server;

    //todo secure
    public Endpoint(MetricRouter metricRouter, CountDownLatch shutdown, Config config) {
        this.metricRouter = metricRouter;
        this.shutdown = shutdown;
        this.config = config;
    }

    public void start() throws Exception {
        logger.info("Starting API endpoint");
        URL url = new URL("http", config.getString(ConfigurationKey.JETTY_HOST),
                config.getInt(ConfigurationKey.JETTY_PORT), "");
        logger.info("Binding API to [{}]", url);
        ResourceConfig config = new ResourceConfig(MetricResource.class).register(new StorageBinder());
        server = JettyHttpContainerFactory.createServer(url.toURI(), config);
        server.start();
    }

    public void stop() throws Exception {
        logger.info("Stopping API endpoint");
        server.stop();
        server.destroy();
    }

    public class StorageBinder extends AbstractBinder {
        @Override
        public void configure() {
            bind(metricRouter).to(MetricRouter.class);
            bind(shutdown).to(CountDownLatch.class);
        }
    }
}
