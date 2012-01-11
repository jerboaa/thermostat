package com.redhat.thermostat.common;

public class VmCpuStat {

    private final long timestamp;
    private final int vmId;
    private final double cpuLoad;

    public VmCpuStat(long timestamp, int vmId, double cpuLoad) {
        this.timestamp = timestamp;
        this.vmId = vmId;
        this.cpuLoad = cpuLoad;
    }

    public long getTimeStamp() {
        return timestamp;
    }

    public int getVmId() {
        return vmId;
    }

    public double getCpuLoad() {
        return cpuLoad;
    }
}
