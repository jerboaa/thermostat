/*
 * Copyright 2012-2015 Red Hat, Inc.
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

package com.redhat.thermostat.vm.numa.agent.internal;

import java.text.ParseException;

import com.redhat.thermostat.vm.numa.common.VmNumaNodeStat;
import com.redhat.thermostat.vm.numa.common.VmNumaStat;

public class VmNumaStatParser {
    public VmNumaStat parse(String input) throws ParseException {
        VmNumaStat stat = new VmNumaStat();
        stat.setTimeStamp(System.currentTimeMillis());
        String[] lines = input.split(System.lineSeparator());
        try {
            VmNumaNodeStat[] stats = parseStats(lines[4], lines[5], lines[6], lines[7]);
            stat.setVmNodeStats(stats);
            return stat;
        } catch (NumberFormatException | ArrayIndexOutOfBoundsException | NegativeArraySizeException e) {
            throw new ParseException("Unexpected input to VmNumaStatParser", 0);
        }
    }

    private VmNumaNodeStat[] parseStats(String HUGE, String HEAP, String STACK, String PRIVATE) {
        VmNumaNodeStat[] stats;

        String[] hugeStats = splitWhitespaces(HUGE);
        String[] heapStats = splitWhitespaces(HEAP);
        String[] stackStats = splitWhitespaces(STACK);
        String[] privateStats = splitWhitespaces(PRIVATE);

        //Take the maximum in-case of erroneous input
        int numberOfNodes = -2 + Math.max(hugeStats.length,
                                    Math.max(heapStats.length,
                                    Math.max(stackStats.length, privateStats.length)));

        stats = new VmNumaNodeStat[numberOfNodes];
        for (int i = 0; i < numberOfNodes; i++) {
            stats[i] = new VmNumaNodeStat();
            stats[i].setNode(i);
            stats[i].setHugeMemory(Double.parseDouble(hugeStats[i + 1]));
            stats[i].setHeapMemory(Double.parseDouble(heapStats[i + 1]));
            stats[i].setStackMemory(Double.parseDouble(stackStats[i + 1]));
            stats[i].setPrivateMemory(Double.parseDouble(privateStats[i + 1]));
        }
        return stats;
    }

    private String[] splitWhitespaces(String s) {
        return s.split("\\s+");
    }
}
