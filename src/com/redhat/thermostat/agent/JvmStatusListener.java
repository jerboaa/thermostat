package com.redhat.thermostat.agent;

public interface JvmStatusListener {

    public void jvmStarted(int pid);

    public void jvmStopped(int pid);
}
