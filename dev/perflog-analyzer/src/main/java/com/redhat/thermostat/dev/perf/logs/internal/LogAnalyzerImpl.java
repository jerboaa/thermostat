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

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Scanner;

import com.redhat.thermostat.dev.perf.logs.LogAnalyzer;
import com.redhat.thermostat.dev.perf.logs.StatsConfig;

public class LogAnalyzerImpl implements LogAnalyzer {

    private static final QueueStatsFilter QUEUESTATS_FILTER = new QueueStatsFilter();
    private static final StatementStatsFilter STATEMENTSTATS_FILTER = new StatementStatsFilter();
    private static final LineStatsFilter<QueueStat, QueueStats> QUEUESTATS_BACKING_FILTER = new LogTagStatsFilterDecorator<>(QUEUESTATS_FILTER, LogTag.STORAGE_BACKING_PROXIED);
    private static final LineStatsFilter<QueueStat, QueueStats> QUEUESTATS_FRONT_FILTER = new LogTagStatsFilterDecorator<>(QUEUESTATS_FILTER, LogTag.STORAGE_FRONT_END);
    private static final LineStatsFilter<StatementStat, StatementStats> STATEMENTSTATS_BACKING_FILTER = new LogTagStatsFilterDecorator<>(STATEMENTSTATS_FILTER, LogTag.STORAGE_BACKING_PROXIED);
    private static final LineStatsFilter<StatementStat, StatementStats> STATEMENTSTATS_FRONT_FILTER = new LogTagStatsFilterDecorator<>(STATEMENTSTATS_FILTER, LogTag.STORAGE_FRONT_END);
    
    private final StatsConfig config;
    
    public LogAnalyzerImpl(StatsConfig config) {
        this.config = config;
    }
    
    @Override
    public void analyze() {
        StatsParser parser = StatsParserBuilder.build();
        LogFileStats stats = null;
        try {
            stats = createLogFileStats(parser.getSharedState());
            readFileCollectStats(parser, stats);
        } catch (ReadException e) {
            System.err.println(e.getMessage());
            return;
        }
        printStats(stats);
    }
    
    private LogFileStats createLogFileStats(final SharedStatementState state) throws ReadException {
        LogFileStats stats = new LogFileStats(state, config);
        try {
            // register filters in this order since it influences printing.
            stats.registerStatsFilter(QUEUESTATS_FRONT_FILTER);
            stats.registerStatsFilter(STATEMENTSTATS_FRONT_FILTER);
            // show backing config only if so configured
            if (config.isShowBacking()) {
                stats.registerStatsFilter(QUEUESTATS_BACKING_FILTER);
                stats.registerStatsFilter(STATEMENTSTATS_BACKING_FILTER);
            }
            return stats;
        } catch (IllegalFilterException e) {
            throw new ReadException("Invalid filter", e);
        }
    }

    private void printStats(LogFileStats stats) {
        System.out.println("Statistics for log file: " + config.getLogFile().getAbsolutePath());
        System.out.println("Total of " + stats.getTotalStats() + " records analyzed.");
        System.out.println();
        
        for (LineStatsFilter<?, ?> filter: stats.getRegisteredFilters()) {
            LineStats<?> lineStats = stats.getStatsForBucket(filter);
            lineStats.printSummary(System.out);
        }
    }

    private void readFileCollectStats(final StatsParser parser, final LogFileStats stats) throws ReadException {
        try (FileReader freader = new FileReader(config.getLogFile());
                Scanner scanner = new Scanner(freader)) {
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();
                LineStat stat = parser.parse(line);
                if (stat != null) {
                    stats.add(stat);
                }
            }
        } catch (FileNotFoundException e) {
            throw new ReadException("File not found: " + config.getLogFile().getAbsolutePath(), e);
        } catch (IOException e) {
            throw new ReadException("Error reading log file: " + config.getLogFile().getAbsolutePath(), e);
        }
    }
    
    private static class ReadException extends Exception {
        
        private static final long serialVersionUID = -5887794425580426338L;

        private ReadException(String msg, Throwable cause) {
            super(msg, cause);
        }
    }

}
