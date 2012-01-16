package com.redhat.thermostat.client;

public class HostRef {

    private final String uid;
    private final String name;

    public HostRef(String id, String name) {
        this.uid = id;
        this.name = name;
    }

    @Override
    public String toString() {
        return name;
    }

    public String getAgentId() {
        return uid;
    }

    public String getName() {
        return name;
    }
}
