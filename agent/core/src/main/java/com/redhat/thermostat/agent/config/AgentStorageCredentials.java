/*
 * Copyright 2012-2014 Red Hat, Inc.
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
package com.redhat.thermostat.agent.config;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import com.redhat.thermostat.agent.locale.LocaleResources;
import com.redhat.thermostat.shared.config.InvalidConfigurationException;
import com.redhat.thermostat.shared.locale.Translate;
import com.redhat.thermostat.storage.core.StorageCredentials;

public class AgentStorageCredentials implements StorageCredentials {

    private static final Translate<LocaleResources> t = LocaleResources.createLocalizer();

    private static char[] pw = {'p', 'a', 's', 's', 'w', 'o', 'r', 'd'};
    private static char[] user = {'u', 's', 'e', 'r', 'n', 'a', 'm', 'e'};
    private static String newLine = System.lineSeparator();
    private static char comment = '#';

    final File authFile;
    private String username = null; // default value

    public AgentStorageCredentials(File agentAuthFile) {
        this.authFile = agentAuthFile;
        long length = authFile.length();
        if (length > Integer.MAX_VALUE || length < 0L) {
            // Unlikely issue with authFile, try to get path to share with user via exception message
            String authPath = "";
            try {
                authPath = authFile.getCanonicalPath();
            } catch (IOException e) {
                authPath = "ERROR_GETTING_CANONICAL_PATH";
            }
            throw new InvalidConfigurationException(t.localize(LocaleResources.FILE_NOT_VALID, authPath));
        }
        // Cache username but not password, instead read that on demand to prevent heap dump
        // password leak attack.
        initUsernameFromFile();
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public char[] getPassword() {
        return readPasswordFromFile();
    }

    private void initUsernameFromFile() {
        char[] authData = getAuthFileData();
        if (authData == null) {
            return;
        }
        try {
            setUsernameFromData(authData, (int) authFile.length());
        } finally {
            clearCharArray(authData);
        }
    }

    private char[] readPasswordFromFile() {
        char[] authData = getAuthFileData();
        if (authData == null) {
            return null;
        }
        char[] password = null;
        try {
            password = getValueFromData(authData, (int) authFile.length(), pw);
        } finally {
            clearCharArray(authData);
        }
        return password;
    }

    private char[] getAuthFileData() {
        char[] authData = null;
        if (authFile != null && authFile.canRead() && authFile.isFile()) {
            int length = (int) authFile.length(); // Verified during constructor that this cast is safe
            try (Reader reader = new InputStreamReader(new FileInputStream(authFile), StandardCharsets.US_ASCII)) {
                // file size in bytes >= # of chars so this size should be sufficient.
                authData = new char[length];
                // This is probably the most sensitive time for password-in-heap exposure.
                // The reader here may contain buffers containing the password.  It will,
                // of course, be garbage collected in due time.
                int chars = reader.read(authData, 0, length);
                if (chars != length) {
                    throw new InvalidConfigurationException("End of auth file stream reached unexpectedly.");
                }
            } catch (IOException e) {
                throw new InvalidConfigurationException(e);
            }
        }
        return authData;
    }

    private void setUsernameFromData(char[] data, int dataLen) {
        try {
            char[] userChars = getValueFromData(data, dataLen, user);
            if (userChars != null) {
                username = new String(userChars);
            }
        } finally {
            
        }
    }

    private char[] getValueFromData(char[] data, int dataLen, char[] target) {
        int position = 0;
        while (position < dataLen) {
            if ((position + 1 == dataLen) || data[position + 1] == newLine.charAt(0)) {
                // Empty line
                position = nextLine(data, position);
                continue;
            }
            if (data[position] == comment) {
                // Comment
                position = nextLine(data, position);
                continue;
            }
            char[] value = getPassword(data, position);
            if (value != null) {
                // Password
                if (pw.equals(target)) {
                    return value;
                } else {
                    clearCharArray(value);
                }
                position = nextLine(data, position);
                value = null;
                continue;
            }
            value = getUserName(data, position);
            if (value != null) {
                // Username
                if (user.equals(target)) {
                    return value;
                } else {
                    clearCharArray(value);
                }
                position = nextLine(data, position);
                value = null;
                continue;
            }
            // Unrecognized content in file
            throw new InvalidConfigurationException(t.localize(LocaleResources.BAD_AGENT_AUTH_CONTENTS));
        }
        return null;
    }

    private int nextLine(char[] data, int current) {
        int next = current + 1;
        while (next < data.length) {
            if (data[next] == newLine.charAt(0)) {
                break;
            }
            next += newLine.length();
        }
        next++;
        return next;
    }

    private char[] getPassword(char[] data, int start) {
        return getValue(data, start, pw);
    }

    private char[] getUserName(char[] data, int start) {
        return getValue(data, start, user);
    }

    private char[] getValue(char[] data, int start, char[] key) {
        if (data[start + key.length] != '=') {
            return null;
        }
        for (int i = 0; i < key.length; i++) {
            if (key[i] != data[start + i]) {
                return null;
            }
        }
        int end = positionOf(newLine.charAt(0), data, start, data.length);
        char[] value = Arrays.copyOfRange(data, start + key.length + 1, end);
        return value;
    }

    private int positionOf(char character, char[] data, int start, int end) {
        int position = -1;
        for (int possible = start; possible < data.length && possible <= end; possible++) {
            if (data[possible] == character) {
                position = possible;
                break;
            }
        }
        return position;
    }

    public void clearCharArray(char[] chars) {
        if (chars != null) {
            Arrays.fill(chars, '\0');
        }
    }

    public String getStorageUrl() {
        return null;
    }
}

