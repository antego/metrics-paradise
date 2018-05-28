package com.github.antego;

import com.github.antego.api.Endpoint;
import com.github.antego.cluster.ClusterWatcherFactory;
import com.github.antego.cluster.Coordinator;
import com.github.antego.storage.LocalStorage;
import com.github.antego.storage.RemoteStorage;
import com.github.antego.storage.RouterStorage;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.apache.zookeeper.ZooKeeper;

import java.util.UUID;
import java.util.concurrent.CountDownLatch;

public class Runner {
    public static final Config config = ConfigFactory.load();

    public static void main(String[] args) throws Exception {
        ZooKeeper zooKeeper = Utils.createZookeeperClient(config);
        Coordinator coordinator = new Coordinator(zooKeeper, config, new ClusterWatcherFactory());
        coordinator.init();

        RouterStorage routerStorage = new RouterStorage(new LocalStorage(), coordinator, new RemoteStorage());

        routerStorage.doRebalanceIfNeeded();

        CountDownLatch shutdown = new CountDownLatch(1);
        Endpoint endpoint = new Endpoint(routerStorage, shutdown);
        endpoint.start();

        coordinator.advertiseSelf(UUID.randomUUID().toString());

        shutdown.await();

        endpoint.stop();

        coordinator.removeSelf();

        routerStorage.doRebalanceIfNeeded();
        routerStorage.close();
        coordinator.close();
    }
}
