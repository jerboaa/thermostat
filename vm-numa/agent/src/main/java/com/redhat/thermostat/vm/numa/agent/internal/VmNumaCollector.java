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

import com.redhat.thermostat.common.Clock;
import com.redhat.thermostat.common.SystemClock;
import com.redhat.thermostat.common.cli.BorderedTableRenderer;
import com.redhat.thermostat.common.utils.LoggingUtils;
import com.redhat.thermostat.shared.config.OS;
import com.redhat.thermostat.vm.numa.common.VmNumaNodeStat;
import com.redhat.thermostat.vm.numa.common.VmNumaStat;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// the "parsing" performed by this class is very rudimentary, but follows the same simple checks performed by the
// "numastat" command
public class VmNumaCollector {

    private static final Logger logger = LoggingUtils.getLogger(VmNumaCollector.class);

    private static final int KILOBYTE = 1024;
    private static final int MEGABYTE = 1024 * KILOBYTE;

    private static final Pattern NODE_PATTERN = Pattern.compile("N([0-9]+)=([0-9]+)");
    private static final Pattern WHITESPACE_PATTERN = Pattern.compile("[\\s]+");

    private final int pid;
    private final Clock clock;
    private final NumaMapsReaderProvider readerProvider;
    private final PageSizeProvider pageSizeProvider;

    public VmNumaCollector(int pid, Clock clock, NumaMapsReaderProvider readerProvider, PageSizeProvider pageSizeProvider) {
        this.pid = pid;
        this.clock = clock;
        this.readerProvider = readerProvider;
        this.pageSizeProvider = pageSizeProvider;
    }

    public VmNumaStat collect() throws IOException {
        Map<Integer, VmNumaNodeStat> statsMap = new TreeMap<>(); // need sorted keys for converting values to array in order later
        try (BufferedReader br = readerProvider.createReader(pid)) {
            for (String line = br.readLine(); line != null; line = br.readLine()) {
                processLine(statsMap, WHITESPACE_PATTERN.split(line));
            }
        }
        return createVmNumaStat(statsMap);
    }

    private void processLine(Map<Integer, VmNumaNodeStat> map, String[] tokens) {
        for (String tok : tokens) {
            Matcher matcher = NODE_PATTERN.matcher(tok);
            if (matcher.matches()) {
                int nodeNumber = Integer.parseInt(matcher.group(1));
                if (!map.containsKey(nodeNumber)) {
                    VmNumaNodeStat stat = new VmNumaNodeStat();
                    stat.setNode(nodeNumber);
                    map.put(nodeNumber, stat);
                }

                Category category = selectCategory(tokens);
                double value = Double.parseDouble(matcher.group(2));
                value *= getMultiplier(category);
                value /= (double) MEGABYTE;

                VmNumaNodeStat stat = map.get(nodeNumber);
                updateStat(stat, category, value);
            }
        }
    }

    private Category selectCategory(String[] tokens) {
        for (String tok : tokens) {
            for (Category c : Category.values()) {
                if (tok.startsWith(c.getToken())) {
                    return c;
                }
            }
        }
        return Category.PRIVATE;
    }

    private long getMultiplier(Category category) {
        switch (category) {
            case HUGE:
                return pageSizeProvider.getHugePageSize();
            default:
                return pageSizeProvider.getPageSize();
        }
    }

    private void updateStat(VmNumaNodeStat stat, Category category, double value) {
        switch (category) {
            case PRIVATE:
                stat.setPrivateMemory(stat.getPrivateMemory() + value);
                break;
            case HEAP:
                stat.setHeapMemory(stat.getHeapMemory() + value);
                break;
            case STACK:
                stat.setStackMemory(stat.getStackMemory() + value);
                break;
            case HUGE:
                stat.setHugeMemory(stat.getHugeMemory() + value);
                break;
            default:
                throw new IllegalStateException("Invalid category: " + category);
        }
    }

    private VmNumaStat createVmNumaStat(Map<Integer, VmNumaNodeStat> map) {
        VmNumaStat numaStat = new VmNumaStat();
        numaStat.setTimeStamp(clock.getRealTimeMillis());
        VmNumaNodeStat[] vmNodeStats = map.values().toArray(new VmNumaNodeStat[map.size()]);
        numaStat.setVmNodeStats(vmNodeStats);
        verifyVmNodeStatsArray(vmNodeStats);
        return numaStat;
    }

    private void verifyVmNodeStatsArray(VmNumaNodeStat[] vmNumaNodeStats) {
        boolean valid = true;
        for (int i = 0; i < vmNumaNodeStats.length; i++) {
            valid = valid && (vmNumaNodeStats[i].getNode() == i);
        }
        if (!valid) {
            logger.log(Level.WARNING, "VmNumaNodeStat array contained invalid array indices");
        }
    }

    public static void main(String[] args) throws IOException {
        if (args.length != 1) {
            throw new RuntimeException("PID argument required");
        }
        if (!OS.IS_LINUX) {
            throw new RuntimeException("Only Linux supported in this manual test case");
        }
        int pid = Integer.parseInt(args[0]);
        Clock clock = new SystemClock();
        NumaMapsReaderProvider readerProvider = new NumaMapsReaderProviderImpl();
        PageSizeProvider pageSizeProvider = new PageSizeProviderImpl();
        VmNumaCollector collector = new VmNumaCollector(pid, clock, readerProvider, pageSizeProvider);
        VmNumaStat stat = collector.collect();

        for (VmNumaNodeStat nodeStat : stat.getVmNodeStats()) {
            BorderedTableRenderer tableRenderer = new BorderedTableRenderer(2);
            tableRenderer.printHeader("Node", String.valueOf(nodeStat.getNode()));
            tableRenderer.printLine("Huge", String.valueOf(nodeStat.getHugeMemory()));
            tableRenderer.printLine("Heap", String.valueOf(nodeStat.getHeapMemory()));
            tableRenderer.printLine("Stack", String.valueOf(nodeStat.getStackMemory()));
            tableRenderer.printLine("Private", String.valueOf(nodeStat.getPrivateMemory()));
            tableRenderer.render(System.out);
        }
    }

    enum Category {
        HUGE("huge"),
        HEAP("heap"),
        STACK("stack"),
        PRIVATE("N");

        private final String token;

        Category(String token) {
            this.token = token;
        }

        public String getToken() {
            return token;
        }
    }

}
