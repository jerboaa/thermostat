package com.redhat.thermostat.client;

public interface MainWindowFacade {

    public abstract HostRef[] getHosts();

    public abstract VmRef[] getVms(HostRef ref);

}
