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

package com.redhat.thermostat.launcher.internal;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isA;
import static org.mockito.Matchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.verifyNew;
import static org.powermock.api.mockito.PowerMockito.whenNew;

import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.redhat.thermostat.bundles.OSGiRegistryService;
import com.redhat.thermostat.common.MultipleServiceTracker;
import com.redhat.thermostat.common.MultipleServiceTracker.Action;
import com.redhat.thermostat.common.cli.Command;
import com.redhat.thermostat.common.utils.ServiceRegistry;
import com.redhat.thermostat.launcher.Launcher;
import com.redhat.thermostat.utils.keyring.Keyring;

@RunWith(PowerMockRunner.class)
@PrepareForTest({Activator.class})
public class ActivatorTest {

    private BundleContext context;
    private MultipleServiceTracker tracker;
    private ServiceReference registryServiceReference, helpCommandReference;
    private ServiceRegistration launcherServiceRegistration, helpCommandRegistration;
    private OSGiRegistryService registryService;
    private Command helpCommand;

    @Before
    public void setUp() throws Exception {
        context = mock(BundleContext.class);

        registryServiceReference = mock(ServiceReference.class);
        launcherServiceRegistration = mock(ServiceRegistration.class);
        registryService = mock(OSGiRegistryService.class);
        when(context.getServiceReference(eq(OSGiRegistryService.class))).thenReturn(registryServiceReference);
        when(context.getService(eq(registryServiceReference))).thenReturn(registryService);
        when(context.registerService(eq(Launcher.class.getName()), any(), (Dictionary) isNull())).
                thenReturn(launcherServiceRegistration);

        helpCommandRegistration = mock(ServiceRegistration.class);
        helpCommandReference = mock(ServiceReference.class);
        helpCommand = mock(Command.class);
        when(helpCommandRegistration.getReference()).thenReturn(helpCommandReference);
        when(context.registerService(eq(Command.class.getName()), any(), isA(Dictionary.class))).
                thenReturn(helpCommandRegistration);
        when(context.getService(helpCommandReference)).thenReturn(helpCommand);
        when(context.getServiceReferences(Command.class.getName(), null)).thenReturn(new ServiceReference[] {helpCommandReference});

        tracker = mock(MultipleServiceTracker.class);
        whenNew(MultipleServiceTracker.class).
                withParameterTypes(BundleContext.class, Class[].class, Action.class).
                withArguments(eq(context), eq(new Class[] {OSGiRegistryService.class, Keyring.class}),
                        isA(Action.class)).thenReturn(tracker);
    }

    @Test
    public void testActivatorLifecycle() throws Exception {
        Activator activator = new Activator();

        activator.start(context);

        Hashtable<String, Object> props = new Hashtable<>();
        props.put(ServiceRegistry.SERVICE_NAME, "help");
        verify(context).registerService(eq(Command.class.getName()), isA(HelpCommand.class), eq(props));

        ArgumentCaptor<Action> actionCaptor = ArgumentCaptor.forClass(Action.class);
        verifyNew(MultipleServiceTracker.class).withArguments(eq(context),
                eq(new Class[] {OSGiRegistryService.class, Keyring.class}),
                actionCaptor.capture());
        Action action = actionCaptor.getValue();

        action.doIt(any(Map.class));
        verify(context).registerService(eq(Launcher.class.getName()), isA(Launcher.class), (Dictionary) isNull());

        activator.stop(context);
        verify(launcherServiceRegistration).unregister();
        verify(tracker).close();
    }
}
