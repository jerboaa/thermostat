/*
 * Copyright 2012, 2013 Red Hat, Inc.
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

package com.redhat.thermostat.agent.proxy.server;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.rmi.RemoteException;
import java.rmi.registry.Registry;
import java.util.Timer;
import java.util.TimerTask;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import com.redhat.thermostat.agent.proxy.common.AgentProxyListener;
import com.redhat.thermostat.agent.proxy.common.AgentProxyLogin;

public class AgentProxyTest {
    
    private AgentProxyNativeUtils nativeUtils;
    private RegistryUtils registryUtils;
    private Registry registry;
    private AgentProxyLogin loginStub;
    private AgentProxyListener listener;
    private Timer timeoutTimer;

    @Before
    public void setup() throws Exception {
        registry = mock(Registry.class);
        listener = mock(AgentProxyListener.class);
        when(registry.lookup(AgentProxyListener.REMOTE_PREFIX + "0")).thenReturn(listener);
        registryUtils = mock(RegistryUtils.class);
        when(registryUtils.getRegistry()).thenReturn(registry);
        loginStub = mock(AgentProxyLogin.class);
        when(registryUtils.exportObject(any(AgentProxyLogin.class))).thenReturn(loginStub);
        
        nativeUtils = mock(AgentProxyNativeUtils.class);
        ProcessUserInfoBuilder builder = mock(ProcessUserInfoBuilder.class);
        when(builder.build(0)).thenReturn(new UnixCredentials(9000, 9001, 0));
        timeoutTimer = mock(Timer.class);
        AgentProxy.setRegistryUtils(registryUtils);
        AgentProxy.setNativeUtils(nativeUtils);
        AgentProxy.setProcessUserInfoBuilder(builder);
        AgentProxy.setTimeoutTimer(timeoutTimer);
    }
    
    @Test
    public void testMainSuccess() throws Exception {
        assertFalse(AgentProxy.isBound());
        
        // Invoke main with PID of 0
        AgentProxy.main(new String[] { "0" });
        
        assertTrue(AgentProxy.isBound());
        
        // Verify timeout set
        verify(timeoutTimer).schedule(any(TimerTask.class), any(Long.class));
        
        // Verify native library loaded and credentials properly set
        verify(nativeUtils).loadLibrary();
        verify(nativeUtils).setCredentials(9000, 9001);
        
        // Verify login object exported
        AgentProxyLogin proxyLogin = AgentProxy.getAgentProxyLogin();
        verify(registryUtils).exportObject(proxyLogin);
        verify(registry).rebind(AgentProxyLogin.REMOTE_PREFIX + "0", loginStub);
        
        // Verify listener notified with positive response
        verify(listener).serverStarted();
        
        // Shutdown server
        ShutdownListener shutdownListener = AgentProxy.getShutdownListener();
        shutdownListener.shutdown();
        
        // Verify login object unexported
        verify(registry).unbind(AgentProxyLogin.REMOTE_PREFIX + "0");
        verify(registryUtils).unexportObject(proxyLogin);
        
        assertFalse(AgentProxy.isBound());
    }
    
    @Test
    public void testMainFailure() throws Exception {
        // Simulate failure binding the login object
        RemoteException ex = new RemoteException("TEST");
        doThrow(ex).when(registry).rebind(AgentProxyLogin.REMOTE_PREFIX + "0", loginStub);
        
        // Invoke main with PID of 0
        AgentProxy.main(new String[] { "0" });
        
        // Verify listener notified with negative response
        ArgumentCaptor<Exception> errorCaptor = ArgumentCaptor.forClass(Exception.class);
        verify(listener).serverFailedToStart(errorCaptor.capture());
        assertEquals(ex, errorCaptor.getValue().getCause());
        
        assertFalse(AgentProxy.isBound());
    }

}
