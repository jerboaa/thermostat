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

package com.redhat.thermostat.backend.system.internal.linux;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

public class EtcOsRelease implements DistributionInformationSource {

    private static final String EMPTY_STRING = "";
    private static final String OS_RELEASE = "/etc/os-release";
    private final String osReleaseFile;
    
    public EtcOsRelease() {
        this.osReleaseFile = OS_RELEASE;
    }
    
    // package-private for testing
    EtcOsRelease(String osReleaseFile) {
        this.osReleaseFile = osReleaseFile;
    }
    
    @Override
    public DistributionInformation getDistributionInformation() throws IOException {
        return getFromOsRelease();
    }

    public DistributionInformation getFromOsRelease() throws IOException {
        return getFromOsRelease(osReleaseFile);
    }

    public DistributionInformation getFromOsRelease(String releaseFile) throws IOException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(releaseFile), StandardCharsets.UTF_8))) {
            return getFromOsRelease(reader);
        }
    }

    public DistributionInformation getFromOsRelease(BufferedReader reader) throws IOException {
        String version = DistributionInformation.UNKNOWN_VERSION;
        String name = DistributionInformation.UNKNOWN_NAME;
        String line = null;
        while ((line = reader.readLine()) != null) {
            // skip whitespace only lines
            line = line.trim();
            if (line.equals(EMPTY_STRING)) {
                continue;
            }
            if (line.matches("^NAME *=.*")) {
                name = readShellVariable(line);
            }
            if (line.matches("^VERSION *=.*")) {
                version = readShellVariable(line);
            }
        }
        return new DistributionInformation(name, version);
    }

    /** Reads and parses a shell variable declaration: {@code FOO="bar"}
     *
     * @return the value of the shell variable
     */
    private String readShellVariable(String line) {
        // TODO we should try to handle shell quotes better
        String result = line.substring(line.indexOf("=")+1);
        result = result.trim();
        if (result.startsWith("\"") && result.endsWith("\"")) {
            result = result.substring(1, result.length()-1);
            result = result.trim();
        }
        return result;
    }


}

