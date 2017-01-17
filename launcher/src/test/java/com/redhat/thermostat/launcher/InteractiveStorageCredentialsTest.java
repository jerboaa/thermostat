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

package com.redhat.thermostat.launcher;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import org.junit.Before;
import org.junit.Test;

import com.redhat.thermostat.common.cli.Console;
import com.redhat.thermostat.common.config.ClientPreferences;
import com.redhat.thermostat.utils.keyring.Keyring;

public class InteractiveStorageCredentialsTest {
    
    private static final String URL = "http://example.com/thermostat/storage";
    private static final String DIFFERENT_URL = "http://example.com/thermostat/storage";
    
    private Console console;
    private ByteArrayOutputStream baout;
    
    @Before
    public void setup() {
        console = mock(Console.class);
        baout = new ByteArrayOutputStream();
        when(console.getOutput()).thenReturn(new PrintStream(baout));
    }

    @Test
    public void canGetUsernameFromPrefs() {
        String input = ""; // should never be used
        when(console.getInput()).thenReturn(new ByteArrayInputStream(input.getBytes()));
        String username = "foouser";
        ClientPreferences prefs = mock(ClientPreferences.class);
        when(prefs.getUserName()).thenReturn(username);
        when(prefs.getConnectionUrl()).thenReturn(URL);
        Keyring keyring = mock(Keyring.class);
        InteractiveStorageCredentials creds = new InteractiveStorageCredentials(prefs, keyring, URL, console);
        String actual = creds.getUsername();
        assertEquals(username, actual);
    }
    
    @Test
    public void canGetPasswordFromKeyring() {
        String input = ""; // should never be used
        when(console.getInput()).thenReturn(new ByteArrayInputStream(input.getBytes()));
        String password = "testme";
        String username = "foouser";
        ClientPreferences prefs = mock(ClientPreferences.class);
        when(prefs.getUserName()).thenReturn(username);
        when(prefs.getConnectionUrl()).thenReturn(URL);
        Keyring keyring = mock(Keyring.class);
        when(keyring.getPassword(URL, username)).thenReturn(password.toCharArray());
        InteractiveStorageCredentials creds = new InteractiveStorageCredentials(prefs, keyring, URL, console);
        char[] actual = creds.getPassword();
        assertNotNull("expected password from keyring", actual);
        assertEquals(password, new String(actual));
    }
    
    @Test
    public void promptsForUsernameIfNotPresentInPreferences() {
        String username = "someuser";
        String input = String.format("%s\n", username);
        when(console.getInput()).thenReturn(new ByteArrayInputStream(input.getBytes()));
        ClientPreferences prefs = mock(ClientPreferences.class);
        when(prefs.getConnectionUrl()).thenReturn(DIFFERENT_URL); // something *not* URL
        Keyring keyring = mock(Keyring.class);
        InteractiveStorageCredentials creds = new InteractiveStorageCredentials(prefs, keyring, URL, console);
        String actual = creds.getUsername();
        assertEquals(username, actual);
    }
    
    @Test
    public void promptsForPasswordIfNotPresentInKeyring() {
        String password = "somepassword";
        String input = String.format("%s\n", password);
        when(console.getInput()).thenReturn(new ByteArrayInputStream(input.getBytes()));
        ClientPreferences prefs = mock(ClientPreferences.class);
        when(prefs.getConnectionUrl()).thenReturn(DIFFERENT_URL); // something *not* URL
        Keyring keyring = mock(Keyring.class);
        InteractiveStorageCredentials creds = new InteractiveStorageCredentials(prefs, keyring, URL, console);
        char[] actual = creds.getPassword();
        assertNotNull("expected password to be read from prompt", actual);
        assertEquals(password, new String(actual));
    }
}
