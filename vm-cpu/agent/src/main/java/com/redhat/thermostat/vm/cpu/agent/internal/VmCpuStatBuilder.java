/*
 * Copyright 2012, 2013 Red Hat, Inc.
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

package com.redhat.thermostat.vm.cpu.agent.internal;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.redhat.thermostat.common.Clock;
import com.redhat.thermostat.common.utils.LoggingUtils;
import com.redhat.thermostat.storage.model.VmCpuStat;

public class VmCpuStatBuilder {

    private static final Logger logger = LoggingUtils.getLogger(VmCpuStatBuilder.class);

    // pid -> ticks
    private final Map<Integer, Long> lastProcessTicks = new HashMap<Integer, Long>();
    // pid -> last time the ticks were updated
    private final Map<Integer, Long> lastProcessTickTime = new HashMap<Integer, Long>();

    private final Clock clock;
    private final int cpuCount;
    private final long ticksPerSecond;
    private final ProcessStatusInfoBuilder statusBuilder;

    public VmCpuStatBuilder(Clock clock, int cpuCount, long ticksPerSecond, ProcessStatusInfoBuilder statusBuilder) {
        this.clock = clock;
        this.cpuCount = cpuCount;
        this.ticksPerSecond = ticksPerSecond;
        this.statusBuilder = statusBuilder;
    }

    /**
     * @param pid the process id
     * @return an object representing the cpu usage of the process, or null if
     * the information can not be found.
     */
    public synchronized VmCpuStat build(Integer pid) {
        if (!lastProcessTicks.containsKey(pid) || !lastProcessTickTime.containsKey(pid)) {
            throw new IllegalArgumentException("unknown pid");
        }

        ProcessStatusInfo info = statusBuilder.build(pid);
        if (info == null) {
            return null;
        }
        long miliTime = clock.getRealTimeMillis();
        long time = clock.getMonotonicTimeNanos();
        long programTicks = (info.getKernelTime() + info.getUserTime());
        double cpuLoad = 0.0;

        double timeDelta = (time - lastProcessTickTime.get(pid)) * 1E-9;
        long programTicksDelta = programTicks - lastProcessTicks.get(pid);
        // 100 as in 100 percent.
        cpuLoad = programTicksDelta * (100.0 / timeDelta / ticksPerSecond / cpuCount);

        if (cpuLoad < 0.0 || cpuLoad > 100.0) {
            logger.log(Level.WARNING, "cpu load for " + pid + " is outside [0,100]: " + cpuLoad);
            logger.log(Level.WARNING, "  (" + pid + ") programTicks: " + programTicks);
            logger.log(Level.WARNING, "  (" + pid + ") programTicksDelta: " + programTicksDelta);
            logger.log(Level.WARNING, "  (" + pid + ") time: " + time);
            logger.log(Level.WARNING, "  (" + pid + ") timeDelta: " + timeDelta);
            logger.log(Level.WARNING, "  (" + pid + ") ticksPerSecond: " + ticksPerSecond);
            logger.log(Level.WARNING, "  (" + pid + ") cpuCount: " + cpuCount);
        }

        lastProcessTicks.put(pid, programTicks);
        lastProcessTickTime.put(pid, time);

        return new VmCpuStat(miliTime, pid, cpuLoad);
    }

    public synchronized boolean knowsAbout(int pid) {
        return (lastProcessTickTime.containsKey(pid) && lastProcessTicks.containsKey(pid));
    }

    public synchronized void learnAbout(int pid) {
        long time = clock.getMonotonicTimeNanos();
        ProcessStatusInfo info = statusBuilder.build(pid);
        if (info == null) {
            logger.log(Level.WARNING, "can not learn about pid " + pid + " : statusBuilder returned null");
            return;
        }

        lastProcessTickTime.put(pid, time);
        lastProcessTicks.put(pid, info.getUserTime()+ info.getKernelTime());
    }

    public synchronized void forgetAbout(int pid) {
        lastProcessTicks.remove(pid);
        lastProcessTickTime.remove(pid);
    }

}

