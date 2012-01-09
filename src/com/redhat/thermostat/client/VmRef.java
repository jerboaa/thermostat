package com.redhat.thermostat.client;

public class VmRef {

    private final AgentRef agentRef;
    private final String uid;
    private final String name;

    public VmRef(AgentRef agentRef, String id, String name) {
        this.agentRef = agentRef;
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
