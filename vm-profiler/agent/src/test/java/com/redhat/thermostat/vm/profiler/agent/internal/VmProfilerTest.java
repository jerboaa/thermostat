/*
 * Copyright 2012-2014 Red Hat, Inc.
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

import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.io.File;
import java.util.Properties;

import javax.management.MBeanServerConnection;
import javax.management.ObjectName;

import org.junit.Before;
import org.junit.Test;

import com.redhat.thermostat.agent.utils.management.MXBeanConnection;
import com.redhat.thermostat.agent.utils.management.MXBeanConnectionPool;
import com.redhat.thermostat.common.Clock;
import com.redhat.thermostat.vm.profiler.agent.internal.VmProfiler.Attacher;
import com.sun.tools.attach.VirtualMachine;

public class VmProfilerTest {

    private VmProfiler profiler;

    private VirtualMachine vm;
    private MBeanServerConnection server;
    private MXBeanConnection connection;
    private MXBeanConnectionPool connectionPool;
    private Attacher attacher;
    private Clock clock;

    private final int PID = 0;

    private final String AGENT_JAR = "foo";
    private final String ASM_JAR = "bar";
    private final long TIME = 1_000_000_000;

    private ObjectName instrumentationName;

    @Before
    public void setUp() throws Exception {
        instrumentationName = new ObjectName("com.redhat.thermostat:type=InstrumentationControl");

        Properties props = new Properties();
        props.setProperty("AGENT_JAR", AGENT_JAR);
        props.setProperty("ASM_JAR", ASM_JAR);

        clock = mock(Clock.class);
        when(clock.getRealTimeMillis()).thenReturn(TIME);

        attacher = mock(Attacher.class);
        vm = mock(VirtualMachine.class);
        when(attacher.attach(isA(String.class))).thenReturn(vm);

        server = mock(MBeanServerConnection.class);

        connection = mock(MXBeanConnection.class);
        when(connection.get()).thenReturn(server);

        connectionPool = mock(MXBeanConnectionPool.class);
        when(connectionPool.acquire(PID)).thenReturn(connection);

        profiler = new VmProfiler(props, connectionPool, attacher, clock);
    }

    @Test
    public void startingProfilingLoadsJvmAgentAndMakesAnRmiCall() throws Exception {
        profiler.startProfiling(PID);

        verify(attacher).attach(String.valueOf(PID));
        verify(vm).loadAgent(AGENT_JAR, "");
        verify(vm).detach();
        verifyNoMoreInteractions(vm);

        verify(server).invoke(instrumentationName, "startProfiling", new Object[0], new String[0]);
        verify(connectionPool).release(PID, connection);
    }

    @Test
    public void stoppingProfilingLoadsJvmAgentAndMakesAnRmiCall() throws Exception {
        final String FILE = "foobar";
        when(server.getAttribute(instrumentationName, "ProfilingDataFile")).thenReturn(FILE);

        ProfileUploader uploader = mock(ProfileUploader.class);

        profiler.stopProfiling(PID, uploader);

        verifyNoMoreInteractions(vm);

        verify(server).invoke(instrumentationName, "stopProfiling", new Object[0], new String[0]);
        verify(uploader).upload(TIME, new File(FILE));
        verify(connectionPool, times(2)).release(PID, connection);


    }
}
