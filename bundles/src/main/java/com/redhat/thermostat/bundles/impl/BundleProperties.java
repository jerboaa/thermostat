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

package com.redhat.thermostat.bundles.impl;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

public class BundleProperties {

    private Map<String, List<String>> bundleDependencies;

    BundleProperties(String thermostatHome) throws FileNotFoundException, IOException {
        bundleDependencies = new HashMap<>();
        File bundlePropFile = new File(thermostatHome + File.separator + "etc", "bundles.properties");
        Properties bundleProps = new Properties();
        bundleProps.load(new FileReader(bundlePropFile));
        for (Entry<Object,Object> entry: bundleProps.entrySet()) {
            String libRoot = thermostatHome + File.separator + "libs";
            String group = (String) entry.getKey();
            List<String> resourceNames = Arrays.asList(((String)entry.getValue()).split(","));
            List<String> paths = new ArrayList<>(resourceNames.size());
            for (String value: resourceNames) {
                File file = new File(libRoot, value.trim());
                String path = file.toURI().toString();
                if (!file.exists()) {
                    throw new FileNotFoundException("Bundle " + path + " required by " +
                            group + " command does not exist in the filesystem.");
                }
                paths.add(path);
            }
            bundleDependencies.put(group, paths);
        }

    }

    public List<String> getDependencyResourceNamesFor(String group) {
        List<String> deps = bundleDependencies.get(group);
        if (deps == null) {
            deps = new ArrayList<String>();
            bundleDependencies.put(group, deps);
        }
        return deps;
    }

}
