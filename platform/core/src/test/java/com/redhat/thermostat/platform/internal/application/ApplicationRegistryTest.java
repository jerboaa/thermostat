/*
 * Copyright 2012-2017 Red Hat, Inc.
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

package com.redhat.thermostat.platform.internal.application;

import com.redhat.thermostat.common.ActionEvent;
import com.redhat.thermostat.common.ActionListener;
import com.redhat.thermostat.common.ActionNotifier;

import com.redhat.thermostat.common.ThermostatExtensionRegistry;
import com.redhat.thermostat.platform.ApplicationProvider;
import com.redhat.thermostat.platform.internal.application.ApplicationRegistry;
import org.junit.Before;
import org.junit.Test;

import org.mockito.ArgumentCaptor;
import org.osgi.framework.BundleContext;

import java.util.Map;

import static org.mockito.Mockito.*;

public class ApplicationRegistryTest {

    private ApplicationRegistry applicationRegistry;
    private ApplicationRegistry.RegistryHandler registryHandler;
    private ApplicationRegistry.Registry registry;
    private ActionNotifier<ApplicationRegistry.RegistryEvent> actionNotifier;
    private BundleContext context;

    @Before
    public void setUp() throws Exception {

        context = mock(BundleContext.class);
        registryHandler = mock(ApplicationRegistry.RegistryHandler.class);
        registry = mock(ApplicationRegistry.Registry.class);
        actionNotifier = mock(ActionNotifier.class);

        applicationRegistry = new ApplicationRegistry(context) {
            @Override
            RegistryHandler createRegistryHandler(BundleContext context) {
                return registryHandler;
            }

            @Override
            Registry createRegistry(ActionNotifier<RegistryEvent> actionNotifier) {
                return registry;
            }

            @Override
            ActionNotifier<RegistryEvent> createNotifier() {
                return actionNotifier;
            }
        };
    }

    @Test
    public void testAddRegistryEventListener() throws Exception {
        ActionListener<ApplicationRegistry.RegistryEvent> listener =
                mock(ActionListener.class);

        applicationRegistry.addRegistryEventListener(listener);
        verify(actionNotifier).addActionListener(listener);
    }

    @Test
    public void testRemoveRegistryEventListener() throws Exception {
        ActionListener<ApplicationRegistry.RegistryEvent> listener =
                mock(ActionListener.class);

        applicationRegistry.removeRegistryEventListener(listener);
        verify(actionNotifier).removeActionListener(listener);
    }

    @Test
    public void testStart() throws Exception {
        applicationRegistry.start();
        verify(registryHandler).start();
    }

    @Test
    public void testStop() throws Exception {
        applicationRegistry.stop();
        verify(registryHandler).stop();
    }

    @Test
    public void testNotifications() throws Exception {

        ApplicationProvider provider = mock(ApplicationProvider.class);

        ArgumentCaptor<ActionListener> registryCaptor = ArgumentCaptor
                .forClass(ActionListener.class);

        verify(registryHandler).addActionListener(registryCaptor.capture());
        ActionListener l = registryCaptor.getValue();

        ActionEvent<ThermostatExtensionRegistry.Action> event =
                new ActionEvent<>(this, ThermostatExtensionRegistry.Action.SERVICE_ADDED);
        event.setPayload(provider);

        l.actionPerformed(event);

        verify(registry).registerApplication(provider);

        event = new ActionEvent<>(this, ThermostatExtensionRegistry.Action.SERVICE_REMOVED);
        event.setPayload(provider);
        l.actionPerformed(event);

        verify(registry).unregisterApplication(provider);
    }

    @Test
    public void testRegistry() throws Exception {

        Map<String, ApplicationProvider> providers = mock(Map.class);
        ActionNotifier<ApplicationRegistry.RegistryEvent> notifier = mock(ActionNotifier.class);
        ApplicationRegistry.Registry registry =
                new ApplicationRegistry.Registry(providers, notifier);

        ApplicationProvider provider = mock(ApplicationProvider.class);
        registry.registerApplication(provider);

        verify(notifier).fireAction(ApplicationRegistry.RegistryEvent.APPLICATION_REGISTERED, provider);

        String name = ApplicationRegistry.getKeyFor(provider);
        verify(providers).put(name, provider);

        registry.unregisterApplication(provider);

        verify(notifier).fireAction(ApplicationRegistry.RegistryEvent.APPLICATION_REMOVED, provider);
        verify(providers).remove(name);
    }
}
