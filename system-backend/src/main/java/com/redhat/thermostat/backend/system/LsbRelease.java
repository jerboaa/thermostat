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

package com.redhat.thermostat.backend.system;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.redhat.thermostat.common.utils.LoggingUtils;

public class LsbRelease implements DistributionInformationSource {

    private static final Logger logger = LoggingUtils.getLogger(LsbRelease.class);

    private static final String DISTRIBUTION_NAME = "distributor id";
    private static final String DISTRIBUTION_VERSION = "release";
    private static final String LSB_RELEASE_SCRIPT = "lsb_release";
    
    private final String lsbRelaseBin;
    
    public LsbRelease() {
        this.lsbRelaseBin = LSB_RELEASE_SCRIPT;
    }
    
    // package-private for testing
    LsbRelease(String lsbReleaseBin) {
        this.lsbRelaseBin = lsbReleaseBin;
    }

    @Override
    public DistributionInformation getDistributionInformation()
            throws IOException {
        return getFromLsbRelease();
    }

    public DistributionInformation getFromLsbRelease() throws IOException {
        BufferedReader reader = null;
        try {
            Process lsbProc = Runtime.getRuntime().exec(new String[] { lsbRelaseBin, "-a" });
            InputStream progOutput = lsbProc.getInputStream();
            reader = new BufferedReader(new InputStreamReader(progOutput));
            DistributionInformation result = getFromLsbRelease(reader);
            int exitValue = lsbProc.waitFor();
            if (exitValue != 0) {
                logger.log(Level.WARNING, "unable to identify distribution, problems running 'lsb_release'");
            }
            return result;
        } catch (InterruptedException e) {
            throw new IOException(e);
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    logger.log(Level.WARNING, "unable to close a child's output stream");
                }
            }
        }

    }

    public DistributionInformation getFromLsbRelease(BufferedReader reader) throws IOException {
        String name = DistributionInformation.UNKNOWN_NAME;
        String version = DistributionInformation.UNKNOWN_VERSION;

        String line;
        while ((line = reader.readLine()) != null) {
            int sepLocation = line.indexOf(":");
            if (sepLocation != -1) {
                String key = line.substring(0, sepLocation).toLowerCase();
                if (key.equals(DISTRIBUTION_NAME)) {
                    name = line.substring(sepLocation + 1).trim();
                } else if (key.equals(DISTRIBUTION_VERSION)) {
                    version = line.substring(sepLocation + 1).trim();
                }
            }
        }

        logger.log(Level.FINE, "distro-name: " + name);
        logger.log(Level.FINE, "distro-version: " + version);

        return new DistributionInformation(name, version);
    }

}

