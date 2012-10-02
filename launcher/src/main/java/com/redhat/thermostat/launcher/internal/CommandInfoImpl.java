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

package com.redhat.thermostat.launcher.internal;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.logging.Logger;

import com.redhat.thermostat.common.cli.CommandInfo;
import com.redhat.thermostat.common.utils.LoggingUtils;


public class CommandInfoImpl implements CommandInfo {

    private static final Logger logger = LoggingUtils.getLogger(CommandInfoSource.class);
    private static final String PROPERTY_BUNDLES = "bundles";
    private static final String PROPERTY_DESC = "description";

    private String name;
    private String description;
    private List<String> dependencies;

    CommandInfoImpl(String name, Properties properties, String thermostatHome) {
        this.name = name;
        for (Entry<Object,Object> entry: properties.entrySet()) {
            String key = (String) entry.getKey();
            if (key.equals(PROPERTY_BUNDLES)) {
                learnDependencies(entry, thermostatHome);
            } else if (key.equals(PROPERTY_DESC)) {
                description = properties.getProperty(key);
            }
            
        }
    }

    private void learnDependencies(Entry<Object, Object> bundlesEntry, String thermostatHome) {
        String libRoot = thermostatHome + File.separator + "libs";
        List<String> resourceNames = Arrays.asList(((String)bundlesEntry.getValue()).split(","));
        dependencies = new ArrayList<>(resourceNames.size());
        for (String value: resourceNames) {
            File file = new File(libRoot, value.trim());
            String path = file.toURI().toString();
            if (!file.exists()) {
                logger.severe("Bundle " + path + " required by " + getName() +
                        " command does not exist in the filesystem.  This will cause" +
                        " osgi wiring issue when attempting to run this command.");
                // Allow to proceed because this command may never be called.
            } else {
                dependencies.add(path);
            }
        }
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public List<String> getDependencyResourceNames() {
        return dependencies;
    }
}
