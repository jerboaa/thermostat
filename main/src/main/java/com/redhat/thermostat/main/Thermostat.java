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

package com.redhat.thermostat.main;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import com.redhat.thermostat.main.impl.FrameworkProvider;
import com.redhat.thermostat.shared.config.CommonPaths;
import com.redhat.thermostat.shared.config.internal.CommonPathsImpl;

public class Thermostat {

    /**
     * @param args the arguments to the program
     */
    public static void main(String[] args) {
        CommonPaths paths = new CommonPathsImpl();

        Thermostat thermostat = new Thermostat();
        thermostat.start(paths, args);
    }

    public void start(CommonPaths paths, String[] args) {
        boolean printOSGiInfo = false;
        boolean ignoreBundleVersions = false;
        String bootDelegation = null;

        List<String> toProcess = new ArrayList<>(Arrays.asList(args));
        Iterator<String> iter = toProcess.iterator();
        while (iter.hasNext()) {
            String arg = iter.next();
            if ("--print-osgi-info".equals(arg)) {
                printOSGiInfo = true;
                iter.remove();
            }
            if ("--ignore-bundle-versions".equals(arg)) {
                ignoreBundleVersions = true;
                iter.remove();
            }
            if (arg.startsWith("--boot-delegation=")) {
                int startIndex = "--boot-delegation=".length();
                bootDelegation = arg.substring(startIndex);
                iter.remove();
            }
        }

        FrameworkProvider frameworkProvider = createFrameworkProvider(paths, printOSGiInfo, ignoreBundleVersions, bootDelegation);
        frameworkProvider.start(toProcess.toArray(new String[0]));
    }

    /* allow overriding for unit testing */
    protected FrameworkProvider createFrameworkProvider(CommonPaths paths, boolean printOSGiInfo, boolean ignoreBundleVersions, String bootDelegation) {
        return new FrameworkProvider(paths, printOSGiInfo, ignoreBundleVersions, bootDelegation);
    }
}

