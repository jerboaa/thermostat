/*
 * Copyright 2012-2017 Red Hat, Inc.
 *
 * This file is part of Thermostat.
 *
 * Thermostat is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published
 * by the Free Software Foundation; either version 2, or (at your
 * option) any later version.
 *
 * Thermostat is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Thermostat; see the file COPYING.  If not see
 * <http://www.gnu.org/licenses/>.
 *
 * Linking this code with other modules is making a combined work
 * based on this code.  Thus, the terms and conditions of the GNU
 * General Public License cover the whole combination.
 *
 * As a special exception, the copyright holders of this code give
 * you permission to link this code with independent modules to
 * produce an executable, regardless of the license terms of these
 * independent modules, and to copy and distribute the resulting
 * executable under terms of your choice, provided that you also
 * meet, for each linked independent module, the terms and conditions
 * of the license of that module.  An independent module is a module
 * which is not derived from or based on this code.  If you modify
 * this code, you may extend this exception to your version of the
 * library, but you are not obligated to do so.  If you do not wish
 * to do so, delete this exception statement from your version.
 */

package com.redhat.thermostat.vm.heap.analysis.common.model;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

import com.redhat.thermostat.testutils.DataObjectTest;

public class HeapInfoTest extends DataObjectTest {

    private HeapInfo heapInfo;

    @Before
    public void setUp() {
        String agentId = "test-agent";
        heapInfo = new HeapInfo(agentId, "vmId", 12345);
    }

    @Test
    public void testProperties() {
        assertEquals("test-agent", heapInfo.getAgentId());
        assertEquals("vmId", heapInfo.getVmId());
        assertEquals(12345, heapInfo.getTimeStamp());
    }

    @Test
    public void testHeapDumpId() {
        assertNull(heapInfo.getHeapDumpId());
        heapInfo.setHeapDumpId("test");
        assertEquals("test", heapInfo.getHeapDumpId());
    }

    @Test
    public void testHistogramId() {
        assertNull(heapInfo.getHistogramId());
        heapInfo.setHistogramId("test");
        assertEquals("test", heapInfo.getHistogramId());
    }

    @Test
    public void testEquals() {
        /**
         * Use new String for distinct objects to catch
         * equals issues
         * "1" == "1" is true
         * new String("1") == new String("1") is false
         * new String("1").equals(new String "1") is true
         */
        HeapInfo a = new HeapInfo(new String("1"), new String("1"), 0l);
        assertThat(a, is(equalTo(a)));

        HeapInfo b = new HeapInfo(new String("1"), new String("1"), 0l);
        assertThat(a, is(equalTo(b)));
    }

    @Test
    public void testNotEquals() {
        HeapInfo a = new HeapInfo(new String("1"), new String("1"), 0l);
        HeapInfo b = new HeapInfo(new String("1"), new String("2"), 0l);

        assertThat(a, is(not(equalTo(b))));
    }

    @Test
    public void testHashEquals() {
        HeapInfo a = new HeapInfo(new String("1"), new String("1"), 0l);
        assertThat(a.hashCode(), is(equalTo(a.hashCode())));

        HeapInfo b = new HeapInfo(new String("1"), new String("1"), 0l);
        assertThat(a.hashCode(), is(equalTo(b.hashCode())));
    }

    @Override
    public Class<?>[] getDataClasses() {
        return new Class[] { HeapInfo.class };
    }

}

