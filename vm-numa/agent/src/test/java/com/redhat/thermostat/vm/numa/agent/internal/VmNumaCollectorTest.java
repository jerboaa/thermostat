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

package com.redhat.thermostat.vm.numa.agent.internal;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;

import com.redhat.thermostat.common.Clock;
import org.junit.Before;
import org.junit.Test;

import com.redhat.thermostat.vm.numa.common.VmNumaNodeStat;
import com.redhat.thermostat.vm.numa.common.VmNumaStat;

public class VmNumaCollectorTest {

    private static final int PID = 100;

    private VmNumaCollector collector;

    private Clock clock;
    private NumaMapsReaderProvider readerProvider;
    private PageSizeProvider pageSizeProvider;

    @Before
    public void setup() {
        clock = mock(Clock.class);
        when(clock.getRealTimeMillis()).thenReturn(100L);

        pageSizeProvider = mock(PageSizeProvider.class);
        when(pageSizeProvider.getHugePageSize()).thenReturn(2048L * 1024L);
        when(pageSizeProvider.getPageSize()).thenReturn(4L * 1024L);
    }

    @Test
    public void testCollectSingleNodeStat() throws IOException {
        readerProvider = mock(NumaMapsReaderProvider.class);
        when(readerProvider.createReader(anyInt())).thenReturn(new BufferedReader(new StringReader(
                "017ec000 default heap anon=1861 dirty=1796 swapcache=65 active=1667 N0=1861 kernelpagesize_kB=4\n" +
                "e09ec000 default stack anon=1776 dirty=1776 swapcache=65 active=1667 N0=1776 kernelpagesize_kB=4\n" +
                "d1200000 default anon=45680 dirty=45680 active=43669 N0=45680 kernelpagesize_kB=4\n" +
                "d0800000 default huge anon=456 dirty=456 active=43669 N0=456 kernelpagesize_kB=4\n"
        )));
        collector = new VmNumaCollector(PID, clock, readerProvider, pageSizeProvider);

        VmNumaStat stat = collector.collect();
        VmNumaNodeStat[] stats = stat.getVmNodeStats();
        assertThat(stats.length, is(1));
        VmNumaNodeStat nodeStat = stats[0];
        assertThat(nodeStat.getNode(), is(0));
        assertThat(nodeStat.getHugeMemory(), is(912.0d));
        assertThat(nodeStat.getHeapMemory(), is(7.26953125d));
        assertThat(nodeStat.getStackMemory(), is(6.9375d));
        assertThat(nodeStat.getPrivateMemory(), is(178.4375d));
    }

    @Test
    public void testCollectMultipleNodeStat() throws IOException {
        readerProvider = mock(NumaMapsReaderProvider.class);
        when(readerProvider.createReader(anyInt())).thenReturn(new BufferedReader(new StringReader(
                "017ec000 default heap anon=1861 dirty=1796 swapcache=65 active=1667 N0=1861 kernelpagesize_kB=4\n" +
                "d1200000 default anon=45680 dirty=45680 active=43669 N1=45680 kernelpagesize_kB=4\n"
        )));
        collector = new VmNumaCollector(PID, clock, readerProvider, pageSizeProvider);

        VmNumaStat stat = collector.collect();
        VmNumaNodeStat[] stats = stat.getVmNodeStats();

        assertThat(stats.length, is(2));

        VmNumaNodeStat nodeStat1 = stats[0];
        assertThat(nodeStat1.getNode(), is(0));
        assertThat(nodeStat1.getHugeMemory(), is (0.0d));
        assertThat(nodeStat1.getHeapMemory(), is(7.26953125d));
        assertThat(nodeStat1.getStackMemory(), is(0.0d));
        assertThat(nodeStat1.getPrivateMemory(), is(0.0d));

        VmNumaNodeStat nodeStat2 = stats[1];
        assertThat(nodeStat2.getNode(), is(1));
        assertThat(nodeStat2.getHugeMemory(), is(0.0d));
        assertThat(nodeStat2.getHeapMemory(), is(0.0d));
        assertThat(nodeStat2.getStackMemory(), is(0.0d));
        assertThat(nodeStat2.getPrivateMemory(), is(178.4375d));
    }

}
