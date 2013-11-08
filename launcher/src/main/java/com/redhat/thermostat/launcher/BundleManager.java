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

package com.redhat.thermostat.launcher;

import java.io.IOException;
import java.util.List;

import org.osgi.framework.BundleException;
import org.osgi.framework.launch.Framework;

import com.redhat.thermostat.annotations.Service;
import com.redhat.thermostat.launcher.internal.BundleLoader;
import com.redhat.thermostat.shared.config.Configuration;

/**
 * A Service that provides features to load bundles for given command names.
 * <p>
 * It can install bundles in to the current runtime and start them. It can find
 * bundles by file paths or by bundle meta-data.
 */
@Service
public abstract class BundleManager {

    /**
     * Load and start bundles using the metadata about a bundle.
     */
    public abstract void loadBundlesByName(List<BundleInformation> bundles) throws BundleException, IOException;

    public static void preLoadBundles(Framework framework, List<String> bundleLocations,
            boolean printOSGiInfo) throws BundleException {
        BundleLoader loader = new BundleLoader();
        loader.setPrintOSGiInfo(printOSGiInfo);
        loader.installAndStartBundles(framework, bundleLocations);
    }

    public abstract Configuration getConfiguration();

}

