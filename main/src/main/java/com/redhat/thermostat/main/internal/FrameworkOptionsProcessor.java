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

package com.redhat.thermostat.main.internal;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.redhat.thermostat.launcher.FrameworkOptions;

/**
 * Thermostat options for the OSGi framework and relevant debug output.
 *
 */
public class FrameworkOptionsProcessor {

    private final Map<FrameworkOptions, String> globalOptions;
    private final String[] otherOptions;

    public FrameworkOptionsProcessor(String[] args) {
        this.globalOptions = new HashMap<>();
        initializeDefaultGlobalOptions();
        this.otherOptions = processGlobalOptions(args);
    }

    private void initializeDefaultGlobalOptions() {
        // set up default boot delegation to allow the vm-profiler to work
        // correctly by default
        globalOptions.put(FrameworkOptions.BOOT_DELEGATION,
                "com.redhat.thermostat.vm.profiler.agent.jvm," +
                "com.redhat.thermostat.vm.profiler.agent.asm," +
                "com.redhat.thermostat.vm.profiler.agent.asm.commons");
    }

    private String[] processGlobalOptions(String[] args) {
        List<String> toProcess = new ArrayList<>(Arrays.asList(args));
        Iterator<String> iter = toProcess.iterator();
        while (iter.hasNext()) {
            String arg = iter.next();
            if (FrameworkOptions.PRINT_OSGI_INFO.getOptString().equals(arg)) {
                globalOptions.put(FrameworkOptions.PRINT_OSGI_INFO,
                        Boolean.TRUE.toString());
                iter.remove();
            }
            if (FrameworkOptions.IGNORE_BUNDLE_VERSIONS.getOptString().equals(arg)) {
                globalOptions.put(FrameworkOptions.IGNORE_BUNDLE_VERSIONS,
                        Boolean.TRUE.toString());
                iter.remove();
            }
            if (arg.startsWith(FrameworkOptions.BOOT_DELEGATION.getOptString() + "=")) {
                int startIndex = (FrameworkOptions.BOOT_DELEGATION.getOptString() + "=")
                        .length();
                String bootDelegation = arg.substring(startIndex);
                if ("".equals(bootDelegation)) {
                    throw new RuntimeException(
                            "Unexpected string used with boot delegation: '"
                                    + bootDelegation + "'");
                }
                globalOptions.put(FrameworkOptions.BOOT_DELEGATION, bootDelegation);
                iter.remove();
            }
        }
        return toProcess.toArray(new String[] {});
    }

    public String[] getOtherOptions() {
        return otherOptions;
    }

    public boolean printOsgiInfo() {
        return globalOptions.containsKey(FrameworkOptions.PRINT_OSGI_INFO);
    }

    public boolean ignoreBundleVersions() {
        return globalOptions.containsKey(FrameworkOptions.IGNORE_BUNDLE_VERSIONS);
    }

    public String bootDelegationValue() {
        return globalOptions.get(FrameworkOptions.BOOT_DELEGATION);
    }

}
