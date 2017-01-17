/*
 * Copyright 2012-2017 Red Hat, Inc.
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

package com.redhat.thermostat.shared.config;

import java.io.File;

import com.redhat.thermostat.shared.locale.Translate;
import com.redhat.thermostat.shared.locale.internal.LocaleResources;

public class DirectoryStructureCreator {

    private static final Translate<LocaleResources> t = LocaleResources.createLocalizer();
    private final CommonPaths paths;

    public DirectoryStructureCreator(CommonPaths paths) {
        this.paths = paths;
    }

    public void createPaths() {
        makeDir(paths.getSystemThermostatHome());
        makeDir(paths.getSystemPluginRoot());
        makeDir(paths.getSystemLibRoot());
        makeDir(paths.getSystemNativeLibsRoot());
        makeDir(paths.getSystemBinRoot());
        makeDir(paths.getSystemConfigurationDirectory());
        makeDir(paths.getSystemPluginConfigurationDirectory());
        makeDir(paths.getUserPluginConfigurationDirectory());
        makeDir(paths.getUserThermostatHome());
        makeDir(paths.getUserConfigurationDirectory());
        makeDir(paths.getUserRuntimeDataDirectory());
        makeDir(paths.getUserLogDirectory());
        makeDir(paths.getUserCacheDirectory());
        makeDir(paths.getUserPersistentDataDirectory());
        makeDir(paths.getUserPluginRoot());
        makeDir(paths.getUserStorageDirectory());

    }

    private static void makeDir(File dir) {
        boolean exists = dir.exists();
        if (!exists) {
            exists = dir.mkdirs();
        }
        if (!exists) {
            throw new InvalidConfigurationException("Directory could not be created: " + dir.getAbsolutePath());
        }
        if (!dir.isDirectory()) {
            throw new InvalidConfigurationException(t.localize(LocaleResources.GENERAL_NOT_A_DIR, dir.getAbsolutePath()));
        }
    }
}

