/*
 * Copyright 2012-2016 Red Hat, Inc.
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

package com.redhat.thermostat.client.ui;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Properties;

import org.junit.Before;
import org.junit.Test;

import com.redhat.thermostat.common.config.ClientPreferences;
import com.redhat.thermostat.shared.config.CommonPaths;
import com.redhat.thermostat.utils.keyring.Keyring;

public class ClientPreferencesModelTest {

    private Keyring keyring;
    private ClientPreferences prefs;
    private File tempFile;
    
    @Before
    public void setup() throws IOException {
        keyring = mock(Keyring.class);
        CommonPaths paths = mock(CommonPaths.class);
        tempFile = File.createTempFile("thermostat", getClass().getName());
        tempFile.deleteOnExit();
        when(paths.getUserClientConfigurationFile()).thenReturn(tempFile);
        prefs = new ClientPreferences(paths);
    }
    
    /*
     * Credentials (username/password) should not be stored in keyring/backing
     * preferences file if save entitlements is false.
     */
    @Test
    public void canSetGetCredentialsMemory() throws IOException {
        ClientPreferencesModel model = new ClientPreferencesModel(keyring, prefs);
        model.setSaveEntitlements(false);
        char[] password = new char[] { 't', 'e', 's', 't' };
        String username = "tester";
        model.setCredentials(username, password);
        assertArrayEquals(password, model.getPassword());
        assertEquals(username, model.getUserName());
        // This should not write the username to disk
        model.flush();
        Properties props = new Properties();
        props.load(new FileInputStream(tempFile));
        assertNull(props.getProperty("username"));
        verify(keyring, times(0)).savePassword(any(String.class), any(String.class), any(char[].class));
    }
    
    /*
     * Credentials (username/password) should only get flushed to the
     * keyring/backing prefs file if saveEntitlements is true.
     */
    @Test
    public void canPersistCredentials() throws IOException {
        ClientPreferencesModel model = new ClientPreferencesModel(keyring, prefs);
        model.setSaveEntitlements(true);
        char[] password = new char[] { 't', 'e', 's', 't' };
        String username = "tester";
        model.setCredentials(username, password);
        // This should not write the username to disk
        model.flush();
        when(keyring.getPassword(any(String.class), any(String.class))).thenReturn(password);
        assertArrayEquals(password, model.getPassword());
        assertEquals(username, model.getUserName());
        Properties props = new Properties();
        props.load(new FileInputStream(tempFile));
        assertEquals(username, props.getProperty("username"));
        verify(keyring, times(1)).savePassword("http://127.0.0.1:8999/thermostat/storage", username, password);
    }
    
    @Test
    public void modelUsesDefaultsFromClientPrefsFile() throws IOException, URISyntaxException {
        CommonPaths paths = mock(CommonPaths.class);
        File prefsFile = new File(getClass().getResource("/test_client_preferences.properties").toURI().getPath());
        when(paths.getUserClientConfigurationFile()).thenReturn(prefsFile);
        ClientPreferences prefs = new ClientPreferences(paths);
        ClientPreferencesModel model = new ClientPreferencesModel(keyring, prefs);
        assertEquals("http://example.com/thermostat/storage", model.getConnectionUrl());
        assertTrue(model.getSaveEntitlements());
        assertEquals("my_default_user", model.getUserName());
    }
    
    private void assertArrayEquals(char[] expected, char[] actual) {
        assertTrue(expected.length == actual.length);
        for (int i = 0; i < expected.length; i++) {
            assertTrue(expected[i] == actual[i]);
        }
    }
}
