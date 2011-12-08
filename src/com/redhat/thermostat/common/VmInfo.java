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
            String javaVersion, String javaHome, String commandLine,
            String vmName, String vmInfo, String vmVersion, String vmArguments,
            Map<String, String> properties, Map<String, String> environment, List<String> loadedNativeLibraries) {
        this.vmPid = vmPid;
        this.startTime = startTime;
        this.stopTime = stopTime;
        this.javaVersion = javaVersion;
        this.javaHome = javaHome;
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
