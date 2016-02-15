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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Objects;
import java.util.Properties;

import com.redhat.thermostat.shared.config.CommonPaths;

class AuthFileWriter extends CredentialsWriter {
    
    private final CommonPaths paths;
    private final CredentialsFileCreator creator;
    
    AuthFileWriter(CommonPaths paths, CredentialsFileCreator creator) {
        this.paths = paths;
        this.creator = creator;
    }

    public void write() throws IOException {
        char[] password = getPassword();
        String username = getUsername();
        Objects.requireNonNull(password);
        Objects.requireNonNull(username);
        Properties credentialProps = new Properties();
        credentialProps.setProperty("username", username);
        credentialProps.setProperty("password", String.valueOf(password));
        File credentialsFile = paths.getUserAgentAuthConfigFile();
        creator.create(credentialsFile);
        try (FileOutputStream fout = new FileOutputStream(credentialsFile)) {
            credentialProps.store(fout, "Credentials used for 'thermostat agent' connections.");
        } finally {
            Arrays.fill(password, '\0');
        }
    }
}
