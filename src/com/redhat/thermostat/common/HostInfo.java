package com.redhat.thermostat.common;

public class HostInfo {

    private final String hostname;
    private final String osName;
    private final String osKernel;
    private final int cpuCount;
    private final long totalMemory;

    public HostInfo(String hostname, String osName, String osKernel, int cpuCount, long totalMemory) {
        this.hostname = hostname;
        this.osName = osName;
        this.osKernel = osKernel;
        this.cpuCount = cpuCount;
        this.totalMemory = totalMemory;
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
}
