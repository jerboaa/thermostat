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

package com.redhat.thermostat.backend.system.internal.windows;

import com.redhat.thermostat.common.utils.LoggingUtils;
import com.redhat.thermostat.shared.config.NativeLibraryResolver;
import com.redhat.thermostat.shared.config.OS;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Utility class to access Windows native code
 */
class WindowsHelperImpl {

    private static final Logger logger = LoggingUtils.getLogger(WindowsHelperImpl.class);

    static {
        if (OS.IS_WINDOWS) {
            try {
                String lib = NativeLibraryResolver.getAbsoluteLibraryPath("WindowsHelperImpl");
                System.load(lib);
                INSTANCE = new WindowsHelperImpl();
            } catch (UnsatisfiedLinkError e) {
                logger.severe("Could not load WindowsHelperImpl DLL");
                INSTANCE = null;
                // do not throw here, because you'll get a NoClassDefFound thrown when running other tests that Mock this class
            }
        } else {
            INSTANCE = null;
        }
    }

    public static WindowsHelperImpl INSTANCE;

    private WindowsHelperImpl() {
    }
    // local host-wide information

    String getHostName() {
        return getHostName0();
    }

    String getOSName() {
        return System.getProperty("os.name");
    }

    String getOSVersion() {
        return "(stub OS version)";
    }

    String getCPUModel() {
        return "(stub CPU model)";
    }

    int getCPUCount() {
        return 88;
    }

    long getTotalMemory() {
        return 999;
    }

    // local process-specific information
    String getUserName(int pid) {
        return getUserName0(pid);
    }

    int getUid(int pid) {
        return getUid0(pid);
    }

    Map<String, String> getEnvironment(int pid) {
        // the environment is returned as a 1D array of alternating env names and values
        final String[] envArray = getEnvironment0(pid);
        if (envArray == null) {
            return Collections.emptyMap();
        }

        if (envArray.length % 2 != 0) {
            throw new AssertionError("environment array length not even");
        }

        Map<String, String> env = new HashMap<>(envArray.length/2);
        for (int i = 0; i < envArray.length / 2; i++) {
            env.put(envArray[i * 2], envArray[i * 2 + 1]);
        }
        return env;
    }

    private static native String getHostName0();
    private static native int getUid0(int pid);
    private static native String getUserName0(int pid);
    private static native String[] getEnvironment0(int pid);
}
