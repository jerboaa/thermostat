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

package com.redhat.thermostat.client.command.internal;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.redhat.thermostat.common.command.Request;
import com.redhat.thermostat.storage.core.AuthToken;
import com.redhat.thermostat.storage.core.SecureStorage;
import com.redhat.thermostat.storage.core.Storage;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ FrameworkUtil.class })
public class RequestQueueImplTest {


    /*
     * Other tests ensure that secure storage is returned from storage providers.
     * This is an attemtp to make sure that authentication hooks are actually
     * called if storage is an instance of SecureStorage.
     * 
     */
    @Test
    public void putRequestAuthenticatesForSecureStorage() {
        PowerMockito.mockStatic(FrameworkUtil.class);
        Bundle mockBundle = mock(Bundle.class);
        BundleContext mockContext = mock(BundleContext.class);
        ServiceReference mockServiceRef = mock(ServiceReference.class);
        when(mockContext.getServiceReference(Storage.class.getName())).thenReturn(mockServiceRef);
        SecureStorage mockStorage = mock(SecureStorage.class);
        AuthToken mockToken = mock(AuthToken.class);
        when(mockStorage.generateToken(any(String.class))).thenReturn(mockToken);
        when(mockContext.getService(mockServiceRef)).thenReturn(mockStorage);
        when(mockBundle.getBundleContext()).thenReturn(mockContext);
        when(FrameworkUtil.getBundle(RequestQueueImpl.class)).thenReturn(mockBundle);
        ConfigurationRequestContext ctx = mock(ConfigurationRequestContext.class);
        RequestQueueImpl queue = new RequestQueueImpl(ctx);
        Request request = mock(Request.class);
        queue.putRequest(request);
        verify(request).setParameter(eq(Request.CLIENT_TOKEN), any(String.class));
        verify(request).setParameter(eq(Request.AUTH_TOKEN), any(String.class));
    }
}
