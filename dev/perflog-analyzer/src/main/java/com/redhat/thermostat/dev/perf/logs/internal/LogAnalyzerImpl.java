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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;

import com.redhat.thermostat.dev.perf.logs.LogAnalyzer;

public class LogAnalyzerImpl implements LogAnalyzer {

    private static final char MICRO_SIGN = '\u00b5';
    private static final String DAYS_SHORT = "days";
    private static final String HOURS_SHORT = "hours";
    private static final String MINUTES_SHORT = "mins";
    private static final String SECONDS_SHORT = "s";
    private static final String MILLIS_SHORT = "ms";
    private static final String MICROS_SHORT = MICRO_SIGN + "s";
    private static final String NANOS_SHORT = "ns";
    
    private final File logFile;
    
    public LogAnalyzerImpl(File logFile) {
        this.logFile = logFile;
    }
    
    @Override
    public void analyze() {
        LogFileStats stats = null;
        try {
            stats = readFileCollectStats();
        } catch (ReadException e) {
            System.err.println(e.getMessage());
            return;
        }
        printStats(stats);
    }

    private void printStats(LogFileStats stats) {
        System.out.println("Statistics for log file: " + logFile.getAbsolutePath());
        System.out.println("Total of " + stats.getTotalStats() + " records analyzed.");
        System.out.println();
        QueueStats queueStats = stats.getQueueStats();
        System.out.print(String.format("Queue size stats (%s records)", queueStats.getTotalNumberOfRecords()));
        if (queueStats.getTotalNumberOfRecords() > 0) {
            System.out.print(": ");
            System.out.print(queueStats.getMax() + "(max) ");
            System.out.print(queueStats.getMin() + "(min) ");
            System.out.println(String.format("%.02f(avg)", queueStats.getAvgQueueSize()));
        } else {
            System.out.println(""); // line-feed
        }
        System.out.println("");
        StatementStats statementStats = stats.getStatementStats();
        System.out.print(String.format("Statement statistics (%s records): ", statementStats.getTotalNumberOfRecords()));
        List<StatementStat> distinctStatements = statementStats.getDistinctStatements();
        String detail = String.format("%s distinct statements (%s reads, %s writes)",
                                       distinctStatements.size(),
                                       statementStats.getNumReads(),
                                       statementStats.getNumWrites());
        System.out.println(detail);
        if (distinctStatements.size() > 0) {
            System.out.println("");
            System.out.println("Statement details:");
            for (StatementStat stat: distinctStatements) {
                String descriptor = stat.getDescriptor();
                long min = statementStats.getMinExecTime(descriptor);
                long max = statementStats.getMaxExecTime(descriptor);
                double avg = statementStats.getAverage(descriptor);
                TimeUnit timeUnit = statementStats.getTimeUnit(descriptor);
                String tu = getTimeUnit(timeUnit);
                String descDetail = String.format("%s%s (min), %s%s (max), %.02f%s (avg), DESCRIPTOR: %s",
                        min, tu, max, tu, avg, tu, descriptor);
                System.out.println(descDetail);
            }
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

    private LogFileStats readFileCollectStats() throws ReadException {
        StatsParser parser = StatsParserBuilder.build();
        LogFileStats stats = new LogFileStats(parser.getSharedState());
        try (FileReader freader = new FileReader(logFile);
                Scanner scanner = new Scanner(freader)) {
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();
                LineStat stat = parser.parse(line);
                if (stat != null) {
                    stats.add(stat);
                }
            }
        } catch (FileNotFoundException e) {
            throw new ReadException("File not found: " + logFile.getAbsolutePath(), e);
        } catch (IOException e) {
            throw new ReadException("Error reading log file: " + logFile.getAbsolutePath(), e);
        }
        return stats;
    }
    
    private static class ReadException extends Exception {
        
        private static final long serialVersionUID = -5887794425580426338L;

        private ReadException(String msg, Throwable cause) {
            super(msg, cause);
        }
    }

}
