package com.redhat.thermostat.thread.collector;

public enum HarvesterCommand {

    START,
    STOP,
    VM_CAPS,
    
    AGENT_ID,
    VM_ID;

    public static final String RECEIVER = "com.redhat.thermostat.thread.harvester.ThreadHarvester";

}
