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

package com.redhat.thermostat.utils.keyring.internal;

import com.redhat.thermostat.utils.keyring.Keyring;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;


public class InsecureFileBasedKeyringImplTest {

    private Keyring keyring = null;

    // tests mostly copied from KeyringImplTest

    private String createTempKeyringKeyringFilename() {

        try {
            final File tmpKeyRingFile = File.createTempFile("thermostat-test-keyring-", "dat");
            tmpKeyRingFile.delete(); // we don't want the file, just the name of the file
            return tmpKeyRingFile.getCanonicalPath();
        } catch (IOException e) {
            e.printStackTrace();
            Assert.fail();
        }
        return null;
    }

    @Before
    public void createPersistantKeyring() {
        // create one keyring for most tests to use
        keyring = new InsecureFileBasedKeyringImpl(createTempKeyringKeyringFilename());
    }

    @Test
    public void verifySavedPasswordIsRetrieveable() {

        final String url = "www.example1.com";
        final String user = "mike";
        final char[] pw = new char[] {'1', '2', '3'};

        try {
            keyring.savePassword(url, user, pw);
            assertEquals(new String(pw), new String(keyring.getPassword(url, user)));
        } finally {
            // Cleanup
            keyring.clearPassword(url, user);
        }
    }

    @Test
    public void verifySavedPasswordIsRetrieveableLater() {

        final String url = "www.example1.com";
        final String user = "mike";
        final char[] pw = new char[] {'1', '2', '3'};
        Keyring k2 = null;

        try {
            keyring.savePassword(url, user, pw);
            assertEquals(new String(pw), new String(keyring.getPassword(url, user)));
            k2 = new InsecureFileBasedKeyringImpl(((InsecureFileBasedKeyringImpl) keyring).getKeyringFile().getAbsolutePath());
            assertEquals(new String(pw), new String(k2.getPassword(url, user)));
        } finally {
            // Cleanup
            keyring.clearPassword(url,user);
            if (k2 != null)
                k2.clearPassword(url, user);
        }

    }

    @Test
    public void verifySavePasswordReplacesExisting() {

        final String url = "www.example2.com";
        final String user = "mike";
        final char[] pw1 = new char[] {'1', '2', '3'};
        final char[] pw2 = new char[] {'4', '5', '6'};

        try {
            keyring.savePassword(url, user, pw1);
            keyring.savePassword(url, user, pw2);

            assertEquals(new String(pw2), new String(keyring.getPassword(url, user)));
        } finally {
            // Cleanup
            keyring.clearPassword(url, user);
        }
    }

    @Test
    public void verifyClearedPasswordIsCleared() {

        String url = "www.example3.com";
        String user = "mike";
        char[] pw = new char[] {'1', '2', '3'};

        keyring.savePassword(url, user, pw);
        assertNotNull(keyring.getPassword(url, user));
        keyring.clearPassword(url, user);
        assertNull(keyring.getPassword(url, user));
    }

    @Test
    public void multipleServersSameUser() {

        String url1 = "www.example4.com";
        String url2 = "www.fake.com";
        String user = "mike";
        char[] pw1 = new char[] {'1', '2', '3'};
        char[] pw2 = new char[] {'4', '5', '6'};

        try {
            keyring.savePassword(url1, user, pw1);
            keyring.savePassword(url2, user, pw2);

            assertEquals(new String(pw1), new String(keyring.getPassword(url1, user)));
            assertEquals(new String(pw2), new String(keyring.getPassword(url2, user)));
        } finally {
            // Cleanup
            keyring.clearPassword(url1, user);
            keyring.clearPassword(url2, user);
        }
    }

    @Test
    public void multipleUsersSameServer() {

        String url = "www.example5.com";
        String user1 = "mike";
        String user2 = "mary";
        char[] pw1 = new char[] {'1', '2', '3'};
        char[] pw2 = new char[] {'4', '5', '6'};

        try {
            keyring.savePassword(url, user1, pw1);
            keyring.savePassword(url, user2, pw2);

            assertEquals(new String(pw1), new String(keyring.getPassword(url, user1)));
            assertEquals(new String(pw2), new String(keyring.getPassword(url, user2)));
        } finally {
            // Cleanup
            keyring.clearPassword(url, user1);
            keyring.clearPassword(url, user2);
        }
    }
}
