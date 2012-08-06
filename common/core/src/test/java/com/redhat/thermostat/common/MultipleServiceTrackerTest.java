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

package com.redhat.thermostat.common;

import java.util.Map;

import junit.framework.Assert;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.redhat.thermostat.common.MultipleServiceTracker.Action;
import com.redhat.thermostat.common.MultipleServiceTracker.InternalServiceTrackerCustomizer;

import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.any;

import static org.powermock.api.mockito.PowerMockito.verifyNew;
import static org.powermock.api.mockito.PowerMockito.whenNew;

@RunWith(PowerMockRunner.class)
@PrepareForTest({MultipleServiceTracker.class, InternalServiceTrackerCustomizer.class})
public class MultipleServiceTrackerTest {

    private Action action;
    private BundleContext context;
    private ServiceTracker objectTracker, stringTracker;
    private ServiceReference objectReference, stringReference;

    @Before
    public void setUp() throws Exception {
        action = mock(Action.class);
        context = mock(BundleContext.class);

        objectReference = mock(ServiceReference.class);
        String[] objObjectClassProperty = {Object.class.getName()};
        when(objectReference.getProperty(eq("objectClass"))).thenReturn(objObjectClassProperty);
        when(context.getService(objectReference)).thenReturn(new Object());
        objectTracker = mock(ServiceTracker.class);
        whenNew(ServiceTracker.class).
                withParameterTypes(BundleContext.class, String.class, ServiceTrackerCustomizer.class).
                withArguments(eq(context), eq(Object.class.getName()),
                        isA(ServiceTrackerCustomizer.class)).thenReturn(objectTracker);

        stringReference = mock(ServiceReference.class);
        String[] stringObjectClassProperty = {String.class.getName()};
        when(stringReference.getProperty(eq("objectClass"))).thenReturn(stringObjectClassProperty);
        when(context.getService(stringReference)).thenReturn("foo");
        stringTracker = mock(ServiceTracker.class);
        whenNew(ServiceTracker.class).
                withParameterTypes(BundleContext.class, String.class, ServiceTrackerCustomizer.class).
                withArguments(eq(context), eq(String.class.getName()),
                        isA(ServiceTrackerCustomizer.class)).thenReturn(stringTracker);
    }

    @Test
    public void testSingleClass() throws Exception {

        Class[] deps = { Object.class };
        MultipleServiceTracker tracker = new MultipleServiceTracker(context, deps, action);

        ArgumentCaptor<ServiceTrackerCustomizer> customizerCaptor = ArgumentCaptor.forClass(ServiceTrackerCustomizer.class);
        verifyNew(ServiceTracker.class).withArguments(eq(context),
                eq(Object.class.getName()),
                customizerCaptor.capture());
        ServiceTrackerCustomizer customizer = customizerCaptor.getValue();

        tracker.open();
        verify(objectTracker).open();

        customizer.addingService(objectReference);
        verify(action).doIt(any(Map.class));
    }

    @Test
    public void testMultipleClasses() throws Exception {
        Class[] deps = { Object.class, String.class };
        MultipleServiceTracker tracker = new MultipleServiceTracker(context, deps, action);

        ArgumentCaptor<Map> serviceMap = ArgumentCaptor.forClass(Map.class);
        
        ArgumentCaptor<ServiceTrackerCustomizer> customizerCaptor = ArgumentCaptor.forClass(ServiceTrackerCustomizer.class);
        verifyNew(ServiceTracker.class).withArguments(eq(context),
                eq(Object.class.getName()),
                customizerCaptor.capture());
        verifyNew(ServiceTracker.class).withArguments(eq(context),
                eq(String.class.getName()),
                customizerCaptor.capture());
        ServiceTrackerCustomizer customizer = customizerCaptor.getValue();

        tracker.open();
        verify(objectTracker).open();
        verify(stringTracker).open();

        customizer.addingService(objectReference);
        customizer.addingService(stringReference);
        verify(action).doIt(serviceMap.capture());
        
        Map caputerServices = serviceMap.getValue();
        Assert.assertTrue(caputerServices.containsKey(Object.class.getName()));
        Assert.assertTrue(caputerServices.containsKey(String.class.getName()));
        Assert.assertEquals(2, caputerServices.size());
    }
}
