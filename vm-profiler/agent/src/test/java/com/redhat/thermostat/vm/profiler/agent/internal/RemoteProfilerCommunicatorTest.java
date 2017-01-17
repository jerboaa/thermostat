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

package com.redhat.thermostat.vm.profiler.agent.internal;

import static org.junit.Assert.*;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import javax.management.MBeanServerConnection;
import javax.management.ObjectName;

import org.junit.Before;
import org.junit.Test;

import com.redhat.thermostat.agent.utils.management.MXBeanConnection;
import com.redhat.thermostat.agent.utils.management.MXBeanConnectionPool;
import com.redhat.thermostat.vm.profiler.agent.internal.RemoteProfilerCommunicator.Attacher;
import com.sun.tools.attach.VirtualMachine;

public class RemoteProfilerCommunicatorTest {

    private static final String OBJECT_NAME = "com.redhat.thermostat:type=InstrumentationControl";
    private static final int PID = 0;

    private MXBeanConnection connection;
    private MXBeanConnectionPool pool;

    private VirtualMachine vm;
    private Attacher attacher;

    private RemoteProfilerCommunicator communicator;

    private MBeanServerConnection server;

    @Before
    public void setUp() throws Exception {
        server = mock(MBeanServerConnection.class);
        connection = mock(MXBeanConnection.class);
        when(connection.get()).thenReturn(server);

        pool = mock(MXBeanConnectionPool.class);
        when(pool.acquire(PID)).thenReturn(connection);

        vm = mock(VirtualMachine.class);
        attacher = mock(Attacher.class);
        when(attacher.attach(eq(String.valueOf(PID)))).thenReturn(vm);

        communicator = new RemoteProfilerCommunicator(pool, attacher);
    }

    @Test
    public void loadAgentIntoPidAttachesAndLoadsTheAgent() throws Exception {
        String AGENT_JAR = "agent-jar";
        String AGENT_OPTIONS = "foo-bar";

        communicator.loadAgentIntoPid(PID, AGENT_JAR, AGENT_OPTIONS);

        verify(attacher).attach(String.valueOf(PID));
        verify(vm).loadAgent(AGENT_JAR, AGENT_OPTIONS);
        verify(vm).detach();
        verifyNoMoreInteractions(vm);
    }

    @Test
    public void startProfilingMakesAnRmiCall() throws Exception {
        communicator.startProfiling(PID);

        verify(server).invoke(
                new ObjectName(OBJECT_NAME),
                "startProfiling",
                new Object[0],
                new String[0]);
        verifyNoMoreInteractions(server);
        verify(pool).release(PID, connection);
    }

    @Test
    public void stopProfilingMakesAnRmiCall() throws Exception {
        communicator.stopProfiling(PID);

        verify(server).invoke(
                new ObjectName(OBJECT_NAME),
                "stopProfiling",
                new Object[0],
                new String[0]);
        verify(pool).release(PID, connection);
    }

    @Test
    public void getDataFileWorks() throws Exception {
        communicator.getProfilingDataFile(PID);

        verify(server).getAttribute(
                new ObjectName(OBJECT_NAME),
                "ProfilingDataFile");
        verify(pool).release(PID, connection);
    }
}
