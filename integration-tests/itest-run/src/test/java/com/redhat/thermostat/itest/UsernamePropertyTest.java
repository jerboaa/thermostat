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

package com.redhat.thermostat.itest;

import java.util.Map;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import expectj.Spawn;

/**
 * Ensures that the Thermostat agent can still run if a bogus
 * user name is set via the "user.name" system property.
 */
public class UsernamePropertyTest extends IntegrationTest {
    
    private static final long TIMEOUT_MS = 2000L;
    private static final String USERNAME = "foo";
    private static final String PASSWORD = "bar";

    @BeforeClass
    public static void setUpOnce() throws Exception {
        setupIntegrationTest(UsernamePropertyTest.class);

        createFakeSetupCompleteFile();

        addUserToStorage(USERNAME, PASSWORD);
        createAgentAuthFile(USERNAME, PASSWORD);

        startStorage();
    }

    @AfterClass
    public static void tearDownOnce() throws Exception {
        stopStorage();
    }

    @Test
    public void testWrongUsernameProperty() throws Exception {
        Map<String, String> testProperties = getVerboseModeProperties();
        
        String [] args = { "-J-Duser.name=not-me", "agent", "-d", "mongodb://127.0.0.1:27518" };
        SpawnResult spawnResult = spawnThermostatWithPropertiesSetAndGetProcess(testProperties, args);
        Spawn service = spawnResult.spawn;

        try {
            service.expect("Agent started.");
            // Give agent some time to startup before killing it
            Thread.sleep(TIMEOUT_MS);
        } finally {
            // service.stop only stops the agent/webservice.
            killRecursively(spawnResult.process);
        }

        service.expectClose();
    }
}
