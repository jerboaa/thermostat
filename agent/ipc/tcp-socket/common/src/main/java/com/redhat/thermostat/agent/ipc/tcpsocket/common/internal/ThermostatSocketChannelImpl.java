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

package com.redhat.thermostat.agent.ipc.tcpsocket.common.internal;

import java.net.SocketAddress;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ByteChannel;
import java.nio.channels.SocketChannel;
import java.nio.channels.spi.AbstractInterruptibleChannel;

public class ThermostatSocketChannelImpl implements ByteChannel {
    
    private static TcpSocketChannelHelper channelHelper = new TcpSocketChannelHelper();
    
    protected final SocketChannel impl;
    private final String name;
    
    protected ThermostatSocketChannelImpl(String name, SocketChannel impl) {
        this.name = name;
        this.impl = impl;
    }
    
    public static ThermostatSocketChannelImpl open(String name, SocketAddress addr) throws IOException {
        SocketChannel impl = channelHelper.open(addr);
        return new ThermostatSocketChannelImpl(name, impl);
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
    static class TcpSocketChannelHelper {
        SocketChannel open(final SocketAddress addr) throws IOException {
            return SocketChannel.open(addr);
        }
        
        int read(final SocketChannel channel, final ByteBuffer dst) throws IOException {
            return channel.read(dst);
        }
        
        int write(final SocketChannel channel, final ByteBuffer src) throws IOException {
            return channel.write(src);
        }
        
        boolean isOpen(final SocketChannel channel) {
            return channel.isOpen();
        }
        
        void close(final AbstractInterruptibleChannel channel) throws IOException {
            channel.close();
        }
        
        ////SocketAddress createAddress(File path) throws IOException {
        //    return new TcpSocketAddress(path);
        //}
    }
    
    public static void setChannelHelper(TcpSocketChannelHelper helper) {
        ThermostatSocketChannelImpl.channelHelper = helper;
    }
    
}
