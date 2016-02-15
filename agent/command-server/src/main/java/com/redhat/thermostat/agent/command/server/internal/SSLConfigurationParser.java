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

package com.redhat.thermostat.agent.command.server.internal;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import com.redhat.thermostat.shared.config.SSLConfiguration;

class SSLConfigurationParser {
    
    SSLConfiguration parse(InputStream in) throws IOException {
        /* Read in SSLConfiguration from agent as follows:
         * '<BEGIN SSL CONFIG>'
         * KSFILE:Keystore File Path
         * KSPASS:Keystore Password
         * Enabled for Command Channel (true/false)
         * Enabled for Backing Storage (true/false)
         * Disable Hostname Verification (true/false)
         * '<END SSL CONFIG>'
         * 
         * If either the keystore file or password are null, the respective
         * KSFILE or KSPASS token will be replaced with KSNULL. This is
         * done to account for the case where the keystore password is the
         * string "null".
         */
        BufferedReader br = new BufferedReader(new InputStreamReader(in, "UTF-8"));
        String line = br.readLine();
        requireNonNull(line, "Expected " + CommandChannelConstants.BEGIN_SSL_CONFIG_TOKEN + ", got EOF");
        if (!CommandChannelConstants.BEGIN_SSL_CONFIG_TOKEN.equals(line)) {
            throw new IOException("Expected " + CommandChannelConstants.BEGIN_SSL_CONFIG_TOKEN + ", got: " + line);
        }
        
        // Read keystore path
        line = br.readLine();
        requireNonNull(line, "Expected path to keystore file, got EOF");
        File keystoreFile;
        if (CommandChannelConstants.KEYSTORE_NULL.equals(line)) {
            keystoreFile = null;
        } else if (line.startsWith(CommandChannelConstants.KEYSTORE_FILE_PREFIX)) {
            String path = line.substring(CommandChannelConstants.KEYSTORE_FILE_PREFIX.length());
            keystoreFile = new File(path);
        } else {
            throw new IOException("Expected " + CommandChannelConstants.KEYSTORE_FILE_PREFIX + ": or " + CommandChannelConstants.KEYSTORE_NULL + ", got: " + line);
        }
        
        // Read keystore password
        line = br.readLine();
        requireNonNull(line, "Expected keystore password, got EOF");
        String keystorePass;
        if (CommandChannelConstants.KEYSTORE_NULL.equals(line)) {
            keystorePass = null;
        } else if (line.startsWith(CommandChannelConstants.KEYSTORE_PASS_PREFIX)) {
            keystorePass = line.substring(CommandChannelConstants.KEYSTORE_PASS_PREFIX.length());
        } else {
            throw new IOException("Expected " + CommandChannelConstants.KEYSTORE_PASS_PREFIX + ": or " + CommandChannelConstants.KEYSTORE_NULL + ", got: " + line);
        }
        
        // Read enabled for command channel
        line = br.readLine();
        requireNonNull(line, "Expected enabled for command channel boolean, got EOF");
        if (!isBoolean(line)) {
            throw new IOException("Expected enabled for command channel boolean, got: " + line);
        }
        Boolean cmdChannel = Boolean.valueOf(line);
        
        // Read enabled for backing storage
        line = br.readLine();
        requireNonNull(line, "Expected enabled for backing storage boolean, got EOF");
        if (!isBoolean(line)) {
            throw new IOException("Expected enabled for backing storage boolean, got: " + line);
        }
        Boolean backingStorage = Boolean.valueOf(line);
        
        // Read disable host verification boolean
        line = br.readLine();
        requireNonNull(line, "Expected disable host verification boolean, got EOF");
        if (!isBoolean(line)) {
            throw new IOException("Expected disable host verification boolean, got: " + line);
        }
        Boolean disableVerification = Boolean.valueOf(line);
        
        line = br.readLine();
        requireNonNull(line, "Expected " + CommandChannelConstants.END_SSL_CONFIG_TOKEN + ", got EOF");
        if (!CommandChannelConstants.END_SSL_CONFIG_TOKEN.equals(line)) {
            throw new IOException("Expected " + CommandChannelConstants.END_SSL_CONFIG_TOKEN + ", got: " + line);
        }
        
        return new CommandChannelSSLConfiguration(keystoreFile, keystorePass, cmdChannel, 
                backingStorage, disableVerification);
    }
    
    private boolean isBoolean(String line) {
        return "true".equalsIgnoreCase(line) || "false".equalsIgnoreCase(line);
    }
    
    private void requireNonNull(String line, String errorMessage) throws IOException {
        if (line == null) {
            throw new IOException(errorMessage);
        }
    }

}
