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

package com.redhat.thermostat.itest;

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import expectj.Spawn;

/**
 * Verifies that all commands OSGi-resolve properly by invoking them
 * with an unknown option.
 *
 */
public class AllCommandsResolvableTest extends IntegrationTest {
    
    private static final String UNKNOWN_CMD_OPTION = "--unknownOption";
    
    @Before
    public void setup() {
        createFakeSetupCompleteFile();
    }
    
    @After
    public void tearDown() throws IOException {
        removeSetupCompleteStampFiles();
    }
    
    @Test
    public void testAllCommandsOsgiResolve() throws Exception {
        List<String> allCmds = parseAvailableCommandsFromHelp();
        System.out.println(allCmds);
        assertTrue("sanity check help output failed", allCmds.containsAll(Arrays.asList("list-vms", "help")));
        for (String command: allCmds) {
            Spawn shell = spawnThermostat(command, UNKNOWN_CMD_OPTION);
            shell.expectClose();
            String stdOut = shell.getCurrentStandardOutContents();
            // storage has "--permitLocalhostException" which matches
            // in assertNoExceptions(). Sanitize output for it.
            if ("storage".equals(command)) {
                stdOut = sanitizeStdOut(stdOut);
            }
            assertNoExceptions(stdOut, shell.getCurrentStandardErrContents());
        }
    }

    private String sanitizeStdOut(String stdOut) {
        Pattern pattern = Pattern.compile("(.*)" + "--permitLocalhostException" + "(.*)", Pattern.DOTALL);
        Matcher matcher = pattern.matcher(stdOut);
        if (matcher.matches()) {
            // return everything but the word we want to get removed
            return sanitizeStdOut(matcher.group(1)) + sanitizeStdOut(matcher.group(2));
        }
        return stdOut;
    }

    /*
     * Parse all available command names from "thermostat help".
     */
    private List<String> parseAvailableCommandsFromHelp() throws Exception {
        Spawn shell = spawnThermostat("help");
        shell.expectClose();

        String stdOut = shell.getCurrentStandardOutContents();
        Scanner scanner = new Scanner(stdOut);
        String nextLine;
        boolean pastListOfCommands = false;
        List<String> cmds = new ArrayList<>();
        while (scanner.hasNextLine()) {
            nextLine = scanner.nextLine();
            nextLine = nextLine.trim();
            if (!pastListOfCommands && "list of commands:".equals(nextLine)) {
                pastListOfCommands = true;
                continue; // skip "list of commands:" literal
            }
            // skip non-commands, such as options etc.
            if (!pastListOfCommands) {
                continue;
            }
            // skip empty lines
            if ("".equals(nextLine)) {
                continue;
            }
            // Separate command name from summary
            String command = nextLine.split("\\s+")[0];
            cmds.add(command);
        }
        scanner.close();
        return cmds;
    }

}
