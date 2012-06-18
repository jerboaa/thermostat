package com.redhat.thermostat.common.model;

import static org.junit.Assert.*;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.junit.Before;
import org.junit.Test;

import com.redhat.thermostat.common.dao.HostRef;
import com.redhat.thermostat.common.dao.VmRef;

public class HeapInfoTest {

    private HeapInfo heapInfo;
    private VmRef vm;

    @Before
    public void setUp() {
        HostRef hostRef = new HostRef("321", "test-host");
        vm = new VmRef(hostRef, 123, "test-vm");
        heapInfo = new HeapInfo(vm, 12345);
    }

    @Test
    public void testProperties() {
        assertSame(vm, heapInfo.getVm());
        assertEquals(12345, heapInfo.getTimestamp());
    }

    @Test
    public void testHeapDump() throws IOException {
        assertNull(heapInfo.getHeapDump());
        byte[] test = new byte[]{ 1 , 2 ,3 };
        heapInfo.setHeapDump(new ByteArrayInputStream(test));
        InputStream in = heapInfo.getHeapDump();
        assertNotNull(in);
        assertEquals(1, in.read());
        assertEquals(2, in.read());
        assertEquals(3, in.read());
        assertEquals(-1, in.read());
    }
}
