package com.redhat.thermostat.client;

import com.redhat.thermostat.common.VmInfo;
import com.redhat.thermostat.common.VmMemoryStat;

public interface VmInformationFacade {

    public VmInfo getVmInfo();

    public String[] getCollectorNames();

    public long getTotalInvocations();

    public long[][] getCollectorData(String collectorName);

    public String getCollectorGeneration(String collectorName);

    public VmMemoryStat getMemoryInfo();

}
