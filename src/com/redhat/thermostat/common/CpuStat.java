package com.redhat.thermostat.common;

public class CpuStat {

    public static final double INVALID_LOAD = Double.MIN_VALUE;

    private final double load5;
    private final double load10;
    private final double load15;
    private final long timeStamp;

    public CpuStat(long timestamp, double load5, double load10, double load15) {
        this.timeStamp = timestamp;
        this.load5 = load5;
        this.load10 = load10;
        this.load15 = load15;
    }

    public double getLoad5() {
        return load5;
    }

    public double getLoad10() {
        return load10;
    }

    public double getLoad15() {
        return load15;
    }

    public double[] getLoad() {
        return new double[] { load5, load10, load15 };
    }

    public long getTimeStamp() {
        return timeStamp;
    }
}
