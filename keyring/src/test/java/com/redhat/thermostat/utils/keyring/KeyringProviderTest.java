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

package com.redhat.thermostat.utils.keyring;

import static org.junit.Assert.*;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.redhat.thermostat.utils.keyring.GnomeKeyringLibraryNative;
import com.redhat.thermostat.utils.keyring.Keyring;
import com.redhat.thermostat.utils.keyring.KeyringProvider;
import com.redhat.thermostat.utils.keyring.MemoryKeyring;

public class KeyringProviderTest {

    private static String defaultKeyringProvider;
    
    @BeforeClass
    public static void setUp() {
        defaultKeyringProvider = System.getProperty(KeyringProvider.KEYRING_FACTORY_PROPERTY);
    }
    
    @Test
    public void testLoadMemoryProvider() {
        System.setProperty(KeyringProvider.KEYRING_FACTORY_PROPERTY, KeyringProvider.MEMORY_KEYRING);
        // setting the system property causes the keyring to load always the same class
        // depending on the order of the tests, since it is a singleton, so we can't rely
        // on that; we test only getDefaultKeyring, which is what initialises the keyring
        Keyring keyring = KeyringProvider.getDefaultKeyring();
        assertTrue(keyring instanceof MemoryKeyring);
    }

    @Test
    public void testLoadDefaultProvider() {
        System.setProperty(KeyringProvider.KEYRING_FACTORY_PROPERTY, KeyringProvider.DEFAULT_KEYRING);
        try {
            Keyring keyring = KeyringProvider.getDefaultKeyring();
            assertTrue(keyring instanceof GnomeKeyringLibraryNative);
        } catch (UnsatisfiedLinkError e) {
            // this is expected if the build has not been configured correctly
            // since it also means that the class has been initialized
            e.printStackTrace();
        }
    }

    @Test(expected=InternalError.class)
    public void testInvalidProvider() {
        System.setProperty(KeyringProvider.KEYRING_FACTORY_PROPERTY, "exception expected!");
        Keyring keyring = KeyringProvider.getDefaultKeyring();
        fail();
    }

    @AfterClass
    public static void tearDown() {
        if (defaultKeyringProvider != null) {
            System.setProperty(KeyringProvider.KEYRING_FACTORY_PROPERTY, defaultKeyringProvider);
        }
    }
}

