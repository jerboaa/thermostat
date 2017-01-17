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

package com.redhat.thermostat.agent.proxy.server;

import java.io.File;
import java.io.IOException;
import java.rmi.RemoteException;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.redhat.thermostat.common.utils.LoggingUtils;
import com.sun.tools.attach.AgentInitializationException;
import com.sun.tools.attach.AgentLoadException;
import com.sun.tools.attach.AttachNotSupportedException;
import com.sun.tools.attach.VirtualMachine;

class AgentProxyControlImpl {
    
    private static final Logger logger = LoggingUtils.getLogger(AgentProxyControlImpl.class);
    private static final String CONNECTOR_ADDRESS_PROPERTY = "com.sun.management.jmxremote.localConnectorAddress";
    private static final String JCMD_NAME = "jcmd";
    private static final String JCMD_MANAGEMENT_AGENT_START_LOCAL = "ManagementAgent.start_local";

    private final int pid;
    private final VirtualMachineUtils vmUtils;
    
    private VirtualMachine vm;
    private boolean attached;
    private String connectorAddress;
    
    AgentProxyControlImpl(int pid) {
        this(pid, new VirtualMachineUtils());
    }
    
    AgentProxyControlImpl(int pid, VirtualMachineUtils vmUtils) {
        this.pid = pid;
        this.vmUtils = vmUtils;
    }

    void attach() throws AttachNotSupportedException, IOException {
        vm = vmUtils.attach(String.valueOf(pid));
        attached = true;

        Properties props = vm.getAgentProperties();
        connectorAddress = props.getProperty(CONNECTOR_ADDRESS_PROPERTY);
        if (connectorAddress == null) {
            String home = null;
            String agent = null;
            try {
                props = vm.getSystemProperties();
                startManagementAgent(props);
                logger.fine("Started management agent for vm '" + pid + "'");
                props = vm.getAgentProperties();
                connectorAddress = props.getProperty(CONNECTOR_ADDRESS_PROPERTY);
            } catch (IOException | AgentLoadException | AgentInitializationException e) {
                throw new RemoteException("Failed to load agent ('" + agent + "', from home '" + home + "') into VM (pid: " + pid + ")", e);
            }
        }
    }

    private void startManagementAgent(Properties props) throws IOException,
                                                               AgentLoadException,
                                                               AgentInitializationException {
        String home = props.getProperty("java.home");
        try {
            // JDK 7 and up have jcmd with JMX management agent start support.
            startManagementAgentUsingJcmd(home, pid);
        } catch (Exception e) {
            // Fall back to old management-agent.jar behaviour.
            logger.log(Level.FINE, "Failed to activate JMX agent via jcmd.", e);
            startManagementAgentUsingJavaAgent(home);
        }
    }

    private void startManagementAgentUsingJcmd(String home, int vmPid) throws IOException, InterruptedException {
        String jcmd = home + File.separator + "bin" + File.separator + JCMD_NAME;
        File jcmdFile = new File(jcmd);
        if (!jcmdFile.exists()) {
            // java.home might be JRE home. Try one level up.
            File binDir = new File(new File(home).getParentFile(), "bin");
            jcmd = binDir.getAbsolutePath() + File.separator + JCMD_NAME;
        }
        String[] args = new String[] { jcmd,
                                       Integer.toString(vmPid),
                                       JCMD_MANAGEMENT_AGENT_START_LOCAL };
        List<String> jcmdArgs = Arrays.asList(args);
        logger.fine("Starting JMX management agent via JCMD: " + jcmdArgs);
        Process process = startProcess(jcmdArgs);
        int result = process.waitFor();
        if (result != 0) {
            throw new IllegalStateException("Failed to execute jcmd. Exit code was: " + result);
        }
    }

    // Package private for testing
    Process startProcess(List<String> jcmdArgs) throws IOException {
        ProcessBuilder builder = new ProcessBuilder(jcmdArgs);
        return builder.start();
    }

    private void startManagementAgentUsingJavaAgent(String home) throws AgentLoadException, AgentInitializationException, IOException {
        String agent = home + File.separator + "lib" + File.separator + "management-agent.jar";
        logger.fine("Loading '" + agent + "' into VM (pid: " + pid + ")");
        vm.loadAgent(agent);
    }

    boolean isAttached() {
        return attached;
    }

    String getConnectorAddress() throws IOException {
        if (!attached) {
            throw new IOException("Agent not attached to target VM");
        }
        return connectorAddress;
    }

    void detach() throws IOException {
        if (attached) {
            vm.detach();
            attached = false;
        }
    }
    
    static class VirtualMachineUtils {
        VirtualMachine attach(String pid) throws AttachNotSupportedException, IOException {
            return VirtualMachine.attach(pid);
        }
    }

}

