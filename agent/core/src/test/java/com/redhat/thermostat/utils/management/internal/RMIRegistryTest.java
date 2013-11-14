package com.redhat.thermostat.utils.management.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.net.InetAddress;
import java.rmi.registry.Registry;
import java.rmi.server.RMIClientSocketFactory;
import java.rmi.server.RMIServerSocketFactory;
import java.rmi.server.RMISocketFactory;

import org.junit.Test;
import org.mockito.ArgumentCaptor;

import com.redhat.thermostat.utils.management.internal.RMIRegistry.RegistryCreator;
import com.redhat.thermostat.utils.management.internal.RMIRegistry.ServerSocketCreator;

public class RMIRegistryTest {
    
    @Test
    public void testRegistryStart() throws IOException {
        RegistryCreator creator = mock(RegistryCreator.class);
        Registry reg = mock(Registry.class);
        when(creator.createRegistry(anyInt(), any(RMIClientSocketFactory.class), 
                any(RMIServerSocketFactory.class))).thenReturn(reg);
        ServerSocketCreator sockCreator = mock(ServerSocketCreator.class);
        
        RMIRegistry registry = new RMIRegistry(creator, sockCreator);
        registry.start();
        
        ArgumentCaptor<Integer> portCaptor = ArgumentCaptor.forClass(Integer.class);
        ArgumentCaptor<RMIClientSocketFactory> csfCaptor = ArgumentCaptor.forClass(RMIClientSocketFactory.class);
        ArgumentCaptor<RMIServerSocketFactory> ssfCaptor = ArgumentCaptor.forClass(RMIServerSocketFactory.class);
        verify(creator).createRegistry(portCaptor.capture(), csfCaptor.capture(), ssfCaptor.capture());
        
        // Ensure defaults used for port and client socket factory
        int port = portCaptor.getValue();
        assertEquals(Registry.REGISTRY_PORT, port);
        
        RMIClientSocketFactory csf = csfCaptor.getValue();
        assertEquals(RMISocketFactory.getDefaultSocketFactory(), csf);
        
        // Ensure bound to loopback address
        RMIServerSocketFactory ssf = ssfCaptor.getValue();
        ssf.createServerSocket(port);
        verify(sockCreator).createSocket(port, 0, InetAddress.getLoopbackAddress());
    }
    
    @Test
    public void testRegistryStop() throws IOException {
        RegistryCreator creator = mock(RegistryCreator.class);
        Registry reg = mock(Registry.class);
        when(creator.createRegistry(anyInt(), any(RMIClientSocketFactory.class), 
                any(RMIServerSocketFactory.class))).thenReturn(reg);
        ServerSocketCreator sockCreator = mock(ServerSocketCreator.class);
        
        RMIRegistry registry = new RMIRegistry(creator, sockCreator);
        registry.start();
        
        registry.stop();
        
        verify(creator).destroyRegistry(reg);
        assertNull(registry.getRegistryImpl());
    }
    
    @Test
    public void testRegistryStopNotStarted() throws IOException {
        RegistryCreator creator = mock(RegistryCreator.class);
        Registry reg = mock(Registry.class);
        when(creator.createRegistry(anyInt(), any(RMIClientSocketFactory.class), 
                any(RMIServerSocketFactory.class))).thenReturn(reg);
        ServerSocketCreator sockCreator = mock(ServerSocketCreator.class);
        
        RMIRegistry registry = new RMIRegistry(creator, sockCreator);
        
        registry.stop();
        
        verify(creator, never()).destroyRegistry(reg);
    }
    
}
