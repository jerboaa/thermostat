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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import com.redhat.thermostat.common.ApplicationService;
import com.redhat.thermostat.common.Timer;
import com.redhat.thermostat.common.cli.CommandException;
import com.redhat.thermostat.common.cli.SimpleArguments;
import com.redhat.thermostat.common.dao.HostRef;
import com.redhat.thermostat.common.dao.VmRef;
import com.redhat.thermostat.common.utils.OSGIUtils;
import com.redhat.thermostat.storage.model.VmCpuStat;
import com.redhat.thermostat.storage.model.VmMemoryStat;
import com.redhat.thermostat.storage.model.VmMemoryStat.Generation;
import com.redhat.thermostat.storage.model.VmMemoryStat.Space;
import com.redhat.thermostat.test.TestCommandContextFactory;
import com.redhat.thermostat.test.TestTimerFactory;
import com.redhat.thermostat.vm.cpu.common.VmCpuStatDAO;
import com.redhat.thermostat.vm.memory.common.VmMemoryStatDAO;

public class VmStatCommandTest {

    private static Locale defaultLocale;
    private static TimeZone defaultTimeZone;

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

    private VMStatCommand cmd;
    private VmCpuStatDAO vmCpuStatDAO;
    private TestCommandContextFactory cmdCtxFactory;
    private VmMemoryStatDAO vmMemoryStatDAO;
    private TestTimerFactory timerFactory;

    @Before
    public void setUp() {
        timerFactory = new TestTimerFactory();
        ApplicationService appSvc = mock(ApplicationService.class);
        when(appSvc.getTimerFactory()).thenReturn(timerFactory);
        setupCommandContextFactory();

        setupDAOs();

        OSGIUtils serviceProvider = mock(OSGIUtils.class);
        when(serviceProvider.getServiceAllowNull(VmCpuStatDAO.class)).thenReturn(vmCpuStatDAO);
        when(serviceProvider.getServiceAllowNull(VmMemoryStatDAO.class)).thenReturn(vmMemoryStatDAO);
        when(serviceProvider.getService(ApplicationService.class)).thenReturn(appSvc);

        cmd = new VMStatCommand(serviceProvider);
    }

    @After
    public void tearDown() {
        vmCpuStatDAO = null;
        cmdCtxFactory = null;
        cmd = null;
        timerFactory = null;
    }

    private void setupCommandContextFactory() {
        cmdCtxFactory = new TestCommandContextFactory();
    }

    private void setupDAOs() {
        vmCpuStatDAO = mock(VmCpuStatDAO.class);
        int vmId = 234;
        HostRef host = new HostRef("123", "dummy");
        VmRef vm = new VmRef(host, 234, "dummy");
        VmCpuStat cpustat1 = new VmCpuStat(2, vmId, 65);
        VmCpuStat cpustat2 = new VmCpuStat(3, vmId, 70);
        List<VmCpuStat> cpuStats = Arrays.asList(cpustat1, cpustat2);
        List<VmCpuStat> cpuStats2 = Collections.emptyList();
        when(vmCpuStatDAO.getLatestVmCpuStats(vm, Long.MIN_VALUE)).thenReturn(cpuStats).thenReturn(cpuStats2);

        VmMemoryStat.Space space1_1_1 = newSpace("space1", 123456, 12345, 1, 0);
        VmMemoryStat.Space space1_1_2 = newSpace("space2", 123456, 12345, 1, 0);
        VmMemoryStat.Space[] spaces1_1 = new VmMemoryStat.Space[] { space1_1_1, space1_1_2 };
        VmMemoryStat.Generation gen1_1 = newGeneration("gen1", "col1", 123456, 12345, spaces1_1);

        VmMemoryStat.Space space1_2_1 = newSpace("space3", 123456, 12345, 1, 0);
        VmMemoryStat.Space space1_2_2 = newSpace("space4", 123456, 12345, 1, 0);
        VmMemoryStat.Space[] spaces1_2 = new VmMemoryStat.Space[] { space1_2_1, space1_2_2 };
        VmMemoryStat.Generation gen1_2 = newGeneration("gen2", "col1", 123456, 12345, spaces1_2);

        VmMemoryStat.Generation[] gens1 = new VmMemoryStat.Generation[] { gen1_1, gen1_2 };

        VmMemoryStat memStat1 = new VmMemoryStat(1, vmId, gens1);

        VmMemoryStat.Space space2_1_1 = newSpace("space1", 123456, 12345, 2, 0);
        VmMemoryStat.Space space2_1_2 = newSpace("space2", 123456, 12345, 2, 0);
        VmMemoryStat.Space[] spaces2_1 = new VmMemoryStat.Space[] { space2_1_1, space2_1_2 };
        VmMemoryStat.Generation gen2_1 = newGeneration("gen1", "col1", 123456, 12345, spaces2_1);

        VmMemoryStat.Space space2_2_1 = newSpace("space3", 123456, 12345, 3, 0);
        VmMemoryStat.Space space2_2_2 = newSpace("space4", 123456, 12345, 4, 0);
        VmMemoryStat.Space[] spaces2_2 = new VmMemoryStat.Space[] { space2_2_1, space2_2_2 };
        VmMemoryStat.Generation gen2_2 = newGeneration("gen2", "col1", 123456, 12345, spaces2_2);

        VmMemoryStat.Generation[] gens2 = new VmMemoryStat.Generation[] { gen2_1, gen2_2 };

        VmMemoryStat memStat2 = new VmMemoryStat(2, vmId, gens2);

        VmMemoryStat.Space space3_1_1 = newSpace("space1", 123456, 12345, 4, 0);
        VmMemoryStat.Space space3_1_2 = newSpace("space2", 123456, 12345, 5, 0);
        VmMemoryStat.Space[] spaces3_1 = new VmMemoryStat.Space[] { space3_1_1, space3_1_2 };
        VmMemoryStat.Generation gen3_1 = newGeneration("gen1", "col1", 123456, 12345, spaces3_1);

        VmMemoryStat.Space space3_2_1 = newSpace("space3", 123456, 12345, 6, 0);
        VmMemoryStat.Space space3_2_2 = newSpace("space4", 123456, 12345, 7, 0);
        VmMemoryStat.Space[] spaces3_2 = new VmMemoryStat.Space[] { space3_2_1, space3_2_2 };
        VmMemoryStat.Generation gen3_2 = newGeneration("gen2", "col1", 123456, 12345, spaces3_2);

        VmMemoryStat.Generation[] gens3 = new VmMemoryStat.Generation[] { gen3_1, gen3_2 };

        VmMemoryStat memStat3 = new VmMemoryStat(3, vmId, gens3);

        VmMemoryStat.Space space4_1_1 = newSpace("space1", 123456, 12345, 8, 0);
        VmMemoryStat.Space space4_1_2 = newSpace("space2", 123456, 12345, 9, 0);
        VmMemoryStat.Space[] spaces4_1 = new VmMemoryStat.Space[] { space4_1_1, space4_1_2 };
        VmMemoryStat.Generation gen4_1 = newGeneration("gen4", "col1", 123456, 12345, spaces4_1);

        VmMemoryStat.Space space4_2_1 = newSpace("space3", 123456, 12345, 10, 0);
        VmMemoryStat.Space space4_2_2 = newSpace("space4", 123456, 12345, 11, 0);
        VmMemoryStat.Space[] spaces4_2 = new VmMemoryStat.Space[] { space4_2_1, space4_2_2 };
        VmMemoryStat.Generation gen4_2 = newGeneration("gen4", "col1", 123456, 12345, spaces4_2);

        VmMemoryStat.Generation[] gens4 = new VmMemoryStat.Generation[] { gen4_1, gen4_2 };

        VmMemoryStat memStat4 = new VmMemoryStat(4, vmId, gens4);

        vmMemoryStatDAO = mock(VmMemoryStatDAO.class);
        when(vmMemoryStatDAO.getLatestVmMemoryStats(vm, Long.MIN_VALUE))
            .thenReturn(Arrays.asList(memStat1, memStat2, memStat3));

        when(vmMemoryStatDAO.getLatestVmMemoryStats(vm, memStat3.getTimeStamp())).thenReturn(Arrays.asList(memStat4));

    }

    private Space newSpace(String name, long maxCapacity, long capacity, long used, int index) {
        VmMemoryStat.Space space = new VmMemoryStat.Space();
        space.setName(name);
        space.setMaxCapacity(maxCapacity);
        space.setCapacity(capacity);
        space.setUsed(used);
        space.setIndex(index);
        return space;
    }

    private Generation newGeneration(String name, String collector, long maxCapacity, long capacity, Space[] spaces) {
        VmMemoryStat.Generation gen = new VmMemoryStat.Generation();
        gen.setName(name);
        gen.setCollector(collector);
        gen.setMaxCapacity(capacity);
        gen.setSpaces(spaces);
        return gen;
    }

    @Test
    public void testBasicCPUMemory() throws CommandException {
        SimpleArguments args = new SimpleArguments();
        args.addArgument("vmId", "234");
        args.addArgument("hostId", "123");
        cmd.run(cmdCtxFactory.createContext(args));
        String expected = "TIME        %CPU MEM.space1 MEM.space2 MEM.space3 MEM.space4\n" +
                          "12:00:00 AM      1 B        1 B        1 B        1 B\n" +
                          "12:00:00 AM 65.0 2 B        2 B        3 B        4 B\n" +
                          "12:00:00 AM 70.0 4 B        5 B        6 B        7 B\n";
        assertEquals(expected, cmdCtxFactory.getOutput());

    }

    @Test
    public void testContinuousMode() throws CommandException {
        
        Thread t = new Thread() {
            public void run() {
                SimpleArguments args = new SimpleArguments();
                args.addArgument("vmId", "234");
                args.addArgument("hostId", "123");
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
        String expected = "TIME        %CPU MEM.space1 MEM.space2 MEM.space3 MEM.space4\n" +
                          "12:00:00 AM      1 B        1 B        1 B        1 B\n" +
                          "12:00:00 AM 65.0 2 B        2 B        3 B        4 B\n" +
                          "12:00:00 AM 70.0 4 B        5 B        6 B        7 B\n";
        assertEquals(expected, cmdCtxFactory.getOutput());
        assertEquals(1, timerFactory.getDelay());
        assertEquals(1, timerFactory.getInitialDelay());
        assertEquals(TimeUnit.SECONDS, timerFactory.getTimeUnit());
        assertEquals(Timer.SchedulingType.FIXED_RATE, timerFactory.getSchedulingType());

        timerFactory.getAction().run();

        expected = "TIME        %CPU MEM.space1 MEM.space2 MEM.space3 MEM.space4\n" +
                   "12:00:00 AM      1 B        1 B        1 B        1 B\n" +
                   "12:00:00 AM 65.0 2 B        2 B        3 B        4 B\n" +
                   "12:00:00 AM 70.0 4 B        5 B        6 B        7 B\n" +
                   "12:00:00 AM 70.0 8 B        9 B        10 B       11 B\n";
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
    public void testName() {
        assertEquals("vm-stat", cmd.getName());
    }

    @Test
    public void testDescAndUsage() {
        assertNotNull(cmd.getUsage());
        assertNotNull(cmd.getUsage());
    }

    @Ignore
    @Test
    public void testOptions() {
        Options options = cmd.getOptions();
        assertNotNull(options);
        assertEquals(3, options.getOptions().size());

        assertTrue(options.hasOption("vmId"));
        Option vm = options.getOption("vmId");
        assertEquals("the ID of the VM to monitor", vm.getDescription());
        assertTrue(vm.isRequired());
        assertTrue(vm.hasArg());

        assertTrue(options.hasOption("hostId"));
        Option host = options.getOption("hostId");
        assertEquals("the ID of the host to monitor", host.getDescription());
        assertTrue(host.isRequired());
        assertTrue(host.hasArg());

        assertTrue(options.hasOption("continuous"));
        Option cont = options.getOption("continuous");
        assertEquals("c", cont.getOpt());
        assertEquals("print data continuously", cont.getDescription());
        assertFalse(cont.isRequired());
        assertFalse(cont.hasArg());
    }

    @Test
    public void testStorageRequired() {
        assertTrue(cmd.isStorageRequired());
    }
}
