/*
 * Copyright 2012-2015 Red Hat, Inc.
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

package com.redhat.thermostat.main.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Thermostat options for the OSGi framework and relevant debug output.
 *
 */
public class FrameworkOptions {

    public enum Option {

        /**
         * Print debug information related to the OSGi framework's boot/shutdown
         * process.
         */
        PRINT_OSGI_INFO("--print-osgi-info"),
        /**
         * Ignore exact bundle versions and use whatever version is available.
         */
        IGNORE_BUNDLE_VERSIONS("--ignore-bundle-versions"),
        /**
         * Boot delegation string passed on to the OSGi framework.
         */
        BOOT_DELEGATION("--boot-delegation"),
        /**
         * Clean the OSGi framework's cache before booting.
         */
        CLEAN_OSGI_CACHE("--clean-osgi-cache"), ;

        private final String optString;

        private Option(String optString) {
            this.optString = optString;
        }

        public String getOptString() {
            return optString;
        }
    }

    private final Map<Option, String> globalOptions;
    private final String[] otherOptions;

    public FrameworkOptions(String[] args) {
        this.globalOptions = new HashMap<>();
        this.otherOptions = processGlobalOptions(args);
    }

    private String[] processGlobalOptions(String[] args) {
        List<String> toProcess = new ArrayList<>(Arrays.asList(args));
        Iterator<String> iter = toProcess.iterator();
        while (iter.hasNext()) {
            String arg = iter.next();
            if (Option.PRINT_OSGI_INFO.getOptString().equals(arg)) {
                globalOptions.put(Option.PRINT_OSGI_INFO,
                        Boolean.TRUE.toString());
                iter.remove();
            }
            if (Option.IGNORE_BUNDLE_VERSIONS.getOptString().equals(arg)) {
                globalOptions.put(Option.IGNORE_BUNDLE_VERSIONS,
                        Boolean.TRUE.toString());
                iter.remove();
            }
            if (arg.startsWith(Option.BOOT_DELEGATION.getOptString() + "=")) {
                int startIndex = (Option.BOOT_DELEGATION.getOptString() + "=")
                        .length();
                String bootDelegation = arg.substring(startIndex);
                if ("".equals(bootDelegation)) {
                    throw new RuntimeException(
                            "Unexpected string used with boot delegation: '"
                                    + bootDelegation + "'");
                }
                globalOptions.put(Option.BOOT_DELEGATION, bootDelegation);
                iter.remove();
            }
            if (Option.CLEAN_OSGI_CACHE.getOptString().equals(arg)) {
                globalOptions.put(Option.CLEAN_OSGI_CACHE,
                        Boolean.TRUE.toString());
                iter.remove();
            }
        }
        return toProcess.toArray(new String[] {});
    }

    public String[] getOtherOptions() {
        return otherOptions;
    }

    public boolean printOsgiInfo() {
        return globalOptions.containsKey(Option.PRINT_OSGI_INFO);
    }

    public boolean ignoreBundleVersions() {
        return globalOptions.containsKey(Option.IGNORE_BUNDLE_VERSIONS);
    }

    public String bootDelegationValue() {
        return globalOptions.get(Option.BOOT_DELEGATION);
    }

    public boolean cleanOsgiCache() {
        return globalOptions.containsKey(Option.CLEAN_OSGI_CACHE);
    }

}
