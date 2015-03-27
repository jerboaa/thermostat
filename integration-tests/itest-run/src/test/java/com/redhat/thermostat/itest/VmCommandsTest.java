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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import expectj.Spawn;

/** Integration tests for the various vm commands */
public class VmCommandsTest extends IntegrationTest {

    @BeforeClass
    public static void setUpOnce() throws Exception {
        createFakeSetupCompleteFile();
        startStorage();

        // TODO insert actual data into the database and test that
    }

    @AfterClass
    public static void tearDownOnce() throws Exception {
        stopStorage();
        removeSetupCompleteStampFiles();
    }

    @Ignore //FIXME when keyring/preferences improvements have been made, un-Ignore
    @Test
    public void testListVms() throws Exception {
        Spawn vmList = commandAgainstMongo("list-vms");
        vmList.expectClose();
        assertOutputEndsWith(vmList.getCurrentStandardOutContents(), "HOST_ID HOST VM_ID STATUS VM_NAME\n\n");
    }

    @Test
    public void testVmStat() throws Exception {
        Spawn vmStat = commandAgainstMongo("vm-stat");
        // TODO include required options to test meaningfully
        vmStat.expectClose();

        assertCommandIsFound(vmStat.getCurrentStandardOutContents(), vmStat.getCurrentStandardErrContents());
        assertNoExceptions(vmStat.getCurrentStandardOutContents(), vmStat.getCurrentStandardErrContents());
    }
    
    @Test
    public void testGcCommonName() throws Exception {
        Spawn gcCommonName = commandAgainstMongo("show-gc-name");
        // TODO include required options to test meaningfully
        gcCommonName.expectClose();

        assertCommandIsFound(gcCommonName.getCurrentStandardOutContents(), gcCommonName.getCurrentStandardErrContents());
        assertNoExceptions(gcCommonName.getCurrentStandardOutContents(), gcCommonName.getCurrentStandardErrContents());
    }

    @Test
    public void testGc() throws Exception {
        Spawn gcCommand = commandAgainstMongo("gc");
        // TODO include required options to test meaningfully
        gcCommand.expectClose();

        assertCommandIsFound(gcCommand.getCurrentStandardOutContents(), gcCommand.getCurrentStandardErrContents());
        assertNoExceptions(gcCommand.getCurrentStandardOutContents(), gcCommand.getCurrentStandardErrContents());
    }

    @Test
    public void testVmInfo() throws Exception {
        Spawn vmInfo = commandAgainstMongo("vm-info");
        // TODO include required options to test meaningfully
        // handleAuthPrompt(vmInfo, "mongodb://127.0.0.1:27518", "", "");
        vmInfo.expectClose();

        assertNoExceptions(vmInfo.getCurrentStandardOutContents(), vmInfo.getCurrentStandardErrContents());
    }

    @Ignore //FIXME when keyring/preferences improvements have been made, un-Ignore
    @Test
    public void testHeapCommands() throws Exception {
        String[] commands = new String[] {
                "dump-heap",
                "list-heap-dumps",
                "save-heap-dump-to-file",
                "show-heap-histogram",
                "find-objects",
                "object-info",
                "find-root"
        };

        for (String command : commands) {
            Spawn heapCommand = commandAgainstMongo(command);
            // TODO include required options to test each command meaningfully
            if (command.equals("list-heap-dumps")) {
                // No missing options, times out waiting for user/pass input without the following:
                handleAuthPrompt(heapCommand, "mongodb://127.0.0.1:27518", "", "");
            }
            heapCommand.expectClose();

            assertCommandIsFound(
                    heapCommand.getCurrentStandardOutContents(),
                    heapCommand.getCurrentStandardErrContents());
            assertNoExceptions(
                    heapCommand.getCurrentStandardOutContents(),
                    heapCommand.getCurrentStandardErrContents());
        }
    }

    @Test
    public void testNormalCommandAndPluginInShell() throws Exception {
        String storageURL = "mongodb://127.0.0.1:27518";
        Spawn shell = spawnThermostat("shell");

        shell.expect(SHELL_DISCONNECT_PROMPT);
        shell.send("list-vms -d " + storageURL + "\n");
        handleAuthPrompt(shell, storageURL, "", "");

        shell.expect(SHELL_CONNECT_PROMPT);

        shell.send("dump-heap\n");

        shell.expect(SHELL_CONNECT_PROMPT);

        shell.send("exit\n");
        shell.expectClose();

        assertCommandIsFound(shell);
        assertNoExceptions(shell);
    }

    private static Spawn commandAgainstMongo(String... args) throws IOException {
        if (args == null || args.length == 0) {
            throw new IllegalArgumentException("args must be an array with something");
        }
        List<String> completeArgs = new ArrayList<>();
        completeArgs.addAll(Arrays.asList(args));
        completeArgs.add("-d");
        completeArgs.add("mongodb://127.0.0.1:27518");
        return spawnThermostat(true, completeArgs.toArray(new String[0]));
    }

}

