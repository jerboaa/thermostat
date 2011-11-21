package com.redhat.thermostat.agent;

public enum AgentState {
    /**
     * the agent in the default state. It has no connection. It is not doing
     * anything
     */
    DISCONNECTED,
    /** just connected. setting up data structures */
    CONNECTED,
    /** collecting data and doing stuff */
    ACTIVE,
    /**
     * server was disconnected abruptly. cache data until we can send it over
     * again
     */
    CACHING,

}
