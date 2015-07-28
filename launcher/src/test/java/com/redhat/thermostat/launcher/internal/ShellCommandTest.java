/*
 * Copyright 2012-2015 Red Hat, Inc.
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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.EnumSet;

import com.redhat.thermostat.common.config.ClientPreferences;
import jline.TerminalFactory;
import jline.TerminalFactory.Flavor;
import jline.TerminalFactory.Type;
import jline.UnixTerminal;
import jline.console.history.PersistentHistory;

import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

import com.redhat.thermostat.launcher.internal.ShellCommand.HistoryProvider;
import com.redhat.thermostat.common.Version;
import com.redhat.thermostat.common.cli.Arguments;
import com.redhat.thermostat.common.cli.CommandContext;
import com.redhat.thermostat.common.cli.CommandException;
import com.redhat.thermostat.common.cli.SimpleArguments;
import com.redhat.thermostat.common.config.experimental.ConfigurationInfoSource;
import com.redhat.thermostat.common.utils.StringUtils;
import com.redhat.thermostat.launcher.Launcher;
import com.redhat.thermostat.test.TestCommandContextFactory;

public class ShellCommandTest {

    static private final String VERSION = "Thermostat some version";
    static private final String VERSION_OUTPUT = VERSION + "\n";
    static private final String USER_GUIDE_OUTPUT = "Please see the User Guide at http://icedtea.classpath.org/wiki/Thermostat/UserGuide\n";
    static private final String INTRO = VERSION_OUTPUT + USER_GUIDE_OUTPUT;
    static private final String PROMPT = "Thermostat " + ShellPrompt.DEFAULT_DISCONNECTED_TOKEN + " > ";

    private ShellCommand cmd;

    private BundleContext bundleContext;
    private HistoryProvider historyProvider;
    private Version version;
    private ConfigurationInfoSource config;
    private CommandInfoSource infos;
    private File dir;
    private ClientPreferences prefs;

    @Before
    public void setUp() {
        bundleContext = mock(BundleContext.class);
        historyProvider = mock(HistoryProvider.class);
        version = mock(Version.class);
        when(version.getVersionInfo()).thenReturn(VERSION);
        config = mock(ConfigurationInfoSource.class);

        prefs = mock(ClientPreferences.class);
        when(prefs.getConnectionUrl()).thenReturn("http://127.0.0.1:mockStorage");
        infos = mock(CommandInfoSource.class);
        cmd = new ShellCommand(bundleContext, version, historyProvider, config, prefs);
        setupCommandInfoSource();

        dir = new File(System.getProperty("java.io.tmpdir") + File.separator + "shellcommand");
        dir.deleteOnExit();
        dir.mkdirs();
    }

    @After
    public void tearDown() {
        cmd = null;
        bundleContext = null;
        TerminalFactory.registerFlavor(Flavor.UNIX, UnixTerminal.class);
        TerminalFactory.reset();
    }

    @Test
    public void testBasic() throws CommandException {
        ServiceReference ref = mock(ServiceReference.class);
        
        when(bundleContext.getServiceReference(Launcher.class.getName())).thenReturn(ref);
        Launcher launcher = mock(Launcher.class);
        when(bundleContext.getService(ref)).thenReturn(launcher);
        TestCommandContextFactory ctxFactory = new TestCommandContextFactory(bundleContext);
        ctxFactory.setInput("help\nexit\n");
        Arguments args = new SimpleArguments();
        CommandContext ctx = ctxFactory.createContext(args);
        cmd.run(ctx);
        verify(launcher).run(new String[]{"help"}, true);
    }

    @Test
    public void testExtraSpacesAreIgnored() throws CommandException {
        ServiceReference ref = mock(ServiceReference.class);
        when(bundleContext.getServiceReference(Launcher.class.getName())).thenReturn(ref);
        Launcher launcher = mock(Launcher.class);
        when(bundleContext.getService(ref)).thenReturn(launcher);
        TestCommandContextFactory ctxFactory = new TestCommandContextFactory(bundleContext);
        ctxFactory.setInput("foo   bar\nexit\n");
        Arguments args = new SimpleArguments();
        CommandContext ctx = ctxFactory.createContext(args);
        cmd.run(ctx);
        verify(launcher).run(new String[]{"foo", "bar"}, true);
    }

    @Test
    public void testQuitAlsoExits() throws CommandException {
        TestCommandContextFactory ctxFactory = new TestCommandContextFactory();
        ctxFactory.setInput("quit\n");
        Arguments args = new SimpleArguments();
        CommandContext ctx = ctxFactory.createContext(args);
        cmd.run(ctx);
        assertEquals(INTRO + PROMPT + "quit\n", ctxFactory.getOutput());
        assertEquals("", ctxFactory.getError());
    }

    @Test
    public void testQAlsoExits() throws CommandException {
        TestCommandContextFactory ctxFactory = new TestCommandContextFactory();
        ctxFactory.setInput("q\n");
        Arguments args = new SimpleArguments();
        CommandContext ctx = ctxFactory.createContext(args);
        cmd.run(ctx);
        assertEquals(INTRO + PROMPT + "q\n", ctxFactory.getOutput());
        assertEquals("", ctxFactory.getError());
    }

    @Test
    public void testEofExits() throws CommandException {
        TestCommandContextFactory ctxFactory = new TestCommandContextFactory();
        ctxFactory.setInput("\u0004"); // EOF
        Arguments args = new SimpleArguments();
        CommandContext ctx = ctxFactory.createContext(args);
        cmd.run(ctx);
        assertEquals(INTRO + PROMPT, ctxFactory.getOutput());
        assertEquals("", ctxFactory.getError());
    }

    @Test
    public void testDoNothingWithoutInput() throws CommandException {
        TestCommandContextFactory ctxFactory = new TestCommandContextFactory();
        ctxFactory.setInput("\nexit\n");
        Arguments args = new SimpleArguments();
        CommandContext ctx = ctxFactory.createContext(args);
        cmd.run(ctx);
        assertEquals(INTRO + PROMPT + "\n" + PROMPT + "exit\n", ctxFactory.getOutput());
    }

    @Test
    public void testHistoryIsQueried() throws CommandException {
        PersistentHistory history = mock(PersistentHistory.class);
        when(history.previous()).thenReturn(true);
        when(history.current()).thenReturn("old-history-value");

        when(historyProvider.get()).thenReturn(history);

        ServiceReference ref = mock(ServiceReference.class);
        
        when(bundleContext.getServiceReference(Launcher.class.getName())).thenReturn(ref);
        Launcher launcher = mock(Launcher.class);
        when(bundleContext.getService(ref)).thenReturn(launcher);
        TestCommandContextFactory ctxFactory = new TestCommandContextFactory(bundleContext);

        // "\u001b[A" is the escape code for up-arrow. use xxd -p to generate
        ctxFactory.setInput("\u001b[A\nexit\n");
        Arguments args = new SimpleArguments();
        CommandContext ctx = ctxFactory.createContext(args);
        cmd.run(ctx);

        assertEquals(INTRO + PROMPT + "old-history-value\n" + PROMPT + "exit\n", ctxFactory.getOutput());
        assertEquals("", ctxFactory.getError());

        verify(launcher).run(new String[] {"old-history-value"}, true);
    }

    @Test
    public void testHistoryIsUpdated() throws CommandException, IOException {
        PersistentHistory mockHistory = mock(PersistentHistory.class);
        when(historyProvider.get()).thenReturn(mockHistory);

        ServiceReference ref = mock(ServiceReference.class);
        when(bundleContext.getServiceReference(Launcher.class.getName())).thenReturn(ref);
        Launcher launcher = mock(Launcher.class);
        when(bundleContext.getService(ref)).thenReturn(launcher);
        TestCommandContextFactory ctxFactory = new TestCommandContextFactory(bundleContext);
        
        ctxFactory.setInput("add-to-history\nexit\n");
        Arguments args = new SimpleArguments();
        CommandContext ctx = ctxFactory.createContext(args);
        cmd.run(ctx);

        verify(launcher).run(new String[] {"add-to-history"}, true);
        verify(mockHistory).add("add-to-history");
        verify(mockHistory).flush();

        assertEquals(INTRO + PROMPT + "add-to-history\n" + PROMPT + "exit\n", ctxFactory.getOutput());
        assertEquals("", ctxFactory.getError());
    }

    @Test(expected=CommandException.class)
    public void testIOException() throws CommandException {
        TestCommandContextFactory ctxFactory = new TestCommandContextFactory();
        ctxFactory.setInputThrowsException(new IOException());
        Arguments args = new SimpleArguments();
        CommandContext ctx = ctxFactory.createContext(args);
        cmd.run(ctx);
    }

    @Test(expected=CommandException.class)
    public void testTerminalRestoreException() throws CommandException {
        TerminalFactory.configure(Type.UNIX);
        TerminalFactory.registerFlavor(Flavor.UNIX, TestTerminal.class);
        TestCommandContextFactory ctxFactory = new TestCommandContextFactory();
        ctxFactory.setInputThrowsException(new IOException());
        Arguments args = new SimpleArguments();
        CommandContext ctx = ctxFactory.createContext(args);
        cmd.run(ctx);
    }

    @Test
    public void testStorageRequired() {
        assertFalse(cmd.isStorageRequired());
    }

    @Test
    public void testNoInputTabCompletes() throws CommandException {
        ServiceReference ref = mock(ServiceReference.class);
        when(bundleContext.getServiceReference(Launcher.class.getName())).thenReturn(ref);
        Launcher launcher = mock(Launcher.class);
        when(bundleContext.getService(ref)).thenReturn(launcher);

        TestCommandContextFactory ctxFactory = new TestCommandContextFactory();
        ctxFactory.setInput("\t\nq\n");
        Arguments args = new SimpleArguments();
        CommandContext ctx = ctxFactory.createContext(args);
        cmd.run(ctx);

        String tabOutput = getTabOutput(getOutputWithoutIntro(ctxFactory));

        assertTrue(tabOutput.contains("test1"));
        assertTrue(tabOutput.contains("test2longname"));
        assertEquals("", ctxFactory.getError());
    }

    private String getOutputWithoutIntro(final TestCommandContextFactory ctxFactory) {
        String[] allOutput = ctxFactory.getOutput().split("\n");
        String[] outputWithoutIntro = Arrays.copyOfRange(allOutput, 2, allOutput.length);
        return StringUtils.join("\n", Arrays.asList(outputWithoutIntro));
    }

    private String getTabOutput(final String outputToProcess) {
        String[] allOutput = outputToProcess.split("\n");
        String tabOutput = "";
        for (String output : allOutput) {
            if (!output.startsWith("Thermostat")) {
                tabOutput += output;
            }
        }
        return tabOutput;
    }

    @Test
    public void testPartiallyFilledInputTabCompletes() throws CommandException {
        ServiceReference ref = mock(ServiceReference.class);
        when(bundleContext.getServiceReference(Launcher.class.getName())).thenReturn(ref);
        Launcher launcher = mock(Launcher.class);
        when(bundleContext.getService(ref)).thenReturn(launcher);

        TestCommandContextFactory ctxFactory = new TestCommandContextFactory(bundleContext);
        ctxFactory.setInput("t\t\nexit\n");
        Arguments args = new SimpleArguments();
        CommandContext ctx = ctxFactory.createContext(args);
        cmd.run(ctx);

        String tabOutput = getTabOutput(getOutputWithoutIntro(ctxFactory));
        assertTrue(tabOutput.contains("test1"));
        assertTrue(tabOutput.contains("test2longname"));
        assertEquals("", ctxFactory.getError());
    }

    @Test
    public void testFullCommandDoesNotTabComplete() throws CommandException {
        ServiceReference ref = mock(ServiceReference.class);
        when(bundleContext.getServiceReference(Launcher.class.getName())).thenReturn(ref);
        Launcher launcher = mock(Launcher.class);
        when(bundleContext.getService(ref)).thenReturn(launcher);

        TestCommandContextFactory ctxFactory = new TestCommandContextFactory();
        ctxFactory.setInput("test1\t\nq\n");
        Arguments args = new SimpleArguments();
        CommandContext ctx = ctxFactory.createContext(args);
        cmd.run(ctx);

        String usefulOutput = getOutputWithoutIntro(ctxFactory);
        String tabOutput = getTabOutput(usefulOutput);
        assertTrue(tabOutput.length() == 0);
        assertEquals(PROMPT + "q", usefulOutput.split("\n")[1]);
        assertEquals("", ctxFactory.getError());
    }

    @Test
    public void testFullInputWithSpaceTabCompletes() throws CommandException {
        ServiceReference ref = mock(ServiceReference.class);
        when(bundleContext.getServiceReference(Launcher.class.getName())).thenReturn(ref);
        Launcher launcher = mock(Launcher.class);
        when(bundleContext.getService(ref)).thenReturn(launcher);

        TestCommandContextFactory ctxFactory = new TestCommandContextFactory(bundleContext);
        ctxFactory.setInput("test1 \t\nexit\n");
        Arguments args = new SimpleArguments();
        CommandContext ctx = ctxFactory.createContext(args);
        cmd.run(ctx);

        String tabOutput = getTabOutput(getOutputWithoutIntro(ctxFactory));
        assertTrue(tabOutput.contains("--Add"));
        assertTrue(tabOutput.contains("--remove"));
        assertTrue(tabOutput.contains("--test"));
        assertTrue(tabOutput.contains("-A"));
        assertTrue(tabOutput.contains("-r"));
        assertTrue(tabOutput.contains("-t"));
        assertEquals("", ctxFactory.getError());

    }

    @Test
    public void testOptionWithOnlyFirstLetterTabCompletes() throws CommandException {
        ServiceReference ref = mock(ServiceReference.class);
        when(bundleContext.getServiceReference(Launcher.class.getName())).thenReturn(ref);
        Launcher launcher = mock(Launcher.class);
        when(bundleContext.getService(ref)).thenReturn(launcher);

        TestCommandContextFactory ctxFactory = new TestCommandContextFactory(bundleContext);
        ctxFactory.setInput("test2longname --c\t\nexit\n");
        Arguments args = new SimpleArguments();
        CommandContext ctx = ctxFactory.createContext(args);
        cmd.run(ctx);

        String tabOutput = getTabOutput(getOutputWithoutIntro(ctxFactory));
        assertTrue(tabOutput.contains("--copy"));
        assertTrue(tabOutput.contains("--copy&paste"));
        assertEquals("", ctxFactory.getError());
    }

    @Test
    public void testOptionWithPartiallyFilledInputTabCompletes() throws CommandException {
        ServiceReference ref = mock(ServiceReference.class);
        when(bundleContext.getServiceReference(Launcher.class.getName())).thenReturn(ref);
        Launcher launcher = mock(Launcher.class);
        when(bundleContext.getService(ref)).thenReturn(launcher);

        TestCommandContextFactory ctxFactory = new TestCommandContextFactory(bundleContext);
        ctxFactory.setInput("test1 te\t\nexit\n");
        Arguments args = new SimpleArguments();
        CommandContext ctx = ctxFactory.createContext(args);
        cmd.run(ctx);

        String usefulOutput = getOutputWithoutIntro(ctxFactory);
        String tabOutput = getTabOutput(usefulOutput);
        assertTrue(tabOutput.length() == 0);
        assertEquals(PROMPT + "exit", usefulOutput.split("\n")[1]);
        assertEquals("", ctxFactory.getError());

    }

    @Test
    public void testDbUrlOptionCompletes() throws CommandException {
        ServiceReference ref = mock(ServiceReference.class);
        when(bundleContext.getServiceReference(Launcher.class.getName())).thenReturn(ref);
        Launcher launcher = mock(Launcher.class);
        when(bundleContext.getService(ref)).thenReturn(launcher);

        TestCommandContextFactory ctxFactory = new TestCommandContextFactory(bundleContext);
        ctxFactory.setInput("validate --dbUrl \t\nexit\n");
        Arguments args = new SimpleArguments();
        CommandContext ctx = ctxFactory.createContext(args);
        cmd.run(ctx);

        String usefulOutput = getOutputWithoutIntro(ctxFactory);
        String tabOutput = getTabOutput(usefulOutput);
        assertTrue(tabOutput.length() == 0);
        assertEquals(PROMPT + "validate --dbUrl http://127.0.0.1:mockStorage ", usefulOutput.split("\n")[0]);
        assertEquals(PROMPT + "exit", usefulOutput.split("\n")[1]);
        assertEquals("", ctxFactory.getError());
    }

    @Test
    public void testFullOptionDoesNotTabComplete() throws CommandException {
        ServiceReference ref = mock(ServiceReference.class);
        when(bundleContext.getServiceReference(Launcher.class.getName())).thenReturn(ref);
        Launcher launcher = mock(Launcher.class);
        when(bundleContext.getService(ref)).thenReturn(launcher);

        TestCommandContextFactory ctxFactory = new TestCommandContextFactory(bundleContext);
        ctxFactory.setInput("test1 test\t\nexit\n");
        Arguments args = new SimpleArguments();
        CommandContext ctx = ctxFactory.createContext(args);
        cmd.run(ctx);

        String usefulOutput = getOutputWithoutIntro(ctxFactory);
        String tabOutput = getTabOutput(usefulOutput);
        assertTrue(tabOutput.length() == 0);
        assertEquals(PROMPT + "exit", usefulOutput.split("\n")[1]);
        assertEquals("", ctxFactory.getError());

    }

    @Test
    public void testFullOptionWithSpaceTabCompletes() throws CommandException {
        ServiceReference ref = mock(ServiceReference.class);
        when(bundleContext.getServiceReference(Launcher.class.getName())).thenReturn(ref);
        Launcher launcher = mock(Launcher.class);
        when(bundleContext.getService(ref)).thenReturn(launcher);

        TestCommandContextFactory ctxFactory = new TestCommandContextFactory(bundleContext);
        ctxFactory.setInput("test2longname --Paste \t\nexit\n");
        Arguments args = new SimpleArguments();
        CommandContext ctx = ctxFactory.createContext(args);
        cmd.run(ctx);

        String tabOutput = getTabOutput(getOutputWithoutIntro(ctxFactory));
        assertTrue(tabOutput.contains("--Paste"));
        assertTrue(tabOutput.contains("--copy"));
        assertTrue(tabOutput.contains("--copy&paste"));
        assertTrue(tabOutput.contains("-c"));
        assertTrue(tabOutput.contains("-p"));
        assertTrue(tabOutput.contains("-v"));
        assertEquals("", ctxFactory.getError());

    }

    @Test
    public void testFullOptionCompletesInline() throws CommandException {
        ServiceReference ref = mock(ServiceReference.class);
        when(bundleContext.getServiceReference(Launcher.class.getName())).thenReturn(ref);
        Launcher launcher = mock(Launcher.class);
        when(bundleContext.getService(ref)).thenReturn(launcher);

        TestCommandContextFactory ctxFactory = new TestCommandContextFactory(bundleContext);

        String input = "test2longn";
        ctxFactory.setInput(input + "\t\nexit\n");
        Arguments args = new SimpleArguments();
        CommandContext ctx = ctxFactory.createContext(args);
        cmd.run(ctx);

        String tabOutput = getTabOutput(getOutputWithoutIntro(ctxFactory));
        String inline = getTabbedInline(ctxFactory, input);

        assertEquals(0, tabOutput.length());
        assertTrue(inline.endsWith("test2longname"));
        assertEquals("", ctxFactory.getError());
    }

    @Test
    public void testSimilarOptionCompletesCommonPortionInline() throws CommandException {
        ServiceReference ref = mock(ServiceReference.class);
        when(bundleContext.getServiceReference(Launcher.class.getName())).thenReturn(ref);
        Launcher launcher = mock(Launcher.class);
        when(bundleContext.getService(ref)).thenReturn(launcher);

        TestCommandContextFactory ctxFactory = new TestCommandContextFactory(bundleContext);

        String input = "tes";
        ctxFactory.setInput(input + "\t\nexit\n");
        Arguments args = new SimpleArguments();
        CommandContext ctx = ctxFactory.createContext(args);
        cmd.run(ctx);

        String tabOutput = getTabOutput(getOutputWithoutIntro(ctxFactory));
        String inline = getTabbedInline(ctxFactory, input);

        assertTrue(tabOutput.length() != 0);
        assertTrue(inline.endsWith("test"));
        assertEquals("", ctxFactory.getError());
    }

    @Test
    public void testSubOptionCompletesInline() throws CommandException {
        ServiceReference ref = mock(ServiceReference.class);
        when(bundleContext.getServiceReference(Launcher.class.getName())).thenReturn(ref);
        Launcher launcher = mock(Launcher.class);
        when(bundleContext.getService(ref)).thenReturn(launcher);

        TestCommandContextFactory ctxFactory = new TestCommandContextFactory(bundleContext);

        String input = "test2longname --Pas";
        ctxFactory.setInput(input + "\t\nexit\n");
        Arguments args = new SimpleArguments();
        CommandContext ctx = ctxFactory.createContext(args);
        cmd.run(ctx);

        String tabOutput = getTabOutput(getOutputWithoutIntro(ctxFactory));
        String inline = getTabbedInline(ctxFactory, input);

        assertEquals(0, tabOutput.length());
        assertTrue(inline.endsWith("--Paste"));
        assertEquals("", ctxFactory.getError());
    }

    @Test
    public void testSimilarSubOptionCompletesCommonPortionInline() throws CommandException {
        ServiceReference ref = mock(ServiceReference.class);
        when(bundleContext.getServiceReference(Launcher.class.getName())).thenReturn(ref);
        Launcher launcher = mock(Launcher.class);
        when(bundleContext.getService(ref)).thenReturn(launcher);

        TestCommandContextFactory ctxFactory = new TestCommandContextFactory(bundleContext);

        String input = "test2longname --cop";
        ctxFactory.setInput(input + "\t\nexit\n");
        Arguments args = new SimpleArguments();
        CommandContext ctx = ctxFactory.createContext(args);
        cmd.run(ctx);

        String tabOutput = getTabOutput(getOutputWithoutIntro(ctxFactory));
        String inline = getTabbedInline(ctxFactory, input);

        assertTrue(tabOutput.contains("--copy"));
        assertTrue(tabOutput.contains("--copy&paste"));
        assertTrue(inline.endsWith("--copy"));
        assertEquals("", ctxFactory.getError());
    }

    @Test
    public void testFilesTabComplete() throws CommandException, IOException {
        ServiceReference ref = mock(ServiceReference.class);
        when(bundleContext.getServiceReference(Launcher.class.getName())).thenReturn(ref);
        Launcher launcher = mock(Launcher.class);
        when(bundleContext.getService(ref)).thenReturn(launcher);

        String filename = "testFilesTabComplete";
        createTempFile(filename);
        createTempFile(filename+"12345678");

        TestCommandContextFactory ctxFactory = new TestCommandContextFactory(bundleContext);
        ctxFactory.setInput("validate " + dir.getAbsolutePath() + File.separator + "testFil\t\nexit\n");
        Arguments args = new SimpleArguments();
        CommandContext ctx = ctxFactory.createContext(args);
        cmd.run(ctx);

        String tabOutput = getTabOutput(getOutputWithoutIntro(ctxFactory));
        assertTrue(tabOutput.contains(filename));
        assertEquals("", ctxFactory.getError());
    }

    @Test
    public void testFilesTabCompleteAfterOptions() throws CommandException, IOException {
        ServiceReference ref = mock(ServiceReference.class);
        when(bundleContext.getServiceReference(Launcher.class.getName())).thenReturn(ref);
        Launcher launcher = mock(Launcher.class);
        when(bundleContext.getService(ref)).thenReturn(launcher);

        String filename = "testFilesTabCompleteAfterOptions";
        createTempFile(filename);
        createTempFile(filename + "12345678");

        TestCommandContextFactory ctxFactory = new TestCommandContextFactory(bundleContext);
        ctxFactory.setInput("validate --fake-option -f " + dir.getAbsolutePath() + File.separator + "testFil\t\nexit\n");
        Arguments args = new SimpleArguments();
        CommandContext ctx = ctxFactory.createContext(args);
        cmd.run(ctx);

        String tabOutput = getTabOutput(getOutputWithoutIntro(ctxFactory));
        assertTrue(tabOutput.contains(filename));
        assertEquals("", ctxFactory.getError());
    }

    @Test
    public void testFilesDoNotTabCompleteWithoutCommand() throws CommandException, IOException {
        ServiceReference ref = mock(ServiceReference.class);
        when(bundleContext.getServiceReference(Launcher.class.getName())).thenReturn(ref);
        Launcher launcher = mock(Launcher.class);
        when(bundleContext.getService(ref)).thenReturn(launcher);

        String filename = "testFilesDoNotTabCompleteWithoutCommand";
        createTempFile(filename);
        createTempFile(filename + "12345678");

        TestCommandContextFactory ctxFactory = new TestCommandContextFactory(bundleContext);
        ctxFactory.setInput(dir.getAbsolutePath() + File.separator + "testFil\t\nexit\n");
        Arguments args = new SimpleArguments();
        CommandContext ctx = ctxFactory.createContext(args);
        cmd.run(ctx);

        String tabOutput = getTabOutput(getOutputWithoutIntro(ctxFactory));
        assertFalse(tabOutput.contains(filename));
        assertEquals("", tabOutput);
        assertEquals("", ctxFactory.getError());
    }

    @Test
    public void testFileTabCompletesInline() throws CommandException, IOException {
        ServiceReference ref = mock(ServiceReference.class);
        when(bundleContext.getServiceReference(Launcher.class.getName())).thenReturn(ref);
        Launcher launcher = mock(Launcher.class);
        when(bundleContext.getService(ref)).thenReturn(launcher);

        File tempDir = makeTempDir("testFileTabCompletesInline");

        String filename = "testFileTabCompletesInline";
        createTempFile(filename, tempDir);

        TestCommandContextFactory ctxFactory = new TestCommandContextFactory(bundleContext);
        String input = "validate " + tempDir.getAbsolutePath() + File.separator + "testFil";
        ctxFactory.setInput(input + "\t\nexit\n");
        Arguments args = new SimpleArguments();
        CommandContext ctx = ctxFactory.createContext(args);
        cmd.run(ctx);

        String inline = getTabbedInline(ctxFactory, input);
        String tabOutput = getTabOutput(getOutputWithoutIntro(ctxFactory));

        setupCommandInfoSource();
        assertEquals(0, tabOutput.length());
        assertTrue(inline.endsWith(filename));
        assertEquals("", ctxFactory.getError());
    }

    @Test
    public void testFilesDoNotTabCompleteInlineFullCommand() throws CommandException, IOException {
        ServiceReference ref = mock(ServiceReference.class);
        when(bundleContext.getServiceReference(Launcher.class.getName())).thenReturn(ref);
        Launcher launcher = mock(Launcher.class);
        when(bundleContext.getService(ref)).thenReturn(launcher);

        File tempDir = makeTempDir("testNotFullCommand");

        String filename = "testFileTabCompletesInline";
        String similarName = "testFileTabNumber";
        createTempFile(filename, tempDir);
        createTempFile(similarName + "1234567", tempDir);
        createTempFile(similarName + "987654321", tempDir);
        createTempFile(similarName + "456123", tempDir);

        TestCommandContextFactory ctxFactory = new TestCommandContextFactory(bundleContext);
        String input = "validate " + tempDir.getAbsolutePath() + File.separator + "testFil";
        ctxFactory.setInput(input + "\t\nexit\n");
        Arguments args = new SimpleArguments();
        CommandContext ctx = ctxFactory.createContext(args);
        cmd.run(ctx);

        String inline = getTabbedInline(ctxFactory, input);
        String tabOutput = getTabOutput(getOutputWithoutIntro(ctxFactory));

        assertTrue(tabOutput.contains(filename));
        assertFalse(inline.contains(filename));
        assertFalse(inline.contains(similarName));
        assertTrue(inline.endsWith("testFileTab"));
        assertEquals("", ctxFactory.getError());
    }

    @Test
    public void testFilesTabCompleteCommonPortionInline() throws CommandException, IOException {
        ServiceReference ref = mock(ServiceReference.class);
        when(bundleContext.getServiceReference(Launcher.class.getName())).thenReturn(ref);
        Launcher launcher = mock(Launcher.class);
        when(bundleContext.getService(ref)).thenReturn(launcher);

        File tempDir = makeTempDir("testFilesTabCompleteCommonPortionInline");

        String filename = "testFileTabCompletesInline";
        String commonPortion = "testFileTab";
        createTempFile(filename,tempDir);
        createTempFile(commonPortion + "1234567", tempDir);
        createTempFile(commonPortion + "987654321", tempDir);
        createTempFile(commonPortion + "456123", tempDir);

        TestCommandContextFactory ctxFactory = new TestCommandContextFactory(bundleContext);
        String input = "validate " + tempDir.getAbsolutePath() + File.separator + "testFil";
        ctxFactory.setInput(input + "\t\nexit\n");
        Arguments args = new SimpleArguments();
        CommandContext ctx = ctxFactory.createContext(args);
        cmd.run(ctx);

        String inline = getTabbedInline(ctxFactory, input);
        String tabOutput = getTabOutput(getOutputWithoutIntro(ctxFactory));

        assertTrue(tabOutput.contains(filename));
        assertFalse(inline.contains(filename));
        assertTrue(inline.endsWith(commonPortion));
        assertEquals("", ctxFactory.getError());
    }

    private void createTempFile(String name) throws IOException {
        File file = new File(dir, name);
        file.deleteOnExit();
        file.createNewFile();
    }

    private void createTempFile(String name, File dir) throws IOException {
        File file = new File(dir, name);
        file.deleteOnExit();
        file.createNewFile();
    }

    private String getTabbedInline(TestCommandContextFactory ctxFactory, String input) {
        String inline = "";
        String[] lines = ctxFactory.getOutput().split("\n");
        for(String line : lines) {
            if (line.contains(input)) {
                inline = line.split(PROMPT + input)[1];
                inline = inline.replaceAll(" ", "");
                return inline;
            }
        }
        return inline;
    }

    private File makeTempDir(String name) {
        File tempDir = new File(System.getProperty("java.io.tmpdir") + File.separator + "sc-" + name);
        tempDir.deleteOnExit();
        tempDir.mkdirs();
        return tempDir;
    }

    private void setupCommandInfoSource() {

        Collection<CommandInfo> infoList = new ArrayList<>();

        CommandInfo info1 = mock(CommandInfo.class);
        when(info1.getName()).thenReturn("test1");
        when(info1.getDescription()).thenReturn("test command 1");
        when(info1.getEnvironments()).thenReturn(EnumSet.of(Environment.CLI, Environment.SHELL));
        when(info1.needsFileTabCompletions()).thenReturn(false);

        ArrayList<Option> optionsList1 = new ArrayList<>();
        Option option1 = mock(Option.class);
        when(option1.getLongOpt()).thenReturn("test");
        when(option1.getOpt()).thenReturn("t");
        Option option2 = mock(Option.class);
        when(option2.getLongOpt()).thenReturn("Add");
        when(option2.getOpt()).thenReturn("A");
        Option option3 = mock(Option.class);
        when(option3.getLongOpt()).thenReturn("remove");
        when(option3.getOpt()).thenReturn("r");
        optionsList1.add(option1);
        optionsList1.add(option2);
        optionsList1.add(option3);

        Options options1 = mock(Options.class);
        when(info1.getOptions()).thenReturn(options1);
        when(options1.getOptions()).thenReturn(new ArrayList(optionsList1));
        when(info1.getOptions().getOptions()).thenReturn(new ArrayList(optionsList1));

        infoList.add(info1);

        CommandInfo info2 = mock(CommandInfo.class);
        when(info2.getName()).thenReturn("test2longname");
        when(info2.getDescription()).thenReturn("test command 2");
        when(info2.getEnvironments()).thenReturn(EnumSet.of(Environment.CLI, Environment.SHELL));
        when(info2.needsFileTabCompletions()).thenReturn(false);

        ArrayList<Option> optionsList2 = new ArrayList<>();
        Option option4 = mock(Option.class);
        when(option4.getLongOpt()).thenReturn("copy");
        when(option4.getOpt()).thenReturn("c");
        Option option5 = mock(Option.class);
        when(option5.getLongOpt()).thenReturn("copy&paste");
        when(option5.getOpt()).thenReturn("p");
        Option option6 = mock(Option.class);
        when(option6.getLongOpt()).thenReturn("Paste");
        when(option6.getOpt()).thenReturn("v");
        optionsList2.add(option4);
        optionsList2.add(option5);
        optionsList2.add(option6);

        Options options2 = mock(Options.class);
        when(info2.getOptions()).thenReturn(options2);
        when(options2.getOptions()).thenReturn(new ArrayList(optionsList2));
        when(info2.getOptions().getOptions()).thenReturn(new ArrayList(optionsList2));

        infoList.add(info2);

        CommandInfo info3 = mock(CommandInfo.class);
        when(info3.getName()).thenReturn("validate");
        when(info3.getDescription()).thenReturn("mock validate command");
        when(info3.getEnvironments()).thenReturn(EnumSet.of(Environment.CLI, Environment.SHELL));
        when(info3.needsFileTabCompletions()).thenReturn(true);

        ArrayList<Option> optionsList3 = new ArrayList<>();
        Option option7 = mock(Option.class);
        when(option7.getLongOpt()).thenReturn("dbUrl");
        when(option7.getOpt()).thenReturn("d");
        Option option8 = mock(Option.class);
        when(option8.getLongOpt()).thenReturn("loglevel");
        when(option8.getOpt()).thenReturn("l");
        Option option9 = mock(Option.class);
        when(option9.getLongOpt()).thenReturn("agent");
        when(option9.getOpt()).thenReturn("a");
        Option option10 = mock(Option.class);
        when(option10.getLongOpt()).thenReturn("fake-option");
        when(option10.getOpt()).thenReturn("f");
        optionsList3.add(option7);
        optionsList3.add(option8);
        optionsList3.add(option9);
        optionsList3.add(option10);

        Options options3 = mock(Options.class);
        when(info3.getOptions()).thenReturn(options3);
        when(options3.getOptions()).thenReturn(new ArrayList(optionsList3));
        when(info3.getOptions().getOptions()).thenReturn(new ArrayList(optionsList3));

        infoList.add(info3);

        when(infos.getCommandInfos()).thenReturn(infoList);
        cmd.setCommandInfoSource(infos);
    }
}

