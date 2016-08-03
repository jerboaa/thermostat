/*
 * Copyright 2012-2016 Red Hat, Inc.
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

package com.redhat.thermostat.vm.byteman.agent.internal;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jboss.byteman.agent.install.Install;

import com.redhat.thermostat.agent.utils.ProcessChecker;
import com.redhat.thermostat.common.utils.LoggingUtils;
import com.redhat.thermostat.shared.config.CommonPaths;
import com.sun.tools.attach.AgentInitializationException;
import com.sun.tools.attach.AgentLoadException;
import com.sun.tools.attach.AttachNotSupportedException;

class BytemanAttacher {
    
    private static final Logger logger = LoggingUtils.getLogger(BytemanAttacher.class);
    private static final int PORT_MIN = 13300;
    private static final int MAX_PORT_SLOTS = 300;
    private static final int PORT_MAX = PORT_MIN + MAX_PORT_SLOTS;
    private static final String BYTEMAN_AGENT_LOADED_PROPERTY = "org.jboss.byteman.agent.loaded";
    private static final String BYTEMAN_AGENT_PORT_PROPERTY = "org.jboss.byteman.thermostat.agent.port";
    private static final String THERMOSTAT_IPC_CONFIG_NAME_PROPERTY = "org.jboss.byteman.thermostat.ipcConfig";
    private static final String THERMOSTAT_HELPER_SOCKET_NAME_PROPERTY = "org.jboss.byteman.thermostat.socketName";
    static final String LOCALHOST = "localhost";
    static final String[] BYTEMAN_STATIC_INSTALL_PROPS = new String[] {
            "org.jboss.byteman.verbose=true",
            "org.jboss.byteman.transform.all=true", // Allow for transformation of java.lang classes
            "org.jboss.byteman.thermostat.transport=localsocket", // make the helper use unix sockets
    };
    private final BtmInstallHelper installer;
    private final CommonPaths paths;
    
    BytemanAttacher(CommonPaths paths) {
        this(new BtmInstallHelper(), paths);
    }
    
    BytemanAttacher(BtmInstallHelper installer, CommonPaths paths) {
        this.installer = installer;
        this.paths = paths;
    }
    
    
    public BytemanAgentInfo attach(String vmId, int pid, String agentId) {
        int port = findPort();
        logger.finest("Attempting to attach byteman agent for VM '" + pid + "' on port '" + port + "'");
        try {
            VmSocketIdentifier sockIdentifier = new VmSocketIdentifier(vmId, pid, agentId);
            String[] btmInstallProps = buildBytemanInstallProps(sockIdentifier, port);
            boolean agentJarToBootClassPath = true;
            int actualPort = installer.install(Integer.toString(pid), agentJarToBootClassPath, false, null /* localhost */, port, btmInstallProps);
            // Port might have changed here if agent rebooted and targed jvm
            // stayed alive
            if (actualPort > 0) {
                return new BytemanAgentInfo(pid, actualPort, null, vmId, agentId, false);
            } else {
                return null;
            }
        } catch (IllegalArgumentException |
                 AttachNotSupportedException |
                 AgentLoadException |
                 AgentInitializationException e) {
            return handleAttachFailure(e, vmId, port, pid);
        } catch (IOException e) {
            ProcessChecker process = getProcessChecker(pid);
            if (!process.exists()) {
                return new BytemanAgentInfo(pid, port, null, vmId, agentId, true);
            }
            return handleAttachFailure(e, vmId, port, pid);
        }
    }
    
    // testing-hook
    ProcessChecker getProcessChecker(int pid) {
        return new ProcessChecker(pid);
    }
    
    private BytemanAgentInfo handleAttachFailure(Throwable cause, String vmId, int port, int pid) {
        logger.log(Level.INFO, "Unable to attach to byteman agent.", cause);
        logger.log(Level.WARNING, "Unable to attach byteman agent to VM '" + pid + "' on port '" + port + "'");
        return null;
    }

    // Note:  Setting properties will not work if they don't start with the
    //        byteman prefix. Namely, "org.jboss.byteman."
    private String[] buildBytemanInstallProps(VmSocketIdentifier sockIdentifier, int port) {
        List<String> properties = new ArrayList<>();
        properties.addAll(Arrays.asList(BYTEMAN_STATIC_INSTALL_PROPS));
        String socketNameProperty = THERMOSTAT_HELPER_SOCKET_NAME_PROPERTY + "=" + sockIdentifier.getName();
        String ipcSocketDirProperty = THERMOSTAT_IPC_CONFIG_NAME_PROPERTY + "=" + paths.getUserIPCConfigurationFile().getAbsolutePath();
        String agentPortProperty = BYTEMAN_AGENT_PORT_PROPERTY + "=" + Integer.valueOf(port).toString();
        properties.add(socketNameProperty);
        properties.add(ipcSocketDirProperty);
        properties.add(agentPortProperty);
        return properties.toArray(new String[] {});
    }

    private int findPort() {
        for (int i = PORT_MIN; i <= PORT_MAX; i++) {
            try {
                try (ServerSocket s = new ServerSocket(i)) {
                    s.close();
                    return i;
                }
            } catch (Exception e) {
                // ignore. try next port
            }
        }
        throw new IllegalStateException("No ports available in range [" + PORT_MIN + "," + PORT_MAX + "]");
    }
    
    static class BtmInstallHelper {
        
        private static final int UNKNOWN_PORT = -1;
        
        int install(String vmPid, boolean addToBoot, boolean setPolicy, String hostname, int port, String[] properties)
                throws IllegalArgumentException, FileNotFoundException,
                       IOException, AttachNotSupportedException,
                       AgentLoadException, AgentInitializationException {
            String propVal = Install.getSystemProperty(vmPid, BYTEMAN_AGENT_LOADED_PROPERTY);
            boolean loaded = Boolean.parseBoolean(propVal);
            if (!loaded) {
                Install.install(vmPid, addToBoot, setPolicy, hostname, port, properties);
                return port;
            } else {
                try {
                    int oldPort = Integer.parseInt(Install.getSystemProperty(vmPid, BYTEMAN_AGENT_PORT_PROPERTY));
                    logger.finest("VM (pid: " + vmPid + "): Not installing byteman agent since one is already attached on port " + oldPort);
                    return oldPort;
                } catch (NumberFormatException e) {
                    logger.info("VM (pid: " + vmPid + "): Has a byteman agent already attached, but it wasn't thermostat that attached it");
                    return UNKNOWN_PORT;
                }
            }
        }
    }
}