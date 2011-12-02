package com.redhat.thermostat.common;

public class MemoryStat {
    private final long timestamp;
    private final long total;
    private final long free;
    private final long buffers;
    private final long cached;
    private final long swapTotal;
    private final long swapFree;
    private final long commitLimit;

    public MemoryStat(long timestamp, long total, long free, long buffers, long cached, long swapTotal, long swapFree, long commitLimit) {
        this.timestamp = timestamp;
        this.total = total;
        this.free = free;
        this.buffers = buffers;
        this.cached = cached;
        this.swapTotal = swapTotal;
        this.swapFree = swapFree;
        this.commitLimit = commitLimit;
    }

    public long getTimeStamp() {
        return timestamp;
    }

    public long getTotal() {
        return total;
    }

    public long getFree() {
        return free;
    }

    public long getBuffers() {
        return buffers;
    }

    public long getCached() {
        return cached;
    }

    public long getSwapTotal() {
        return swapTotal;
    }

    public long getSwapFree() {
        return swapFree;
    }

    public long getCommitLimit() {
        return commitLimit;
    }

}
