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

package com.redhat.thermostat.client.osgi.service;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;

import com.redhat.thermostat.common.ActionEvent;
import com.redhat.thermostat.common.ActionListener;
import com.redhat.thermostat.common.ThermostatExtensionRegistry;

public class MenuRegistryTest {

    @Test
    public void verifyMenuRegistryReactsToMenuActions() throws InvalidSyntaxException {
        ArgumentCaptor<ServiceListener> serviceListenerCaptor = ArgumentCaptor.forClass(ServiceListener.class);
        ArgumentCaptor<String> filterCaptor = ArgumentCaptor.forClass(String.class);

        ActionListener<ThermostatExtensionRegistry.Action> menuListener = mock(ActionListener.class);
        MenuAction menuAction = mock(MenuAction.class);

        BundleContext context = mock(BundleContext.class);
        doNothing().when(context).addServiceListener(serviceListenerCaptor.capture(), filterCaptor.capture());

        ServiceReference ref = mock(ServiceReference.class);
        when(ref.getProperty("objectClass")).thenReturn(MenuAction.class.getName());

        when(context.getService(ref)).thenReturn(menuAction);

        MenuRegistry registry = new MenuRegistry(context);
        registry.addActionListener(menuListener);
        registry.start();

        ServiceListener serviceListener = serviceListenerCaptor.getValue();
        serviceListener.serviceChanged(new ServiceEvent(ServiceEvent.REGISTERED, ref));

        ArgumentCaptor<ActionEvent> eventCaptor = ArgumentCaptor.forClass(ActionEvent.class);
        serviceListener.serviceChanged(new ServiceEvent(ServiceEvent.UNREGISTERING, ref));

        verify(menuListener, times(2)).actionPerformed(eventCaptor.capture());

        ActionEvent firstEvent = eventCaptor.getAllValues().get(0);
        ActionEvent secondEvent = eventCaptor.getAllValues().get(1);

        assertEquals(ThermostatExtensionRegistry.Action.SERVICE_ADDED, firstEvent.getActionId());
        assertEquals(ThermostatExtensionRegistry.Action.SERVICE_REMOVED, secondEvent.getActionId());

    }
}

