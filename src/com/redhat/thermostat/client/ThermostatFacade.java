package com.redhat.thermostat.client;

public interface ThermostatFacade {
    public abstract AgentRef[] getConnectedAgents();

    public abstract VmRef[] getConnectedVms();

    public abstract HostInformationFacade getHost(AgentRef ref);

}
