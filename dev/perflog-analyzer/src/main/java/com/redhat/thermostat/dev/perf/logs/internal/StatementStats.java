/*
 * Copyright 2012-2016 Red Hat, Inc.
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

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;

import com.redhat.thermostat.dev.perf.logs.Direction;
import com.redhat.thermostat.dev.perf.logs.SortBy;

public class StatementStats extends BasicStats<StatementStat> {
    
    private static final char MICRO_SIGN = '\u00b5';
    private static final String DAYS_SHORT = "days";
    private static final String HOURS_SHORT = "hours";
    private static final String MINUTES_SHORT = "mins";
    private static final String SECONDS_SHORT = "s";
    private static final String MILLIS_SHORT = "ms";
    private static final String MICROS_SHORT = MICRO_SIGN + "s";
    private static final String NANOS_SHORT = "ns";
    private boolean analysisDone = false;
    private Map<Integer, StatementStatSummary> cachedSummaries;
    private int numReads = 0;
    private int numWrites = 0;
    
    // for testing
    StatementStats(List<StatementStat> stats, SharedStatementState state) {
        super(stats, state);
    }
    
    StatementStats() {
        // main no-arg constructor as used by factory
    }
    
    
    TimeUnit getTimeUnit(String descriptor) {
        if (!analysisDone) {
            doAnalysis();
        }
        int id = sharedState.getMappedStatementId(descriptor);
        StatementStatSummary summary = cachedSummaries.get(id);
        return summary.getTimeUnit();
    }

    double getAverage(String descriptor) {
        if (!analysisDone) {
            doAnalysis();
        }
        int id = sharedState.getMappedStatementId(descriptor);
        StatementStatSummary summary = cachedSummaries.get(id);
        return summary.getAvg();
    }
    
    long getMaxExecTime(String descriptor) {
        if (!analysisDone) {
            doAnalysis();
        }
        int id = sharedState.getMappedStatementId(descriptor);
        StatementStatSummary summary = cachedSummaries.get(id);
        return summary.getMax();
    }
    
    long getMinExecTime(String descriptor) {
        if (!analysisDone) {
            doAnalysis();
        }
        int id = sharedState.getMappedStatementId(descriptor);
        StatementStatSummary summary = cachedSummaries.get(id);
        return summary.getMin();
    }
    
    long getTotalCount(String descriptor) {
        if (!analysisDone) {
            doAnalysis();
        }
        int id = sharedState.getMappedStatementId(descriptor);
        StatementStatSummary summary = cachedSummaries.get(id);
        return summary.getCount();
    }
    
    /**
     * 
     * @return All distinct statements in undefined order.
     */
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
    
    private List<StatementStatSummary> getDistinctSummaries() {
        if (!analysisDone) {
            doAnalysis();
        }
        List<StatementStatSummary> summaries = new ArrayList<>();
        for (StatementStatSummary sum: cachedSummaries.values()) {
            summaries.add(sum);
        }
        return summaries;
    }
    
    /**
     * 
     * @param property The property to sort by.
     * @return All distinct statements in sorted order as specified by
     *         {@code property}.
     */
    List<StatementStat> getDistinctStatements(SortBy property, Direction direction) {
        boolean invert = direction == Direction.DSC ? true : false;
        Comparator<StatementStatSummary> comparator = null;
        switch(property) {
        case AVG:
            comparator = new StatementStatSummaryAvgComparator(invert);
            break;
        case COUNT:
            comparator = new StatementStatSummaryCountComparator(invert);
            break;
        case MAX:
            comparator = new StatementStatSummaryMaxComparator(invert);
            break;
        case MIN:
            comparator = new StatementStatSummaryMinComparator(invert);
            break;
        default:
            throw new IllegalStateException("Unknown sort by property " + property);
        }
        List<StatementStatSummary> stats = getDistinctSummaries();
        Collections.sort(stats, comparator);
        List<StatementStat> retval = new ArrayList<>();
        for (StatementStatSummary sumary: stats) {
            retval.add(sumary.getStatement());
        }
        return retval;
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
    
    static abstract class BaseStatComparator implements Comparator<StatementStatSummary> {
        
        private final boolean invert;
        
        private BaseStatComparator(boolean invert) {
            this.invert = invert;
        }
        
        abstract int getComparison(StatementStatSummary one, StatementStatSummary other);
        
        @Override
        public int compare(StatementStatSummary one, StatementStatSummary other) {
            int retval = getComparison(one, other);
            if (invert) {
                return -retval;
            } else {
                return retval;
            }
        }
        
    }
    
    static class StatementStatSummaryAvgComparator extends BaseStatComparator {
        
        StatementStatSummaryAvgComparator(boolean invert) {
            super(invert);
        }

        @Override
        public int getComparison(StatementStatSummary one, StatementStatSummary other) {
            if (one.getAvg() < other.getAvg()) {
                return -1;
            } else if (one.getAvg() > other.getAvg()) {
                return 1;
            } else {
                return 0;
            }
        }
        
    }
    
    static class StatementStatSummaryMinComparator extends BaseStatComparator {

        StatementStatSummaryMinComparator(boolean invert) {
            super(invert);
        }
        
        @Override
        public int getComparison(StatementStatSummary one, StatementStatSummary other) {
            if (one.getMin() < other.getMin()) {
                return -1;
            } else if (one.getMin() > other.getMin()) {
                return 1;
            } else {
                return 0;
            }
        }
        
    }
    
    static class StatementStatSummaryMaxComparator extends BaseStatComparator {

        StatementStatSummaryMaxComparator(boolean invert) {
            super(invert);
        }
        
        @Override
        public int getComparison(StatementStatSummary one, StatementStatSummary other) {
            if (one.getMax() < other.getMax()) {
                return -1;
            } else if (one.getMax() > other.getMax()) {
                return 1;
            } else {
                return 0;
            }
        }
        
    }
    
    static class StatementStatSummaryCountComparator extends BaseStatComparator {

        StatementStatSummaryCountComparator(boolean invert) {
            super(invert);
        }
        
        @Override
        public int getComparison(StatementStatSummary one, StatementStatSummary other) {
            if (one.getCount() < other.getCount()) {
                return -1;
            } else if (one.getCount() > other.getCount()) {
                return 1;
            } else {
                return 0;
            }
        }
        
    }
    
    static class StatementStatSummary {
        
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
        
        private long getCount() {
            return runningCount;
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

    @Override
    public void printSummary(PrintStream out) {
        out.print(String.format("Statement statistics (%s records): ", getTotalNumberOfRecords()));
        List<StatementStat> distinctStatements = getDistinctStatements(config.getSortBy(), config.getDirection());
        String detail = String.format("%s distinct statements (%s reads, %s writes)",
                distinctStatements.size(),
                getNumReads(),
                getNumWrites());
        out.println(detail);
        if (distinctStatements.size() > 0) {
            out.println("");
            out.println("Statement details (" + getSortDetailsMsg() + "):");
            for (StatementStat stat: distinctStatements) {
                String descriptor = stat.getDescriptor();
                long min = getMinExecTime(descriptor);
                long max = getMaxExecTime(descriptor);
                double avg = getAverage(descriptor);
                long count = getTotalCount(descriptor);
                TimeUnit timeUnit = getTimeUnit(descriptor);
                String tu = getTimeUnit(timeUnit);
                String descDetail = String.format("Total #: %s, %s%s (min), %s%s (max), %.02f%s (avg), DESCRIPTOR: %s",
                        count, min, tu, max, tu, avg, tu, descriptor);
                out.println(descDetail);
            }
        }
        out.println(""); // Extra line at end of summary
    }
    
    private String getSortDetailsMsg() {
        String sortByMsg = "sorted by ";
        switch(config.getSortBy()) {
        case AVG:
            return sortByMsg + "avg exec time";
        case COUNT:
            return sortByMsg + "total occurances";
        case MAX:
            return sortByMsg + "max exec time";
        case MIN:
            return sortByMsg + "min exec time";
        default:
            throw new IllegalArgumentException("Unknown sort value " + config.getSortBy());
        }
    }

    private String getTimeUnit(TimeUnit timeUnit) {
        switch(timeUnit) {
        case DAYS:
            return DAYS_SHORT;
        case HOURS:
            return HOURS_SHORT;
        case MICROSECONDS:
            return MICROS_SHORT;
        case MILLISECONDS:
            return MILLIS_SHORT;
        case MINUTES:
            return MINUTES_SHORT;
        case NANOSECONDS:
            return NANOS_SHORT;
        case SECONDS:
            return SECONDS_SHORT;
        default:
            throw new IllegalStateException("Unknown time unit " + timeUnit);
        }
    }
}
