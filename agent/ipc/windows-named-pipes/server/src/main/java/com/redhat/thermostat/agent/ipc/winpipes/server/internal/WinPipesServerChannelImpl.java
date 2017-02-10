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
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

import com.redhat.thermostat.agent.ipc.server.ThermostatIPCCallbacks;
import com.redhat.thermostat.agent.ipc.winpipes.common.internal.WinPipe;
import com.redhat.thermostat.agent.ipc.winpipes.common.internal.WinPipesIPCProperties;

/**
 * Creates the named pipe.
 * There is one channel per pipe name.
 * Creates and starts the accept thread for this pipe.
 */
class WinPipesServerChannelImpl {

    private WinPipesServerChannelHelper channelHelper = new WinPipesServerChannelHelper();

    private static final ThreadCreator threadCreator = new ThreadCreator();

    // unadorned name of this server (not full pipe name)
    private final String name;

    // WinPipes implementation
    private final WinPipe pipe;

    // callbacks on message completion
    private final ThermostatIPCCallbacks callbacks;

    private WinPipesServerChannelImpl(String name, WinPipe pipe, ThermostatIPCCallbacks callbacks) {
        this(name, pipe, callbacks, new WinPipesServerChannelHelper());
    }
    private WinPipesServerChannelImpl(String name, WinPipe pipe, ThermostatIPCCallbacks callbacks, WinPipesServerChannelHelper helper) {
        this.name = name;
        this.pipe = pipe;
        this.callbacks = callbacks;
        this.channelHelper = helper;
        ExecutorService execService = Executors.newFixedThreadPool(determineDefaultThreadPoolSize(), new CountingThreadFactory());
        AcceptThread acceptThread = threadCreator.createAcceptThread(this, execService);
        acceptThread.start();
    }

    static WinPipesServerChannelImpl createChannel(String name, ThermostatIPCCallbacks callbacks,
                                                   WinPipesIPCProperties props) throws IOException {
        return createChannel(name, callbacks, props, new WinPipesServerChannelHelper());
    }

    static WinPipesServerChannelImpl createChannel(String name, ThermostatIPCCallbacks callbacks,
                                                   WinPipesIPCProperties props, WinPipesServerChannelHelper helper) throws IOException {
        final String pipeName = props.getPipeName(name);
        final WinPipe pipe = helper.open(pipeName);
        return helper.createServerChannel(pipeName, pipe, callbacks);
    }
     
    boolean isOpen() {
        return getChannelHelper().isOpen(getPipe());
    }

    WinPipe getPipe() {
        return pipe;
    }
    
    ThermostatIPCCallbacks getCallbacks() {
        return callbacks;
    }
    
    String getName() {
        return name;
    }
    
    public void close() throws IOException {
        getChannelHelper().close(getPipe());
    }

    /* For testing purposes */
    static class ThreadCreator {
        AcceptThread createAcceptThread(WinPipesServerChannelImpl channel, ExecutorService execService) {
            return new AcceptThread(channel, execService);
        }
    }

    WinPipesServerChannelHelper getChannelHelper() {
        return channelHelper;
    }

    // Wraps methods that can't be mocked
    static class WinPipesServerChannelHelper {
        WinPipe open(final String name) throws IOException {
            return new WinPipe(name);
        }

        WinPipesServerChannelImpl createServerChannel(String pipeName, WinPipe pipe, ThermostatIPCCallbacks callbacks) {
            return new WinPipesServerChannelImpl(pipeName, pipe, callbacks, this);
        }

        boolean isOpen(WinPipe pipe) {
            return pipe.isOpen();
        }
        
        void close(WinPipe pipe) throws IOException {
            pipe.close();
        }
    }

    private static int determineDefaultThreadPoolSize() {
        // Make the number of default thread pool size a function of available processors.
        return Runtime.getRuntime().availableProcessors() * 2;
    }

    private static class CountingThreadFactory implements ThreadFactory {

        private final AtomicInteger threadCount;

        private CountingThreadFactory() {
            this.threadCount = new AtomicInteger();
        }

        @Override
        public Thread newThread(Runnable r) {
            // Create threads with a recognizable name
            return new Thread(r, "AcceptThread-" + threadCount.incrementAndGet());
        }
    }
    
}
