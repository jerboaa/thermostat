/*
 * Copyright 2012-2014 Red Hat, Inc.
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

package com.redhat.thermostat.dev.perf.logs.internal;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;

public class StatementStats {
    
    private final List<StatementStat> stats;
    private final SharedStatementState state; 
    private boolean analysisDone = false;
    private Map<Integer, StatementStatSummary> cachedSummaries;
    private int numReads = 0;
    private int numWrites = 0;
    
    StatementStats(List<StatementStat> stats, SharedStatementState state) {
        this.stats = stats;
        this.state = state;
    }
    
    TimeUnit getTimeUnit(String descriptor) {
        if (!analysisDone) {
            doAnalysis();
        }
        int id = state.getMappedStatementId(descriptor);
        StatementStatSummary summary = cachedSummaries.get(id);
        return summary.getTimeUnit();
    }

    double getAverage(String descriptor) {
        if (!analysisDone) {
            doAnalysis();
        }
        int id = state.getMappedStatementId(descriptor);
        StatementStatSummary summary = cachedSummaries.get(id);
        return summary.getAvg();
    }
    
    long getMaxExecTime(String descriptor) {
        if (!analysisDone) {
            doAnalysis();
        }
        int id = state.getMappedStatementId(descriptor);
        StatementStatSummary summary = cachedSummaries.get(id);
        return summary.getMax();
    }
    
    long getMinExecTime(String descriptor) {
        if (!analysisDone) {
            doAnalysis();
        }
        int id = state.getMappedStatementId(descriptor);
        StatementStatSummary summary = cachedSummaries.get(id);
        return summary.getMin();
    }
    
    List<StatementStat> getDistinctStatements() {
        if (!analysisDone) {
            doAnalysis();
        }
        List<StatementStat> distincts = new ArrayList<>();
        for (Entry<Integer, StatementStatSummary> entry: cachedSummaries.entrySet()) {
            distincts.add(entry.getValue().getStatement());
        }
        return distincts;
    }
    
    int getNumReads() {
        if (!analysisDone) {
            doAnalysis();
        }
        return numReads;
    }
    
    int getNumWrites() {
        if (!analysisDone) {
            doAnalysis();
        }
        return numWrites;
    }
    
    int getTotalNumberOfRecords() {
        return stats.size();
    }
    
    List<StatementStat> getAllStats() {
        return stats;
    }
    
    private void doAnalysis() {
        cachedSummaries = new HashMap<>();
        for (StatementStat s: stats) {
            if (cachedSummaries.containsKey(s.getDescId())) {
                StatementStatSummary summary = cachedSummaries.get(s.getDescId());
                summary.recalculate(s.getExecTime());
            } else {
                StatementStatSummary summary = new StatementStatSummary(s);
                summary.recalculate(s.getExecTime());
                cachedSummaries.put(s.getDescId(), summary);
            }
            if (s instanceof ReadStatementStat) {
                numReads++;
            }
            if (s instanceof WriteStatementStat) {
                numWrites++;
            }
        }
        analysisDone = true;
    }
    
    private static class StatementStatSummary {
        
        private final StatementStat stat;
        private TimeUnit timeUnit;
        private long maxVal;
        private long minVal;
        private long runningSum;
        private long runningCount;
        
        private StatementStatSummary(StatementStat stat) {
            this.stat = stat;
            this.maxVal = Long.MIN_VALUE;
            this.minVal = Long.MAX_VALUE;
            this.runningCount = 0;
            this.runningSum = 0;
        }

        private void recalculate(Duration newValue) {
            this.timeUnit = newValue.getTimeUnit();
            long value = newValue.getVal();
            if (value < minVal) {
                minVal = value;
            }
            if (value > maxVal) {
                maxVal = value;
            }
            runningCount++;
            runningSum += value;
        }
        
        private long getMax() {
            return maxVal;
        }
        
        private long getMin() {
            return minVal;
        }
        
        private double getAvg() {
            return runningSum/(double)runningCount;
        }
        
        private TimeUnit getTimeUnit() {
            return timeUnit;
        }
        
        private StatementStat getStatement() {
            return this.stat;
        }
    }
}
