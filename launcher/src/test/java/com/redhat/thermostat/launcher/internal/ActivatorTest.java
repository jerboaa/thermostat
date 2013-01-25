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

package com.redhat.thermostat.launcher.internal;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isA;
import static org.mockito.Matchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.whenNew;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Hashtable;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.framework.launch.Framework;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.redhat.thermostat.common.MultipleServiceTracker;
import com.redhat.thermostat.common.MultipleServiceTracker.Action;
import com.redhat.thermostat.common.cli.Command;
import com.redhat.thermostat.common.cli.CommandInfo;
import com.redhat.thermostat.common.cli.CommandInfoSource;
import com.redhat.thermostat.common.config.Configuration;
import com.redhat.thermostat.common.utils.ServiceRegistry;
import com.redhat.thermostat.launcher.BundleManager;
import com.redhat.thermostat.launcher.Launcher;
import com.redhat.thermostat.launcher.internal.Activator.RegisterLauncherCustomizer;
import com.redhat.thermostat.test.StubBundleContext;
import com.redhat.thermostat.utils.keyring.Keyring;

@RunWith(PowerMockRunner.class)
@PrepareForTest({Activator.class, Activator.RegisterLauncherCustomizer.class, FrameworkUtil.class})
public class ActivatorTest {

    private BundleContext context;
    private MultipleServiceTracker tracker;
    private ServiceReference registryServiceReference, helpCommandReference;
    private ServiceRegistration launcherServiceRegistration, helpCommandRegistration;
    private BundleManager registryService;
    private Command helpCommand;

    @Before
    public void setUp() throws Exception {
        Path tempDir = createStubThermostatHome();
        System.setProperty("THERMOSTAT_HOME", tempDir.toString());
        
        context = mock(BundleContext.class);
        setupOsgiRegistryImplMock();

        registryServiceReference = mock(ServiceReference.class);
        launcherServiceRegistration = mock(ServiceRegistration.class);
        registryService = mock(BundleManager.class);
        when(context.getServiceReference(eq(BundleManager.class))).thenReturn(registryServiceReference);
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

        Configuration config = mock(Configuration.class);
        when(config.getThermostatHome()).thenReturn("");
        when(registryService.getConfiguration()).thenReturn(config);

        CommandInfoSourceImpl commands = mock(CommandInfoSourceImpl.class);
        when(commands.getCommandInfos()).thenReturn(new ArrayList<CommandInfo>());
        whenNew(CommandInfoSourceImpl.class).
                withParameterTypes(String.class, String.class).
                withArguments(isA(String.class), isA(String.class)).thenReturn(commands);

        tracker = mock(MultipleServiceTracker.class);
        whenNew(MultipleServiceTracker.class).
                withParameterTypes(BundleContext.class, Class[].class, Action.class).
                withArguments(eq(context), eq(new Class[] {BundleManager.class, Keyring.class}),
                        isA(Action.class)).thenReturn(tracker);
    }

    @Test
    public void testActivatorLifecycle() throws Exception {
        ArgumentCaptor<RegisterLauncherCustomizer> customizerCaptor = ArgumentCaptor.forClass(RegisterLauncherCustomizer.class);
        ServiceTracker mockTracker = mock(ServiceTracker.class);
        whenNew(ServiceTracker.class).withParameterTypes(BundleContext.class, Class.class, ServiceTrackerCustomizer.class).withArguments(eq(context),
                any(Keyring.class), customizerCaptor.capture()).thenReturn(mockTracker);
        
        Activator activator = new Activator();
        activator.start(context);

        Hashtable<String, Object> props = new Hashtable<>();
        props.put(ServiceRegistry.SERVICE_NAME, "help");
        verify(context).registerService(eq(Command.class.getName()), isA(HelpCommand.class), eq(props));

        verify(mockTracker).open();
        
        RegisterLauncherCustomizer customizer = customizerCaptor.getValue();
        assertNotNull(customizer);
        activator.stop(context);
        verify(mockTracker).close();
    }
    
    @Test
    public void testServiceTrackerCustomizer() throws Exception {
        StubBundleContext context = new StubBundleContext();
        ArgumentCaptor<RegisterLauncherCustomizer> customizerCaptor = ArgumentCaptor.forClass(RegisterLauncherCustomizer.class);
        ServiceTracker mockTracker = mock(ServiceTracker.class);
        whenNew(ServiceTracker.class).withParameterTypes(BundleContext.class, Class.class, ServiceTrackerCustomizer.class).withArguments(eq(context),
                any(Keyring.class), customizerCaptor.capture()).thenReturn(mockTracker);
        
        Activator activator = new Activator();
        context.registerService(Keyring.class, mock(Keyring.class), null);
        activator.start(context);
        
        assertTrue(context.isServiceRegistered(Command.class.getName(), HelpCommand.class));
        
        RegisterLauncherCustomizer customizer = customizerCaptor.getValue();
        assertNotNull(customizer);
        Keyring keyringService = mock(Keyring.class);
        context.registerService(Keyring.class, keyringService, null);
        ServiceReference ref = context.getServiceReference(Keyring.class);
        customizer.addingService(ref);
        
        assertTrue(context.isServiceRegistered(CommandInfoSource.class.getName(), mock(CommandInfoSourceImpl.class).getClass()));
        assertTrue(context.isServiceRegistered(BundleManager.class.getName(), BundleManagerImpl.class));
        assertTrue(context.isServiceRegistered(Launcher.class.getName(), LauncherImpl.class));

        customizer.removedService(null, null);
        
        assertFalse(context.isServiceRegistered(CommandInfoSource.class.getName(), CommandInfoSourceImpl.class));
        assertFalse(context.isServiceRegistered(BundleManager.class.getName(), BundleManagerImpl.class));
        assertFalse(context.isServiceRegistered(Launcher.class.getName(), LauncherImpl.class));
    }
    
    private Path createStubThermostatHome() throws Exception {
        Path tempDir = Files.createTempDirectory("test");
        tempDir.toFile().deleteOnExit();
        System.setProperty("THERMOSTAT_HOME", tempDir.toString());
        
        File tempEtc = new File(tempDir.toFile(), "etc");
        tempEtc.mkdirs();
        tempEtc.deleteOnExit();
        
        File tempProps = new File(tempEtc, "osgi-export.properties");
        tempProps.createNewFile();
        tempProps.deleteOnExit();

        File tempBundleProps = new File(tempEtc, "bundles.properties");
        tempBundleProps.createNewFile();
        tempBundleProps.deleteOnExit();
        
        File tempLibs = new File(tempDir.toFile(), "libs");
        tempLibs.mkdirs();
        tempLibs.deleteOnExit();
        return tempDir;
    }

    private void setupOsgiRegistryImplMock() {
        PowerMockito.mockStatic(FrameworkUtil.class);
        Bundle mockBundle = mock(Bundle.class);
        when(FrameworkUtil.getBundle(BundleManagerImpl.class)).thenReturn(mockBundle);
        when(mockBundle.getBundleContext()).thenReturn(context);
        Bundle mockFramework = mock(Framework.class);
        when(context.getBundle(0)).thenReturn(mockFramework);
        when(mockFramework.getBundleContext()).thenReturn(context);
        when(context.getBundles()).thenReturn(new Bundle[0]);
    }
}

