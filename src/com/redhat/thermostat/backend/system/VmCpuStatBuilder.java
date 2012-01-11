package com.redhat.thermostat.backend.system;

import java.util.HashMap;
import java.util.Map;

import com.redhat.thermostat.common.VmCpuStat;

public class VmCpuStatBuilder {

    // pid -> ticks
    private static Map<Integer, Long> lastProcessTicks = new HashMap<Integer, Long>();
    // pid -> last time the ticks were updated
    private static Map<Integer, Long> lastProcessTickTime = new HashMap<Integer, Long>();

    private static long clockTicksPerSecond = SysConf.getClockTicksPerSecond();

    /**
     * To build the stat, this method needs to be called repeatedly. The first
     * time (in the entire run time) it is called, the result will most be
     * useless. The second (and later calls) should produce usable results.
     *
     * @param pid
     * @return
     */
    public static synchronized VmCpuStat build(Integer pid) {

        ProcessStatusInfo info = ProcessStatusInfo.getFor(pid);
        long miliTime = System.currentTimeMillis();
        long time = System.nanoTime();
        long programTicks = (info.getKernelTime() + info.getUserTime());
        double cpuLoad = 0.0;

        if (lastProcessTicks.containsKey(pid)) {
            double timeDelta = (time - lastProcessTickTime.get(pid)) * 1E-9;
            long programTicksDelta = programTicks - lastProcessTicks.get(pid);
            cpuLoad = programTicksDelta * (100.0 / timeDelta / clockTicksPerSecond);
        }

        lastProcessTicks.put(pid, programTicks);
        lastProcessTickTime.put(pid, time);


        return new VmCpuStat(miliTime, pid, cpuLoad);
    }

}
