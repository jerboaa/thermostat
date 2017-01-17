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

package com.redhat.thermostat.tools.dependency.internal;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

/**
 */
public class OSGiSearchProcessor extends PathProcessor {

    public static class BundleInfo {
        public Path library;
        public String symbolicName;
        public String version;
    }

    private String target;
    private BundleProperties what;
    private BundleInfo info;

    public OSGiSearchProcessor(String target) {
        this(target, BundleProperties.EXPORT);
    }

    public OSGiSearchProcessor(String target, BundleProperties what) {
        this.target = target;
        this.what = what;
    }

    @Override
    protected void process(Path jar) {

        if (info != null) {
            return;
        }

        try {
            Manifest manifest = new JarFile(jar.toFile()).getManifest();
            Attributes attributes = manifest.getMainAttributes();
            String bundleAttribute = attributes.getValue(what.id());
            if (bundleAttribute != null) {
                List<String> dependencies = OSGIManifestScanner.parseHeader(bundleAttribute);
                if (dependencies.contains(target)) {
                    info = new BundleInfo();
                    info.library = jar;
                    info.symbolicName = attributes.getValue(BundleProperties.SYMBOLIC_NAME.id());
                    info.version = attributes.getValue(BundleProperties.VERSION.id());
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public BundleInfo getBundleInfo() {
        return info;
    }
}
