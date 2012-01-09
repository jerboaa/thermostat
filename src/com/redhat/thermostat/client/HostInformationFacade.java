package com.redhat.thermostat.client;

import com.redhat.thermostat.common.HostInfo;
import com.redhat.thermostat.common.NetworkInfo;

public interface HostInformationFacade {
    public abstract VmRef[] getVms();

    public abstract VmInformationFacade getVm(VmRef vmRef);

    public abstract HostInfo getHostInfo();

    public abstract NetworkInfo getNetworkInfo();

    public abstract double[][] getCpuLoad();

    public abstract long[][] getMemoryUsage(MemoryType type);

    public abstract MemoryType[] getMemoryTypesToDisplay();

    public abstract boolean isMemoryTypeDisplayed(MemoryType type);

    public abstract void setDisplayMemoryType(MemoryType type, boolean selected);

}
