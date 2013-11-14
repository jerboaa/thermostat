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

package com.redhat.thermostat.utils.management.internal;

import java.io.File;
import java.io.IOException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.Registry;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import com.redhat.thermostat.agent.proxy.common.AgentProxyControl;
import com.redhat.thermostat.agent.proxy.common.AgentProxyListener;
import com.redhat.thermostat.agent.proxy.common.AgentProxyLogin;
import com.redhat.thermostat.common.tools.ApplicationException;
import com.redhat.thermostat.common.utils.LoggedExternalProcess;
import com.redhat.thermostat.common.utils.LoggingUtils;
import com.redhat.thermostat.shared.config.Configuration;

class AgentProxyClient implements AgentProxyListener {
    
    private static final long SERVER_TIMEOUT_MS = 5000L;
    private static final String SERVER_NAME = "thermostat-agent-proxy";
    private static final Logger logger = LoggingUtils.getLogger(AgentProxyClient.class);
    
    private final RMIRegistry registry;
    private final int pid;
    private final ProcessCreator procCreator;
    private final Configuration config;
    private final CountDownLatch started;
    
    private AgentProxyControl proxy;
    private Exception serverError;
    
    AgentProxyClient(RMIRegistry registry, int pid) {
        this(registry, pid, new Configuration(), new CountDownLatch(1), 
                new ProcessCreator());
    }

    AgentProxyClient(RMIRegistry registry, int pid, Configuration config,
            CountDownLatch started, ProcessCreator procCreator) {
        this.registry = registry;
        this.pid = pid;
        this.config = config;
        this.started = started;
        this.procCreator = procCreator;
    }

    void createProxy() throws IOException, ApplicationException {
        // Export our listener
        AgentProxyListener stub = (AgentProxyListener) registry.export(this);
        String listenerName = REMOTE_PREFIX + String.valueOf(pid);
        Registry reg = registry.getRegistry();
        reg.rebind(listenerName, stub);
        logger.fine("Registered proxy listener for " + pid);

        // Start the agent proxy, and wait until it exports itself
        try {
            startProcess();
        } finally {
            // Got started event or timed out, unregister our listener
            try {
                reg.unbind(listenerName);
                registry.unexport(this);
            } catch (NotBoundException e) {
                throw new RemoteException("Error unregistering listener", e);
            }
        }

        // Check if server started successfully
        if (serverError != null) {
            throw new RemoteException("Server failed to start", serverError);
        }

        // Lookup server
        String serverName = AgentProxyLogin.REMOTE_PREFIX + String.valueOf(pid);
        try {
            // Need to authenticate in order to obtain proxy object
            AgentProxyLogin proxyLogin = (AgentProxyLogin) reg.lookup(serverName);
            proxy = proxyLogin.login();
        } catch (NotBoundException e) {
            throw new RemoteException("Unable to find remote interface", e);
        }
    }

    private void startProcess() throws IOException, ApplicationException {
        String serverPath = config.getSystemBinRoot() + File.separator + SERVER_NAME;
        procCreator.createAndRunProcess(new String[] { serverPath, String.valueOf(pid) });
        try {
            boolean result = started.await(SERVER_TIMEOUT_MS, TimeUnit.MILLISECONDS);
            if (!result) {
                throw new RemoteException("Timeout while waiting for server");
            }
        } catch (InterruptedException e) {
            // Restore interrupted status
            Thread.currentThread().interrupt();
        }
    }
    
    void attach() throws RemoteException {
        proxy.attach();
    }
    
    boolean isAttached() throws RemoteException {
        return proxy.isAttached();
    }
    
    String getConnectorAddress() throws RemoteException {
        return proxy.getConnectorAddress();
    }
    
    void detach() throws RemoteException {
        proxy.detach();
    }
    
    @Override
    public void serverStarted() throws RemoteException {
        started.countDown();
    }

    @Override
    public void serverFailedToStart(Exception error) throws RemoteException {
        serverError = error;
        started.countDown();
    }
    
    /*
     * For testing purposes only.
     */
    AgentProxyControl getProxy() {
        return proxy;
    }
    
    static class ProcessCreator {
        Process createAndRunProcess(String[] args) throws IOException, ApplicationException {
            LoggedExternalProcess process = new LoggedExternalProcess(args);
            return process.runAndReturnProcess();
        }
    }
    
}
