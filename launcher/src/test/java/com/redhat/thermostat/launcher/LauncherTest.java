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

package com.redhat.thermostat.launcher;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Matchers.any;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceRegistration;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.redhat.thermostat.bundles.OSGiRegistry;
import com.redhat.thermostat.common.ActionListener;
import com.redhat.thermostat.common.ActionNotifier;
import com.redhat.thermostat.common.ApplicationInfo;
import com.redhat.thermostat.common.Version;
import com.redhat.thermostat.common.appctx.ApplicationContext;
import com.redhat.thermostat.common.appctx.ApplicationContextUtil;
import com.redhat.thermostat.common.cli.Arguments;
import com.redhat.thermostat.common.cli.CommandContext;
import com.redhat.thermostat.common.cli.CommandException;
import com.redhat.thermostat.common.cli.SimpleArgumentSpec;
import com.redhat.thermostat.common.config.ClientPreferences;
import com.redhat.thermostat.common.locale.LocaleResources;
import com.redhat.thermostat.common.locale.Translate;
import com.redhat.thermostat.common.tools.ApplicationState;
import com.redhat.thermostat.common.tools.BasicCommand;
import com.redhat.thermostat.common.utils.OSGIUtils;
import com.redhat.thermostat.launcher.internal.HelpCommand;
import com.redhat.thermostat.launcher.internal.LauncherImpl;
import com.redhat.thermostat.test.TestCommandContextFactory;
import com.redhat.thermostat.test.TestTimerFactory;
import com.redhat.thermostat.test.cli.TestCommand;
import com.redhat.thermostat.utils.keyring.Keyring;
import com.redhat.thermostat.utils.keyring.KeyringProvider;

@RunWith(PowerMockRunner.class)
@PrepareForTest({FrameworkUtil.class, DbServiceFactory.class})
public class LauncherTest {
    
    private static String defaultKeyringProvider;
      
    @BeforeClass
    public static void beforeClassSetUp() {
        defaultKeyringProvider = System.getProperty(KeyringProvider.KEYRING_FACTORY_PROPERTY);
    }
    
    @AfterClass
    public static void afterClassTearDown() {
        if (defaultKeyringProvider != null) {
            System.setProperty(KeyringProvider.KEYRING_FACTORY_PROPERTY, defaultKeyringProvider);
        }
    }
    
    private static class TestCmd1 implements TestCommand.Handle {

        @Override
        public void run(CommandContext ctx) {
            Arguments args = ctx.getArguments();
            ctx.getConsole().getOutput().print(args.getArgument("arg1") + ", " + args.getArgument("arg2"));
        }
    }

    private static class TestCmd2 implements TestCommand.Handle {
        @Override
        public void run(CommandContext ctx) {
            Arguments args = ctx.getArguments();
            ctx.getConsole().getOutput().print(args.getArgument("arg4") + ": " + args.getArgument("arg3"));
        }
    }

    private TestCommandContextFactory  ctxFactory;
    private BundleContext bundleContext;
    private TestTimerFactory timerFactory;
    private OSGiRegistry registry;
    private ActionNotifier<ApplicationState> notifier;

    @Before
    public void setUp() {

        ApplicationContextUtil.resetApplicationContext();
        timerFactory = new TestTimerFactory();
        ApplicationContext.getInstance().setTimerFactory(timerFactory);
        setupCommandContextFactory();

        TestCommand cmd1 = new TestCommand("test1", new TestCmd1());
        SimpleArgumentSpec arg1 = new SimpleArgumentSpec("arg1", null);
        arg1.setUsingAdditionalArgument(true);
        SimpleArgumentSpec arg2 = new SimpleArgumentSpec("arg2", null);
        arg2.setUsingAdditionalArgument(true);
        cmd1.addArguments(arg1, arg2);
        cmd1.setDescription("description 1");
        TestCommand cmd2 = new TestCommand("test2", new TestCmd2());
        SimpleArgumentSpec arg3 = new SimpleArgumentSpec("arg3", null);
        arg3.setUsingAdditionalArgument(true);
        SimpleArgumentSpec arg4 = new SimpleArgumentSpec("arg4", null);
        arg4.setUsingAdditionalArgument(true);
        cmd2.addArguments(arg3, arg4);
        cmd2.setDescription("description 2");

        TestCommand cmd3 = new TestCommand("test3");
        cmd3.setStorageRequired(true);
        cmd3.setDescription("description 3");

        BasicCommand basicCmd = mock(BasicCommand.class);
        when(basicCmd.getName()).thenReturn("basic");
        when(basicCmd.getDescription()).thenReturn("nothing that means anything");
        when(basicCmd.isStorageRequired()).thenReturn(false);
        notifier = mock(ActionNotifier.class);
        when(basicCmd.getNotifier()).thenReturn(notifier);

        ctxFactory.getCommandRegistry().registerCommands(Arrays.asList(new HelpCommand(), cmd1, cmd2, cmd3, basicCmd));

        registry = mock(OSGiRegistry.class);
    }

    private void setupCommandContextFactory() {
        Bundle sysBundle = mock(Bundle.class);
        bundleContext = mock(BundleContext.class);
        when(bundleContext.getBundle(0)).thenReturn(sysBundle);
        ctxFactory = new TestCommandContextFactory(bundleContext);
    }


    @After
    public void tearDown() {
        ctxFactory = null;
        ApplicationContextUtil.resetApplicationContext();
    }

    @Test
    public void testMain() {
        runAndVerifyCommand(new String[] {"test1", "--arg1", "Hello", "--arg2", "World"}, "Hello, World");

        ctxFactory.reset();

        runAndVerifyCommand(new String[] {"test2", "--arg3", "Hello", "--arg4", "World"}, "World: Hello");
    }

    @Test
    public void testMainNoArgs() {
        String expected = "list of commands:\n\n"
                        + " help          show help for a given command or help overview\n"
                        + " basic         nothing that means anything\n"
                        + " test1         description 1\n"
                        + " test2         description 2\n"
                        + " test3         description 3\n";
        runAndVerifyCommand(new String[0], expected);
    }

    @Test
    public void verifySetLogLevel() {
        runAndVerifyCommand(new String[] {"test1", "--logLevel", "WARNING", "--arg1", "Hello", "--arg2", "World"}, "Hello, World");
        Logger globalLogger = Logger.getLogger("com.redhat.thermostat");
        assertEquals(Level.WARNING, globalLogger.getLevel());
    }

    @Test
    public void testMainBadCommand1() {
        String expected = "unknown command '--help'\n"
            + "list of commands:\n\n"
            + " help          show help for a given command or help overview\n"
            + " basic         nothing that means anything\n"
            + " test1         description 1\n"
            + " test2         description 2\n"
            + " test3         description 3\n";
        runAndVerifyCommand(new String[] {"--help"}, expected);
    }

    @Test
    public void testMainBadCommand2() {
        String expected = "unknown command '-help'\n"
            + "list of commands:\n\n"
            + " help          show help for a given command or help overview\n"
            + " basic         nothing that means anything\n"
            + " test1         description 1\n"
            + " test2         description 2\n"
            + " test3         description 3\n";
        runAndVerifyCommand(new String[] {"-help"}, expected);
    }

    @Test
    public void testMainBadCommand3() {
        String expected = "unknown command 'foobarbaz'\n"
            + "list of commands:\n\n"
            + " help          show help for a given command or help overview\n"
            + " basic         nothing that means anything\n"
            + " test1         description 1\n"
            + " test2         description 2\n"
            + " test3         description 3\n";
        runAndVerifyCommand(new String[] {"foobarbaz"}, expected);
    }

    @Test
    public void testMainBadCommand4() {
        String expected = "unknown command 'foo'\n"
            + "list of commands:\n\n"
            + " help          show help for a given command or help overview\n"
            + " basic         nothing that means anything\n"
            + " test1         description 1\n"
            + " test2         description 2\n"
            + " test3         description 3\n";
        runAndVerifyCommand(new String[] {"foo",  "--bar", "baz"}, expected);
    }

    @Test
    public void testMainExceptionInCommand() {
        TestCommand errorCmd = new TestCommand("error", new TestCommand.Handle() {

            @Override
            public void run(CommandContext ctx) throws CommandException {
                throw new CommandException("test error");
            }

        });
        ctxFactory.getCommandRegistry().registerCommands(Arrays.asList(errorCmd));

        LauncherImpl launcher = new LauncherImpl(bundleContext, ctxFactory, registry);
        Keyring keyring = mock(Keyring.class);
        launcher.setPreferences(new ClientPreferences(keyring));
        launcher.setArgs(new String[] { "error" });
        launcher.run();
        assertEquals("test error\n", ctxFactory.getError());

    }

    private void runAndVerifyCommand(String[] args, String expected) {
        LauncherImpl launcher = new LauncherImpl(bundleContext, ctxFactory, registry);
        
        Keyring keyring = mock(Keyring.class);
        launcher.setPreferences(new ClientPreferences(keyring));
        launcher.setArgs(args);
        launcher.run();
        assertEquals(expected, ctxFactory.getOutput());
        assertTrue(timerFactory.isShutdown());
    }

    @Test
    public void verifyStorageCommandSetsUpDAOFactory() {
        LauncherImpl launcher = new LauncherImpl(bundleContext, ctxFactory, registry);
        Keyring keyring = mock(Keyring.class);
        Bundle sysBundle = mock(Bundle.class);
        PowerMockito.mockStatic(FrameworkUtil.class);
        when(FrameworkUtil.getBundle(OSGIUtils.class)).thenReturn(sysBundle);
        when(sysBundle.getBundleContext()).thenReturn(bundleContext);
        PowerMockito.mockStatic(DbServiceFactory.class);
        String dbUrl = "mongo://fluff:12345";
        DbService dbService = mock(DbService.class);
        when(DbServiceFactory.createDbService(null, null, dbUrl)).thenReturn(dbService);
        launcher.setPreferences(new ClientPreferences(keyring));
        
        launcher.setArgs(new String[] { "test3" , "--dbUrl", dbUrl });
        launcher.run();
        verify(dbService).connect();
        verify(dbService).setServiceRegistration(any(ServiceRegistration.class));
    }

    @Test
    public void verifyStorageCommandSetsUpDAOFactoryWithAuth() {
        LauncherImpl launcher = new LauncherImpl(bundleContext, ctxFactory, registry);
        Keyring keyring = mock(Keyring.class);
        Bundle sysBundle = mock(Bundle.class);
        PowerMockito.mockStatic(FrameworkUtil.class);
        when(FrameworkUtil.getBundle(OSGIUtils.class)).thenReturn(sysBundle);
        when(sysBundle.getBundleContext()).thenReturn(bundleContext);
        PowerMockito.mockStatic(DbServiceFactory.class);
        String dbUrl = "mongo://fluff:12345";
        String testUser = "testUser";
        String testPasswd = "testPassword";
        DbService dbService = mock(DbService.class);
        when(DbServiceFactory.createDbService(testUser, testPasswd, dbUrl)).thenReturn(dbService);
        
        launcher.setPreferences(new ClientPreferences(keyring));
        
        launcher.setArgs(new String[] { "test3" , "--dbUrl", dbUrl, "--username", testUser, "--password", testPasswd });
        launcher.run();
        verify(dbService).connect();
        verify(dbService).setServiceRegistration(any(ServiceRegistration.class));
    }

    public void verifyPrefsAreUsed() {
        ClientPreferences prefs = mock(ClientPreferences.class);
        String dbUrl = "mongo://fluff:12345";
        when(prefs.getConnectionUrl()).thenReturn(dbUrl);
        LauncherImpl l = new LauncherImpl(bundleContext, ctxFactory, registry);
        Bundle sysBundle = mock(Bundle.class);
        PowerMockito.mockStatic(FrameworkUtil.class);
        when(FrameworkUtil.getBundle(OSGIUtils.class)).thenReturn(sysBundle);
        when(sysBundle.getBundleContext()).thenReturn(bundleContext);
        PowerMockito.mockStatic(DbServiceFactory.class);
        DbService dbService = mock(DbService.class);
        // this makes sure that dbUrl is indeed retrieved from preferences
        when(DbServiceFactory.createDbService(null, null, dbUrl)).thenReturn(dbService);
        l.setPreferences(prefs);
        l.setArgs(new String[] { "test3" });
        l.run();
        verify(dbService).connect();
        verify(dbService).setServiceRegistration(any(ServiceRegistration.class));
    }
    
    @Test
    public void verifyVersionInfoQuery() {
        int major = 0;
        int minor = 3;
        int micro = 0;
        
        ApplicationInfo appInfo = new ApplicationInfo();
        Translate t = LocaleResources.createLocalizer();
        String format = MessageFormat.format(
                t.localize(LocaleResources.APPLICATION_VERSION_INFO),
                appInfo.getName())
                + " " + Version.VERSION_NUMBER_FORMAT;
        
        String expectedVersionInfo = String.format(format,
                major, minor, micro) + "\n";
        
        String qualifier = "201207241700";
        Bundle sysBundle = mock(Bundle.class);
        Bundle framework = mock(Bundle.class);
        org.osgi.framework.Version ver = org.osgi.framework.Version
                .parseVersion(String.format(Version.VERSION_NUMBER_FORMAT,
                        major, minor, micro) + "." + qualifier);
        when(sysBundle.getVersion()).thenReturn(ver);
        bundleContext = mock(BundleContext.class);
        when(bundleContext.getBundle(0)).thenReturn(framework);
        
        PowerMockito.mockStatic(FrameworkUtil.class);
        when(FrameworkUtil.getBundle(Version.class)).thenReturn(sysBundle);
        LauncherImpl launcher = new LauncherImpl(bundleContext, ctxFactory, registry);
        
        launcher.setArgs(new String[] {Version.VERSION_OPTION});
        launcher.run();
        assertEquals(expectedVersionInfo, ctxFactory.getOutput());
        assertTrue(timerFactory.isShutdown());
    }

    @Test
    public void verifyListenersAdded() {
        ActionListener<ApplicationState> listener = mock(ActionListener.class);
        Collection<ActionListener<ApplicationState>> listeners = new ArrayList<>();
        listeners.add(listener);
        String[] args = new String[] {"basic"};
        LauncherImpl launcher = new LauncherImpl(bundleContext, ctxFactory, registry);
        Keyring keyring = mock(Keyring.class);
        launcher.setPreferences(new ClientPreferences(keyring));

        launcher.setArgs(args);
        launcher.run(listeners);
        verify(notifier).addActionListener(listener);
    }
}
