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

package com.redhat.thermostat.agent.proxy.server;

import java.io.IOException;
import java.io.PrintStream;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.redhat.thermostat.common.utils.LoggingUtils;
import com.sun.tools.attach.AttachNotSupportedException;

public class AgentProxy {
    
    private static final Logger logger = LoggingUtils.getLogger(AgentProxy.class);
    
    private static int pid = -1;
    private static AgentProxyControlImpl agent = null;
    private static ControlCreator creator = new ControlCreator();
    private static PrintStream outStream = System.out;
    
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
        
        // Start proxy agent
        agent = creator.create(pid);
        
        try {
            agent.attach();
        } catch (AttachNotSupportedException | IOException e) {
            logger.log(Level.SEVERE, "Failed to attach to VM (pid: " + pid + ")", e);
            return;
        }
        
        try {
            String connectorAddress = agent.getConnectorAddress();
            outStream.println(connectorAddress);
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Failed to retrieve JMX connection URL", e);
        }
        
        try {
            agent.detach();
        } catch (IOException e) {
            logger.log(Level.WARNING, "Failed to detach from VM (pid: " + pid + ")", e); 
        }
    }

    private static void usage() {
        throw new RuntimeException("usage: java " + AgentProxy.class.getName() + " <pidOfTargetJvm>");
    }
    
    static class ControlCreator {
        AgentProxyControlImpl create(int pid) {
            return new AgentProxyControlImpl(pid);
        }
    }
    
    /*
     * For testing purposes only.
     */
    static void setControlCreator(ControlCreator creator) {
        AgentProxy.creator = creator;
    }
    
    /*
     * For testing purposes only.
     */
    static void setOutStream(PrintStream stream) {
        AgentProxy.outStream = stream;
    }
    
}

