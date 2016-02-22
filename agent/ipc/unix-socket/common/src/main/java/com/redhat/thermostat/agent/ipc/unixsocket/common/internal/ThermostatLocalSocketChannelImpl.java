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

package com.redhat.thermostat.agent.ipc.unixsocket.common.internal;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ByteChannel;
import java.nio.channels.SelectableChannel;
import java.nio.channels.spi.AbstractInterruptibleChannel;
import java.nio.channels.spi.AbstractSelectableChannel;

import jnr.unixsocket.UnixSocketAddress;
import jnr.unixsocket.UnixSocketChannel;

public class ThermostatLocalSocketChannelImpl implements ByteChannel {
    
    // If true, dumps header information for each header read/written
    private static final boolean DEBUG_HEADER = false;
    // Maximum size of message (excluding header) in bytes
    private static final int DEFAULT_MAX_MESSAGE_SIZE = 8092;
    // End of stream indicated by -1 from read
    private static final int EOF = -1;
    
    private static UnixSocketChannelHelper channelHelper = new UnixSocketChannelHelper();
    
    private final String name;
    private final UnixSocketChannel impl;
    private final int maxMessageSize;
    
    protected ThermostatLocalSocketChannelImpl(String name, UnixSocketChannel impl) {
        this(name, impl, DEFAULT_MAX_MESSAGE_SIZE);
    }
    
    ThermostatLocalSocketChannelImpl(String name, UnixSocketChannel impl, int maxMessageSize) {
        this.name = name;
        this.impl = impl;
        this.maxMessageSize = maxMessageSize;
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
        // Use slice of dst
        return doRead(dst.slice());
    }
    
    private int doRead(ByteBuffer dst) throws IOException {
        boolean done = false;
        int parts = 0;
        
        int read = 0;
        while (!done) {
            // Read enough of header to determine header size
            int minSize = MessageHeader.getMinimumHeaderSize();
            ByteBuffer buf = ByteBuffer.allocate(minSize);
            int headerRead = doBlockingRead(buf);
            if (headerRead < 0 && parts == 0) {
                // Nothing to read, EOF
                done = true;
                read = EOF;
            } else if (headerRead <= 0 && parts > 0) {
                throw new IOException("Premature end to multipart message");
            } else if (headerRead < minSize) {
                throw new IOException("Header size too short");
            } else {
                buf.position(0);
                MessageHeader header = MessageHeader.fromByteBuffer(buf);

                // Read remaining part of header
                int remaining = header.getHeaderSize() - minSize;
                buf = ByteBuffer.allocate(remaining);
                headerRead = doBlockingRead(buf);
                if (headerRead < 0) {
                    throw new IOException("EOF while reading message header");
                } else if (headerRead < remaining) {
                    throw new IOException("Message header is truncated");
                }
                
                buf.position(0);
                header.setRemainingFields(buf);

                if (DEBUG_HEADER) {
                    header.dumpHeader("[Read] ");
                }
                
                // Check there's enough space in dst buffer
                int messageSize = header.getMessageSize();
                // Set limit so we don't read more than header specifies
                int newLimit = dst.position() + messageSize;
                if (newLimit > dst.capacity()) {
                    throw new IOException("Provided buffer too small to read message");
                }
                dst.limit(newLimit);
                
                // Read payload
                int readPart = doBlockingRead(dst);
                if (readPart < 0) {
                    throw new IOException("EOF while reading message");
                } else if (readPart < messageSize) {
                    throw new IOException("Message payload is truncated");
                }
                
                // Check if there's more data to read
                done = !header.isMoreData();
                read += readPart;
                parts++;
            }
        }
        return read;
    }
    
    private int doBlockingRead(ByteBuffer buf) throws IOException {
        int bytesRead = 0;
        while ((bytesRead = channelHelper.read(impl, buf)) == 0);
        return bytesRead;
    }

    @Override
    public int write(ByteBuffer src) throws IOException {
        // Use slice of src
        return doWrite(src.slice());
    }
    
    private int doWrite(ByteBuffer src) throws IOException {
        // Available size in buffer is from current position to limit
        int totalSize = src.remaining();
        int written = 0;

        boolean done = false;
        while (!done) {
            // Prefix data with our MessageHeader
            MessageHeader header = new MessageHeader();
            // If size is larger than maxMessageSize, send in multiple parts
            int messageSize = maxMessageSize;
            if (totalSize <= maxMessageSize) {
                messageSize = totalSize;
                done = true;
            }
            totalSize -= messageSize;
            header.setMessageSize(messageSize);
            header.setMoreData(!done);
            
            byte[] headerBytes = header.toByteArray();
            ByteBuffer headerBuf = ByteBuffer.allocate(headerBytes.length);
            headerBuf.put(headerBytes);
            headerBuf.position(0);
            
            if (DEBUG_HEADER) {
                header.dumpHeader("[Write] ");
            }
            
            channelHelper.write(impl, headerBuf);

            // Write at most MAX_MESSAGE_SIZE
            src.limit(src.position() + messageSize);
            
            // Write payload
            int writtenPart = channelHelper.write(impl, src);
            
            written += writtenPart;
        }
        return written;
    }
    
    public void configureBlocking(boolean block) throws IOException {
        channelHelper.configureBlocking(impl, block);
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
        
        SelectableChannel configureBlocking(AbstractSelectableChannel channel, boolean block) throws IOException {
            return channel.configureBlocking(block);
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
