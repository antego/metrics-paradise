package com.github.antego.api;

import com.github.antego.util.ConfigurationKey;
import com.github.antego.core.MetricRouter;
import com.typesafe.config.Config;
import org.eclipse.jetty.security.ConstraintMapping;
import org.eclipse.jetty.security.ConstraintSecurityHandler;
import org.eclipse.jetty.security.HashLoginService;
import org.eclipse.jetty.security.SecurityHandler;
import org.eclipse.jetty.security.UserStore;
import org.eclipse.jetty.security.authentication.BasicAuthenticator;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.util.security.Constraint;
import org.eclipse.jetty.util.security.Credential;
import org.glassfish.hk2.utilities.binding.AbstractBinder;
import org.glassfish.jersey.jetty.JettyHttpContainer;
import org.glassfish.jersey.jetty.JettyHttpContainerFactory;
import org.glassfish.jersey.server.ContainerFactory;
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
        ResourceConfig config = new ResourceConfig(MetricResource.class)
                .register(new StorageBinder())
                .register(MetricResource.GeneralExceptionMapper.class);
        server = JettyHttpContainerFactory.createServer(url.toURI(), config, false);
        if (this.config.getBoolean(ConfigurationKey.JETTY_SECURITY_ENABLED)) {
            SecurityHandler handler = basicAuth();
            handler.setHandler(ContainerFactory.createContainer(JettyHttpContainer.class, config));
            server.setHandler(handler);
        }
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

    private SecurityHandler basicAuth() {
        String user = config.getString(ConfigurationKey.JETTY_USER);
        String password = config.getString(ConfigurationKey.JETTY_PASSWORD);

        UserStore store = new UserStore();
        store.addUser(user, Credential.getCredential(password), new String[] {user});

        HashLoginService l = new HashLoginService();
        l.setName("realm");
        l.setUserStore(store);

        Constraint constraint = new Constraint();
        constraint.setName(Constraint.__BASIC_AUTH);
        constraint.setRoles(new String[]{user});
        constraint.setAuthenticate(true);

        ConstraintMapping cm = new ConstraintMapping();
        cm.setConstraint(constraint);
        cm.setPathSpec("/*");

        ConstraintSecurityHandler csh = new ConstraintSecurityHandler();
        csh.setAuthenticator(new BasicAuthenticator());
        csh.setRealmName("realm");
        csh.addConstraintMapping(cm);
        csh.setLoginService(l);

        return csh;
    }
}
