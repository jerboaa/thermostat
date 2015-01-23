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

package com.redhat.thermostat.dev.perf.logs.internal;

import java.io.PrintStream;
import java.util.List;

class QueueStats extends BasicStats<QueueStat> {

    public static final int NOT_APPLICABLE = -1;
    private boolean analysisDone = false;
    private long runningSum = 0;
    private long runningCount = 0;
    private long currMax = Long.MIN_VALUE;
    private long currMin = Long.MAX_VALUE;
    
    // for testing
    QueueStats(List<QueueStat> stats) {
        super(stats);
    }
    
    QueueStats() {
        // no-arg main constructor used by factory
    }
    
    long getMax() {
        if (!analysisDone) {
            doAnalysis();
        }
        if (currMax == Long.MIN_VALUE) {
            return NOT_APPLICABLE;
        }
        return currMax;
    }
    
    long getMin() {
        if (!analysisDone) {
            doAnalysis();
        }
        if (currMin == Long.MAX_VALUE) {
            return NOT_APPLICABLE;
        }
        return currMin;
    }
    
    double getAvgQueueSize() {
        if (!analysisDone) {
            doAnalysis();
        }
        if (runningCount == 0) {
            return NOT_APPLICABLE;
        }
        return runningSum/(double)runningCount;
    }
    
    private void doAnalysis() {
        for (QueueStat s: stats) {
            runningCount++;
            long val = s.getSizeValue();
            runningSum += val;
            if (val > currMax) {
                currMax = val;
            }
            if (val < currMin) {
                currMin = val;
            }
        }
        analysisDone = true;
    }

    @Override
    public void printSummary(PrintStream out) {
        out.print(String.format("Queue size stats (%s records)", getTotalNumberOfRecords()));
        if (getTotalNumberOfRecords() > 0) {
            out.print(": ");
            out.print(getMax() + "(max) ");
            out.print(getMin() + "(min) ");
            out.println(String.format("%.02f(avg)", getAvgQueueSize()));
        } else {
            out.println(""); // line-feed
        }
        out.println(""); // Extra line at end of summary
    }

}
