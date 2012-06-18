package com.redhat.thermostat.common.model;

import java.io.InputStream;

import com.redhat.thermostat.common.dao.VmRef;

public class HeapInfo {

    private VmRef vm;
    private long timestamp;
    private InputStream heapDump;

    public HeapInfo(VmRef vm, long timestamp) {
        this.vm = vm;
        this.timestamp = timestamp;
    }

    public VmRef getVm() {
        return vm;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public InputStream getHeapDump() {
        return heapDump;
    }

    public void setHeapDump(InputStream heapDump) {
        this.heapDump = heapDump;
    }
}
