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

package com.redhat.thermostat.common.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.times;

import java.util.prefs.Preferences;

import org.junit.Test;

import com.redhat.thermostat.utils.keyring.Credentials;
import com.redhat.thermostat.utils.keyring.Keyring;

public class ClientPreferencesTest {

    @Test
    public void testGetConnectionUrl() {

        Keyring keyring = mock(Keyring.class);
        Preferences prefs = mock(Preferences.class);
        when(prefs.get(eq(ClientPreferences.CONNECTION_URL), any(String.class))).thenReturn("mock-value");

        ClientPreferences clientPrefs = new ClientPreferences(prefs, keyring);
        String value = clientPrefs.getConnectionUrl();

        assertEquals("mock-value", value);
        verify(prefs).get(eq(ClientPreferences.CONNECTION_URL), any(String.class));
    }

    @Test
    public void testSetConnectionUrl() {

        Keyring keyring = mock(Keyring.class);
        Preferences prefs = mock(Preferences.class);

        ClientPreferences clientPrefs = new ClientPreferences(prefs, keyring);
        clientPrefs.setConnectionUrl("test");

        verify(prefs).put(eq(ClientPreferences.CONNECTION_URL), eq("test"));
    }

    @Test
    public void testSetUsernamePassowrd() {
        
        Keyring keyring = mock(Keyring.class);
        Preferences prefs = mock(Preferences.class);
        when(prefs.getBoolean(eq(ClientPreferences.SAVE_ENTITLEMENTS), any(boolean.class))).thenReturn(true);
        
        ClientPreferences clientPrefs = new ClientPreferences(prefs, keyring);
        clientPrefs.setCredentials("fluff", "fluffPassword");

        verify(prefs).put(eq(ClientPreferences.USERNAME), eq("fluff"));
        verify(prefs, times(0)).put(eq(ClientPreferences.PASSWORD), anyString());
    }
    
    @Test
    public void testGetUsername() {
        
        Keyring keyring = mock(Keyring.class);
        Preferences prefs = mock(Preferences.class);
        when(prefs.get(eq(ClientPreferences.USERNAME), any(String.class))).thenReturn("mock-value");
        
        ClientPreferences clientPrefs = new ClientPreferences(prefs, keyring);
        String username = clientPrefs.getUserName();

        assertEquals("mock-value", username);
        verify(prefs, atLeastOnce()).get(eq(ClientPreferences.USERNAME), any(String.class));
    }
    
    @Test
    public void testGetPassword() {
        
        Keyring keyring = mock(Keyring.class);
        
        Preferences prefs = mock(Preferences.class);
        when(prefs.get(eq(ClientPreferences.USERNAME), any(String.class))).thenReturn("mock-value");
        when(prefs.getBoolean(eq(ClientPreferences.SAVE_ENTITLEMENTS), any(boolean.class))).
            thenReturn(false).thenReturn(false).thenReturn(true);
                
        ClientPreferences clientPrefs = new ClientPreferences(prefs, keyring);
        String password = clientPrefs.getPassword();
        verify(prefs, times(0)).get(eq(ClientPreferences.PASSWORD), any(String.class));
        verify(keyring, times(0)).loadPassword(any(Credentials.class));
        
        assertEquals("", password);
        
        clientPrefs.getPassword();
        verify(keyring, atLeastOnce()).loadPassword(any(Credentials.class));
    }
    
    @Test
    public void testGetSaveEntitlements() {
        
        Keyring keyring = mock(Keyring.class);
        Preferences prefs = mock(Preferences.class);
        when(prefs.getBoolean(eq(ClientPreferences.SAVE_ENTITLEMENTS), any(boolean.class))).thenReturn(true);
        
        ClientPreferences clientPrefs = new ClientPreferences(prefs, keyring);
        boolean saveEntitlements = clientPrefs.getSaveEntitlements();

        assertEquals(true, saveEntitlements);
        verify(prefs, atLeastOnce()).getBoolean(eq(ClientPreferences.SAVE_ENTITLEMENTS), any(boolean.class));
    }
    
    @Test
    public void testSetSaveEntitlements() {
        
        Keyring keyring = mock(Keyring.class);
        Preferences prefs = mock(Preferences.class);

        ClientPreferences clientPrefs = new ClientPreferences(prefs, keyring);
        clientPrefs.setSaveEntitlements(false);

        verify(prefs).putBoolean(eq(ClientPreferences.SAVE_ENTITLEMENTS), eq(false));
        
        clientPrefs.setSaveEntitlements(true);
        verify(prefs).putBoolean(eq(ClientPreferences.SAVE_ENTITLEMENTS), eq(true));
    }
}
