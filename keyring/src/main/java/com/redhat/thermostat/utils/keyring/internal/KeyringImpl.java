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

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.util.Arrays;

import com.redhat.thermostat.shared.config.NativeLibraryResolver;
import com.redhat.thermostat.utils.keyring.Keyring;
import com.redhat.thermostat.utils.keyring.KeyringException;

public class KeyringImpl implements Keyring {

    private static String DESC_PREFIX = "Thermostat auth info for: ";

    static {
        String lib = NativeLibraryResolver.getAbsoluteLibraryPath("GnomeKeyringWrapper");
        System.load(lib);
    }

    @Override
    public synchronized void savePassword(String url, String username, char[] password) {
        byte[] pwBytes = null;
        boolean success = false;
        try {
            String desc = DESC_PREFIX + username + "@" + url;
            if (password == null) {
                pwBytes = new byte[]{};
            } else {
                pwBytes = Charset.defaultCharset().encode(CharBuffer.wrap(password)).array();
            }
            success = gnomeKeyringWrapperSavePasswordNative(url, username, pwBytes, desc);
        } finally {
            if (pwBytes != null) {
                Arrays.fill(pwBytes, (byte) 0);
            }
        }
        if (!success) {
            throw new KeyringException("Couldn't save password.");
        }
    }

    @Override
    public synchronized char[] getPassword(String url, String username) {
        byte[] pwBytes = gnomeKeyringWrapperGetPasswordNative(url, username);
        char[] password = null;
        if (pwBytes != null) {
            try {
                password = Charset.defaultCharset().decode(ByteBuffer.wrap(pwBytes)).array();
            } finally {
                Arrays.fill(pwBytes, (byte) 0);
            }
        }
        return password;
    }

    @Override
    public synchronized void clearPassword(String url, String username) {
        if (!gnomeKeyringWrapperClearPasswordNative(url, username)) {
            throw new RuntimeException("Couldn't clear password.");
        }
        
    }

    private static native boolean gnomeKeyringWrapperSavePasswordNative(String url, String userName, byte[] password, String description);
    private static native byte[] gnomeKeyringWrapperGetPasswordNative(String url, String userName);
    private static native boolean gnomeKeyringWrapperClearPasswordNative(String url, String userName);
}

