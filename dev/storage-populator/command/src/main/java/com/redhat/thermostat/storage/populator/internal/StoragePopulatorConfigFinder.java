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

package com.redhat.thermostat.storage.populator.internal;

import com.redhat.thermostat.common.cli.AbstractCompletionFinder;
import com.redhat.thermostat.common.cli.CompletionInfo;
import com.redhat.thermostat.common.cli.DependencyServices;
import com.redhat.thermostat.common.cli.DirectoryContentsCompletionFinder;
import com.redhat.thermostat.shared.config.CommonPaths;
import com.redhat.thermostat.storage.populator.StoragePopulatorCommand;

import java.io.File;
import java.io.FileFilter;
import java.util.Collections;
import java.util.List;

public class StoragePopulatorConfigFinder extends AbstractCompletionFinder {

    private DirectoryContentsCompletionFinder directoryFinder;

    public StoragePopulatorConfigFinder(DependencyServices dependencyServices) {
        super(dependencyServices);
    }

    /* Testing hook only */
    void setDirectoryFinder(DirectoryContentsCompletionFinder directoryFinder) {
        this.directoryFinder = directoryFinder;
    }

    @Override
    protected Class<?>[] getRequiredDependencies() {
        return new Class<?>[]{ CommonPaths.class };
    }

    @Override
    public List<CompletionInfo> findCompletions() {
        if (!allDependenciesAvailable()) {
            return Collections.emptyList();
        }
        if (directoryFinder == null) {
            CommonPaths paths = getService(CommonPaths.class);
            String configDirectory = StoragePopulatorCommand.getConfigFileDirectoryPath(paths);
            directoryFinder = new DirectoryContentsCompletionFinder(new File(configDirectory));
            directoryFinder.setFileFilter(new StoragePopulatorConfigFilter());
        }
        return directoryFinder.findCompletions();
    }

    static class StoragePopulatorConfigFilter implements FileFilter {
        @Override
        public boolean accept(File file) {
            return file.isFile() && file.getName().toLowerCase().endsWith(".json");
        }
    }

}
