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

import java.io.File;
import java.io.IOException;

import java.net.SocketAddress;
import java.net.InetSocketAddress;
import java.net.InetAddress;

import java.util.Properties;

import com.redhat.thermostat.agent.ipc.common.internal.IPCProperties;
import com.redhat.thermostat.agent.ipc.common.internal.IPCType;

public class TcpSocketIPCProperties extends IPCProperties {
    
    private static final String PROP_TCP_PREFIX = "";
    private static final String PROP_TCP_SUFFIX = ".tcpsocket.port";

    private final Properties props;
    
    TcpSocketIPCProperties(Properties props, File propFile) throws IOException {
        this(props, propFile, new PathUtils());
    }

    TcpSocketIPCProperties(Properties props, File propFile, PathUtils pathUtils) throws IOException {
        super(IPCType.TCP_SOCKET, propFile);
        this.props = props;
    }
    
    public SocketAddress getSocketAddr(final String serverName) throws IOException {
        final String propName = getPropertyNameFromServerName(serverName);
        final String sockPortStr = props.getProperty(propName);
        if (sockPortStr == null)
          throw new IOException("Property '"+propName+"' not found for server '"+serverName+"'.");
        final int sockPort;
        try {
          sockPort = Integer.parseInt(sockPortStr);
        } catch (NumberFormatException e) {
          throw new IOException("Invalid port '"+sockPortStr+"' specified for property '"+propName+"'.");
        }
        // this code works on Java 1.6, but forces the use of IPV4; localaddr = InetAddress.getLoopbackAddress() is a better option for java 1.7+
        final byte[] loopbackAddr = new byte[] { 127, 0, 0, 1 };
        final InetAddress localhost = InetAddress.getByAddress(loopbackAddr);
        return new InetSocketAddress(localhost,sockPort);
    }
    
    public static String getPropertyNameFromServerName(final String serverName) {
      return (serverName == null || serverName.isEmpty()) 
                ? PROP_TCP_PREFIX + "default" + PROP_TCP_SUFFIX
                : PROP_TCP_PREFIX + serverName + PROP_TCP_SUFFIX;
    }
    
    // Helper class for testing purposes
    static class PathUtils {
        String getSystemProperty(String name) {
            return System.getProperty(name);
        }
        String getEnvironmentVariable(String name) {
            return System.getenv(name);
        }
    }
}
