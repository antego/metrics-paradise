package com.github.antego.cluster;

import java.nio.charset.StandardCharsets;

public class Node {
    private final String id;
    private final String host;
    private final int port;

    public Node(String id, String host, int port) {
        this.id = id;
        this.host = host;
        this.port = port;
    }

    public String getId() {
        return id;
    }

    public static Node fromIdAndData(String id, byte[] data) {
        if (data == null) {
            throw new NullPointerException("Data is null");
        }
        String[] hostPort = new String(data, StandardCharsets.UTF_8).split(":");
        String host = hostPort[0];
        int port = Integer.valueOf(hostPort[1]);
        return new Node(id, host, port);
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }
}
