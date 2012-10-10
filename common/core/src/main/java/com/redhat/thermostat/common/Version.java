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
package com.redhat.thermostat.common;

import java.text.MessageFormat;

import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;

import com.redhat.thermostat.common.locale.LocaleResources;
import com.redhat.thermostat.common.locale.Translate;

/**
 * Utility class for Thermostat version info.
 *
 */
public class Version {
    public static final String VERSION_OPTION = "--version";
    public static final String VERSION_NUMBER_FORMAT = "%d.%d.%d";
    
    private Bundle bundle;
    
    /**
     * Use this only if you want the version info of core thermostat.
     */
    public Version() {
        this(FrameworkUtil.getBundle(Version.class));
    }
    
    /**
     * 
     * @param bundle The bundle to determine the version for.
     */
    public Version(Bundle bundle) {
        this.bundle = bundle;
    }
    
    /**
     * 
     * @return A human readable form of the version in use
     */
    public String getVersionInfo() {
        ApplicationInfo appInfo = new ApplicationInfo();
        Translate<LocaleResources> t = LocaleResources.createLocalizer();
        String format = MessageFormat.format(
                t.localize(LocaleResources.APPLICATION_VERSION_INFO),
                appInfo.getName())
                + " " + VERSION_NUMBER_FORMAT;
        return String.format(format, getMajor(), getMinor(), getMicro());
    }
    
    /**
     * 
     * @return A machine parseable version format: "#major.#minor.#micro"
     */
    public String getVersionNumber() {
        return String.format(VERSION_NUMBER_FORMAT, getMajor(), getMinor(), getMicro());
    }
    
    /**
     * 
     * @return The major version
     */
    public int getMajor() {
        return this.bundle.getVersion().getMajor();
    }
    
    /**
     * 
     * @return The minor version
     */
    public int getMinor() {
        return this.bundle.getVersion().getMinor();
    }
    
    /**
     * 
     * @return The micro version
     */
    public int getMicro() {
        return this.bundle.getVersion().getMicro();
    }
}
