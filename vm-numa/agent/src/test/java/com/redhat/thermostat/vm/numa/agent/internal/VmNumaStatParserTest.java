/*
 * Copyright 2012-2017 Red Hat, Inc.
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

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.text.ParseException;

import org.junit.Test;

import com.redhat.thermostat.vm.numa.common.VmNumaNodeStat;
import com.redhat.thermostat.vm.numa.common.VmNumaStat;

public class VmNumaStatParserTest {

    private final VmNumaStatParser parser = new VmNumaStatParser();

    @Test
    public void testParseSingleNodeStat() throws ParseException {
        final String input = "\n" +
                "Per-node process memory usage (in MBs) for PID 16816 (java)\n" +
                "                           Node 0           Total\n" +
                "                  --------------- ---------------\n" +
                "Huge                         0.00            0.00\n" +
                "Heap                         0.05            0.05\n" +
                "Stack                        6.27            6.27\n" +
                "Private                    385.07          385.07\n" +
                "----------------  --------------- ---------------\n" +
                "Total                      391.39          391.39";

        VmNumaStat stat = parser.parse(input);
        VmNumaNodeStat[] stats = stat.getVmNodeStats();
        assertTrue(stats.length == 1);
        VmNumaNodeStat nodeStat = stats[0];
        assertTrue(nodeStat.getNode() == 0);
        assertTrue(nodeStat.getHugeMemory() == 0);
        assertTrue(nodeStat.getHeapMemory() == 0.05d);
        assertTrue(nodeStat.getStackMemory() == 6.27d);
        assertTrue(nodeStat.getPrivateMemory() == 385.07d);
    }

    @Test
    public void testParseMultipleNodeStat() throws ParseException {
        final String input = "\n" +
                "Per-node process memory usage (in MBs) for PID 3 (ksoftirqd/0)\n" +
                "                           Node 0          Node 1           Total\n" +
                "                  --------------- --------------- ---------------\n" +
                "Huge                         0.00            0.00            0.00\n" +
                "Heap                         0.00            0.00            0.00\n" +
                "Stack                        4.00            1.00            5.00\n" +
                "Private                      5.00            2.00            7.00\n" +
                "----------------  --------------- --------------- ---------------\n" +
                "Total                        9.00            3.00           12.00";

        VmNumaStat stat = parser.parse(input);
        VmNumaNodeStat[] stats = stat.getVmNodeStats();

        assertTrue(stats.length == 2);

        VmNumaNodeStat nodeStat1 = stats[0];
        assertTrue(nodeStat1.getNode() == 0);
        assertTrue(nodeStat1.getHugeMemory() == 0d);
        assertTrue(nodeStat1.getHeapMemory() == 0d);
        assertTrue(nodeStat1.getStackMemory() == 4d);
        assertTrue(nodeStat1.getPrivateMemory() == 5d);

        VmNumaNodeStat nodeStat2 = stats[1];
        assertTrue(nodeStat2.getNode() == 1);
        assertTrue(nodeStat2.getHugeMemory() == 0d);
        assertTrue(nodeStat2.getHeapMemory() == 0d);
        assertTrue(nodeStat2.getStackMemory() == 1d);
        assertTrue(nodeStat2.getPrivateMemory() == 2d);
    }

    @Test (expected = ParseException.class)
    public void testParseEmptyString() throws ParseException {
        String input = "";
        VmNumaStat stat = parser.parse(input);
    }

    @Test (expected = ParseException.class)
    public void testParseIncorrectMemoryData() throws ParseException {
        final String input = "\n" +
                "Per-node process memory usage (in MBs) for PID 3 (ksoftirqd/0)\n" +
                "                           Node 0          Node 1           Total\n" +
                "                  --------------- --------------- ---------------\n" +
                "Huge                         ABCD            0.00            0.00\n" +
                "Heap                         0.00            0.00            0.00\n" +
                "Stack                        4.00            1.00            5.00\n" +
                "Private                      5.00            2.00            7.00\n" +
                "----------------  --------------- --------------- ---------------\n" +
                "Total                        9.00            3.00           12.00";

        parser.parse(input);
    }

    @Test (expected = ParseException.class)
    public void testParseIncorrectMemory() throws ParseException {
        final String input = "\n" +
                "Per-node process memory usage (in MBs) for PID 3 (ksoftirqd/0)\n" +
                "                           Node 0          Node 1           Total\n" +
                "                  --------------- --------------- ---------------\n" +
                "Huge\n" +
                "Heap                         0.00            0.00            0.00\n" +
                "Stack                        4.00            1.00            5.00\n" +
                "Private                      5.00            2.00            7.00\n" +
                "----------------  --------------- --------------- ---------------\n" +
                "Total                        9.00            3.00           12.00";

        parser.parse(input);
    }
}
