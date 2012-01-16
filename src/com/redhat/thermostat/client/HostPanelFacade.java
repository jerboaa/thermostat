package com.redhat.thermostat.client;

import com.redhat.thermostat.common.HostInfo;
import com.redhat.thermostat.common.NetworkInfo;

public interface HostPanelFacade {
    public abstract HostInfo getHostInfo();

    public abstract NetworkInfo getNetworkInfo();

    public abstract DiscreteTimeData<Double>[] getCpuLoad();

    public abstract DiscreteTimeData<Long>[] getMemoryUsage(MemoryType type);

    public abstract MemoryType[] getMemoryTypesToDisplay();

    public abstract boolean isMemoryTypeDisplayed(MemoryType type);

    public abstract void setDisplayMemoryType(MemoryType type, boolean selected);

}
