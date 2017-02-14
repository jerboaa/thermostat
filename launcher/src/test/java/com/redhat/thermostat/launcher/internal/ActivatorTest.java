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

package com.redhat.thermostat.launcher.internal;

import com.redhat.thermostat.common.ExitStatus;
import com.redhat.thermostat.common.MultipleServiceTracker;
import com.redhat.thermostat.common.MultipleServiceTracker.Action;
import com.redhat.thermostat.common.MultipleServiceTracker.DependencyProvider;
import com.redhat.thermostat.common.cli.Command;
import com.redhat.thermostat.common.config.experimental.ConfigurationInfoSource;
import com.redhat.thermostat.launcher.BundleManager;
import com.redhat.thermostat.launcher.Launcher;
import com.redhat.thermostat.shared.config.CommonPaths;
import com.redhat.thermostat.shared.config.SSLConfiguration;
import com.redhat.thermostat.storage.dao.AgentInfoDAO;
import com.redhat.thermostat.storage.dao.VmInfoDAO;
import com.redhat.thermostat.testutils.StubBundleContext;
import com.redhat.thermostat.utils.keyring.Keyring;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceRegistration;
import org.osgi.framework.launch.Framework;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

import static com.redhat.thermostat.testutils.Asserts.assertCommandIsRegistered;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.whenNew;

@RunWith(PowerMockRunner.class)
@PrepareForTest({Activator.class, Activator.RegisterLauncherAction.class, FrameworkUtil.class})
public class ActivatorTest {

    private StubBundleContext context;
    private MultipleServiceTracker tracker;
    private BundleManager registryService;
    private Command helpCommand;

    @Before
    public void setUp() throws Exception {
        Path tempDir = createStubThermostatHome();
        System.setProperty("THERMOSTAT_HOME", tempDir.toString());

        context = new StubBundleContext();
        setupOsgiRegistryImplMock();

        registryService = mock(BundleManager.class);
        context.registerService(BundleManager.class, registryService, null);

        helpCommand = mock(Command.class);
        Hashtable<String,String> props = new Hashtable<>();
        props.put(Command.NAME, "help");
        context.registerService(Command.class, helpCommand, props);

        CommonPaths config = mock(CommonPaths.class);
        when(config.getSystemThermostatHome()).thenReturn(new File(""));
        when(registryService.getCommonPaths()).thenReturn(config);

        BuiltInCommandInfoSource source1 = mock(BuiltInCommandInfoSource.class);
        when(source1.getCommandInfos()).thenReturn(new ArrayList<CommandInfo>());
        whenNew(BuiltInCommandInfoSource.class).
                withParameterTypes(String.class, String.class).
                withArguments(isA(String.class), isA(String.class)).thenReturn(source1);

        PluginInfoSource source2 = mock(PluginInfoSource.class);
        when(source2.getCommandInfos()).thenReturn(new ArrayList<CommandInfo>());
        whenNew(PluginInfoSource.class)
                .withParameterTypes(String.class, String.class, String.class, String.class, String.class)
                .withArguments(anyString(), anyString(), anyString(), anyString(), anyString())
                .thenReturn(source2);

        CompoundCommandInfoSource commands = mock(CompoundCommandInfoSource.class);
        whenNew(CompoundCommandInfoSource.class)
                .withParameterTypes(CommandInfoSource.class, CommandInfoSource.class)
                .withArguments(source1, source2)
                .thenReturn(commands);

        tracker = mock(MultipleServiceTracker.class);
        whenNew(MultipleServiceTracker.class).
                withParameterTypes(BundleContext.class, Class[].class, Action.class).
                withArguments(eq(context), eq(new Class[] {BundleManager.class, Keyring.class}),
                        isA(Action.class)).thenReturn(tracker);
    }

    @Test
    public void testActivatorLifecycle() throws Exception {
        ArgumentCaptor<Action> actionCaptor = ArgumentCaptor.forClass(Action.class);
        MultipleServiceTracker mockTracker = mock(MultipleServiceTracker.class);
        whenNew(MultipleServiceTracker.class)
                .withParameterTypes(BundleContext.class, Class[].class, Action.class)
                .withArguments(eq(context), any(Class[].class), actionCaptor.capture())
                .thenReturn(mockTracker);

        Activator activator = new Activator();
        activator.start(context);

        assertCommandIsRegistered(context, "help", HelpCommand.class);

        verify(mockTracker, times(3)).open();

        Action action = actionCaptor.getValue();
        assertNotNull(action);
        activator.stop(context);
        verify(mockTracker, times(3)).close();
    }
    
    @Test
    public void testServiceTrackerCustomizerForLauncherDepsTracker() throws Exception {
        StubBundleContext context = new StubBundleContext();
        ArgumentCaptor<Action> actionCaptor = ArgumentCaptor.forClass(Action.class);
        MultipleServiceTracker launcherDepsTracker = mock(MultipleServiceTracker.class);
        Class<?>[] launcherDeps = new Class[] {
                Keyring.class,
                CommonPaths.class,
                SSLConfiguration.class,
        };
        whenNew(MultipleServiceTracker.class).withParameterTypes(BundleContext.class, Class[].class, Action.class).withArguments(eq(context),
                eq(launcherDeps), actionCaptor.capture()).thenReturn(launcherDepsTracker);

        MultipleServiceTracker unusedTracker = mock(MultipleServiceTracker.class);
        Class<?>[] shellDeps = new Class[] {
                CommonPaths.class,
                ConfigurationInfoSource.class,
        };
        whenNew(MultipleServiceTracker.class).withParameterTypes(BundleContext.class, Class[].class, Action.class).withArguments(eq(context),
                eq(shellDeps), actionCaptor.capture()).thenReturn(unusedTracker);
        Class<?>[] vmIdCompleterDeps = new Class[] {
                VmInfoDAO.class,
                AgentInfoDAO.class
        };
        whenNew(MultipleServiceTracker.class).withParameterTypes(BundleContext.class, Class[].class, Action.class).withArguments(eq(context),
                eq(vmIdCompleterDeps), actionCaptor.capture()).thenReturn(unusedTracker);
        Class<?>[] agentIdCompleterDeps = new Class[] {
                AgentInfoDAO.class
        };
        whenNew(MultipleServiceTracker.class).withParameterTypes(BundleContext.class, Class[].class, Action.class).withArguments(eq(context),
                eq(agentIdCompleterDeps), actionCaptor.capture()).thenReturn(unusedTracker);
        Class<?>[] helpCommandDeps = new Class[] {
                CommandInfoSource.class,
                CommandGroupMetadataSource.class
        };
        whenNew(MultipleServiceTracker.class).withParameterTypes(BundleContext.class, Class[].class, Action.class).withArguments(eq(context),
                eq(helpCommandDeps), actionCaptor.capture()).thenReturn(unusedTracker);

        Activator activator = new Activator();
        context.registerService(Keyring.class, mock(Keyring.class), null);
        ConfigurationInfoSource configurationInfoSource = mock(ConfigurationInfoSource.class);
        when(configurationInfoSource.getConfiguration("shell-command", "shell-prompt.conf")).thenReturn(new HashMap<String, String>());
        context.registerService(ConfigurationInfoSource.class, configurationInfoSource, null);

        activator.start(context);
        
        assertTrue(context.isServiceRegistered(Command.class.getName(), HelpCommand.class));
        
        Action action = actionCaptor.getAllValues().get(0);
        assertNotNull(action);
        SSLConfiguration sslConfiguration = mock(SSLConfiguration.class);
        Keyring keyringService = mock(Keyring.class);
        CommonPaths paths = mock(CommonPaths.class);
        when(paths.getSystemThermostatHome()).thenReturn(mock(File.class));
        when(paths.getUserThermostatHome()).thenReturn(mock(File.class));
        File systemLibRoot = mock(File.class);
        when(systemLibRoot.listFiles()).thenReturn(new File[] {});
        when(paths.getSystemLibRoot()).thenReturn(systemLibRoot);
        when(paths.getSystemPluginRoot()).thenReturn(new File(""));
        when(paths.getUserPluginRoot()).thenReturn(new File(""));
        when(paths.getUserClientConfigurationFile()).thenReturn(new File(""));
        when(paths.getSystemPluginConfigurationDirectory()).thenReturn(new File(""));
        when(paths.getUserPluginConfigurationDirectory()).thenReturn(new File(""));
        @SuppressWarnings("rawtypes")
        ServiceRegistration keyringReg = context.registerService(Keyring.class, keyringService, null);
        @SuppressWarnings("rawtypes")
        ServiceRegistration pathsReg = context.registerService(CommonPaths.class, paths, null);
        Map<String, Object> services = new HashMap<>();
        services.put(Keyring.class.getName(), keyringService);
        services.put(CommonPaths.class.getName(), paths);
        services.put(ConfigurationInfoSource.class.getName(), configurationInfoSource);
        services.put(SSLConfiguration.class.getName(), sslConfiguration);
        action.dependenciesAvailable(new DependencyProvider(services));
        
        assertTrue(context.isServiceRegistered(CommandInfoSource.class.getName(), mock(CompoundCommandInfoSource.class).getClass()));
        assertTrue(context.isServiceRegistered(BundleManager.class.getName(), BundleManagerImpl.class));
        assertTrue(context.isServiceRegistered(Launcher.class.getName(), LauncherImpl.class));
        assertTrue(context.isServiceRegistered(ExitStatus.class.getName(), ExitStatusImpl.class));

        action.dependenciesUnavailable();
        keyringReg.unregister();
        pathsReg.unregister();
        
        assertFalse(context.isServiceRegistered(CommandInfoSource.class.getName(), CompoundCommandInfoSource.class));
        assertFalse(context.isServiceRegistered(BundleManager.class.getName(), BundleManagerImpl.class));
        assertFalse(context.isServiceRegistered(Launcher.class.getName(), LauncherImpl.class));
    }

    @Test
    public void testServiceTrackerCustomizerForShellTracker() throws Exception {
        StubBundleContext context = new StubBundleContext();
        ArgumentCaptor<Action> actionCaptor = ArgumentCaptor.forClass(Action.class);
        MultipleServiceTracker unusedTracker = mock(MultipleServiceTracker.class);
        Class<?>[] launcherDeps = new Class[] {
                Keyring.class,
                CommonPaths.class,
                SSLConfiguration.class,
        };
        whenNew(MultipleServiceTracker.class).withParameterTypes(BundleContext.class, Class[].class, Action.class).withArguments(eq(context),
                eq(launcherDeps), actionCaptor.capture()).thenReturn(unusedTracker);

        MultipleServiceTracker shellTracker = mock(MultipleServiceTracker.class);
        Class<?>[] shellDeps = new Class[] {
                CommonPaths.class,
                ConfigurationInfoSource.class,
        };
        whenNew(MultipleServiceTracker.class).withParameterTypes(BundleContext.class, Class[].class, Action.class).withArguments(eq(context),
                eq(shellDeps), actionCaptor.capture()).thenReturn(shellTracker);
        Class<?>[] vmIdCompleterDeps = new Class[] {
                VmInfoDAO.class,
                AgentInfoDAO.class
        };
        whenNew(MultipleServiceTracker.class).withParameterTypes(BundleContext.class, Class[].class, Action.class).withArguments(eq(context),
                eq(vmIdCompleterDeps), actionCaptor.capture()).thenReturn(unusedTracker);
        Class<?>[] agentIdCompleterDeps = new Class[] {
                AgentInfoDAO.class
        };
        whenNew(MultipleServiceTracker.class).withParameterTypes(BundleContext.class, Class[].class, Action.class).withArguments(eq(context),
                eq(agentIdCompleterDeps), actionCaptor.capture()).thenReturn(unusedTracker);
        Class<?>[] helpCommandDeps = new Class[] {
                CommandInfoSource.class,
                CommandGroupMetadataSource.class
        };
        whenNew(MultipleServiceTracker.class).withParameterTypes(BundleContext.class, Class[].class, Action.class).withArguments(eq(context),
                eq(helpCommandDeps), actionCaptor.capture()).thenReturn(unusedTracker);

        Activator activator = new Activator();
        ConfigurationInfoSource configurationInfoSource = mock(ConfigurationInfoSource.class);
        when(configurationInfoSource.getConfiguration("shell-command", "shell-prompt.conf")).thenReturn(new HashMap<String, String>());
        context.registerService(ConfigurationInfoSource.class, configurationInfoSource, null);

        activator.start(context);

        Action action = actionCaptor.getAllValues().get(1);

        assertNotNull(action);
        CommonPaths paths = mock(CommonPaths.class);
        when(paths.getSystemLibRoot()).thenReturn(new File(""));
        when(paths.getSystemPluginRoot()).thenReturn(new File(""));
        when(paths.getUserPluginRoot()).thenReturn(new File(""));
        when(paths.getUserClientConfigurationFile()).thenReturn(new File(""));
        when(paths.getSystemPluginConfigurationDirectory()).thenReturn(new File(""));
        when(paths.getUserPluginConfigurationDirectory()).thenReturn(new File(""));
        @SuppressWarnings("rawtypes")
        ServiceRegistration pathsReg = context.registerService(CommonPaths.class, paths, null);
        Map<String, Object> services = new HashMap<>();
        services.put(CommonPaths.class.getName(), paths);
        services.put(ConfigurationInfoSource.class.getName(), configurationInfoSource);
        action.dependenciesAvailable(new DependencyProvider(services));

        assertTrue(context.isServiceRegistered(Command.class.getName(), ShellCommand.class));

        action.dependenciesUnavailable();
        pathsReg.unregister();

        assertFalse(context.isServiceRegistered(CommandInfoSource.class.getName(), CompoundCommandInfoSource.class));
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

    private void setupOsgiRegistryImplMock() throws InvalidSyntaxException {
        PowerMockito.mockStatic(FrameworkUtil.class);
        when(FrameworkUtil.createFilter(anyString())).thenCallRealMethod();
        Bundle mockBundle = mock(Bundle.class);
        when(FrameworkUtil.getBundle(BundleManagerImpl.class)).thenReturn(mockBundle);
        when(mockBundle.getBundleContext()).thenReturn(context);
        Bundle mockFramework = mock(Framework.class);
        context.setBundle(0, mockFramework);
        when(mockFramework.getBundleContext()).thenReturn(context);
    }
}

