package com.github.antego.cluster;

import java.nio.charset.StandardCharsets;

public class Node {
    private String id;
    private String host;
    private int port;

    public Node(String id, String host, int port) {
        this.id = id;
        this.host = host;
        this.port = port;
    }

    public String getId() {
        return id;
    }

    public static Node fromIdAndData(String id, byte[] data) {
        String[] hostPort = new String(data, StandardCharsets.UTF_8).split(":");
        String host = hostPort[0];
        int port = Integer.valueOf(hostPort[1]);
        return new Node(id, host, port);
    }
}