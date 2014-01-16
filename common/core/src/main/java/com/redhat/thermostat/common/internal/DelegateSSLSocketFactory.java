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

package com.redhat.thermostat.common.internal;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;

import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

/**
 * This class allows us to configure the SSLSocket with the given parameters
 * before the socket it returned to the consumer.
 *
 */
public final class DelegateSSLSocketFactory extends SSLSocketFactory {

    private final SSLSocketFactory factoryDelegate;
    private final SSLParameters params;
    
    public DelegateSSLSocketFactory(SSLSocketFactory delegate, SSLParameters params) {
        this.factoryDelegate = delegate;
        this.params = params;
    }
    @Override
    public String[] getDefaultCipherSuites() {
        return factoryDelegate.getDefaultCipherSuites();
    }

    @Override
    public String[] getSupportedCipherSuites() {
        return factoryDelegate.getSupportedCipherSuites();
    }
    
    @Override
    public Socket createSocket() throws IOException {
        SSLSocket socket = (SSLSocket)factoryDelegate.createSocket();
        configureSocket(socket);
        return socket;
    }

    @Override
    public Socket createSocket(Socket s, String host, int port,
            boolean autoClose) throws IOException {
        SSLSocket socket = (SSLSocket)factoryDelegate.createSocket(s, host, port, autoClose);
        configureSocket(socket);
        return socket;
    }

    @Override
    public Socket createSocket(String host, int port) throws IOException,
            UnknownHostException {
        SSLSocket socket = (SSLSocket)factoryDelegate.createSocket(host, port);
        configureSocket(socket);
        return socket;
    }

    @Override
    public Socket createSocket(String host, int port,
            InetAddress localHost, int localPort) throws IOException,
            UnknownHostException {
        SSLSocket socket = (SSLSocket) factoryDelegate.createSocket(host,
                port, localHost, localPort);
        configureSocket(socket);
        return socket;
    }

    @Override
    public Socket createSocket(InetAddress host, int port)
            throws IOException {
        SSLSocket socket = (SSLSocket) factoryDelegate.createSocket(host,
                port);
        configureSocket(socket);
        return socket;
    }

    @Override
    public Socket createSocket(InetAddress address, int port,
            InetAddress localAddress, int localPort) throws IOException {
        SSLSocket socket = (SSLSocket) factoryDelegate.createSocket(
                address, port, localAddress, localPort);
        configureSocket(socket);
        return socket;
    }
    
    private void configureSocket(SSLSocket socket) throws IOException {
        // Disable Nagle's algorithm
        socket.setTcpNoDelay(true);
        socket.setSSLParameters(params);
    }
    
    
}

