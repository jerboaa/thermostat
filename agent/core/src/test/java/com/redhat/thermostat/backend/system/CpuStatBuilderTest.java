/*
 * Copyright 2012 Red Hat, Inc.
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

package com.redhat.thermostat.backend.system;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;

import org.junit.Test;

import com.redhat.thermostat.common.Clock;
import com.redhat.thermostat.common.SystemClock;
import com.redhat.thermostat.common.model.CpuStat;

public class CpuStatBuilderTest {

    @Test
    public void testSimpleBuild() {
        ProcDataSource dataSource = new ProcDataSource();
        CpuStatBuilder builder= new CpuStatBuilder(new SystemClock(), dataSource, 100l);
        builder.initialize();
        CpuStat stat = builder.build();
        assertNotNull(stat);
    }

    @Test (expected=IllegalStateException.class)
    public void buildWithoutInitializeThrowsException() {
        Clock clock = mock(Clock.class);
        ProcDataSource dataSource = mock(ProcDataSource.class);
        long ticksPerSecond = 1;
        CpuStatBuilder builder = new CpuStatBuilder(clock, dataSource, ticksPerSecond);
        builder.build();
    }

    @Test
    public void testBuildCpuStatFromFile() throws IOException {
        long CLOCK1 = 1000;
        long CLOCK2 = 2000;

        String firstReadContents =
            "cpu 100 0 0 1000 1000\n" +
            "cpu0 100 0 0 1000 1000\n" +
            "cpu1 10 80 10 1000 1000\n";
        BufferedReader reader1 = new BufferedReader(new StringReader(firstReadContents));

        String secondReadContents =
            "cpu 400 0 0 1000 1000\n" +
            "cpu0 200 0 0 1000 1000\n" +
            "cpu1 30 50 120 1000 1000\n";
        BufferedReader reader2 = new BufferedReader(new StringReader(secondReadContents));

        long ticksPerSecond = 100;
        Clock clock = mock(Clock.class);
        when(clock.getRealTimeMillis()).thenReturn(CLOCK2);
        when(clock.getMonotonicTimeNanos()).thenReturn((long)(CLOCK1 * 1E6)).thenReturn((long)(CLOCK2 * 1E6));

        ProcDataSource dataSource = mock(ProcDataSource.class);
        when(dataSource.getStatReader()).thenReturn(reader1).thenReturn(reader2);
        CpuStatBuilder builder = new CpuStatBuilder(clock, dataSource, ticksPerSecond);

        builder.initialize();

        CpuStat stat = builder.build();

        verify(dataSource, times(2)).getStatReader();
        assertArrayEquals(new double[] {100, 100}, stat.getPerProcessorUsage(), 0.01);
    }

}
