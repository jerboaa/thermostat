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

package com.redhat.thermostat.agent.ipc.unixsocket.server.internal;

import java.io.File;
import java.io.IOException;
import java.nio.channels.Channel;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.spi.AbstractInterruptibleChannel;
import java.nio.channels.spi.AbstractSelectableChannel;

import com.redhat.thermostat.agent.ipc.server.ThermostatIPCCallbacks;

import jnr.unixsocket.UnixServerSocketChannel;
import jnr.unixsocket.UnixSocketAddress;
import jnr.unixsocket.UnixSocketChannel;

class ThermostatLocalServerSocketChannelImpl implements Channel {
    
    // See manual for unix(7)
    private static final int UNIX_PATH_MAX = 108;
    
    private static UnixServerSocketChannelHelper channelHelper = new UnixServerSocketChannelHelper();
    
    private final String name;
    private final UnixServerSocketChannel impl;
    private final File socketFile;
    private final ThermostatIPCCallbacks callbacks;
    private final Selector selector;
    
    private SelectionKey key;
    
    ThermostatLocalServerSocketChannelImpl(String name, UnixServerSocketChannel impl, File socketFile, 
            ThermostatIPCCallbacks callbacks, Selector selector, SelectionKey key) {
        this.name = name;
        this.impl = impl;
        this.socketFile = socketFile;
        this.callbacks = callbacks;
        this.selector = selector;
        this.key = key;
    }

    static ThermostatLocalServerSocketChannelImpl open(String name, File path, ThermostatIPCCallbacks callbacks, 
            Selector selector) throws IOException {
        if (path.getAbsolutePath().length() > UNIX_PATH_MAX) {
            throw new IOException("Socket path name is too long");
        }
        
        // Fail early if socket file exists
        if (path.exists()) {
            throw new IOException("Socket file already exists");
        }
        
        UnixServerSocketChannel impl = channelHelper.open();
        UnixSocketAddress addr = channelHelper.createAddress(path);
        // Bind to socket file
        channelHelper.bind(impl, addr);
        // Set non-blocking
        channelHelper.configureBlocking(impl, false);
        // Register for selection
        SelectionKey key = channelHelper.register(impl, selector, SelectionKey.OP_ACCEPT);
        // Attach wrapper socket to key, for use in select loop
        ThermostatLocalServerSocketChannelImpl sock =
                new ThermostatLocalServerSocketChannelImpl(name, impl, path, callbacks, selector, key);
        channelHelper.attachToKey(key, sock);
        // Send wakeup to trigger re-selection
        selector.wakeup();
        return sock;
    }
     
    public boolean isOpen() {
        return channelHelper.isOpen(impl);
    }
    
    private void unregister() {
        key.cancel();
    }
    
    AcceptedLocalSocketChannelImpl accept() throws IOException {
        if (!isOpen()) {
            throw new IOException("Socket is closed");
        }
        UnixSocketChannel clientImpl = impl.accept();
        // Set non-blocking
        channelHelper.configureBlocking(clientImpl, false);
        // Register for selection
        SelectionKey key = channelHelper.register(clientImpl, selector, SelectionKey.OP_READ);
        return new AcceptedLocalSocketChannelImpl(name, clientImpl, key);
    }

    File getSocketFile() {
        return socketFile;
    }
    
    ThermostatIPCCallbacks getCallbacks() {
        return callbacks;
    }
    
    String getName() {
        return name;
    }
    
    public void close() throws IOException {
        unregister();
        channelHelper.close(impl);
    }
    
    // ---- For testing purposes ----
    
    // Wraps methods that can't be mocked
    static class UnixServerSocketChannelHelper {
        UnixServerSocketChannel open() throws IOException {
            return UnixServerSocketChannel.open();
        }
        
        void bind(UnixServerSocketChannel channel, UnixSocketAddress addr) throws IOException {
            channel.socket().bind(addr);
        }
        
        SelectionKey register(SelectableChannel channel, Selector sel, int ops) throws IOException {
            return channel.register(sel, ops);
        }
        
        SelectableChannel configureBlocking(AbstractSelectableChannel channel, boolean block) throws IOException {
            return channel.configureBlocking(block);
        }
        
        Object attachToKey(SelectionKey key, Object attachment) {
            return key.attach(attachment);
        }
        
        boolean isOpen(UnixServerSocketChannel channel) {
            return channel.isOpen();
        }
        
        void close(AbstractInterruptibleChannel channel) throws IOException {
            channel.close();
        }
        
        UnixSocketAddress createAddress(File path) {
            return new UnixSocketAddress(path);
        }
    }
    
    static void setChannelHelper(UnixServerSocketChannelHelper helper) {
        ThermostatLocalServerSocketChannelImpl.channelHelper = helper;
    }
    
}
