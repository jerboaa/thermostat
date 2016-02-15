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

package com.redhat.thermostat.platform.internal.application;

import com.google.gson.Gson;
import com.redhat.thermostat.shared.config.CommonPaths;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;

/**
 */
public class ConfigurationManager {

    private class ApplicationConfiguration extends SimpleFileVisitor<Path> {

        private ApplicationInfo infos;
        private PathMatcher matcher;

        public ApplicationConfiguration() {
            infos = new ApplicationInfo();
            infos.applications = new ArrayList<>();
            matcher = FileSystems.getDefault().getPathMatcher("glob:*.json");
        }

        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
                throws IOException
        {
            Gson gson = new Gson();
            Path name = file.getFileName();
            if (name != null && matcher.matches(name)) {
                BufferedReader reader = new BufferedReader(new FileReader(file.toFile()));
                ApplicationInfo info = gson.fromJson(reader, ApplicationInfo.class);
                if (info != null) {
                    infos.applications.addAll(info.applications);
                }
            }

            return FileVisitResult.CONTINUE;
        }

        public ApplicationInfo getInfos() {
            return infos;
        }
    }

    private CommonPaths paths;
    public ConfigurationManager(CommonPaths paths) {
        this.paths = paths;
    }

    public ApplicationInfo getApplicationConfigs() {

        String sysConfig = paths.getSystemPluginConfigurationDirectory().toString();
        String userConfig = paths.getUserPluginConfigurationDirectory().toString();

        String sysPath = sysConfig + "/platform/";
        String userPath = userConfig + "/platform/";

        Path[] paths = {
                Paths.get(sysPath),
                Paths.get(userPath)
        };

        ApplicationConfiguration visitor = new ApplicationConfiguration();
        for (Path path : paths) {
            try {
                if (Files.isDirectory(path) && Files.isReadable(path)) {
                    Files.walkFileTree(path, visitor);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return visitor.getInfos();
    }

    public ApplicationInfo.Application getApplicationConfig(String applicationId) {
        ApplicationInfo infos = getApplicationConfigs();
        for (ApplicationInfo.Application info : infos.applications) {
            if (info.name.equalsIgnoreCase(applicationId)) {
                return info;
            }
        }

        return null;
    }
}
