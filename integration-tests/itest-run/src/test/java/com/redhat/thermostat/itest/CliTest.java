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

package com.redhat.thermostat.itest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;

import org.junit.Test;

import expectj.Spawn;

/**
 * Integration tests to exercise the basics of the thermostat command line.
 */
public class CliTest extends IntegrationTest {

    @Test
    public void testExpectIsSane() throws Exception {
        Spawn shell = spawnThermostat();

        try {
            shell.expect("some-random-text-that-is-not-really-possible");
            fail("should never match");
        } catch (IOException endOfStream) {
            assertTrue(endOfStream.getMessage().contains("End of stream reached, no match found"));
        }
        shell.expectClose();
    }

    @Test
    public void testSimpleInvocationPrintsHelp() throws Exception {
        Spawn shell = spawnThermostat();
        shell.expectClose();

        String stdOut = shell.getCurrentStandardOutContents();

        assertMatchesHelpCommandList(stdOut);

        String stdErr = shell.getCurrentStandardErrContents();
        assertEquals(stdErr, "");
    }

    @Test
    public void testHelpCommandInvocation() throws Exception {
        Spawn shell = spawnThermostat("help");
        shell.expectClose();

        String stdOut = shell.getCurrentStandardOutContents();
        String stdErr = shell.getCurrentStandardErrContents();

        assertMatchesHelpCommandList(stdOut);
        assertEquals(stdErr, "");
    }

    @Test
    public void testHelpOnHelp() throws Exception {
        Spawn shell = spawnThermostat("help", "help");
        shell.expectClose();

        String stdOut = shell.getCurrentStandardOutContents();
        String stdErr = shell.getCurrentStandardErrContents();

        String[] lines = stdOut.split("\n");
        String usage = lines[0];
        assertEquals("usage: thermostat help [command-name]", usage);

        assertEquals(stdErr, "");
    }

    @Test
    public void testVersionArgument() throws Exception {
        Spawn shell = spawnThermostat("--version");
        shell.expectClose();

        String stdOut = shell.getCurrentStandardOutContents();
        String stdErr = shell.getCurrentStandardErrContents();

        assertTrue(stdOut.matches("Thermostat version \\d+\\.\\d+\\.\\d+\n"));
        assertEquals(stdErr, "");
    }

    @Test
    public void testShell() throws Exception {
        Spawn shell = spawnThermostat("shell");

        shell.expect(SHELL_PROMPT);
        shell.send("help\n");

        shell.expect(SHELL_PROMPT);

        assertMatchesShellHelpCommandList(shell.getCurrentStandardOutContents());

        shell.send("exit\n");

        shell.expectClose();
    }

    @Test
    public void testShellPrintsVersionOnStartup() throws Exception {
        Spawn shell = spawnThermostat("shell");

        shell.expect(SHELL_PROMPT);

        String stdOut = shell.getCurrentStandardOutContents();
        assertTrue(stdOut.contains("Thermostat version "));
    }
    
    @Test
    public void versionArgumentInShellIsNotAllowed() throws Exception {
        Spawn shell = spawnThermostat("shell");

        shell.expect(SHELL_PROMPT);
        shell.send("--version\n");

        shell.expect(SHELL_PROMPT);

        String stdOut = shell.getCurrentStandardOutContents();
        String stdErr = shell.getCurrentStandardErrContents();

        assertMatchesShellHelpCommandList(shell.getCurrentStandardOutContents());
        // use the Pattern.DOTALL flag (?s) so that line terminators match with
        // ".*". stdOut contains the SHELL_PROMPT too.
        assertTrue(stdOut.matches("(?s)^.*\nunknown command '--version'\n.*$"));
        assertEquals(stdErr, "");
        
        shell.send("exit\n");

        shell.expectClose();
    }

    @Test
    public void testShellHelp() throws Exception {
        Spawn shell = spawnThermostat("help", "shell");
        shell.expectClose();

        String stdOut = shell.getCurrentStandardOutContents();

        String[] lines = stdOut.split("\n");
        String usage = lines[0];
        assertTrue(usage.matches("^usage: thermostat shell$"));
        String description = lines[1];
        assertTrue(description.matches("^\\s+launches the Thermostat interactive shell$"));
        assertTrue(lines[3].matches("thermostat shell"));
    }

    @Test
    public void testShellHelpArgument() throws Exception {
        Spawn shell = spawnThermostat("shell", "--help");
        shell.expectClose();

        String stdOut = shell.getCurrentStandardOutContents();

        String[] lines = stdOut.split("\n");
        String usage = lines[0];
        assertTrue(usage.matches("^usage: thermostat shell$"));
        String description = lines[1];
        assertTrue(description.matches("^\\s+launches the Thermostat interactive shell$"));
        assertTrue(lines[3].matches("thermostat shell"));
    }

    @Test
    public void testShellUnrecognizedArgument() throws Exception {
        Spawn shell = spawnThermostat("shell", "--foo");
        shell.expectClose();
        String stdOut = shell.getCurrentStandardOutContents();
        String expectedOut = "Could not parse options: Unrecognized option: --foo\n"
                           + "usage: thermostat shell\n"
                           + "                  launches the Thermostat interactive shell\n"
                           + "\n"
                           + "thermostat shell\n\n";
        assertEquals(expectedOut, stdOut);
    }

    @Test
    public void testUnrecognizedEventsInShell() throws Exception {
        // test '!' events
        Spawn shell = spawnThermostat("shell");

        shell.expect(SHELL_PROMPT);
        shell.send("what!?!\n");
        shell.expect(SHELL_PROMPT);
        shell.send("exit\n");
        shell.expectClose();

        assertTrue(shell.getCurrentStandardErrContents().contains("!?!: event not found"));
        assertNoExceptions(shell.getCurrentStandardOutContents(), shell.getCurrentStandardErrContents());
    }

    @Test
    public void testInvalidCommand() throws Exception {
        Spawn shell = spawnThermostat("foobar", "baz");

        // TODO should this be stderr?
        shell.expect("unknown command 'foobar'");
        shell.expectClose();

        String stdOut = shell.getCurrentStandardOutContents();

        assertMatchesHelpCommandList(stdOut);
    }

    private static void assertMatchesHelpCommandList(String actual) {
        assertTrue(actual.contains("list of commands"));
        assertTrue(actual.contains("help"));
        assertTrue(actual.contains("agent"));
        assertTrue(actual.contains("gui"));
        assertTrue(actual.contains("ping"));
        assertTrue(actual.contains("shell"));
    }

    private static void assertMatchesShellHelpCommandList(String actual) {
        assertTrue(actual.contains("list of commands"));
        assertTrue(actual.contains("help"));
        assertTrue(actual.contains("connect"));
        assertTrue(actual.contains("disconnect"));
        assertTrue(actual.contains("ping"));
    }

}

