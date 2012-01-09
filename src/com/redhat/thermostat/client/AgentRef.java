package com.redhat.thermostat.client;

public class AgentRef {

    private final String uid;
    private final String name;

    public AgentRef(String id, String name) {
        this.uid = id;
        this.name = name;
    }

    @Override
    public String toString() {
        return name;
    }

    public String getId() {
        return uid;
    }

    public String getName() {
        return name;
    }
}
