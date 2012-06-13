/*
 * Copyright 2012 Red Hat, Inc.
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

package com.redhat.thermostat.client.heap;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Dictionary;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Matchers;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;

import com.redhat.thermostat.client.osgi.service.ApplicationService;
import com.redhat.thermostat.client.osgi.service.ContextAction;
import com.redhat.thermostat.client.osgi.service.VMContextAction;

public class ActivatorTest {

    private Activator activator;
    private BundleContext bundleContext;
    private ArgumentCaptor<ServiceListener> serviceListener;
    private ServiceReference ref1;
    private ContextAction service1;
    private ServiceReference ref2;
    private ApplicationService service2;
    private ServiceReference ref3;

    @Before
    public void setUp() throws InvalidSyntaxException {
        activator = new Activator();
        bundleContext = mock(BundleContext.class);
        serviceListener = ArgumentCaptor.forClass(ServiceListener.class);
        doNothing().when(bundleContext).addServiceListener(serviceListener.capture(), anyString());
        ref1 = mock(ServiceReference.class);
        service1 = mock(ContextAction.class);
        when(bundleContext.getService(ref1)).thenReturn(service1);
        ref2 = mock(ServiceReference.class);
        service2 = mock(ApplicationService.class);
        when(bundleContext.getService(ref2)).thenReturn(service2);
        ref3 = mock(ServiceReference.class);
        when(bundleContext.getService(ref3)).thenReturn(new Object());
    }

    @Test
    public void testStart() throws Exception {
        activator.start(bundleContext);
        verifyRegistration(false);
        fireServiceEventAndVerifyRegistration(ServiceEvent.REGISTERED, ref1, false);
        fireServiceEventAndVerifyRegistration(ServiceEvent.REGISTERED, ref2, true);
    }

    @Test
    public void testStartAppServiceFirst() throws Exception {
        activator.start(bundleContext);
        verifyRegistration(false);
        fireServiceEventAndVerifyRegistration(ServiceEvent.REGISTERED, ref2, false);
        fireServiceEventAndVerifyRegistration(ServiceEvent.REGISTERED, ref1, true);
    }

    @Test
    public void testStartOtherService() throws Exception {
        activator.start(bundleContext);
        verifyRegistration(false);
        fireServiceEventAndVerifyRegistration(ServiceEvent.REGISTERED, ref2, false);
        fireServiceEventAndVerifyRegistration(ServiceEvent.REGISTERED, ref3, false);
        fireServiceEventAndVerifyRegistration(ServiceEvent.REGISTERED, ref1, true);
    }

    @Test
    public void testStartDifferentEventType() throws Exception {
        activator.start(bundleContext);
        verifyRegistration(false);
        fireServiceEventAndVerifyRegistration(ServiceEvent.REGISTERED, ref2, false);
        fireServiceEventAndVerifyRegistration(ServiceEvent.MODIFIED, ref1, false);
        fireServiceEventAndVerifyRegistration(ServiceEvent.REGISTERED, ref1, true);
    }

    @Test
    public void testStartExistingServices() throws Exception {

        when(bundleContext.getServiceReferences(null, null)).thenReturn(new ServiceReference[] { ref1, ref2 });

        activator.start(bundleContext);

        verifyRegistration(true);
    }

    @Test
    public void testStop() throws Exception {
        ServiceRegistration serviceReg = mock(ServiceRegistration.class);
        when(bundleContext.registerService(eq(VMContextAction.class.getName()), any(), any(Dictionary.class))).thenReturn(serviceReg);
        testStart(); // Performs all the registration tasks.
        verify(serviceReg, never()).unregister();
        activator.stop(bundleContext);
        verify(serviceReg).unregister();
    }

    @Test
    public void testStopIncompleteRegistration() throws Exception {
        ServiceRegistration serviceReg = mock(ServiceRegistration.class);
        when(bundleContext.registerService(eq(VMContextAction.class.getName()), any(), any(Dictionary.class))).thenReturn(serviceReg);

        activator.start(bundleContext);
        fireServiceEvent(ServiceEvent.REGISTERED, ref2);
        verify(serviceReg, never()).unregister();

        activator.stop(bundleContext);
        verify(serviceReg, never()).unregister();
    }

    private void fireServiceEventAndVerifyRegistration(int type, ServiceReference ref, boolean expectRegisterCalled) {
        fireServiceEvent(type, ref);
        verifyRegistration(expectRegisterCalled);
    }

    private void fireServiceEvent(int type, ServiceReference ref) {
        ServiceEvent event = new ServiceEvent(type, ref);
        serviceListener.getValue().serviceChanged(event);
    }

    private void verifyRegistration(boolean expectRegisterCalled) {
        if (expectRegisterCalled) {
            verify(bundleContext).registerService(eq(VMContextAction.class.getName()), Matchers.isA(HeapDumpAction.class), any(Dictionary.class));
        } else {
            verify(bundleContext, never()).registerService(anyString(), any(), any(Dictionary.class));
        }
    }

}
