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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.ExecutorService;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.redhat.thermostat.common.ActionListener;
import com.redhat.thermostat.common.ActionNotifier;
import com.redhat.thermostat.common.ApplicationInfo;
import com.redhat.thermostat.common.ApplicationService;
import com.redhat.thermostat.common.Version;
import com.redhat.thermostat.common.cli.AbstractStateNotifyingCommand;
import com.redhat.thermostat.common.cli.Arguments;
import com.redhat.thermostat.common.cli.Command;
import com.redhat.thermostat.common.cli.CommandContext;
import com.redhat.thermostat.common.cli.CommandException;
import com.redhat.thermostat.common.cli.CommandInfo;
import com.redhat.thermostat.common.cli.CommandInfoNotFoundException;
import com.redhat.thermostat.common.cli.CommandInfoSource;
import com.redhat.thermostat.common.config.ClientPreferences;
import com.redhat.thermostat.common.locale.LocaleResources;
import com.redhat.thermostat.common.locale.Translate;
import com.redhat.thermostat.common.tools.ApplicationState;
import com.redhat.thermostat.launcher.BundleManager;
import com.redhat.thermostat.launcher.TestCommand;
import com.redhat.thermostat.launcher.internal.HelpCommand;
import com.redhat.thermostat.launcher.internal.LauncherImpl;
import com.redhat.thermostat.launcher.internal.LauncherImpl.LoggingInitializer;
import com.redhat.thermostat.storage.core.DbService;
import com.redhat.thermostat.storage.core.DbServiceFactory;
import com.redhat.thermostat.storage.core.Storage;
import com.redhat.thermostat.test.TestCommandContextFactory;
import com.redhat.thermostat.test.TestTimerFactory;
import com.redhat.thermostat.testutils.StubBundleContext;
import com.redhat.thermostat.utils.keyring.Keyring;
import com.redhat.thermostat.utils.keyring.KeyringProvider;

@RunWith(PowerMockRunner.class)
@PrepareForTest({FrameworkUtil.class, HelpCommand.class})
public class LauncherImplTest {
    
    private static String defaultKeyringProvider;
    private static final String name1 = "test1";
    private static final String name2 = "test2";
    private static final String name3 = "test3";
      
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
    private StubBundleContext bundleContext;
    private Bundle sysBundle;
    private TestTimerFactory timerFactory;
    private BundleManager registry;
    private LoggingInitializer loggingInitializer;
    private DbServiceFactory dbServiceFactory;
    private CommandInfoSource infos;
    private ActionNotifier<ApplicationState> notifier;

    private LauncherImpl launcher;
    private Storage storage;

    @Before
    public void setUp() {
        setupCommandContextFactory();

        TestCommand cmd1 = new TestCommand(name1, new TestCmd1());
        CommandInfo info1 = mock(CommandInfo.class);
        when(info1.getName()).thenReturn(name1);
        when(info1.getUsage()).thenReturn(name1 + " <--arg1 <arg>> [--arg2 <arg>]");
        Options options1 = new Options();
        Option opt1 = new Option(null, "arg1", true, null);
        opt1.setRequired(true);
        options1.addOption(opt1);
        Option opt2 = new Option(null, "arg2", true, null);
        options1.addOption(opt2);
        // cmd1 needs logLevel option since it is used in tests if logLevel
        // option is properly set up
        Option logLevel = new Option("l", "logLevel", true, null);
        options1.addOption(logLevel);
        cmd1.addOptions(opt1, opt2, logLevel);
        cmd1.setDescription("description 1");
        when(info1.getDescription()).thenReturn("description 1");
        when(info1.getOptions()).thenReturn(options1);
        TestCommand cmd2 = new TestCommand("test2", new TestCmd2());
        CommandInfo info2 = mock(CommandInfo.class);
        when(info2.getName()).thenReturn(name2);
        Options options2 = new Options();
        Option opt3 = new Option(null, "arg3", true, null);
        options2.addOption(opt3);
        Option opt4 = new Option(null, "arg4", true, null);
        options2.addOption(opt4);
        cmd2.addOptions(opt3, opt4);
        cmd2.setDescription("description 2");
        when(info2.getDescription()).thenReturn("description 2");
        when(info2.getOptions()).thenReturn(options2);

        TestCommand cmd3 = new TestCommand(name3);
        CommandInfo info3 = mock(CommandInfo.class);
        when(info3.getName()).thenReturn(name3);
        cmd3.setStorageRequired(true);
        cmd3.setDescription("description 3");
        when(info3.getDescription()).thenReturn("description 3");
        when(info3.getOptions()).thenReturn(new Options());

        AbstractStateNotifyingCommand basicCmd = mock(AbstractStateNotifyingCommand.class);
        CommandInfo basicInfo = mock(CommandInfo.class);
        when(basicCmd.getName()).thenReturn("basic");
        when(basicInfo.getName()).thenReturn("basic");
        when(basicCmd.getDescription()).thenReturn("nothing that means anything");
        when(basicInfo.getDescription()).thenReturn("nothing that means anything");
        when(basicCmd.isStorageRequired()).thenReturn(false);
        when(basicCmd.isAvailableInShell()).thenReturn(true);
        when(basicCmd.isAvailableOutsideShell()).thenReturn(true);
        Options options = new Options();
        when(basicCmd.getOptions()).thenReturn(options);
        when(basicInfo.getOptions()).thenReturn(options);
        notifier = mock(ActionNotifier.class);
        when(basicCmd.getNotifier()).thenReturn(notifier);
        CommandInfo helpCommandInfo = mock(CommandInfo.class);
        when(helpCommandInfo.getName()).thenReturn("help");
        when(helpCommandInfo.getDescription()).thenReturn("print help information");
        when(helpCommandInfo.getDependencyResourceNames()).thenReturn(new ArrayList<String>());
        when(helpCommandInfo.getOptions()).thenReturn(new Options());
        when(helpCommandInfo.getUsage()).thenReturn("thermostat help");

        HelpCommand helpCommand = new HelpCommand();

        ctxFactory.getCommandRegistry().registerCommands(Arrays.asList(helpCommand, cmd1, cmd2, cmd3, basicCmd));

        registry = mock(BundleManager.class);

        infos = mock(CommandInfoSource.class);
        when(infos.getCommandInfo(name1)).thenReturn(info1);
        when(infos.getCommandInfo(name2)).thenReturn(info2);
        when(infos.getCommandInfo(name3)).thenReturn(info3);
        when(infos.getCommandInfo("basic")).thenReturn(basicInfo);
        when(infos.getCommandInfo("help")).thenReturn(helpCommandInfo);

        Collection<CommandInfo> infoList = new ArrayList<CommandInfo>();
        infoList.add(helpCommandInfo);
        infoList.add(basicInfo);
        infoList.add(info1);
        infoList.add(info2);
        infoList.add(info3);
        when(infos.getCommandInfos()).thenReturn(infoList);

        helpCommand.setCommandInfoSource(infos);

        PowerMockito.mockStatic(FrameworkUtil.class);

        storage = mock(Storage.class);
        ServiceReference storageRef = mock(ServiceReference.class);
        Bundle launcherBundle = mock(Bundle.class);
        BundleContext launcherBundleCtx = mock(BundleContext.class);
        when(launcherBundleCtx.getServiceReference(Storage.class)).thenReturn(storageRef);
        when(launcherBundleCtx.getService(storageRef)).thenReturn(storage);
        when(launcherBundle.getBundleContext()).thenReturn(launcherBundleCtx);
        when(FrameworkUtil.getBundle(LauncherImpl.class)).thenReturn(launcherBundle);

        timerFactory = new TestTimerFactory();
        ExecutorService exec = mock(ExecutorService.class);
        ApplicationService appSvc = mock(ApplicationService.class);
        when(appSvc.getTimerFactory()).thenReturn(timerFactory);
        when(appSvc.getApplicationExecutor()).thenReturn(exec);
        bundleContext.registerService(ApplicationService.class, appSvc, null);

        loggingInitializer = mock(LoggingInitializer.class);
        dbServiceFactory = mock(DbServiceFactory.class);

        launcher = new LauncherImpl(bundleContext, ctxFactory, registry, loggingInitializer, dbServiceFactory);

        Keyring keyring = mock(Keyring.class);
        launcher.setPreferences(new ClientPreferences(keyring));
    }

    private void setupCommandContextFactory() {
        sysBundle = mock(Bundle.class);
        bundleContext = new StubBundleContext();
        bundleContext.setBundle(0, sysBundle);
        ctxFactory = new TestCommandContextFactory(bundleContext);
    }


    @After
    public void tearDown() {
        ctxFactory = null;
        bundleContext = null;
    }

    @Test
    public void testMain() {
        runAndVerifyCommand(new String[] {name1, "--arg1", "Hello", "--arg2", "World"}, "Hello, World", false);

        ctxFactory.reset();

        runAndVerifyCommand(new String[] {"test2", "--arg3", "Hello", "--arg4", "World"}, "World: Hello", false);
    }

    @Test
    public void testMainNoArgs() {
        String expected = "list of commands:\n\n"
                        + " help          print help information\n"
                        + " basic         nothing that means anything\n"
                        + " test1         description 1\n"
                        + " test2         description 2\n"
                        + " test3         description 3\n";
        runAndVerifyCommand(new String[0], expected, false);
    }

    @Test
    public void verifySetLogLevel() {
        runAndVerifyCommand(new String[] {name1, "--logLevel", "WARNING", "--arg1", "Hello", "--arg2", "World"}, "Hello, World", false);
        Logger globalLogger = Logger.getLogger("com.redhat.thermostat");
        assertEquals(Level.WARNING, globalLogger.getLevel());
    }

    @Test
    public void testMainBadCommand1() {
        when(infos.getCommandInfo("--help")).thenThrow(new CommandInfoNotFoundException("--help"));

        String expected = "unknown command '--help'\n"
            + "list of commands:\n\n"
            + " help          print help information\n"
            + " basic         nothing that means anything\n"
            + " test1         description 1\n"
            + " test2         description 2\n"
            + " test3         description 3\n";
        runAndVerifyCommand(new String[] {"--help"}, expected, false);
    }

    @Test
    public void testMainBadCommand2() {
        when(infos.getCommandInfo("-help")).thenThrow(new CommandInfoNotFoundException("-help"));

        String expected = "unknown command '-help'\n"
            + "list of commands:\n\n"
            + " help          print help information\n"
            + " basic         nothing that means anything\n"
            + " test1         description 1\n"
            + " test2         description 2\n"
            + " test3         description 3\n";
        runAndVerifyCommand(new String[] {"-help"}, expected, false);
    }

    @Test
    public void testMainBadCommand3() {
        when(infos.getCommandInfo("foobarbaz")).thenThrow(new CommandInfoNotFoundException("foobarbaz"));

        String expected = "unknown command 'foobarbaz'\n"
            + "list of commands:\n\n"
            + " help          print help information\n"
            + " basic         nothing that means anything\n"
            + " test1         description 1\n"
            + " test2         description 2\n"
            + " test3         description 3\n";
        runAndVerifyCommand(new String[] {"foobarbaz"}, expected, false);
    }

    @Test
    public void testMainBadCommand4() {
        when(infos.getCommandInfo("foo")).thenThrow(new CommandInfoNotFoundException("foo"));

        String expected = "unknown command 'foo'\n"
            + "list of commands:\n\n"
            + " help          print help information\n"
            + " basic         nothing that means anything\n"
            + " test1         description 1\n"
            + " test2         description 2\n"
            + " test3         description 3\n";
        runAndVerifyCommand(new String[] {"foo",  "--bar", "baz"}, expected, false);
    }

    @Test
    public void testBadOption() {
        String expected = "Unrecognized option: --argNotAccepted\n"
                + "usage: thermostat test1 <--arg1 <arg>> [--arg2 <arg>]\n"
                + "                  description 1\n"
                + "thermostat test1\n"
                + "     --arg1 <arg>\n"
                + "     --arg2 <arg>\n"
                + "  -l,--logLevel <arg>\n";
        runAndVerifyCommand(new String[] {"test1", "--arg1", "arg1value", "--argNotAccepted"}, expected, false);
    }

    @Test
    public void testMissingRequiredOption() {
        String expected = "Missing required option: --arg1\n"
                + "usage: thermostat test1 <--arg1 <arg>> [--arg2 <arg>]\n"
                + "                  description 1\n"
                + "thermostat test1\n"
                + "     --arg1 <arg>\n"
                + "     --arg2 <arg>\n"
                + "  -l,--logLevel <arg>\n";
        runAndVerifyCommand(new String[] {"test1"}, expected, false);
    }

    @Test
    public void testOptionMissingRequiredArgument() {
        String expected = "Missing argument for option: arg1\n"
                + "usage: thermostat test1 <--arg1 <arg>> [--arg2 <arg>]\n"
                + "                  description 1\n"
                + "thermostat test1\n"
                + "     --arg1 <arg>\n"
                + "     --arg2 <arg>\n"
                + "  -l,--logLevel <arg>\n";
        runAndVerifyCommand(new String[] {"test1", "--arg1"}, expected, false);
    }

    @Test
    public void testCommandInfoNotFound() throws CommandInfoNotFoundException, BundleException, IOException {
        when(infos.getCommandInfo("foo")).thenThrow(new CommandInfoNotFoundException("foo"));
        doThrow(new CommandInfoNotFoundException("foo")).when(registry).addBundlesFor("foo");

        String expected = "unknown command 'foo'\n"
                + "list of commands:\n\n"
                + " help          print help information\n"
                + " basic         nothing that means anything\n"
                + " test1         description 1\n"
                + " test2         description 2\n"
                + " test3         description 3\n";
            runAndVerifyCommand(new String[] {"foo"}, expected, false);
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

        launcher.setArgs(new String[] { "error" });
        launcher.run(false);
        assertEquals("test error\n", ctxFactory.getError());

    }

    private void runAndVerifyCommand(String[] args, String expected, boolean inShell) {
        launcher.setArgs(args);
        launcher.run(inShell);
        assertEquals(expected, ctxFactory.getOutput());
        assertTrue(timerFactory.isShutdown());
    }

    @Test
    public void verifyPrefsAreUsed() {
        ClientPreferences prefs = mock(ClientPreferences.class);
        String dbUrl = "mongo://fluff:12345";
        when(prefs.getConnectionUrl()).thenReturn(dbUrl);

        DbService dbService = mock(DbService.class);
        ArgumentCaptor<String> dbUrlCaptor = ArgumentCaptor.forClass(String.class);
        when(dbServiceFactory.createDbService(anyString(), anyString(), dbUrlCaptor.capture())).thenReturn(dbService);
        launcher.setPreferences(prefs);
        launcher.setArgs(new String[] { "test3" });
        launcher.run(false);
        verify(dbService).connect();
        verify(prefs).getConnectionUrl();
        assertEquals(dbUrl, dbUrlCaptor.getValue());
    }

    @Test
    public void verifyDbServiceConnectIsCalledForStorageCommand() throws Exception {
        Command mockCmd = mock(Command.class);
        when(mockCmd.getName()).thenReturn("dummy");
        when(mockCmd.isStorageRequired()).thenReturn(true);
        Options options = mock(Options.class);
        when(mockCmd.getOptions()).thenReturn(options);
        when(mockCmd.isAvailableInShell()).thenReturn(true);
        when(mockCmd.isAvailableOutsideShell()).thenReturn(true);
        
        ctxFactory.getCommandRegistry().registerCommand(mockCmd);
        
        DbService dbService = mock(DbService.class);
        when(dbServiceFactory.createDbService(anyString(), anyString(), anyString())).thenReturn(dbService);

        launcher.setArgs(new String[] { "dummy" });
        launcher.run(false);
        verify(dbService).connect();
    }

    @Test
    public void verifyVersionInfoQuery() {
        int major = 0;
        int minor = 3;
        int micro = 0;
        
        ApplicationInfo appInfo = new ApplicationInfo();
        Translate<LocaleResources> t = LocaleResources.createLocalizer();
        String format = MessageFormat.format(
                t.localize(LocaleResources.APPLICATION_VERSION_INFO),
                appInfo.getName())
                + " " + Version.VERSION_NUMBER_FORMAT;
        
        String expectedVersionInfo = String.format(format,
                major, minor, micro) + "\n";
        
        String qualifier = "201207241700";

        org.osgi.framework.Version ver = org.osgi.framework.Version
                .parseVersion(String.format(Version.VERSION_NUMBER_FORMAT,
                        major, minor, micro) + "." + qualifier);
        when(sysBundle.getVersion()).thenReturn(ver);
        
        PowerMockito.mockStatic(FrameworkUtil.class);
        when(FrameworkUtil.getBundle(Version.class)).thenReturn(sysBundle);
        launcher.setArgs(new String[] {Version.VERSION_OPTION});
        launcher.run(false);

        assertEquals(expectedVersionInfo, ctxFactory.getOutput());
        assertTrue(timerFactory.isShutdown());
    }

    @Test
    public void verifyListenersAdded() {
        ActionListener<ApplicationState> listener = mock(ActionListener.class);
        Collection<ActionListener<ApplicationState>> listeners = new ArrayList<>();
        listeners.add(listener);
        String[] args = new String[] {"basic"};

        launcher.setArgs(args);
        launcher.run(listeners, false);
        verify(notifier).addActionListener(listener);
    }

    @Test
    public void verifyLoggingIsInitialized() {
        launcher.setArgs(new String[] { "test1" });
        launcher.run(false);

        verify(loggingInitializer).initialize();
    }

    @Test
    public void verifyShutdown() throws BundleException {
        launcher.setArgs(new String[] { "test1" });
        launcher.run(false);

        verify(sysBundle).stop();
    }

    @Test
    public void verifyCommandSupportedInShellBehavesNormally() {
    	runWithShellStatus(true, "fluff", true, true, "");
    }

    @Test
    public void verifyCommandSupportedOutsideShellBehavesNormally() {
    	runWithShellStatus(false, "fluff", true, true, "");
    }

    @Test
    public void verifyCommandNotSupportedInShellDisplaysMessage() {
    	runWithShellStatus(true, "fluff", false, true, "The fluff command is not supported from within the thermostat shell.\n");
    }

    @Test
    public void verifyCommandNotSupportedOutsideShellDisplaysMessage() {
    	runWithShellStatus(false, "fluff", true, false, "The fluff command is not supported from outside the thermostat shell.\n");
    }

    private void runWithShellStatus(boolean isInShell, String cmdName, boolean isAvailableInShell,
    		boolean isAvailableOutsideShell, String expected) {
    	Command mockCmd = mock(Command.class);
        when(mockCmd.getName()).thenReturn(cmdName);
        when(mockCmd.isStorageRequired()).thenReturn(false);
        Options options = mock(Options.class);
        when(mockCmd.getOptions()).thenReturn(options);
        when(mockCmd.isAvailableInShell()).thenReturn(isAvailableInShell);
        when(mockCmd.isAvailableOutsideShell()).thenReturn(isAvailableOutsideShell);

        ctxFactory.getCommandRegistry().registerCommand(mockCmd);
        runAndVerifyCommand(new String[] { cmdName }, expected, isInShell);
    }
}

