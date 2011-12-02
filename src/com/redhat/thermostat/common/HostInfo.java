package com.redhat.thermostat.common;

import java.util.List;
import java.util.Map;

public class HostInfo {

    private final String hostname;
    private final String osName;
    private final String osKernel;
    private final int cpuCount;
    private final long totalMemory;
    private final Map<String, List<String>> networkInfo;

    public HostInfo(String hostname, String osName, String osKernel, int cpuCount, long totalMemory, Map<String, List<String>> networkInfo) {
        this.hostname = hostname;
        this.osName = osName;
        this.osKernel = osKernel;
        this.cpuCount = cpuCount;
        this.totalMemory = totalMemory;
        this.networkInfo = networkInfo;
    }

    public String getHostname() {
        return hostname;
    }

    public String getOsName() {
        return osName;
    }

    public String getOsKernel() {
        return osKernel;
    }

    public int getCpuCount() {
        return cpuCount;
    }

    /**
     * Total memory in bytes
     */
    public long getTotalMemory() {
        return totalMemory;
    }

    /**
     * @returns a map of the following form: {iface-name: [ipv4_addr,
     * ipv6_addr]}
     */
    public Map<String, List<String>> getNetworkInfo() {
        return networkInfo;
    }

}
