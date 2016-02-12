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

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import expectj.ExpectJException;
import expectj.Spawn;
import expectj.TimeoutException;

public class StorageConnectionTest extends IntegrationTest {

    // @BeforeClass // reinstate once we actually need storage running (see ignored tests)
    public static void setUpOnce() throws Exception {
        clearStorageDataDirectory();
        startStorage();
    }

    // @AfterClass // reinstate once we actually need storage running
    public static void tearDownOnce() throws Exception {
        stopStorage();
    }
    
    @Before
    public void setup() {
        createFakeSetupCompleteFile();
    }
    
    @After
    public void tearDown() throws IOException {
        removeSetupCompleteStampFiles();
    }

    @Ignore //FIXME when keyring/preferences improvements have been made, un-Ignore
    @Test
    public void testConnect() throws ExpectJException, TimeoutException, IOException {
        Spawn shell = spawnThermostat(true, "shell");

        shell.expect(SHELL_DISCONNECT_PROMPT);
        shell.send("connect -d mongodb://127.0.0.1:27518\n");
        handleAuthPrompt(shell, "mongodb://127.0.0.1:27518", "", "");
        shell.expect(SHELL_CONNECT_PROMPT);
        shell.send("exit\n");
        shell.expectClose();

        assertCommandIsFound(shell.getCurrentStandardOutContents(), shell.getCurrentStandardErrContents());
        assertNoExceptions(shell.getCurrentStandardOutContents(), shell.getCurrentStandardErrContents());
    }

    @Test
    public void testDisconnectWithoutConnecting() throws ExpectJException, TimeoutException, IOException {
        Spawn shell = spawnThermostat("shell");

        shell.expect(SHELL_DISCONNECT_PROMPT);
        shell.send("disconnect\n");
        shell.expect(SHELL_DISCONNECT_PROMPT);
        shell.send("exit\n");
        shell.expectClose();

        assertCommandIsFound(shell.getCurrentStandardOutContents(), shell.getCurrentStandardErrContents());
        assertNoExceptions(shell.getCurrentStandardOutContents(), shell.getCurrentStandardErrContents());
    }

    @Ignore //FIXME when keyring/preferences improvements have been made, un-Ignore
    @Test
    public void testConnectAndDisconnectInShell() throws IOException, TimeoutException, ExpectJException {
        Spawn shell = spawnThermostat(true, "shell");

        shell.expect(SHELL_DISCONNECT_PROMPT);
        shell.send("connect -d mongodb://127.0.0.1:27518\n");
        handleAuthPrompt(shell, "mongodb://127.0.0.1:27518", "", "");
        shell.expect(SHELL_CONNECT_PROMPT);
        shell.send("disconnect\n");
        shell.send("exit\n");
        shell.expectClose();

        assertNoExceptions(shell.getCurrentStandardOutContents(), shell.getCurrentStandardErrContents());
    }

    @Test
    public void testConnectOutsideShell() throws IOException, TimeoutException, ExpectJException {
        Spawn shell = spawnThermostat(true, "connect");
        shell.expect("The connect command is not supported from outside the thermostat shell.");

        shell.expectClose();

        assertNoExceptions(shell.getCurrentStandardOutContents(), shell.getCurrentStandardErrContents());
    }

    @Test
    public void testDisConnectOutsideShell() throws IOException, TimeoutException, ExpectJException {
        Spawn shell = spawnThermostat(true, "disconnect");
        shell.expect("The disconnect command is not supported from outside the thermostat shell.");

        shell.expectClose();

        assertNoExceptions(shell.getCurrentStandardOutContents(), shell.getCurrentStandardErrContents());
    }

}

