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

import java.io.IOException;

import javax.management.MBeanServerConnection;
import javax.management.ObjectName;

import com.redhat.thermostat.agent.utils.management.MXBeanConnection;
import com.redhat.thermostat.agent.utils.management.MXBeanConnectionPool;
import com.sun.tools.attach.AgentInitializationException;
import com.sun.tools.attach.AgentLoadException;
import com.sun.tools.attach.AttachNotSupportedException;
import com.sun.tools.attach.VirtualMachine;

class RemoteProfilerCommunicator {

    // TODO should this maintain enough state to know when to attach and load agent?

    // published by the jvm-agent
    private static final String INSTRUMENTATION_OBJECT = "com.redhat.thermostat:type=InstrumentationControl";

    static class Attacher {
        VirtualMachine attach(String pid) throws AttachNotSupportedException, IOException {
            return VirtualMachine.attach(pid);
        }
    }

    private MXBeanConnectionPool connectionPool;
    private final Attacher attacher;

    public RemoteProfilerCommunicator(MXBeanConnectionPool connectionPool) {
        this(connectionPool, new Attacher());
    }

    public RemoteProfilerCommunicator(MXBeanConnectionPool connectionPool, Attacher attacher) {
        this.connectionPool = connectionPool;
        this.attacher = attacher;
    }

    public void loadAgentIntoPid(int pid, String jar, String options) throws ProfilerException {
        try {
            VirtualMachine vm = attacher.attach(String.valueOf(pid));
            try {
                vm.loadAgent(jar, options);
            } catch (AgentLoadException | AgentInitializationException e) {
                throw new ProfilerException("Error starting profiler", e);
            } finally {
                vm.detach();
            }
        } catch (IOException | AttachNotSupportedException e) {
            throw new ProfilerException("Error starting profiler", e);
        }
    }

    public void startProfiling(int pid) throws ProfilerException {
        invokeMethodOnInstrumentation(pid, "startProfiling");
    }

    public void stopProfiling(int pid) throws ProfilerException {
        invokeMethodOnInstrumentation(pid, "stopProfiling");
    }

    public String getProfilingDataFile(int pid) throws ProfilerException {
        return (String) getInstrumentationAttribute(pid, "ProfilingDataFile");
    }

    private Object invokeMethodOnInstrumentation(int pid, String name) throws ProfilerException {
        try {
            MXBeanConnection connection = connectionPool.acquire(pid);
            try {
                ObjectName instrumentation = new ObjectName(INSTRUMENTATION_OBJECT);
                MBeanServerConnection server = connection.get();
                return server.invoke(instrumentation, name, new Object[0], new String[0]);
            } finally {
                connectionPool.release(pid, connection);
            }
        } catch (Exception e) {
            throw new ProfilerException("Unable to communicate with remote profiler", e);
        }
    }

    private Object getInstrumentationAttribute(int pid, String name) throws ProfilerException {
        try {
            MXBeanConnection connection = connectionPool.acquire(pid);
            try {
                ObjectName instrumentation = new ObjectName(INSTRUMENTATION_OBJECT);
                MBeanServerConnection server = connection.get();
                return server.getAttribute(instrumentation, name);
            } finally {
                connectionPool.release(pid, connection);
            }
        } catch (Exception e) {
            throw new ProfilerException("Unable to communicate with remote profiler", e);
        }
    }

}
