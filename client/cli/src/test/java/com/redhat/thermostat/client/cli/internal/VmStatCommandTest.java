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

package com.redhat.thermostat.client.cli.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.redhat.thermostat.client.cli.VMStatPrintDelegate;
import com.redhat.thermostat.client.cli.VmArgument;
import com.redhat.thermostat.common.ApplicationService;
import com.redhat.thermostat.common.Timer;
import com.redhat.thermostat.common.cli.CommandException;
import com.redhat.thermostat.common.cli.SimpleArguments;
import com.redhat.thermostat.storage.core.AgentId;
import com.redhat.thermostat.storage.core.VmId;
import com.redhat.thermostat.storage.dao.VmInfoDAO;
import com.redhat.thermostat.storage.model.TimeStampedPojo;
import com.redhat.thermostat.storage.model.VmInfo;
import com.redhat.thermostat.test.TestCommandContextFactory;
import com.redhat.thermostat.test.TestTimerFactory;
import com.redhat.thermostat.testutils.StubBundleContext;

public class VmStatCommandTest {
    private static Locale defaultLocale;
    private static TimeZone defaultTimeZone;
    private static int NUM_ROWS = 3;

    @BeforeClass
    public static void setUpClass() {
        defaultLocale = Locale.getDefault();
        Locale.setDefault(Locale.US);
        defaultTimeZone = TimeZone.getDefault();
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
    }

    @AfterClass
    public static void tearDownClass() {
        TimeZone.setDefault(defaultTimeZone);
        Locale.setDefault(defaultLocale);
    }

    private VMStatPrintDelegate[] delegates;
    private TestCommandContextFactory cmdCtxFactory;
    private TestTimerFactory timerFactory;
    private ApplicationService appSvc;
    private VmInfo vmInfo;
    private StubBundleContext context;
    private VMStatCommand cmd;

    @Before
    public void setUp() {
        vmInfo = new VmInfo("123", "vmId", 234, 0, 0, null, null, null, null, null, null, null,
                null, null, null, null, 0, "myUsername");
        context = new StubBundleContext();
        cmd = new VMStatCommand(context);

        VmInfoDAO vmInfoDAO = mock(VmInfoDAO.class);
        cmd.setVmInfoDAO(vmInfoDAO);
        when(vmInfoDAO.getVmInfo(any(VmId.class))).thenReturn(vmInfo);

        delegates = new VMStatPrintDelegate[2];
        final String[][] headers = {
                { "FIRST", "SECOND", "THIRD" }, 
                { "FOURTH", "FIFTH" } };
        
        final String[][][] rows = {
                {
                    { "1", "2", "3" },
                    { "6", "7", "8" },
                    { "11", "12", "13" },
                },
                {
                    { "4", "5" },
                    { "9", "10" },
                    { "14", "15" }
                }
        };
        timerFactory = new TestTimerFactory();
        appSvc = mock(ApplicationService.class);
        when(appSvc.getTimerFactory()).thenReturn(timerFactory);
        setupCommandContextFactory();
        
        delegates[0] = mockDelegate(headers[0], rows[0]);
        delegates[1] = mockDelegate(headers[1], rows[1]);
    }
    
    private VMStatPrintDelegate mockDelegate(String[] headers, String[][] data) {
        VMStatPrintDelegate delegate = mock(VMStatPrintDelegate.class);
        List<TimeStampedPojo> stats = new ArrayList<>();
        for (int i = 0; i < NUM_ROWS; i++) {
            TimeStampedPojo stat = mock(TimeStampedPojo.class);
            when(stat.getTimeStamp()).thenReturn(i * 1000L); // Increment by one second
            stats.add(stat);
        }
        
        // Need this syntax due to generics
        doReturn(stats).when(delegate).getLatestStats(any(AgentId.class), any(VmId.class), eq(Long.MIN_VALUE));
        when(delegate.getHeaders(stats.get(0))).thenReturn(Arrays.asList(headers));
        for (int i = 0; i < data.length; i++) {
            List<String> row = Arrays.asList(data[i]);
            doReturn(row).when(delegate).getStatRow(eq(stats.get(i)));
        }
        
        return delegate;
    }

    @After
    public void tearDown() {
        cmdCtxFactory = null;
        timerFactory = null;
    }

    private void setupCommandContextFactory() {
        cmdCtxFactory = new TestCommandContextFactory();
    }

    @Test
    public void testOutput() throws CommandException {
        context.registerService(VMStatPrintDelegate.class.getName(), delegates[0], null);
        context.registerService(VMStatPrintDelegate.class.getName(), delegates[1], null);
        
        cmd.run(cmdCtxFactory.createContext(setupArguments()));
        String expected = "TIME        FIRST SECOND THIRD FOURTH FIFTH\n"
                + "12:00:00 AM 1     2      3     4      5\n"
                + "12:00:01 AM 6     7      8     9      10\n"
                + "12:00:02 AM 11    12     13    14     15\n";
        assertEquals(expected, cmdCtxFactory.getOutput());
    }

    @Test
    public void testNoDelegates() throws CommandException {
        cmd.run(cmdCtxFactory.createContext(setupArguments()));
        String expected = "TIME\n";
        assertEquals(expected, cmdCtxFactory.getOutput());
    }

    @Test
    public void testContinuousMode() throws CommandException {
        final String[][] data = {
                { "16", "17", "18" },
                { "19", "20" }
        };
        context.registerService(ApplicationService.class.getName(), appSvc, null);
        context.registerService(VMStatPrintDelegate.class.getName(), delegates[0], null);
        context.registerService(VMStatPrintDelegate.class.getName(), delegates[1], null);
        
        // Add one more stat
        TimeStampedPojo stat = mock(TimeStampedPojo.class);
        // One second after previous timestamps
        when(stat.getTimeStamp()).thenReturn(3000L);
        List<TimeStampedPojo> stats = new ArrayList<>();
        stats.add(stat);
        
        doReturn(stats).when(delegates[0]).getLatestStats(any(AgentId.class), any(VmId.class), eq(2000L));
        doReturn(stats).when(delegates[1]).getLatestStats(any(AgentId.class), any(VmId.class), eq(2000L));
        doReturn(Arrays.asList(data[0])).when(delegates[0]).getStatRow(eq(stat));
        doReturn(Arrays.asList(data[1])).when(delegates[1]).getStatRow(eq(stat));

        Thread t = new Thread() {
            public void run() {
                SimpleArguments args = setupArguments();
                args.addArgument("continuous", "true");
                try {
                    cmd.run(cmdCtxFactory.createContext(args));
                } catch (CommandException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        };
        t.start();
        try {
            Thread.sleep(200);
        } catch (InterruptedException e) {
            return;
        }
        assertTrue(timerFactory.isActive());
        String expected = "TIME        FIRST SECOND THIRD FOURTH FIFTH\n" +
                "12:00:00 AM 1     2      3     4      5\n" +
                "12:00:01 AM 6     7      8     9      10\n" +
                "12:00:02 AM 11    12     13    14     15\n";
        assertEquals(expected, cmdCtxFactory.getOutput());
        assertEquals(1, timerFactory.getDelay());
        assertEquals(1, timerFactory.getInitialDelay());
        assertEquals(TimeUnit.SECONDS, timerFactory.getTimeUnit());
        assertEquals(Timer.SchedulingType.FIXED_RATE, timerFactory.getSchedulingType());

        timerFactory.getAction().run();

        expected = "TIME        FIRST SECOND THIRD FOURTH FIFTH\n" +
                "12:00:00 AM 1     2      3     4      5\n" +
                "12:00:01 AM 6     7      8     9      10\n" +
                "12:00:02 AM 11    12     13    14     15\n" +
                "12:00:03 AM 16    17     18    19     20\n";
        assertEquals(expected, cmdCtxFactory.getOutput());
        cmdCtxFactory.setInput(" ");
        try {
            Thread.sleep(200);
        } catch (InterruptedException e) {
            return;
        }
        assertFalse(timerFactory.isActive());
    }

    @Test
    public void testNoStats() throws CommandException {
        // Fail stats != null check
        VMStatPrintDelegate badDelegate = mock(VMStatPrintDelegate.class);
        when(badDelegate.getLatestStats(any(AgentId.class), any(VmId.class), anyLong())).thenReturn(null);

        context.registerService(VMStatPrintDelegate.class, delegates[0], null);
        context.registerService(VMStatPrintDelegate.class, badDelegate, null);
        context.registerService(VMStatPrintDelegate.class, delegates[1], null);

        cmd.run(cmdCtxFactory.createContext(setupArguments()));
        String expected = "TIME        FIRST SECOND THIRD FOURTH FIFTH\n"
                + "12:00:00 AM 1     2      3     4      5\n"
                + "12:00:01 AM 6     7      8     9      10\n"
                + "12:00:02 AM 11    12     13    14     15\n";
        assertEquals(expected, cmdCtxFactory.getOutput());
    }

    @Test
    public void testNoHeaders() throws CommandException {
        // Pass stats check, but fail headers check
        VMStatPrintDelegate badDelegate = mock(VMStatPrintDelegate.class);
        TimeStampedPojo stat = mock(TimeStampedPojo.class);
        doReturn(Arrays.asList(stat)).when(badDelegate).getLatestStats(any(AgentId.class),
                any(VmId.class), anyLong());
        when(badDelegate.getHeaders(any(TimeStampedPojo.class))).thenReturn(null);

        context.registerService(VMStatPrintDelegate.class, delegates[0], null);
        context.registerService(VMStatPrintDelegate.class, badDelegate, null);
        context.registerService(VMStatPrintDelegate.class, delegates[1], null);

        cmd.run(cmdCtxFactory.createContext(setupArguments()));
        String expected = "TIME        FIRST SECOND THIRD FOURTH FIFTH\n"
                + "12:00:00 AM 1     2      3     4      5\n"
                + "12:00:01 AM 6     7      8     9      10\n"
                + "12:00:02 AM 11    12     13    14     15\n";
        assertEquals(expected, cmdCtxFactory.getOutput());
    }

    @Test
    public void testUnevenStat() throws CommandException {
        // Fewer stats than other delegates
        VMStatPrintDelegate badDelegate = mock(VMStatPrintDelegate.class);
        TimeStampedPojo stat1 = mock(TimeStampedPojo.class);
        when(stat1.getTimeStamp()).thenReturn(1000L);
        TimeStampedPojo stat2 = mock(TimeStampedPojo.class);
        when(stat2.getTimeStamp()).thenReturn(2000L);
        doReturn(Arrays.asList(stat1, stat2)).when(badDelegate).getLatestStats(any(AgentId.class),
                any(VmId.class), anyLong());
        when(badDelegate.getHeaders(any(TimeStampedPojo.class))).thenReturn(Arrays.asList("BAD"));
        when(badDelegate.getStatRow(any(TimeStampedPojo.class))).thenReturn(Arrays.asList("0"));
        
        context.registerService(VMStatPrintDelegate.class, delegates[0], null);
        context.registerService(VMStatPrintDelegate.class, badDelegate, null);
        context.registerService(VMStatPrintDelegate.class, delegates[1], null);
        
        cmd.run(cmdCtxFactory.createContext(setupArguments()));
        String expected = "TIME        FIRST SECOND THIRD BAD FOURTH FIFTH\n"
                + "12:00:00 AM 1     2      3         4      5\n"
                + "12:00:01 AM 6     7      8     0   9      10\n"
                + "12:00:02 AM 11    12     13    0   14     15\n";
        assertEquals(expected, cmdCtxFactory.getOutput());
    }

    @Test
    public void testNoVmId() {
        SimpleArguments args = new SimpleArguments();
        try {
            cmd.run(cmdCtxFactory.createContext(args));
            fail("A CommandException should have been thrown.");
        } catch (CommandException e) {
            assertEquals("A vmId is required", e.getMessage());
        }
    }

    @Test
    public void testStorageRequired() {
        StubBundleContext context = new StubBundleContext();
        VMStatCommand cmd = new VMStatCommand(context);
        assertTrue(cmd.isStorageRequired());
    }

    private SimpleArguments setupArguments() {
        SimpleArguments args = new SimpleArguments();
        args.addArgument(VmArgument.ARGUMENT_NAME, "234");
        args.addArgument("since", "all");
        return args;
    }
}

