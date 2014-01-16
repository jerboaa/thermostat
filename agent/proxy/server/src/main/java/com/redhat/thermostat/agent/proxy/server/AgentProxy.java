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

package com.redhat.thermostat.agent.proxy.server;

import java.io.IOException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.Registry;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.redhat.thermostat.agent.proxy.common.AgentProxyListener;
import com.redhat.thermostat.agent.proxy.common.AgentProxyLogin;
import com.redhat.thermostat.common.utils.LoggingUtils;

public class AgentProxy {
    
    private static final Logger logger = LoggingUtils.getLogger(AgentProxy.class);
    private static final long TIMEOUT_MS = 300000L; // 5 minutes should be more than enough
    private static final ShutdownListener shutdownListener = new ShutdownListener() {
        @Override
        public void shutdown() throws RemoteException {
            shutdownProxy();
        }
    };
    private static final TimerTask timeoutTask = new TimerTask() {
        @Override
        public void run() {
            try {
                shutdownProxy();
                logger.warning("Server timed out");
            } catch (RemoteException e) {
                logger.log(Level.SEVERE, "Exception while shutting down "
                        + "timed out server" , e);
            }
        }
    };

    private static String name = null;
    private static int pid = -1;
    private static Registry registry = null;
    private static boolean bound = false;
    private static AgentProxyLogin agent = null;
    private static RegistryUtils registryUtils = new RegistryUtils();
    private static AgentProxyNativeUtils nativeUtils = new AgentProxyNativeUtils();
    private static ProcessUserInfoBuilder builder = new ProcessUserInfoBuilder(new ProcDataSource());
    private static Timer timeoutTimer = new Timer(true);
    
    public static void main(String[] args) {
        if (args.length < 1) {
            usage();
        }
        
        try {
            // First argument is pid of target VM
            pid = Integer.parseInt(args[0]);
        } catch (NumberFormatException e) {
            usage();
        }
        
        // Schedule a timeout
        timeoutTimer.schedule(timeoutTask, TIMEOUT_MS);
        
        // Load the native library
        nativeUtils.loadLibrary();

        // Look for registered status listener
        AgentProxyListener listener;
        try {
            String listenerName = AgentProxyListener.REMOTE_PREFIX + String.valueOf(pid);
            registry = registryUtils.getRegistry();
            listener = (AgentProxyListener) registry.lookup(listenerName);
        } catch (RemoteException e) {
            throw new RuntimeException("Failed to locate registry", e);
        } catch (NotBoundException e) {
            throw new RuntimeException("No listener registered", e);
        }

        // Start proxy agent
        Exception ex = null;
        try {
            setupProxy(pid);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Failed to setup agent proxy for " + pid, e);
            ex = e;
        }
        
        // Notify listener of result
        try {
            if (ex == null) {
                // Success
                listener.serverStarted();
            }
            else {
                // Send exception to client
                listener.serverFailedToStart(ex);
            }
        } catch (RemoteException e) {
            throw new RuntimeException("Failed to notify listener", e);
        }
    }

    private static void setupProxy(int pid) throws Exception {
        try {
            UnixCredentials creds;
            try {
                creds = builder.build(pid);
            } catch (IOException e) {
                throw new Exception("Failed to read credentials", e);
            }
            
            try {
                // Set UID/GID to owner of target VM
                nativeUtils.setCredentials(creds.getUid(), creds.getGid());
            } catch (Exception e) {
                throw new Exception("Failed to set credentials to " + creds.getUid() 
                        + ":" + creds.getGid() , e);
            }

            agent = new AgentProxyLoginImpl(creds, pid, shutdownListener);
            name = AgentProxyLogin.REMOTE_PREFIX + String.valueOf(pid);
            AgentProxyLogin stub = (AgentProxyLogin) registryUtils.exportObject(agent);
            registry.rebind(name, stub);
            bound = true;
            logger.info(name + " bound to RMI registry");
        } catch (RemoteException e) {
            throw new Exception("Failed to create remote object", e);
        }
    }
    
    private static void shutdownProxy() throws RemoteException {
        // Unbind from RMI registry
        if (bound) {
            try {
                registry.unbind(name);
                registryUtils.unexportObject(agent);
                logger.info(name + " unbound from RMI registry");
                bound = false;
            } catch (NotBoundException e) {
                throw new RemoteException("Object not bound", e);
            }
        }
    }

    private static void usage() {
        throw new RuntimeException("usage: java " + AgentProxy.class.getName() + " <pidOfTargetJvm>");
    }
    
    /*
     * For testing purposes only.
     */
    static AgentProxyLogin getAgentProxyLogin() {
        return agent;
    }
    
    /*
     * For testing purposes only.
     */
    static ShutdownListener getShutdownListener() {
        return shutdownListener;
    }
    
    /*
     * For testing purposes only.
     */
    static boolean isBound() {
        return bound;
    }

    /*
     * For testing purposes only.
     */
    static void setRegistryUtils(RegistryUtils registryUtils) {
        AgentProxy.registryUtils = registryUtils;
    }
    
    /*
     * For testing purposes only.
     */
    static void setNativeUtils(AgentProxyNativeUtils nativeUtils) {
        AgentProxy.nativeUtils = nativeUtils;
    }
    
    /*
     * For testing purposes only.
     */
    static void setProcessUserInfoBuilder(ProcessUserInfoBuilder builder) {
        AgentProxy.builder = builder;
    }
    
    /*
     * For testing purposes only.
     */
    static void setTimeoutTimer(Timer timeoutTimer) {
        AgentProxy.timeoutTimer = timeoutTimer;
    }
}

