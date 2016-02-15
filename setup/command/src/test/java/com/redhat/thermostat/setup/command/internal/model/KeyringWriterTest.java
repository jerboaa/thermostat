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

package com.redhat.thermostat.setup.command.internal.model;

import static org.junit.Assert.fail;
import static org.junit.Assert.assertArrayEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.io.IOException;

import org.junit.Test;

import com.redhat.thermostat.common.config.ClientPreferences;
import com.redhat.thermostat.utils.keyring.Keyring;
import com.redhat.thermostat.utils.keyring.KeyringException;

public class KeyringWriterTest {
    
    @Test
    public void writeStoresToKeyringSuccess() throws IOException {
        Keyring keyring = mock(Keyring.class); // well behaved keyring
        doKeyringTest(keyring);
    }

    private void doKeyringTest(Keyring keyring) throws IOException {
        ClientPreferences prefs = mock(ClientPreferences.class);
        KeyringWriter keyringWriter = new KeyringWriter(prefs, keyring);
        try {
            String clientUser = "client-admin";
            char[] clientPass = new char[] { 't' };
            String storageUrl = "http://somestorage.example.com";
            keyringWriter.setCredentials(clientUser, clientPass);
            keyringWriter.setStorageUrl(storageUrl);
            keyringWriter.write();
            verify(keyring).savePassword(storageUrl, clientUser, clientPass);
            verify(prefs).flush();
            verify(prefs).setSaveEntitlements(true);
            verify(prefs).setUserName(clientUser);
            verify(prefs).setConnectionUrl(storageUrl);
            assertArrayEquals("Expected password array to have been cleared", new char[] { '\0' }, clientPass);
        } catch (IOException e) {
            e.printStackTrace();
            fail("Did not expect failure on commit()");
        }
    }
    
    @Test
    public void keyringStoreFailureIsNonFatal() throws IOException {
        Keyring keyring = mock(Keyring.class);
        doThrow(new MockKeyringException("This is a test")).when(keyring).savePassword(any(String.class), any(String.class), any(char[].class));
        doKeyringTest(keyring);
    }
    
    @Test(expected = NullPointerException.class)
    public void notSettingCredsBeforeWriteThrowsNPE() throws IOException {
        KeyringWriter writer = new KeyringWriter(mock(ClientPreferences.class), mock(Keyring.class));
        writer.setStorageUrl("http://somestorage.example.com");
        writer.write(); // expected creds to be set first.
    }
    
    @Test(expected = NullPointerException.class)
    public void notSettingStorageUrlBeforeWriteThrowsNPE() throws IOException {
        KeyringWriter writer = new KeyringWriter(mock(ClientPreferences.class), mock(Keyring.class));
        writer.setCredentials("something", new char[] { 'x' }); // doesn't matter, just non-null
        writer.write(); // expected storage URL to be set first
    }
    
    @SuppressWarnings("serial")
    private static class MockKeyringException extends KeyringException {

        public MockKeyringException(String string) {
            super(string);
        }
        
    }

}
