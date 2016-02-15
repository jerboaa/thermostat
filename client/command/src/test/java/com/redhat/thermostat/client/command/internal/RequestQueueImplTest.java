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

package com.redhat.thermostat.client.command.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.InetSocketAddress;
import java.util.Arrays;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.redhat.thermostat.common.command.Request;
import com.redhat.thermostat.common.command.RequestResponseListener;
import com.redhat.thermostat.common.command.Response;
import com.redhat.thermostat.common.command.Response.ResponseType;
import com.redhat.thermostat.storage.core.AuthToken;
import com.redhat.thermostat.storage.core.SecureStorage;
import com.redhat.thermostat.storage.core.Storage;
import com.redhat.thermostat.storage.core.StorageException;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ FrameworkUtil.class })
public class RequestQueueImplTest {

    private BundleContext mockContext;
    private ServiceReference mockServiceRef;
    
    @Before
    public void setup() {
        PowerMockito.mockStatic(FrameworkUtil.class);
        Bundle mockBundle = mock(Bundle.class);
        mockContext = mock(BundleContext.class);
        mockServiceRef = mock(ServiceReference.class);
        when(mockContext.getServiceReference(Storage.class.getName())).thenReturn(mockServiceRef);
        when(mockBundle.getBundleContext()).thenReturn(mockContext);
        when(FrameworkUtil.getBundle(RequestQueueImpl.class)).thenReturn(mockBundle);
    }

    @Test
    public void putRequestRejectsRequestWithNullTarget() {
        Request request = createRequest(null, "foobar");
        ConfigurationRequestContext ctx = mock(ConfigurationRequestContext.class);
        RequestQueueImpl queue = new RequestQueueImpl(ctx);
        try {
            queue.putRequest(request);
            fail("expected assertion not thrown");
        } catch (AssertionError e) {
            // okay
        }
    }

    @Test
    public void putRequestRejectsRequestWithNullReceiver() {
        Request request = createRequest(mock(InetSocketAddress.class), null);
        ConfigurationRequestContext ctx = mock(ConfigurationRequestContext.class);
        RequestQueueImpl queue = new RequestQueueImpl(ctx);
        try {
            queue.putRequest(request);
            fail("expected assertion not thrown");
        } catch (AssertionError e) {
            // okay
        }
    }

    /*
     * Other tests ensure that secure storage is returned from storage providers.
     * This is an attempt to make sure that authentication hooks are actually
     * called if storage is an instance of SecureStorage.
     * 
     */
    @Test
    public void putRequestAuthenticatesForSecureStorage() {
        SecureStorage mockStorage = mock(SecureStorage.class);
        when(mockContext.getService(mockServiceRef)).thenReturn(mockStorage);
        
        AuthToken mockToken = mock(AuthToken.class);
        when(mockStorage.generateToken(any(String.class))).thenReturn(mockToken);
        
        ConfigurationRequestContext ctx = mock(ConfigurationRequestContext.class);
        RequestQueueImpl queue = new RequestQueueImpl(ctx);
        Request request = createRequest(mock(InetSocketAddress.class), "");
        queue.putRequest(request);
        verify(request).setParameter(eq(Request.CLIENT_TOKEN), any(String.class));
        verify(request).setParameter(eq(Request.AUTH_TOKEN), any(String.class));
        assertTrue(queue.getQueue().contains(request));
    }
    
    @Test
    public void testNoEnqueueIfAuthFailed() {
        SecureStorage mockStorage = mock(SecureStorage.class);
        when(mockContext.getService(mockServiceRef)).thenReturn(mockStorage);
        
        when(mockStorage.generateToken(any(String.class))).thenThrow(new StorageException());
        
        ConfigurationRequestContext ctx = mock(ConfigurationRequestContext.class);
        RequestQueueImpl queue = new RequestQueueImpl(ctx);
        Request request = createRequest(mock(InetSocketAddress.class), "");
        
        RequestResponseListener listener = mock(RequestResponseListener.class);
        when(request.getListeners()).thenReturn(Arrays.asList(listener));
        queue.putRequest(request);
        
        ArgumentCaptor<Response> responseCaptor = ArgumentCaptor.forClass(Response.class);
        verify(listener).fireComplete(eq(request), responseCaptor.capture());
        assertEquals(ResponseType.AUTH_FAILED, responseCaptor.getValue().getType());
        assertFalse(queue.getQueue().contains(request));
    }
    
    @Test
    public void testEnqueueNoSecureStorage() {
        Storage mockStorage = mock(Storage.class);
        when(mockContext.getService(mockServiceRef)).thenReturn(mockStorage);
        
        ConfigurationRequestContext ctx = mock(ConfigurationRequestContext.class);
        RequestQueueImpl queue = new RequestQueueImpl(ctx);
        Request request = createRequest(mock(InetSocketAddress.class), "");
        queue.putRequest(request);
        assertTrue(queue.getQueue().contains(request));
    }

    private static Request createRequest(InetSocketAddress agentAddress, String receiver) {
        Request request = mock(Request.class);
        when(request.getReceiver()).thenReturn(receiver);
        when(request.getTarget()).thenReturn(agentAddress);
        return request;
    }
}

