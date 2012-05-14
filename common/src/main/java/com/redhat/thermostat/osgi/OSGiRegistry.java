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

package com.redhat.thermostat.osgi;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import com.redhat.thermostat.common.config.ConfigUtils;
import com.redhat.thermostat.common.config.InvalidConfigurationException;

public class OSGiRegistry {

    public static String getOSGiPublicPackages() throws InvalidConfigurationException, FileNotFoundException, IOException {
        String home = ConfigUtils.getThermostatHome();
        File thermostatEtc = new File(home, "etc");
        File osgiBundleDefinitions = new File(thermostatEtc, "osgi-export.properties");
        
        Properties bundles = new Properties();
        bundles.load(new FileInputStream(osgiBundleDefinitions));
        
        StringBuilder publicPackages = new StringBuilder();
        boolean firstPackage = true;
        for (Object bundle : bundles.keySet()) {
            if (!firstPackage) {
                publicPackages.append(",\n");
            }
            firstPackage = false;
            publicPackages.append(bundle);
            String bundleVersion = (String) bundles.get(bundle);
            if (!bundleVersion.isEmpty()) {
                publicPackages.append("; version=").append(bundleVersion);
            }
        }
                
        return publicPackages.toString();
    }

    public static List<String> getSystemBundles() throws InvalidConfigurationException, IOException {
        
        String home = ConfigUtils.getThermostatHome();
        Path thermostatHome = new File(home, "libs").toPath();
        OSGiBundlesVisitor visitor = new OSGiBundlesVisitor();
        Files.walkFileTree(thermostatHome, visitor);
        return visitor.jars;
    }
    
    private static class OSGiBundlesVisitor extends SimpleFileVisitor<Path> {
        
        private List<String> jars = new ArrayList<>(); 
        private PathMatcher matcher =
                FileSystems.getDefault().getPathMatcher("glob:{thermostat-osgi,thermostat-common,thermostat-client}*.jar");
        
        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
            if (file.getFileName() != null && matcher.matches(file.getFileName())) {
                jars.add("file:" + file.toAbsolutePath().toString());
            }
            return FileVisitResult.CONTINUE;
        }
    }
}
