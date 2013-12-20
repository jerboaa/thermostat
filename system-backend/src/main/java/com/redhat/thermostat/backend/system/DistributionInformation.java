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

package com.redhat.thermostat.backend.system;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.redhat.thermostat.common.utils.LoggingUtils;

public class DistributionInformation {

    public static final String UNKNOWN_NAME = "Unknown Distribution";
    public static final String UNKNOWN_VERSION = "Unknown Version";

    private static final Logger logger = LoggingUtils.getLogger(DistributionInformation.class);

    private final String name;
    private final String version;

    public DistributionInformation(String name, String version) {
        this.name = name;
        this.version = version;
    }

    public static DistributionInformation get() {
        EtcOsRelease etcOsRelease = new EtcOsRelease();
        LsbRelease lsbRelease = new LsbRelease();
        return get(etcOsRelease, lsbRelease);
    }
    
    // package-private for testing
    static DistributionInformation get(EtcOsRelease etcOsRelease, LsbRelease lsbRelease) {
        try {
            return etcOsRelease.getDistributionInformation();
        } catch (IOException e) {
            // Log only at level FINE, since we have the LSB fallback
            logger.log(Level.FINE, "unable to use os-release", e);
        }
        try {
            return lsbRelease.getDistributionInformation();
        } catch (IOException e) {
            // Log exception at level FINE only.
            logger.log(Level.FINE, "unable to use lsb_release", e);
            logger.log(Level.WARNING, "unable to use os-release AND lsb_release");
        }
        return new DistributionInformation(UNKNOWN_NAME, UNKNOWN_VERSION);
    }

    /**
     * @return the name of the distribution, or {@link #UNKNOWN_NAME} if it can not be
     * identified
     */
    public String getName() {
        return name;
    }

    /**
     * @return the release of the distribution or {@link #UNKNOWN_VERSION} if it can not be
     * identified
     */
    public String getVersion() {
        return version;
    }

}

