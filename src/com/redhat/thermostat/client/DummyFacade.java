package com.redhat.thermostat.client;

import static com.redhat.thermostat.client.Translate._;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import com.redhat.thermostat.common.HostInfo;
import com.redhat.thermostat.common.NetworkInfo;
import com.redhat.thermostat.common.NetworkInterfaceInfo;
import com.redhat.thermostat.common.VmInfo;
import com.redhat.thermostat.common.VmMemoryStat;
import com.redhat.thermostat.common.VmMemoryStat.Generation;
import com.redhat.thermostat.common.VmMemoryStat.Space;

public class DummyFacade implements SummaryPanelFacade, HostPanelFacade, VmPanelFacade, MainWindowFacade {

    private final Random r = new Random();
    private List<MemoryType> toDisplay = new ArrayList<MemoryType>();

    private HostRef onlyAgent = new HostRef("a-random-string-of-letters-and-numbers", "agent on localhost");
    private VmRef onlyVm = new VmRef(onlyAgent, "a-random-string-of-letters-and-numbers-or-perhaps-a-process-id", "super crazy awesome java app");

    public DummyFacade() {
        toDisplay.addAll(Arrays.asList(MemoryType.values()));
    }

    @Override
    public long getTotalConnectedVms() {
        return new VmRef[] { onlyVm }.length;
    }

    @Override
    public long getTotalConnectedAgents() {
        return 1;
    }

    @Override
    public HostRef[] getHosts() {
        return new HostRef[] { onlyAgent };
    }

    @Override
    public VmRef[] getVms(HostRef ref) {
        return new VmRef[] { onlyVm };
    }

    @Override
    public List<String> getIssues() {
        return new ArrayList<String>();
    }

    @Override
    public HostInfo getHostInfo() {
        String hostname = "host.example.com";
        String osName = "Fedora 99";
        String osKernel = "Linux 9.9.9.9";
        String cpuModel = "Some CPU @ some speed GHz";
        int cpuCount = 99;
        long totalMemory = 1;
        return new HostInfo(hostname, osName, osKernel, cpuModel, cpuCount, totalMemory);
    }

    @Override
    public NetworkInfo getNetworkInfo() {
        NetworkInfo info = new NetworkInfo();

        NetworkInterfaceInfo eth0 = new NetworkInterfaceInfo("eth0");
        eth0.setIp4Addr("1.1.1.1");
        eth0.setIp6Addr("1:::::::::1");
        info.addNetworkInterfaceInfo(eth0);

        NetworkInterfaceInfo em0 = new NetworkInterfaceInfo("em0");
        em0.setIp4Addr("256.256.256.256");
        info.addNetworkInterfaceInfo(em0);

        return info;
    }

    @Override
    public VmInfo getVmInfo() {

        // TODO hook into storage and return the actual VmInfo object
        int vmPid = 0;
        long startTime = System.currentTimeMillis() - 10000;
        long stopTime = Integer.MIN_VALUE;
        String javaVersion = "2.9.9";
        String javaHome = "/usr/lib/jvm/java/jre/";
        String mainClass = "com.foo.bar";
        String commandLine = "some java program";
        String vmName = "hotspot usb compiler";
        String vmInfo = "future predictive mode";
        String vmVersion = "99b99";
        String vmArguments = "-XX:+EvenFasterPlease --X:+UNROLL_ALL_THE_LOOPS!";
        Map<String, String> properties = new HashMap<String, String>();
        Map<String, String> environment = new HashMap<String, String>();
        List<String> loadedNativeLibraries = new ArrayList<String>();
        return new VmInfo(vmPid, startTime, stopTime, javaVersion, javaHome, mainClass, commandLine, vmName, vmInfo, vmVersion, vmArguments, properties, environment, loadedNativeLibraries);
    }

    @Override
    public DiscreteTimeData<Double>[] getCpuLoad() {
        List<DiscreteTimeData<Double>> cpuData = new ArrayList<DiscreteTimeData<Double>>();

        long currentTime = System.currentTimeMillis();
        long oneMinute = 1000 * 60;

        cpuData.add(new DiscreteTimeData<Double>(currentTime, r.nextDouble()));
        cpuData.add(new DiscreteTimeData<Double>(currentTime - 1 * oneMinute, r.nextDouble()));
        cpuData.add(new DiscreteTimeData<Double>(currentTime - 2 * oneMinute, r.nextDouble()));
        cpuData.add(new DiscreteTimeData<Double>(currentTime - 3 * oneMinute, r.nextDouble()));
        cpuData.add(new DiscreteTimeData<Double>(currentTime - 4 * oneMinute, r.nextDouble()));
        cpuData.add(new DiscreteTimeData<Double>(currentTime - 5 * oneMinute, r.nextDouble()));
        cpuData.add(new DiscreteTimeData<Double>(currentTime - 6 * oneMinute, r.nextDouble()));

        return (DiscreteTimeData<Double>[]) cpuData.toArray(new DiscreteTimeData<?>[0]);
    }

    @Override
    public DiscreteTimeData<Long>[] getMemoryUsage(MemoryType type) {
        List<DiscreteTimeData<Long>> data = new ArrayList<DiscreteTimeData<Long>>();

        long currentTime = System.currentTimeMillis();
        long oneMinute = 1000 * 60;

        data.add(new DiscreteTimeData<Long>(currentTime, r.nextLong()));
        data.add(new DiscreteTimeData<Long>(currentTime - 1 * oneMinute, r.nextLong()));
        data.add(new DiscreteTimeData<Long>(currentTime - 2 * oneMinute, r.nextLong()));
        data.add(new DiscreteTimeData<Long>(currentTime - 3 * oneMinute, r.nextLong()));
        data.add(new DiscreteTimeData<Long>(currentTime - 4 * oneMinute, r.nextLong()));
        data.add(new DiscreteTimeData<Long>(currentTime - 5 * oneMinute, r.nextLong()));
        data.add(new DiscreteTimeData<Long>(currentTime - 6 * oneMinute, r.nextLong()));
        data.add(new DiscreteTimeData<Long>(currentTime - 7 * oneMinute, r.nextLong()));
        data.add(new DiscreteTimeData<Long>(currentTime - 8 * oneMinute, r.nextLong()));

        return (DiscreteTimeData<Long>[]) data.toArray(new DiscreteTimeData<?>[0]);
    }

    @Override
    public MemoryType[] getMemoryTypesToDisplay() {
        return toDisplay.toArray(new MemoryType[0]);
    }

    @Override
    public boolean isMemoryTypeDisplayed(MemoryType type) {
        return toDisplay.contains(type);
    }

    @Override
    public void setDisplayMemoryType(MemoryType type, boolean selected) {
        if (selected) {
            if (!toDisplay.contains(type)) {
                toDisplay.add(type);
            }
        } else {
            toDisplay.remove(type);
        }

    }

    @Override
    public String[] getCollectorNames() {
        return new String[] { "PSScavenge", "PSMarkSweep" };
    }

    @Override
    public DiscreteTimeData<Long>[] getCollectorRunTime(String collectorName) {
        List<DiscreteTimeData<Long>> data = new ArrayList<DiscreteTimeData<Long>>();

        long last = 2;
        data.add(new DiscreteTimeData<Long>(100000, (last = last + r.nextInt(10))));
        data.add(new DiscreteTimeData<Long>(100010, (last = last + r.nextInt(10))));
        data.add(new DiscreteTimeData<Long>(100020, (last = last + r.nextInt(10))));
        data.add(new DiscreteTimeData<Long>(100030, (last = last + r.nextInt(10))));
        data.add(new DiscreteTimeData<Long>(100040, (last = last + r.nextInt(10))));
        data.add(new DiscreteTimeData<Long>(100050, (last = last + r.nextInt(10))));
        data.add(new DiscreteTimeData<Long>(100060, (last = last + r.nextInt(10))));
        data.add(new DiscreteTimeData<Long>(100070, (last = last + r.nextInt(10))));
        data.add(new DiscreteTimeData<Long>(100080, (last = last + r.nextInt(10))));
        data.add(new DiscreteTimeData<Long>(100090, (last = last + r.nextInt(10))));
        data.add(new DiscreteTimeData<Long>(100110, (last = last + r.nextInt(10))));

        return (DiscreteTimeData<Long>[]) data.toArray(new DiscreteTimeData<?>[0]);
    }

    @Override
    public String getCollectorGeneration(String collectorName) {
        if (collectorName.equals("PSScavenge")) {
            return _("YOUNG_GEN");
        } else if (collectorName.equals("PSMarkSweep")) {
            return _("OLD_GEN");
        }
        return ("UNKNOWN_GEN");
    }

    @Override
    public VmMemoryStat getLatestMemoryInfo() {
        long timestamp = -1;
        List<Generation> generations = new ArrayList<Generation>();

        Generation youngGen = new Generation();
        youngGen.name = "eden";
        generations.add(youngGen);

        List<Space> youngSpaces = new ArrayList<Space>();

        Space eden = new Space();
        eden.name = _("EDEN_GEN");
        eden.used = 100;
        eden.capacity = 200;
        eden.maxCapacity = 300;
        youngSpaces.add(eden);

        Space s0 = new Space();
        s0.name = _("S0_GEN");
        s0.used = 100;
        s0.capacity = 200;
        s0.maxCapacity = 400;
        youngSpaces.add(s0);

        Space s1 = new Space();
        s1.name = _("S1_GEN");
        s1.used = 150;
        s1.capacity = 200;
        s1.maxCapacity = 400;
        youngSpaces.add(s1);

        youngGen.spaces = youngSpaces;

        Generation oldGen = new Generation();
        generations.add(oldGen);

        Space oldSpace = new Space();
        oldSpace.name = _("OLD_GEN");
        oldSpace.used = 400;
        oldSpace.capacity = 500;
        oldSpace.maxCapacity = 600;

        oldGen.spaces = Arrays.asList(new Space[] { oldSpace });

        Generation permGen = new Generation();
        generations.add(permGen);

        Space permSpace = new Space();
        permSpace.name = _("PERM_GEN");
        permSpace.used = 50;
        permSpace.capacity = 200;
        permSpace.maxCapacity = 200;

        permGen.spaces = Arrays.asList(new Space[] { permSpace });

        VmMemoryStat stat = new VmMemoryStat(timestamp, 0, generations);
        return stat;
    }

}
