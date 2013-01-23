/*
 * Copyright 2012, 2013 Red Hat, Inc.
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

package com.redhat.thermostat.vm.cpu.agent.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.StringReader;

import org.junit.Test;

import com.redhat.thermostat.utils.ProcDataSource;
import com.redhat.thermostat.vm.cpu.agent.internal.ProcessStatusInfo;
import com.redhat.thermostat.vm.cpu.agent.internal.ProcessStatusInfoBuilder;

public class ProcessStatusInfoBuilderTest {

    @Test
    public void testSimpleProcessStatus() {
        ProcDataSource dataSource = new ProcDataSource();
        ProcessStatusInfo stat = new ProcessStatusInfoBuilder(dataSource).build(1);
        assertNotNull(stat);
    }

    @Test
    public void testKnownProcessStatus() throws IOException {
        final int PID = 10363;
        String PROCESS_NAME = "(bash)";
        String STATE = "S";
        String PPID = "1737";
        String PROCESS_GROUP_ID = "10363";
        String SESSION_ID = "10363";
        String TTY_NUMBER = "34817";
        String TTY_PROCESS_GROUP_ID = "11404";
        String FLAGS_WORD = "4202496";
        String MINOR_FAULTS = "8093";
        String MINOR_FAULTS_CHILDREN = "607263";
        String MAJOR_FAULTS = "1";
        String MAJOR_FAULTS_CHILDREN = "251";
        final long USER_TIME_TICKS = 21;
        final long KERNEL_TIME_TICKS = 7;
        final long USER_TIME_CHILDREN = 10;
        String KERNEL_TIME_CHILDREN = "1000";
        String PRIORITY = "20";
        String statString = "" +
                PID + " " + PROCESS_NAME + " " + STATE + " " + PPID + " "
                + PROCESS_GROUP_ID + " " + SESSION_ID + " " + TTY_NUMBER + " "
                + TTY_PROCESS_GROUP_ID + " " + FLAGS_WORD + " " + MINOR_FAULTS + " "
                + MINOR_FAULTS_CHILDREN + " " + MAJOR_FAULTS + " " + MAJOR_FAULTS_CHILDREN + " " +
                USER_TIME_TICKS + " " + KERNEL_TIME_TICKS + " " + USER_TIME_CHILDREN + " " +
                KERNEL_TIME_CHILDREN + " " + PRIORITY;

        ProcDataSource dataSource = mock(ProcDataSource.class);
        when(dataSource.getStatReader(any(Integer.class))).thenReturn(new StringReader(statString));
        ProcessStatusInfoBuilder builder = new ProcessStatusInfoBuilder(dataSource);
        ProcessStatusInfo stat = builder.build(PID);

        verify(dataSource).getStatReader(PID);
        assertNotNull(stat);
        assertEquals(PID, stat.getPid());
        assertEquals(USER_TIME_TICKS, stat.getUserTime());
        assertEquals(KERNEL_TIME_TICKS, stat.getKernelTime());
    }

    @Test
    public void testBadProcessName() throws IOException {
        final int PID = 10363;
        String PROCESS_NAME = "(secretly-bad process sleep 10 20 ) 6)";
        String STATE = "S";
        String PPID = "1737";
        String PROCESS_GROUP_ID = "10363";
        String SESSION_ID = "10363";
        String TTY_NUMBER = "34817";
        String TTY_PROCESS_GROUP_ID = "11404";
        String FLAGS_WORD = "4202496";
        String MINOR_FAULTS = "8093";
        String MINOR_FAULTS_CHILDREN = "607263";
        String MAJOR_FAULTS = "1";
        String MAJOR_FAULTS_CHILDREN = "251";
        final long USER_TIME_TICKS = 21;
        final long KERNEL_TIME_TICKS = 7;
        final long USER_TIME_CHILDREN = 10;
        String KERNEL_TIME_CHILDREN = "1000";
        String PRIORITY = "20";
        String statString = "" +
                PID + " " + PROCESS_NAME + " " + STATE + " " + PPID + " "
                + PROCESS_GROUP_ID + " " + SESSION_ID + " " + TTY_NUMBER + " "
                + TTY_PROCESS_GROUP_ID + " " + FLAGS_WORD + " " + MINOR_FAULTS + " "
                + MINOR_FAULTS_CHILDREN + " " + MAJOR_FAULTS + " " + MAJOR_FAULTS_CHILDREN + " " +
                USER_TIME_TICKS + " " + KERNEL_TIME_TICKS + " " + USER_TIME_CHILDREN + " " +
                KERNEL_TIME_CHILDREN + " " + PRIORITY;

        ProcDataSource dataSource = mock(ProcDataSource.class);
        when(dataSource.getStatReader(any(Integer.class))).thenReturn(new StringReader(statString));
        ProcessStatusInfoBuilder builder = new ProcessStatusInfoBuilder(dataSource);
        ProcessStatusInfo stat = builder.build(PID);

        verify(dataSource).getStatReader(PID);
        assertNotNull(stat);
        assertEquals(PID, stat.getPid());
        assertEquals(USER_TIME_TICKS, stat.getUserTime());
        assertEquals(KERNEL_TIME_TICKS, stat.getKernelTime());
    }

}

