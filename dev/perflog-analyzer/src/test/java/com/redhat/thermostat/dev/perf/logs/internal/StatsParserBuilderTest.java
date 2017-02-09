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

package com.redhat.thermostat.dev.perf.logs.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLDecoder;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Scanner;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

import org.junit.Test;

import com.redhat.thermostat.dev.perf.logs.internal.StatsParserBuilder.StatsParserImpl;

public class StatsParserBuilderTest {

    private static final String SEP = "|";
    
    @Test
    public void canParseBasicTokensWithDuration() throws LineParseException {
        StatsParserImpl parser = (StatsParserImpl)StatsParserBuilder.build();
        String date = "2014-07-02T17:31:17.920+0200";
        LogTag logToken = LogTag.STORAGE_BACKING_PROXIED;
        String hasDuration = "1";
        String msg = "some message";
        String duration = "34050 ns";
        String line = date + SEP + hasDuration + SEP + logToken + SEP + msg + SEP + duration; 
        TokenizedLine lineTokens = parser.splitIntoTokens(line);
        Calendar cal = Calendar.getInstance();
        cal.clear();
        TimeZone tz = TimeZone.getTimeZone("Europe/Vienna");
        cal.setTimeZone(tz);
        cal.set(Calendar.DAY_OF_MONTH, 2);
        cal.set(Calendar.YEAR, 2014);
        cal.set(Calendar.MONTH, Calendar.JULY);
        cal.set(Calendar.HOUR_OF_DAY, 17);
        cal.set(Calendar.MINUTE, 31);
        cal.set(Calendar.SECOND, 17);
        cal.set(Calendar.MILLISECOND, 920);       
        Date expectedDate = cal.getTime();
        assertEquals(expectedDate, lineTokens.getDate());
        assertEquals(logToken, lineTokens.getLogToken());
        assertEquals(true, lineTokens.hasDuration());
        assertEquals(msg, lineTokens.getMessageDuration().getMsg());
        Duration expectedD = new Duration(34050, TimeUnit.NANOSECONDS);
        assertEquals(expectedD, lineTokens.getMessageDuration().getDuration());
    }
    
    @Test
    public void canParseBasicTokensNoDuration() throws LineParseException {
        StatsParserImpl parser = (StatsParserImpl)StatsParserBuilder.build();
        String date = "2014-07-02T17:31:17.920+0200";
        LogTag logToken = LogTag.STORAGE_BACKING_PROXIED;
        String hasDuration = "0";
        String msg = "some message";
        String line = date + SEP + hasDuration + SEP + logToken + SEP + msg; 
        TokenizedLine lineTokens = parser.splitIntoTokens(line);
        Calendar cal = Calendar.getInstance();
        cal.clear();
        TimeZone tz = TimeZone.getTimeZone("Europe/Vienna");
        cal.setTimeZone(tz);
        cal.set(Calendar.DAY_OF_MONTH, 2);
        cal.set(Calendar.YEAR, 2014);
        cal.set(Calendar.MONTH, Calendar.JULY);
        cal.set(Calendar.HOUR_OF_DAY, 17);
        cal.set(Calendar.MINUTE, 31);
        cal.set(Calendar.SECOND, 17);
        cal.set(Calendar.MILLISECOND, 920);       
        Date expectedDate = cal.getTime();
        assertEquals(expectedDate, lineTokens.getDate());
        assertEquals(logToken, lineTokens.getLogToken());
        assertEquals(false, lineTokens.hasDuration());
        assertEquals(msg, lineTokens.getMessageDuration().getMsg());
        assertNull(lineTokens.getMessageDuration().getDuration());
    }
    
    @Test
    public void canParseBasicLogFile() throws FileNotFoundException {
        StatsParser parser = StatsParserBuilder.build();
        File perfLogFile = new File(decodeFilePath(this.getClass().getResource("/testPerfLogFile.log")));
        Scanner scanner = new Scanner(perfLogFile);
        ArrayList<LineStat> stats = new ArrayList<>();
        while (scanner.hasNextLine()) {
            String line = scanner.nextLine();
            LineStat stat = parser.parse(line);
            stats.add(stat);
        }
        scanner.close();
        assertEquals(80, stats.size());
        LineStat s = stats.get(3); // fetch 4'th line of file
        assertTrue(s instanceof ReadStatementStat);
        ReadStatementStat readRec = (ReadStatementStat)s;
        // DB_READ QUERY vm-info WHERE 'agentId' = ?s AND 'vmId' = ?s LIMIT 1|4724 µs
        assertEquals(4724, readRec.getExecTime().getVal());
        assertEquals(TimeUnit.MICROSECONDS, readRec.getExecTime().getTimeUnit());
        String desc = "QUERY vm-info WHERE 'agentId' = ?s AND 'vmId' = ?s LIMIT 1";
        String actualDesc = readRec.getDescriptor();
        assertEquals(desc, actualDesc);
    }
    
    @Test
    public void canParseLogFileWithMultipleLogTags() throws FileNotFoundException, IllegalFilterException, ParseException {
        StatsParser parser = StatsParserBuilder.build();
        File perfLogFile = new File(decodeFilePath(this.getClass().getResource("/perflogMultipleLogTags.log")));
        Scanner scanner = new Scanner(perfLogFile);
        LogFileStats stats = new LogFileStats(new SharedStatementState(), null);
        // add composite filters for queue stats.
        LineStatsFilter<QueueStat, QueueStats> qFrontFilter = new LogTagStatsFilterDecorator<>(new QueueStatsFilter(), LogTag.STORAGE_FRONT_END);
        LineStatsFilter<QueueStat, QueueStats> qBackFilter = new LogTagStatsFilterDecorator<>(new QueueStatsFilter(), LogTag.STORAGE_BACKING_PROXIED);
        LineStatsFilter<StatementStat, StatementStats> stmtStatsBackFilter = new LogTagStatsFilterDecorator<>(new StatementStatsFilter(), LogTag.STORAGE_BACKING_PROXIED);
        LineStatsFilter<StatementStat, StatementStats> stmtStatsFrontFilter = new LogTagStatsFilterDecorator<>(new StatementStatsFilter(), LogTag.STORAGE_FRONT_END);
        stats.registerStatsFilter(qFrontFilter);
        stats.registerStatsFilter(qBackFilter);
        stats.registerStatsFilter(stmtStatsBackFilter);
        stats.registerStatsFilter(stmtStatsFrontFilter);
        int skipped = 0; // number of skipped entries
        while (scanner.hasNextLine()) {
            String line = scanner.nextLine();
            LineStat stat = parser.parse(line);
            if (stat != null) {
                stats.add(stat);
            } else {
                skipped++;
            }
        }
        scanner.close();
        
        assertEquals("There are 7 non-PERFLOG entries in the test log file", 7, skipped);
        assertEquals("There are 55 PERFLOG entries in the test log file", 55, stats.getTotalStats());
        
        // Get the front-end queue stats
        QueueStats qstats = stats.getStatsForBucket(qFrontFilter);
        List<QueueStat> qStatList = qstats.getAllStats();
        assertEquals(11, qStatList.size());
        
        QueueStat s = qStatList.get(3); // fetch 4'th line of file
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
        Date expected = dateFormat.parse("2014-08-08T16:07:07.247+0200");
        assertEquals(expected, s.getTimeStamp());
        assertEquals("Expected queue size of 5", 5, s.getSizeValue());
        
        // Get the back-end statement stats
        StatementStats stmtStats = stats.getStatsForBucket(stmtStatsBackFilter);
        assertEquals(14, stmtStats.getTotalNumberOfRecords());
        List<StatementStat> allBackStmtStats = stmtStats.getAllStats();
        // DB_WRITE(1) REMOVE backend-info WHERE 'name' = ?s|532724 ns
        StatementStat stmtStatItem = allBackStmtStats.get(3);
        assertTrue(stmtStatItem instanceof WriteStatementStat);
        assertEquals(532724, stmtStatItem.getExecTime().getVal());
        assertEquals(TimeUnit.NANOSECONDS, stmtStatItem.getExecTime().getTimeUnit());
        String expectedDescr = "REMOVE backend-info WHERE 'name' = ?s";
        assertEquals(expectedDescr, stmtStatItem.getDescriptor());
        
        // Make sure we've parsed all stats in log-file
        StatementStats stmtStatsFront = stats.getStatsForBucket(stmtStatsFrontFilter);
        QueueStats queueStatsBack = stats.getStatsForBucket(qBackFilter);
        /* We have 55 stats in total (already asserted)
         *   11 front-end queue size stats (already asserted)
         *   15 back-end queue size stats
         *   15 front-end statement stats
         *   14 back-end statement stats (already asserted)
         */
        assertEquals("Expected 15 front-end stmt stats", 15, stmtStatsFront.getTotalNumberOfRecords());
        assertEquals("Expected 15 back-end queue size stats", 15, queueStatsBack.getTotalNumberOfRecords());
    }

    @Test
    public void canExtractMessageFromLogEntry() {
        StatsParserImpl parser = (StatsParserImpl)StatsParserBuilder.build();

        String logEntry = "PERFLOG - CountingDecorator: 2014-07-24T19:18:04.124-0600|0|CountingDecorator|Q_SIZE 2";
        String expectedMessage = "2014-07-24T19:18:04.124-0600|0|CountingDecorator|Q_SIZE 2";
        String message = parser.extractMessage(logEntry);
        assertEquals(expectedMessage, message);

        logEntry = "PERFLOG FooFooFoo: MessageMessageMessage";
        expectedMessage = "MessageMessageMessage";
        message = parser.extractMessage(logEntry);
        assertEquals(expectedMessage, message);
    }

    @Test
    public void ignoreNonPerfLogEntries() {
        StatsParserImpl parser = (StatsParserImpl)StatsParserBuilder.build();

        String logEntry = "WARNING - CountingDecorator: 2014-07-24T19:18:04.124-0600|0|CountingDecorator|Q_SIZE 2";
        LineStat stat = parser.parse(logEntry);
        assertNull(stat);

        logEntry = "Any String that does not start with \"PERFLOG\" should not parse.";
        stat = parser.parse(logEntry);
        assertNull(stat);
    }
    
    private static String decodeFilePath(URL url) {
        try {
            // Spaces are encoded as %20 in URLs. Use URLDecoder.decode() so
            // as to handle cases like that.
            return URLDecoder.decode(url.getFile(), "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new AssertionError("UTF-8 not supported, huh?");
        }
    }
}
