package com.github.antego;

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
}
