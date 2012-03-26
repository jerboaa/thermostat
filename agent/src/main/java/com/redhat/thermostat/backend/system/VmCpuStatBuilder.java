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

package com.redhat.thermostat.backend.system;

import java.util.HashMap;
import java.util.Map;

import com.redhat.thermostat.common.model.VmCpuStat;

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
