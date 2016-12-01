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

package com.redhat.thermostat.agent.ipc.tcpsocket.server.internal;

import java.io.IOException;
import java.net.SocketAddress;
import java.nio.channels.Selector;
import java.nio.channels.spi.SelectorProvider;
import java.nio.file.attribute.UserPrincipal;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

import com.redhat.thermostat.agent.ipc.common.internal.IPCProperties;
import com.redhat.thermostat.agent.ipc.common.internal.IPCType;
import com.redhat.thermostat.agent.ipc.server.ServerTransport;
import com.redhat.thermostat.agent.ipc.server.ThermostatIPCCallbacks;
import com.redhat.thermostat.agent.ipc.tcpsocket.common.internal.TcpSocketIPCProperties;
import com.redhat.thermostat.common.utils.LoggingUtils;

class TcpSocketServerTransport implements ServerTransport {
    
    private static final Logger logger = LoggingUtils.getLogger(TcpSocketServerTransport.class);
    
    private final SelectorProvider selectorProvider;
    // Access/modification of this field should by synchronized
    private final Map<String, ThermostatServerSocketChannelImpl> sockets;
    private final ExecutorService execService;
    private final ChannelUtils channelUtils;
    private final ThreadCreator threadCreator;
    
    private TcpSocketIPCProperties props;
    private AcceptThread acceptThread;
    private Selector selector;
    
    TcpSocketServerTransport(SelectorProvider selectorProvider) {
        this(selectorProvider, Executors.newFixedThreadPool(determineDefaultThreadPoolSize(), new CountingThreadFactory()), 
                new ThreadCreator(), new ChannelUtils());
    }
    
    TcpSocketServerTransport(SelectorProvider selectorProvider, ExecutorService execService,
            ThreadCreator threadCreator, ChannelUtils channelCreator) {
        this.selectorProvider = selectorProvider;
        this.sockets = new HashMap<>();
        this.execService = execService;
        this.channelUtils = channelCreator;
        this.threadCreator = threadCreator;
    }
    
    @Override
    public void start(IPCProperties props) throws IOException {
        if (!(props instanceof TcpSocketIPCProperties)) {
            IPCType type = props.getType();
            throw new IOException("Unsupported IPC type: " + type.getConfigValue());
        }
        this.props = (TcpSocketIPCProperties) props;
        // Prepare socket directory with strict permissions, which will contain the socket file when bound
        
        // Open the Selector and start accepting connections
        this.selector = selectorProvider.openSelector();
        this.acceptThread = threadCreator.createAcceptThread(selector, execService);
        //acceptThread.start();
        logger.info("Agent IPC service started");
    }
    
    @Override
    public IPCType getType() {
        return IPCType.TCP_SOCKET;
    }


    private void checkName(String name) throws IOException {
        Objects.requireNonNull(name, "Server name cannot be null");
        if (name.isEmpty()) {
            throw new IOException("Server name cannot be empty");
        }
    }

    @Override
    public synchronized void createServer(String name, ThermostatIPCCallbacks callbacks) throws IOException {
        // Check if the socket has already been created and we know about it
        if (sockets.containsKey(name)) {
            throw new IOException("IPC server with name \"" + name + "\" already exists");
        }

        // TODO - check permissions, owner, etc if appropriate for protocol
        
        final SocketAddress addr = this.props.getSocketAddr(name);
        
        // Create socket
        ThermostatServerSocketChannelImpl socket = 
                channelUtils.createServerSocketChannel(name, addr, callbacks, props, selector, acceptThread);

        sockets.put(name, socket);
        if (!acceptThread.isAlive())
            acceptThread.start();
    }
    
    @Override
    public void createServer(String name, ThermostatIPCCallbacks callbacks, UserPrincipal owner) throws IOException {
        // UserPrincipal is unused in TCP implementation
        createServer(name, callbacks);
    }
    
    @Override
    public synchronized boolean serverExists(String name) throws IOException {
        return sockets.containsKey(name);
    }     
    
    @Override
    public synchronized void destroyServer(String name) throws IOException {
        if (!sockets.containsKey(name)) {
            throw new IOException("IPC server with name \"" + name + "\" does not exist");
        }
        // Remove socket from known sockets
        ThermostatServerSocketChannelImpl socket = sockets.remove(name);
        
        try {
            // Close socket
            socket.close();
        } finally {
          ;
        }
    }

    @Override
    public void shutdown() throws IOException {
        // Stop accepting connections and close selector afterward
        acceptThread.shutdown();
        channelUtils.closeSelector(selector);
        logger.info("Agent IPC service stopped");
    }
    
    private static int determineDefaultThreadPoolSize() {
        // Make the number of default thread pool size a function of available
        // processors.
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
 
    
    /* For testing purposes */
    Map<String, ThermostatServerSocketChannelImpl> getSockets() {
        return sockets;
    }

    /* for testing purposes */
    Thread getAcceptThread() { return acceptThread; }

    
    /* For testing purposes */
    static class ThreadCreator {
        AcceptThread createAcceptThread(Selector selector, ExecutorService execService) {
            return new AcceptThread(selector, execService);
        }
    }
    
    /* For testing purposes */
    static class ChannelUtils {
        ThermostatServerSocketChannelImpl createServerSocketChannel(String name, SocketAddress addr, 
                ThermostatIPCCallbacks callbacks, IPCProperties props, Selector selector, AcceptThread acceptThread) throws IOException {
            return ThermostatServerSocketChannelImpl.open(name, addr, callbacks, props, selector, acceptThread);
        }
        void closeSelector(Selector selector) throws IOException {
            selector.close();
        }
    }
    
    /* For testing purposes */
    static class ProcessCreator {
        Process startProcess(ProcessBuilder builder) throws IOException {
            return builder.start();
        }
    }
}
