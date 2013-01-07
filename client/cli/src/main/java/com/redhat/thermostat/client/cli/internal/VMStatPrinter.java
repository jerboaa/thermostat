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

package com.redhat.thermostat.client.cli.internal;

import java.io.PrintStream;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import com.redhat.thermostat.common.Size;
import com.redhat.thermostat.common.cli.TableRenderer;
import com.redhat.thermostat.common.dao.VmMemoryStatDAO;
import com.redhat.thermostat.common.dao.VmRef;
import com.redhat.thermostat.common.locale.Translate;
import com.redhat.thermostat.storage.model.TimeStampedPojo;
import com.redhat.thermostat.storage.model.TimeStampedPojoComparator;
import com.redhat.thermostat.storage.model.TimeStampedPojoCorrelator;
import com.redhat.thermostat.storage.model.VmCpuStat;
import com.redhat.thermostat.storage.model.VmMemoryStat;
import com.redhat.thermostat.vm.cpu.common.VmCpuStatDAO;

class VMStatPrinter {

    private static final Translate<LocaleResources> translator = LocaleResources.createLocalizer();

    private static final String CPU_PERCENT = translator.localize(LocaleResources.COLUMN_HEADER_CPU_PERCENT);
    private static final String TIME = translator.localize(LocaleResources.COLUMN_HEADER_TIME);

    private VmRef vm;
    private VmCpuStatDAO vmCpuStatDAO;
    private VmMemoryStatDAO vmMemoryStatDAO;
    private PrintStream out;
    private TimeStampedPojoCorrelator correlator = new TimeStampedPojoCorrelator(2);
    private TableRenderer table;
    private int numSpaces;

    private long lastCpuStatTimeStamp = Long.MIN_VALUE;
    private long lastMemoryStatTimeStamp = Long.MIN_VALUE;

    VMStatPrinter(VmRef vm, VmCpuStatDAO vmCpuStatDAO, VmMemoryStatDAO vmMemoryStatDAO, PrintStream out) {
        this.vm = vm;
        this.vmCpuStatDAO = vmCpuStatDAO;
        this.vmMemoryStatDAO = vmMemoryStatDAO;
        this.out = out;
    }

    void printStats() {
        List<VmCpuStat> cpuStats = vmCpuStatDAO.getLatestVmCpuStats(vm, lastCpuStatTimeStamp);
        List<VmMemoryStat> memStats = vmMemoryStatDAO.getLatestVmMemoryStats(vm, lastMemoryStatTimeStamp);

        lastCpuStatTimeStamp = getLatestTimeStamp(lastCpuStatTimeStamp, cpuStats);
        lastMemoryStatTimeStamp = getLatestTimeStamp(lastMemoryStatTimeStamp, memStats);

        printStats(cpuStats, memStats);
    }

    void printUpdatedStats() {
        correlator.clear();
        List<VmCpuStat> cpuStats = vmCpuStatDAO.getLatestVmCpuStats(vm, lastCpuStatTimeStamp);
        List<VmMemoryStat> memStats = vmMemoryStatDAO.getLatestVmMemoryStats(vm, lastMemoryStatTimeStamp);

        lastCpuStatTimeStamp = getLatestTimeStamp(lastCpuStatTimeStamp, cpuStats);
        lastMemoryStatTimeStamp = getLatestTimeStamp(lastMemoryStatTimeStamp, memStats);

        correlate(cpuStats, memStats);
        printUpdatedStatsImpl();
    }

    private long getLatestTimeStamp(long currentTimeStamp, List<? extends TimeStampedPojo> list) {
        try {
            return Math.max(currentTimeStamp, Collections.max(list, new TimeStampedPojoComparator<>()).getTimeStamp());
        } catch (NoSuchElementException listIsEmpty) {
            return currentTimeStamp;
        }
    }

    private void printStats(List<VmCpuStat> cpuStats, List<VmMemoryStat> memStats) {
        correlate(cpuStats, memStats);
        numSpaces = getNumSpaces(memStats);
        int numColumns = numSpaces + 2;
        table = new TableRenderer(numColumns);
        printHeaders(memStats, numSpaces, numColumns, table);
        printUpdatedStatsImpl();
    }

    private void printStats(int numSpaces, TableRenderer table, Iterator<TimeStampedPojoCorrelator.Correlation> i) {

        TimeStampedPojoCorrelator.Correlation correlation = i.next();

        VmCpuStat cpuStat = (VmCpuStat) correlation.get(0);
        DecimalFormat format = new DecimalFormat("#0.0");
        String cpuLoad = cpuStat != null ? format.format(cpuStat.getCpuLoad()) : "";

        DateFormat dateFormat = DateFormat.getTimeInstance();
        String time = dateFormat.format(new Date(correlation.getTimeStamp()));

        String[] memoryUsage = getMemoryUsage((VmMemoryStat) correlation.get(1), numSpaces);

        String[] line = new String[numSpaces + 2];
        System.arraycopy(memoryUsage, 0, line, 2, numSpaces);
        line[0] = time;
        line[1] = cpuLoad;
        table.printLine(line);
    }

    private void printHeaders(List<VmMemoryStat> memStats, int numSpaces, int numColumns, TableRenderer table) {
        String[] spacesNames = getSpacesNames(memStats, numSpaces);
        String[] headers = new String[numColumns];
        headers[0] = TIME;
        headers[1] = CPU_PERCENT;
        System.arraycopy(spacesNames, 0, headers, 2, numSpaces);
        table.printLine(headers);
    }

    private String[] getMemoryUsage(VmMemoryStat vmMemoryStat, int numSpaces) {
        String[] memoryUsage = new String[numSpaces];
        if (vmMemoryStat == null) {
            Arrays.fill(memoryUsage, "");
            return memoryUsage;
        }
        int i = 0;
        for (VmMemoryStat.Generation gen : vmMemoryStat.getGenerations()) {
            for (VmMemoryStat.Space space : gen.getSpaces()) {
                memoryUsage[i] = Size.bytes(space.getUsed()).toString();
                i++;
            }
        }
        return memoryUsage;
    }

    private String[] getSpacesNames(List<VmMemoryStat> memStats, int numSpaces) {
        if (numSpaces < 1) {
            return new String[0];
        }
        String[] spacesNames = new String[numSpaces];
        VmMemoryStat stat = memStats.get(0);
        int i = 0;
        for (VmMemoryStat.Generation gen : stat.getGenerations()) {
            for (VmMemoryStat.Space space : gen.getSpaces()) {
                spacesNames[i] = translator.localize(LocaleResources.COLUMN_HEADER_MEMORY_PATTERN, space.getName());
                i++;
            }
        }
        return spacesNames;
    }

    private int getNumSpaces(List<VmMemoryStat> memStats) {
        if (memStats.size() < 1) {
            return 0;
        }
        VmMemoryStat stat = memStats.get(0);
        int numSpaces = 0;
        for (VmMemoryStat.Generation gen : stat.getGenerations()) {
            numSpaces += gen.getSpaces().length;
        }
        return numSpaces;
    }

    private void correlate(List<VmCpuStat> cpuStats, List<VmMemoryStat> memStats) {
        for(VmCpuStat cpuStat : cpuStats) {
            correlator.add(0, cpuStat);
        }
        for (VmMemoryStat memStat : memStats) {
            correlator.add(1, memStat);
        }
    }

    void printUpdatedStatsImpl() {
        Iterator<TimeStampedPojoCorrelator.Correlation> iterator = correlator.iterator();
        while (iterator.hasNext()) {
            printStats(numSpaces, table, iterator);
        }
        table.render(out);
    }

}
