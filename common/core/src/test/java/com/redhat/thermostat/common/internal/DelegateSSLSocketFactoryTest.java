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

package com.redhat.thermostat.common.internal;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.net.InetAddress;

import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class DelegateSSLSocketFactoryTest {

    private SSLSocketFactory mockDelegate;
    private SSLSocket socket;
    private SSLParameters params;
    
    @Before
    public void setup() {
        mockDelegate = mock(SSLSocketFactory.class);
        socket = mock(SSLSocket.class);
        params = mock(SSLParameters.class);
    }
    
    @After
    public void teardown() {
        mockDelegate = null;
        socket = null;
        params = null;
    }
    
    @Test
    public void getDefaultCipherSuitesDelegates() {
        DelegateSSLSocketFactory factory = new DelegateSSLSocketFactory(mockDelegate, params);
        factory.getDefaultCipherSuites();
        verify(mockDelegate).getDefaultCipherSuites();
        verifyNoMoreInteractions(mockDelegate);
    }
    
    @Test
    public void getSupportedCipherSuitesDelegates() {
        DelegateSSLSocketFactory factory = new DelegateSSLSocketFactory(mockDelegate, params);
        factory.getSupportedCipherSuites();
        verify(mockDelegate).getSupportedCipherSuites();
        verifyNoMoreInteractions(mockDelegate);
    }
    
    @Test
    public void createSocketConfiguresSocket() throws IOException {
        DelegateSSLSocketFactory factory = new DelegateSSLSocketFactory(mockDelegate, params);
        when(mockDelegate.createSocket()).thenReturn(socket);
        factory.createSocket();
        verify(socket).setSSLParameters(params);
    }
    
    @Test
    public void createSocketConfiguresSocket2() throws IOException {
        DelegateSSLSocketFactory factory = new DelegateSSLSocketFactory(mockDelegate, params);
        when(mockDelegate.createSocket(null, "blah", 2, true)).thenReturn(socket);
        factory.createSocket(null, "blah", 2, true);
        verify(socket).setSSLParameters(params);
    }
    
    @Test
    public void createSocketConfiguresSocket3() throws IOException {
        DelegateSSLSocketFactory factory = new DelegateSSLSocketFactory(mockDelegate, params);
        when(mockDelegate.createSocket("testhost.example.com", 2)).thenReturn(socket);
        factory.createSocket("testhost.example.com", 2);
        verify(socket).setSSLParameters(params);
    }
    
    @Test
    public void createSocketConfiguresSocket4() throws IOException {
        DelegateSSLSocketFactory factory = new DelegateSSLSocketFactory(mockDelegate, params);
        when(mockDelegate.createSocket("testhost.example.com", 2, null, 3)).thenReturn(socket);
        factory.createSocket("testhost.example.com", 2, null, 3);
        verify(socket).setSSLParameters(params);
    }
    
    @Test
    public void createSocketConfiguresSocket5() throws IOException {
        DelegateSSLSocketFactory factory = new DelegateSSLSocketFactory(mockDelegate, params);
        InetAddress addr = mock(InetAddress.class);
        when(mockDelegate.createSocket(addr, 2)).thenReturn(socket);
        factory.createSocket(addr, 2);
        verify(socket).setSSLParameters(params);
    }
    
    @Test
    public void createSocketConfiguresSocket6() throws IOException {
        DelegateSSLSocketFactory factory = new DelegateSSLSocketFactory(mockDelegate, params);
        InetAddress addr = mock(InetAddress.class);
        when(mockDelegate.createSocket(addr, 2, addr, 3)).thenReturn(socket);
        factory.createSocket(addr, 2, addr, 3);
        verify(socket).setSSLParameters(params);
    }
    
}

