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

package com.redhat.thermostat.agent.ipc.winpipes.server.internal;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.redhat.thermostat.agent.ipc.winpipes.common.internal.MessageLimits;
import com.redhat.thermostat.agent.ipc.winpipes.common.internal.WinPipe;
import com.redhat.thermostat.common.utils.LoggingUtils;

class AcceptThread extends Thread {
    
    private static final Logger logger = LoggingUtils.getLogger(AcceptThread.class);
    private final ExecutorService execService;
    private final WinPipe pipe;
    private final WinPipesServerChannelImpl channel;

    // buffer size
    private static final int BUFSIZE = new MessageLimits().getBufferSize();

    // number of simulataneous clients
    private final int NUM_INSTANCES = 1;

    private final WindowsEventSelector selector;

    // array of all pipe instances
    private final ClientPipeInstance[] instances;
    
    private boolean shutdown;

    AcceptThread(WinPipesServerChannelImpl channel, ExecutorService execService) {
        this.channel = channel;
        this.execService = execService;
        this.pipe = channel.getPipe();
        this.shutdown = false;
        this.selector = new WindowsEventSelector(NUM_INSTANCES);
        this.instances = new ClientPipeInstance[NUM_INSTANCES];
    }

    private void createInstances() throws IOException {
        logger.info("AcceptThread '" + pipe.getPipeName() + "' creating " + NUM_INSTANCES + " pipe instances");
        for (int i = 0; i < NUM_INSTANCES && !shutdown; i++) {
            final ClientPipeInstance pi = new ClientPipeInstance(pipe.getPipeName(), NUM_INSTANCES, BUFSIZE, execService, channel.getCallbacks());
            instances[i] = pi;
            pi.connectToNewClient();
            logger.fine("AcceptThread '" + pipe.getPipeName() + "' created " + pi);
        }
    }

    @Override
    public void run() {

        try {
            createInstances();

            logger.info("AcceptThread '" + pipe.getPipeName() + "' Ready to accept client pipe connections");

            // normally usage would be to add on an enqueu and remove on a operation complete,
            // but we reuse the events here to save cycles
            for (final ClientPipeInstance pi : instances) {
                selector.add(pi.getReadHandler());
                selector.add(pi.getWriteHandler());
            }

            // main loop
            while (!shutdown) {
                WindowsEventSelector.EventHandler pi = selector.waitForEvent();
                pi.processEvent();
            }

        } catch (IOException e) {
            logger.log(Level.SEVERE, "Error occurred during selection", e);
            shutdown = true;
        } finally {
            logger.info("Shutting down");
            for (ClientPipeInstance pi : instances) {
                try {
                    pi.close();
                } catch (IOException e) {
                    logger.log(Level.SEVERE, "Error occurred during close() of " + pi.getName(), e);
                }
                selector.remove(pi.getReadHandler());
                selector.remove(pi.getWriteHandler());
            }
            execService.shutdown();
        }
    }

    void shutdown() throws IOException {
        this.shutdown = true;
        // Interrupt accept thread
        this.interrupt();
        
        try {
            this.join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
    
    boolean isShutdown() {
        return shutdown;
    }

}