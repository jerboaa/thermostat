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

package com.redhat.thermostat.utils.management.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.rmi.RemoteException;
import java.rmi.registry.Registry;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.Before;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import com.redhat.thermostat.agent.internal.RMIRegistryImpl;
import com.redhat.thermostat.agent.proxy.common.AgentProxyControl;
import com.redhat.thermostat.agent.proxy.common.AgentProxyListener;
import com.redhat.thermostat.agent.proxy.common.AgentProxyLogin;
import com.redhat.thermostat.common.tools.ApplicationException;
import com.redhat.thermostat.utils.management.internal.AgentProxyClient.ProcessCreator;

public class AgentProxyClientTest {
    
    private AgentProxyClient client;
    private RMIRegistryImpl rmi;
    private Registry registry;
    private ProcessCreator procCreator;
    private CountDownLatch latch;
    private AgentProxyListener listenerStub;
    private AgentProxyLogin proxyLogin;
    private AgentProxyControl proxyControl;
    private File binPath;
    
    @Before
    public void setup() throws Exception {
        rmi = mock(RMIRegistryImpl.class);
        listenerStub = mock(AgentProxyListener.class);
        when(rmi.export(any(AgentProxyListener.class))).thenReturn(listenerStub);
        registry = mock(Registry.class);
        when(rmi.getRegistry()).thenReturn(registry);
        proxyLogin = mock(AgentProxyLogin.class);
        when(registry.lookup(AgentProxyLogin.REMOTE_PREFIX + "0")).thenReturn(proxyLogin);
        proxyControl = mock(AgentProxyControl.class);
        when(proxyLogin.login()).thenReturn(proxyControl);
        
        procCreator = mock(ProcessCreator.class);
        binPath = new File("/path/to/thermostat/bin");
        latch = mock(CountDownLatch.class);
    }
    
    @Test
    public void testCreateProxy() throws Exception {
        createClient();
        
        // Verify listener exported and bound
        verify(rmi).export(client);
        verify(registry).rebind(AgentProxyListener.REMOTE_PREFIX + "0", listenerStub);
        
        // Verify server created
        String progName = "/path/to/thermostat/bin" + File.separator + "thermostat-agent-proxy";
        verify(procCreator).createAndRunProcess(new String[] { progName, "0" });
        verify(latch).countDown();
        
        // Verify listener removed
        verify(registry).unbind(AgentProxyListener.REMOTE_PREFIX + "0");
        verify(rmi).unexport(client);
        
        // Verify login
        verify(registry).lookup(AgentProxyLogin.REMOTE_PREFIX + "0");
        verify(proxyLogin).login();
        
        // Check returned proxy control
        assertEquals(proxyControl, client.getProxy());
    }

    private void createClient() throws InterruptedException, IOException,
            ApplicationException {
        client = new AgentProxyClient(rmi, 0, binPath, latch, procCreator);
        
        doAnswer(new Answer<Boolean>() {
            @Override
            public Boolean answer(InvocationOnMock invocation) throws Throwable {
                // Trigger server started
                client.serverStarted();
                return true;
            }
        }).when(latch).await(any(Long.class), any(TimeUnit.class));
        
        client.createProxy();
    }
    
    @Test
    public void testCreateProxyFailed() throws Exception {
        client = new AgentProxyClient(rmi, 0, binPath, latch, procCreator);
        
        final Exception error = mock(Exception.class);
        doAnswer(new Answer<Boolean>() {
            @Override
            public Boolean answer(InvocationOnMock invocation) throws Throwable {
                // Trigger server started
                client.serverFailedToStart(error);
                return true;
            }
        }).when(latch).await(any(Long.class), any(TimeUnit.class));
        
        try {
            client.createProxy();
            fail("Expected RemoteException");
        } catch (RemoteException e) {
            assertEquals(error, e.getCause());
        }
        
        // Verify listener exported and bound
        verify(rmi).export(client);
        verify(registry).rebind(AgentProxyListener.REMOTE_PREFIX + "0", listenerStub);
        
        // Verify server created
        String progName = "/path/to/thermostat/bin" + File.separator + "thermostat-agent-proxy";
        verify(procCreator).createAndRunProcess(new String[] { progName, "0" });
        verify(latch).countDown();
        
        // Verify listener removed
        verify(registry).unbind(AgentProxyListener.REMOTE_PREFIX + "0");
        verify(rmi).unexport(client);
    }
    
    @Test
    public void testCreateProxyTimeout() throws Exception {
        when(latch.await(any(Long.class), any(TimeUnit.class))).thenReturn(false);
        client = new AgentProxyClient(rmi, 0, binPath, latch, procCreator);
        
        try {
            client.createProxy();
            fail("Expected RemoteException");
        } catch (RemoteException e) {
            // Verify listener exported and bound
            verify(rmi).export(client);
            verify(registry).rebind(AgentProxyListener.REMOTE_PREFIX + "0", listenerStub);
            
            // Verify server created
            String progName = "/path/to/thermostat/bin" + File.separator + "thermostat-agent-proxy";
            verify(procCreator).createAndRunProcess(new String[] { progName, "0" });
            
            // Verify listener removed
            verify(registry).unbind(AgentProxyListener.REMOTE_PREFIX + "0");
            verify(rmi).unexport(client);
        }
    }
    
    @Test
    public void testAttach() throws Exception {
        createClient();
        
        client.attach();
        verify(proxyControl).attach();
    }
    
    @Test
    public void testIsAttached() throws Exception {
        createClient();
        when(proxyControl.isAttached()).thenReturn(true);
        
        boolean result = client.isAttached();
        verify(proxyControl).isAttached();
        assertTrue(result);
    }
    
    @Test
    public void testDetach() throws Exception {
        createClient();
        
        client.detach();
        verify(proxyControl).detach();
    }
    
}

