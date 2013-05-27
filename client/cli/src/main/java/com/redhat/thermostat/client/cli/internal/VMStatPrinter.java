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

package com.redhat.thermostat.client.cli.internal;

import java.io.PrintStream;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import com.redhat.thermostat.client.cli.VMStatPrintDelegate;
import com.redhat.thermostat.common.OrderedComparator;
import com.redhat.thermostat.common.cli.TableRenderer;
import com.redhat.thermostat.shared.locale.Translate;
import com.redhat.thermostat.storage.core.VmRef;
import com.redhat.thermostat.storage.model.TimeStampedPojo;
import com.redhat.thermostat.storage.model.TimeStampedPojoComparator;
import com.redhat.thermostat.storage.model.TimeStampedPojoCorrelator;

class VMStatPrinter {

    private static final Translate<LocaleResources> translator = LocaleResources.createLocalizer();

    private static final String TIME = translator.localize(LocaleResources.COLUMN_HEADER_TIME).getContents();

    private VmRef vm;
    private List<VMStatPrintDelegate> delegates;
    private PrintStream out;
    private TimeStampedPojoCorrelator correlator;
    private TableRenderer table;
    private int numCols;
    private Map<VMStatPrintDelegate, DelegateInfo> delegateInfo;

    VMStatPrinter(VmRef vm, List<VMStatPrintDelegate> delegates, PrintStream out) {
        this.vm = vm;
        this.delegates = delegates;
        this.out = out;
        int numDelegates = delegates.size();
        this.delegateInfo = new HashMap<>();
        this.correlator = new TimeStampedPojoCorrelator(numDelegates);
        
        // Sort the delegates list
        Collections.sort(delegates, new OrderedComparator<>());
        
        for (VMStatPrintDelegate delegate : delegates) {
            DelegateInfo info = new DelegateInfo();
            info.lastTimeStamp = Long.MIN_VALUE;
            delegateInfo.put(delegate, info);
        }
    }

    void printStats() {
        List<List<? extends TimeStampedPojo>> allStats = new ArrayList<>();
        List<String> allHeaders = new ArrayList<>();
        allHeaders.add(TIME);
        
        // Copy since we can remove elements in loop body
        List<VMStatPrintDelegate> delegatesCopy = new ArrayList<>(delegates);
        for (VMStatPrintDelegate delegate : delegatesCopy) {
            long timeStamp = delegateInfo.get(delegate).lastTimeStamp;
            List<? extends TimeStampedPojo> latestStats = delegate.getLatestStats(vm, timeStamp);
            if (latestStats == null || latestStats.isEmpty()) {
                // Skipping delegate
                delegates.remove(delegate);
            }
            else {
                List<String> headers = delegate.getHeaders(latestStats.get(0));
                if (headers == null || headers.isEmpty()) {
                    // Skipping delegate
                    delegates.remove(delegate);
                }
                else {
                    DelegateInfo info = delegateInfo.get(delegate);
                    info.colsPerDelegate = headers.size();
                    allHeaders.addAll(headers);
                    allStats.add(latestStats);
                    info.lastTimeStamp = getLatestTimeStamp(timeStamp, latestStats);
                }
            }
        }
        
        printStats(allStats, allHeaders);
    }

    void printUpdatedStats() {
        correlator.clear();
        
        List<List<? extends TimeStampedPojo>> allStats = new ArrayList<>();
        for (VMStatPrintDelegate delegate : delegates) {
            DelegateInfo info = delegateInfo.get(delegate);
            List<? extends TimeStampedPojo> latestStats = delegate.getLatestStats(vm, info.lastTimeStamp);
            allStats.add(latestStats);
            info.lastTimeStamp = getLatestTimeStamp(info.lastTimeStamp, latestStats);
        }

        correlate(allStats);
        printUpdatedStatsImpl();
    }

    private long getLatestTimeStamp(long currentTimeStamp, List<? extends TimeStampedPojo> list) {
        try {
            return Math.max(currentTimeStamp, Collections.max(list, new TimeStampedPojoComparator<>()).getTimeStamp());
        } catch (NoSuchElementException listIsEmpty) {
            return currentTimeStamp;
        }
    }

    private void printStats(List<List<? extends TimeStampedPojo>> allStats, List<String> headers) {
        correlate(allStats);
        numCols = headers.size();
        table = new TableRenderer(numCols);
        printHeaders(table, headers);
        printUpdatedStatsImpl();
    }

    private void printStats(TableRenderer table, Iterator<TimeStampedPojoCorrelator.Correlation> iter) {
        TimeStampedPojoCorrelator.Correlation correlation = iter.next();

        String[] line = new String[numCols];
        DateFormat dateFormat = DateFormat.getTimeInstance();
        String time = dateFormat.format(new Date(correlation.getTimeStamp()));
        line[0] = time;
        
        int off = 1; // time is first index
        for (int i = 0; i < delegates.size(); i++) {
            TimeStampedPojo stat = correlation.get(i);
            VMStatPrintDelegate delegate = delegates.get(i);
            if (stat == null) {
                // Fill with blanks
                DelegateInfo info = delegateInfo.get(delegate);
                Arrays.fill(line, off, off + info.colsPerDelegate, "");
                off += info.colsPerDelegate;
            }
            else {
                List<String> data = delegate.getStatRow(stat);
                if (data == null) {
                    throw new NullPointerException("Returned null stat row");
                }
                else if (data.size() != delegateInfo.get(delegate).colsPerDelegate) {
                    throw new IllegalStateException("Delegate "
                            + delegate.toString() + " provided "
                            + delegateInfo.get(delegate).colsPerDelegate
                            + " column headers, but only " + data.size()
                            + " stat row values");
                }
                else {
                    System.arraycopy(data.toArray(), 0, line, off, data.size());
                    off += data.size();
                }
            }
        }
        
        table.printLine(line);
    }

    private void printHeaders(TableRenderer table, List<String> headers) {
        table.printLine(headers.toArray(new String[headers.size()]));
    }

    private void correlate(List<List<? extends TimeStampedPojo>> allStats) {
        int count = 0;
        for (List<? extends TimeStampedPojo> stats : allStats) {
            for(TimeStampedPojo cpuStat : stats) {
                correlator.add(count, cpuStat);
            }
            count++;
        }
    }

    void printUpdatedStatsImpl() {
        Iterator<TimeStampedPojoCorrelator.Correlation> iterator = correlator.iterator();
        while (iterator.hasNext()) {
            printStats(table, iterator);
        }
        table.render(out);
    }
    
    private static class DelegateInfo {
        int colsPerDelegate;
        long lastTimeStamp;
    }

}

