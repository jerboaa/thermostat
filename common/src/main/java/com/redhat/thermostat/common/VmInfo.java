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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class VmInfo {

    private int vmPid = 0;
    private long startTime = System.currentTimeMillis();
    private long stopTime = Long.MIN_VALUE;
    private String javaVersion = "unknown";
    private String javaHome = "unknown";
    private String javaCommandLine = "unknown";
    private String mainClass = "unknown";
    private String vmName = "unknown";
    private String vmInfo = "unknown";
    private String vmVersion = "unknown";
    private String vmArguments = "unknown";
    private Map<String, String> properties = new HashMap<String, String>();
    private Map<String, String> environment = new HashMap<String, String>();
    private List<String> loadedNativeLibraries;

    public VmInfo() {
        /* use defaults */
    }

    public VmInfo(int vmPid, long startTime, long stopTime,
            String javaVersion, String javaHome,
            String mainClass, String commandLine,
            String vmName, String vmInfo, String vmVersion, String vmArguments,
            Map<String, String> properties, Map<String, String> environment, List<String> loadedNativeLibraries) {
        this.vmPid = vmPid;
        this.startTime = startTime;
        this.stopTime = stopTime;
        this.javaVersion = javaVersion;
        this.javaHome = javaHome;
        this.mainClass = mainClass;
        this.javaCommandLine = commandLine;
        this.vmName = vmName;
        this.vmInfo = vmInfo;
        this.vmVersion = vmVersion;
        this.vmArguments = vmArguments;
        this.properties = properties;
        this.environment = environment;
        this.loadedNativeLibraries = loadedNativeLibraries;
    }

    public int getVmId() {
        return vmPid;
    }

    public int getVmPid() {
        return vmPid;
    }

    public long getStartTimeStamp() {
        return startTime;
    }

    public long getStopTimeStamp() {
        return stopTime;
    }

    public String getJavaVersion() {
        return javaVersion;
    }

    public String getJavaHome() {
        return javaHome;
    }

    public String getMainClass() {
        return mainClass;
    }

    public String getJavaCommandLine() {
        return javaCommandLine;
    }

    public String getVmName() {
        return vmName;
    }

    public String getVmArguments() {
        return vmArguments;
    }

    public String getVmInfo() {
        return vmInfo;
    }

    public String getVmVersion() {
        return vmVersion;
    }

    public Map<String, String> getProperties() {
        return properties;
    }

    public Map<String, String> getEnvironment() {
        return environment;
    }

    public List<String> getLoadedNativeLibraries() {
        return loadedNativeLibraries;
    }

}
