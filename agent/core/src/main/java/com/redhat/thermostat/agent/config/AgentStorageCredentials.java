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

    private static final char[] pw = {'p', 'a', 's', 's', 'w', 'o', 'r', 'd'};
    private static final char[] user = {'u', 's', 'e', 'r', 'n', 'a', 'm', 'e'};
    private static final char comment = '#';

    private final String newLine;

    private final File authFile;
    private final Reader testingAuthReader;
    private final int authDataLength;
    private String username;

    public AgentStorageCredentials(File agentAuthFile) {
        if (agentAuthFile == null) {
            throw new IllegalArgumentException("agentAuthFile must not be null");
        }
        this.testingAuthReader = null;
        this.authFile = agentAuthFile;
        newLine = System.lineSeparator();
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
        authDataLength = (int) length;
        // Cache username but not password, instead read that on demand to prevent heap dump
        // password leak attack.
        initUsername();
    }

    // Testing constructor
    AgentStorageCredentials(Reader agentAuthReader, String lineSeparator) {
        if (agentAuthReader == null) {
            throw new IllegalArgumentException("agentAuthReader must not be null");
        }
        this.testingAuthReader = agentAuthReader;
        newLine = lineSeparator;
        long length = -1;
        try {
            length = testingAuthReader.skip(Long.MAX_VALUE);
            if (length > Integer.MAX_VALUE) {
                throw new IllegalArgumentException("agentAuthReader larger than supported Integer.MAX_VALUE");
            }
            testingAuthReader.reset();
        } catch (IOException e) {
            throw new IllegalArgumentException("IOException from agentAuthReader", e);
        }
        authDataLength = (int) length;
        this.authFile = null;
        initUsername();
    }

    // Testing constructor
    AgentStorageCredentials(Reader agentAuthReader) {
        this(agentAuthReader, System.lineSeparator());
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public char[] getPassword() {
        return readPassword();
    }

    private Reader getReader() throws IOException {
        if (testingAuthReader != null) {
           return testingAuthReader;
        }
        if (authFile == null || !authFile.canRead() || !authFile.isFile()) {
            throw new IllegalStateException("Invalid agent.auth file: " + authFile.getCanonicalPath());
        }
        return new InputStreamReader(new FileInputStream(authFile), StandardCharsets.US_ASCII);
    }

    private void initUsername() {
        char[] authData = getAuthData();
        if (authData == null) {
            return;
        }
        try {
            setUsernameFromData(authData, authDataLength);
        } finally {
            clearCharArray(authData);
        }
    }

    private char[] readPassword() {
        char[] authData = getAuthData();
        if (authData == null) {
            return null;
        }
        char[] password = null;
        try {
            password = getValueFromData(authData, authDataLength, pw);
        } finally {
            clearCharArray(authData);
        }
        return password;
    }

    private char[] getAuthData() {
        char[] authData = null;
        Reader reader = null;
        try {
            try {
                reader = getReader();
            } catch (IllegalStateException e) {
                // Callers will assume null auth parameters.
                return null;
            }
            // file size in bytes >= # of chars so this size should be sufficient.
            authData = new char[authDataLength];
            // This is probably the most sensitive time for password-in-heap exposure.
            // The reader here may contain buffers containing the password.  It will,
            // of course, be garbage collected in due time.
            int chars = reader.read(authData, 0, authDataLength);
            if (chars != authDataLength) {
                throw new InvalidConfigurationException("End of auth file stream reached unexpectedly.");
            }
            if (reader == testingAuthReader) {
                reader.reset();
            }
        } catch (IOException e) {
            throw new InvalidConfigurationException(e);
        } finally {
            if (reader != testingAuthReader) {
                try {
                    reader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
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
            while (Character.isWhitespace(data[position])) {
                // skip leading whitespace.
                position++;
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
        int nextNewLine = getPositionOfNextNewline(data, current);
        return nextNewLine + newLine.length();
    }

    private int getPositionOfNextNewline(char[] data, int current) {
        int next = current;
        while (next < data.length) {
            boolean newLineFound = false;
            for (int i = 0; i < newLine.length(); i++) {
                if (data[next + i] == newLine.charAt(i)) {
                    newLineFound = true;
                } else {
                    newLineFound = false;
                }
            }
            if (newLineFound) {
                break;
            }
            next++;
        }
        return next;
    }

    private char[] getPassword(char[] data, int start) {
        return getValue(data, start, pw);
    }

    private char[] getUserName(char[] data, int start) {
        return getValue(data, start, user);
    }

    private char[] getValue(char[] data, int start, char[] key) {
        int position = start;
        for (int i = 0; i < key.length; i++) {
            if (key[i] != data[position]) {
                return null;
            }
            position++;
        }
        while (Character.isWhitespace(data[position])) {
            // skip whitespace between key and "=".
            position++;
        }
        if (data[position] != '=') {
            return null;
        } else {
            position++;
        }
        while (Character.isWhitespace(data[position])) {
            // skip whitespace between "=" and value.
            position++;
        }
        int valueStart = position;
        int valueEnd = getPositionOfNextNewline(data, position);
        while (Character.isWhitespace(data[valueEnd])) {
            // Ignore whitespace after value.
            valueEnd--;
        }
        valueEnd++;
        char[] value = Arrays.copyOfRange(data, valueStart, valueEnd);
        return value;
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

