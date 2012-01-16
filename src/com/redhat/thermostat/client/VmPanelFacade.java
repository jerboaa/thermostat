package com.redhat.thermostat.client;

import com.redhat.thermostat.common.VmInfo;
import com.redhat.thermostat.common.VmMemoryStat;

/**
 * Represents information specific to a JVM running on a host somewhere. This is
 * used to populate the UI for a VM's information.
 */
public interface VmPanelFacade {

    /**
     * @return generic overview information about a vm
     */
    public VmInfo getVmInfo();

    /**
     * @return names of the garbage collectors that are operating
     */
    public String[] getCollectorNames();

    /**
     * @param collectorName the name of the garbage collector
     * @return a list of (time, cumulative time collector has run )
     */
    public DiscreteTimeData<Long>[] getCollectorRunTime(String collectorName);

    public String getCollectorGeneration(String collectorName);

    public VmMemoryStat getLatestMemoryInfo();

}
