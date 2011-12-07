package com.redhat.thermostat.agent.storage;

import java.net.UnknownHostException;
import java.util.UUID;

import com.redhat.thermostat.agent.config.StartupConfiguration;
import com.redhat.thermostat.common.CpuStat;
import com.redhat.thermostat.common.HostInfo;
import com.redhat.thermostat.common.MemoryStat;

public interface Storage {
    public void connect(String uri) throws UnknownHostException;

    public void setAgentId(UUID id);

    public void addAgentInformation(StartupConfiguration config);

    public void removeAgentInformation();

    public void addCpuStat(CpuStat stat);

    public void addMemoryStat(MemoryStat stat);

    public void updateHostInfo(HostInfo hostInfo);

    /**
     * @return {@code null} if the value is invalid or missing
     */
    public String getBackendConfig(String backendName, String configurationKey);

}
