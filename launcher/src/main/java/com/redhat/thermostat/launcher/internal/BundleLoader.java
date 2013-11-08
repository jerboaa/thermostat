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

package com.redhat.thermostat.launcher.internal;

import java.util.ArrayList;
import java.util.List;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.framework.launch.Framework;

public class BundleLoader {

    private boolean printOSGiInfo = false;

    public BundleLoader() {
    }

    public void setPrintOSGiInfo(boolean printOSGiInfo) {
        this.printOSGiInfo = printOSGiInfo;
    }

    /**
     * Install and start bundles in one step (solving any interdependencies).
     *
     * @param framework the {@link Framework} to use for installing and starting
     * the bundles.
     * @param bundleLocations a {@link List} of {@link String}s where each
     * {@code String} is a URL of the bundle to load
     * @return a {@link List} of {@link Bundle}s that were started.
     * @throws BundleException
     */
    public List<Bundle> installAndStartBundles(Framework framework,
            List<String> bundleLocations) throws BundleException {
        List<Bundle> bundles = new ArrayList<>();
        BundleContext ctx = framework.getBundleContext();
        for (String location : bundleLocations) {
            Bundle bundle = ctx.installBundle(location);
            if (printOSGiInfo) {
                System.out.println("BundleLoader: installed bundle: \"" + 
                        location + "\" as id " + bundle.getBundleId());
            }
            bundles.add(bundle);
        }
        startBundles(bundles);
        return bundles;
    }

    private void startBundles(List<Bundle> bundles) throws BundleException {
        for (Bundle bundle : bundles) {

            if (bundle.getHeaders().get(Constants.FRAGMENT_HOST) != null) {
                if (printOSGiInfo) {
                    System.out.println("BundleLoader: bundle \"" + bundle.getBundleId() + "\" is a fragment; not starting it");
                }
                continue;
            }

            if (printOSGiInfo) {
                System.out.println("BundleLoader: starting bundle: \"" + bundle.getBundleId() + "\"");
            }
            // We don't want for the framework to set the auto-start bit. Thus, passing
            // START_TRANSIENT explicitly
            bundle.start(Bundle.START_TRANSIENT);
        }
    }

}

