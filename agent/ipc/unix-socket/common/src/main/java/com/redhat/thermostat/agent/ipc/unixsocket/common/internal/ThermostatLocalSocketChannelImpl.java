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

package com.redhat.thermostat.agent.ipc.unixsocket.common.internal;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ByteChannel;
import java.nio.channels.spi.AbstractInterruptibleChannel;

import jnr.unixsocket.UnixSocketAddress;
import jnr.unixsocket.UnixSocketChannel;

public class ThermostatLocalSocketChannelImpl implements ByteChannel {
    
    private static UnixSocketChannelHelper channelHelper = new UnixSocketChannelHelper();
    
    protected final UnixSocketChannel impl;
    private final String name;
    
    protected ThermostatLocalSocketChannelImpl(String name, UnixSocketChannel impl) {
        this.name = name;
        this.impl = impl;
    }
    
    public static ThermostatLocalSocketChannelImpl open(String name, File path) throws IOException {
        UnixSocketAddress addr = channelHelper.createAddress(path);
        UnixSocketChannel impl = channelHelper.open(addr);
        return new ThermostatLocalSocketChannelImpl(name, impl);
    }
    
    public String getName() {
        return name;
    }

    @Override
    public int read(ByteBuffer dst) throws IOException {
        return channelHelper.read(impl, dst);
    }
    
    public int write(ByteBuffer src) throws IOException {
        return channelHelper.write(impl, src);
    }
    
    @Override
    public boolean isOpen() {
        return channelHelper.isOpen(impl);
    }

    @Override
    public void close() throws IOException {
        channelHelper.close(impl);
    }
    
    // ---- For testing purposes ----
    
    // Wraps methods that can't be mocked
    static class UnixSocketChannelHelper {
        UnixSocketChannel open(UnixSocketAddress addr) throws IOException {
            return UnixSocketChannel.open(addr);
        }
        
        int read(UnixSocketChannel channel, ByteBuffer dst) throws IOException {
            return channel.read(dst);
        }
        
        int write(UnixSocketChannel channel, ByteBuffer src) throws IOException {
            return channel.write(src);
        }
        
        boolean isOpen(UnixSocketChannel channel) {
            return channel.isOpen();
        }
        
        void close(AbstractInterruptibleChannel channel) throws IOException {
            channel.close();
        }
        
        UnixSocketAddress createAddress(File path) throws IOException {
            return new UnixSocketAddress(path);
        }
    }
    
    public static void setChannelHelper(UnixSocketChannelHelper helper) {
        ThermostatLocalSocketChannelImpl.channelHelper = helper;
    }
    
}
