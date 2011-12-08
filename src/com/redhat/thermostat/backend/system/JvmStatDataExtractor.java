package com.redhat.thermostat.backend.system;

import sun.jvmstat.monitor.Monitor;
import sun.jvmstat.monitor.MonitorException;
import sun.jvmstat.monitor.MonitoredVm;
import sun.jvmstat.monitor.MonitoredVmUtil;

/**
 * A helper class to provide type-safe access to commonly used jvmstat monitors
 */
public class JvmStatDataExtractor {

    /*
     * Note, there may be a performance issue to consider here. We have a lot of
     * string constants. When we start adding some of the more heavyweight
     * features, and running into CPU issues this may need to be reconsidered in
     * order to avoid the String pool overhead. See also:
     * http://docs.oracle.com/javase/6/docs/api/java/lang/String.html#intern()
     */

    private final MonitoredVm vm;

    public JvmStatDataExtractor(MonitoredVm vm) {
        this.vm = vm;
    }

    public String getCommandLine() throws MonitorException {
        return MonitoredVmUtil.commandLine(vm);
    }

    public String getJavaVersion() throws MonitorException {
        return (String) vm.findByName("java.property.java.version").getValue();
    }

    public String getJavaHome() throws MonitorException {
        return (String) vm.findByName("java.property.java.home").getValue();
    }

    public String getVmName() throws MonitorException {
        return (String) vm.findByName("java.property.java.vm.name").getValue();
    }

    public String getVmInfo() throws MonitorException {
        return (String) vm.findByName("java.property.java.vm.info").getValue();
    }

    public String getVmVersion() throws MonitorException {
        return (String) vm.findByName("java.property.java.vm.version").getValue();
    }

    public String getVmArguments() throws MonitorException {
        return MonitoredVmUtil.jvmArgs(vm);
    }

    public long getTotalCollectors() throws MonitorException {
        return (Long) vm.findByName("sun.gc.policy.collectors").getValue();
    }

    public String getCollectorName(long collector) throws MonitorException {
        return (String) vm.findByName("sun.gc.collector." + collector + ".name").getValue();
    }

    public long getCollectorTime(long collector) throws MonitorException {
        return (Long) vm.findByName("sun.gc.collector." + collector + ".time").getValue();
    }

    public long getCollectorInvocations(long collector) throws MonitorException {
        return (Long) vm.findByName("sun.gc.collector." + collector + ".invocations").getValue();
    }

    public long getTotalGcGenerations() throws MonitorException {
        return (Long) vm.findByName("sun.gc.policy.generations").getValue();
    }

    public String getGenerationName(long generation) throws MonitorException {
        return (String) vm.findByName("sun.gc.generation." + generation + ".name").getValue();
    }

    public long getGenerationCapacity(long generation) throws MonitorException {
        return (Long) vm.findByName("sun.gc.generation." + generation + ".capacity").getValue();
    }

    public long getGenerationMaxCapacity(long generation) throws MonitorException {
        return (Long) vm.findByName("sun.gc.generation." + generation + ".maxCapacity").getValue();
    }

    public String getGenerationCollector(long generation) throws MonitorException {
        Monitor m = vm.findByName("sun.gc.collector." + generation + ".name");
        if (m == null) {
            throw new IllegalArgumentException("not found");
        }
        return (String) m.getValue();
    }

    public long getTotalSpaces(long generation) throws MonitorException {
        return (Long) vm.findByName("sun.gc.generation." + generation + ".spaces").getValue();
    }

    public String getSpaceName(long generation, long space) throws MonitorException {
        return (String) vm.findByName("sun.gc.generation." + generation + ".space." + space + ".name").getValue();
    }

    public long getSpaceCapacity(long generation, long space) throws MonitorException {
        return (Long) vm.findByName("sun.gc.generation." + generation + ".space." + space + ".capacity").getValue();
    }

    public long getSpaceMaxCapacity(long generation, long space) throws MonitorException {
        return (Long) vm.findByName("sun.gc.generation." + generation + ".space." + space + ".maxCapacity").getValue();
    }

    public long getSpaceUsed(long generation, long space) throws MonitorException {
        return (Long) vm.findByName("sun.gc.generation." + generation + ".space." + space + ".used").getValue();
    }

}
