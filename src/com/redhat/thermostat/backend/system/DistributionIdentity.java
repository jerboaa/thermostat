/*
 * Copyright 2012 Red Hat, Inc.
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

/**
 * Implementation note: this relies on the {@code lsb_release} program to work.
 */
public class DistributionIdentity {

    private static final String DISTRIBUTION_NAME = "distributor id";
    private static final String DISTRIBUTION_VERSION = "release";

    private static final Logger logger = LoggingUtils.getLogger(DistributionIdentity.class);

    private final String name;
    private final String version;

    public DistributionIdentity() {
        String tempName = "Unknown Distribution";
        String tempVersion = "Unknown";
        BufferedReader reader = null;
        try {
            Process lsbProc = Runtime.getRuntime().exec(new String[] { "lsb_release", "-a" });
            InputStream progOutput = lsbProc.getInputStream();
            reader = new BufferedReader(new InputStreamReader(progOutput));
            String line;
            while ((line = reader.readLine()) != null) {
                int sepLocation = line.indexOf(":");
                if (sepLocation != -1) {
                    String key = line.substring(0, sepLocation).toLowerCase();
                    if (key.equals(DISTRIBUTION_NAME)) {
                        tempName = line.substring(sepLocation + 1).trim();
                    } else if (key.equals(DISTRIBUTION_VERSION)) {
                        tempVersion = line.substring(sepLocation + 1).trim();
                    }
                }
            }
        } catch (IOException e) {
            logger.log(Level.WARNING, "unable to identify distribution");
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    logger.log(Level.WARNING, "unable to close a child's output stream");
                }
            }
        }
        name = tempName;
        version = tempVersion;

        logger.log(Level.FINE, "distro-name: " + name);
        logger.log(Level.FINE, "distro-version: " + version);
    }

    /**
     * @return the name of the distrbution, or {@code null} if it can not be
     * identified
     */
    public String getName() {
        return name;
    }

    /**
     * @return the release of the distribution or {@code null} if it can not be
     * identified
     */
    public String getVersion() {
        return version;
    }

}