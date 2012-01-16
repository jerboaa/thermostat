package com.redhat.thermostat.client;

public class VmRef {

    private final HostRef hostRef;
    private final String uid;
    private final String name;

    public VmRef(HostRef hostRef, String id, String name) {
        this.hostRef = hostRef;
        this.uid = id;
        this.name = name;
    }

    @Override
    public String toString() {
        return name;
    }

    public HostRef getAgent() {
        return hostRef;
    }

    public String getId() {
        return uid;
    }

    public String getName() {
        return name;
    }
}
