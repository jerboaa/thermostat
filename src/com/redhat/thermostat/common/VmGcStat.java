package com.redhat.thermostat.common;

public class VmGcStat {

    private final long timestamp;
    private final int vmId;
    private final String collectorName;
    private final long runCount;
    private final long wallTime;

    public VmGcStat(int vmId, long timestamp, String collectorName, long runCount, long wallTime) {
        this.timestamp = timestamp;
        this.vmId = vmId;
        this.collectorName = collectorName;
        this.runCount = runCount;
        this.wallTime = wallTime;
    }
    public int getVmId() {
        return vmId;
    }
    public String getCollectorName() {
        return collectorName;
    }
    public long getRunCount() {
        return runCount;
    }
    public long getWallTime() {
        return wallTime;
    }

    public long getTimeStamp() {
        return timestamp;
    }
}
