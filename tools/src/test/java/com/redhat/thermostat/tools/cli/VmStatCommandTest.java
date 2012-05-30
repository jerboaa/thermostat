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

package com.redhat.thermostat.tools.cli;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.redhat.thermostat.common.appctx.ApplicationContext;
import com.redhat.thermostat.common.appctx.ApplicationContextUtil;
import com.redhat.thermostat.common.cli.AppContextSetup;
import com.redhat.thermostat.common.cli.ArgumentSpec;
import com.redhat.thermostat.common.cli.CommandException;
import com.redhat.thermostat.common.cli.SimpleArgumentSpec;
import com.redhat.thermostat.common.cli.SimpleArguments;
import com.redhat.thermostat.common.dao.DAOFactory;
import com.redhat.thermostat.common.dao.HostRef;
import com.redhat.thermostat.common.dao.VmCpuStatDAO;
import com.redhat.thermostat.common.dao.VmMemoryStatDAO;
import com.redhat.thermostat.common.dao.VmRef;
import com.redhat.thermostat.common.model.VmCpuStat;
import com.redhat.thermostat.common.model.VmMemoryStat;
import com.redhat.thermostat.common.model.VmMemoryStat.Generation;
import com.redhat.thermostat.common.model.VmMemoryStat.Space;
import com.redhat.thermostat.test.TestCommandContextFactory;

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
    private AppContextSetup appContextSetup;
    private TestCommandContextFactory cmdCtxFactory;
    private VmMemoryStatDAO vmMemoryStatDAO;

    @Before
    public void setUp() {
        ApplicationContextUtil.resetApplicationContext();
        setupCommandContextFactory();

        cmd = new VMStatCommand();

        setupDAOs();

    }

    @After
    public void tearDown() {

        vmCpuStatDAO = null;
        appContextSetup = null;
        cmdCtxFactory = null;
        cmd = null;
        ApplicationContextUtil.resetApplicationContext();
    }

    private void setupCommandContextFactory() {
        appContextSetup = mock(AppContextSetup.class);
        cmdCtxFactory = new TestCommandContextFactory() {
            @Override
            protected AppContextSetup getAppContextSetup() {
                return appContextSetup;
            }
        };
    }

    private void setupDAOs() {
        vmCpuStatDAO = mock(VmCpuStatDAO.class);
        int vmId = 234;
        HostRef host = new HostRef("123", "dummy");
        VmRef vm = new VmRef(host, 234, "dummy");
        VmCpuStat cpustat1 = new VmCpuStat(2, vmId, 65);
        VmCpuStat cpustat2 = new VmCpuStat(3, vmId, 70);
        List<VmCpuStat> cpuStats = Arrays.asList(cpustat1, cpustat2);
        when(vmCpuStatDAO.getLatestVmCpuStats(vm)).thenReturn(cpuStats);
        DAOFactory daoFactory = mock(DAOFactory.class);
        when(daoFactory.getVmCpuStatDAO()).thenReturn(vmCpuStatDAO);
        ApplicationContext.getInstance().setDAOFactory(daoFactory);

        VmMemoryStat.Space space1_1_1 = newSpace("space1", 123456, 12345, 1, 0);
        VmMemoryStat.Space space1_1_2 = newSpace("space2", 123456, 12345, 1, 0);
        List<VmMemoryStat.Space> spaces1_1 = Arrays.asList(space1_1_1, space1_1_2);
        VmMemoryStat.Generation gen1_1 = newGeneration("gen1", "col1", 123456, 12345, spaces1_1);

        VmMemoryStat.Space space1_2_1 = newSpace("space3", 123456, 12345, 1, 0);
        VmMemoryStat.Space space1_2_2 = newSpace("space4", 123456, 12345, 1, 0);
        List<VmMemoryStat.Space> spaces1_2 = Arrays.asList(space1_2_1, space1_2_2);
        VmMemoryStat.Generation gen1_2 = newGeneration("gen2", "col1", 123456, 12345, spaces1_2);

        List<VmMemoryStat.Generation> gens1 = Arrays.asList(gen1_1, gen1_2);

        VmMemoryStat memStat1 = new VmMemoryStat(1, vmId, gens1);

        VmMemoryStat.Space space2_1_1 = newSpace("space1", 123456, 12345, 2, 0);
        VmMemoryStat.Space space2_1_2 = newSpace("space2", 123456, 12345, 2, 0);
        List<VmMemoryStat.Space> spaces2_1 = Arrays.asList(space2_1_1, space2_1_2);
        VmMemoryStat.Generation gen2_1 = newGeneration("gen1", "col1", 123456, 12345, spaces2_1);

        VmMemoryStat.Space space2_2_1 = newSpace("space3", 123456, 12345, 3, 0);
        VmMemoryStat.Space space2_2_2 = newSpace("space4", 123456, 12345, 4, 0);
        List<VmMemoryStat.Space> spaces2_2 = Arrays.asList(space2_2_1, space2_2_2);
        VmMemoryStat.Generation gen2_2 = newGeneration("gen2", "col1", 123456, 12345, spaces2_2);

        List<VmMemoryStat.Generation> gens2 = Arrays.asList(gen2_1, gen2_2);

        VmMemoryStat memStat2 = new VmMemoryStat(2, vmId, gens2);

        VmMemoryStat.Space space3_1_1 = newSpace("space1", 123456, 12345, 4, 0);
        VmMemoryStat.Space space3_1_2 = newSpace("space2", 123456, 12345, 5, 0);
        List<VmMemoryStat.Space> spaces3_1 = Arrays.asList(space3_1_1, space3_1_2);
        VmMemoryStat.Generation gen3_1 = newGeneration("gen1", "col1", 123456, 12345, spaces3_1);

        VmMemoryStat.Space space3_2_1 = newSpace("space3", 123456, 12345, 6, 0);
        VmMemoryStat.Space space3_2_2 = newSpace("space4", 123456, 12345, 7, 0);
        List<VmMemoryStat.Space> spaces3_2 = Arrays.asList(space3_2_1, space3_2_2);
        VmMemoryStat.Generation gen3_2 = newGeneration("gen2", "col1", 123456, 12345, spaces3_2);

        List<VmMemoryStat.Generation> gens3 = Arrays.asList(gen3_1, gen3_2);

        VmMemoryStat memStat3 = new VmMemoryStat(3, vmId, gens3);

        vmMemoryStatDAO = mock(VmMemoryStatDAO.class);
        when(vmMemoryStatDAO.getLatestVmMemoryStats(vm)).thenReturn(Arrays.asList(memStat1, memStat2, memStat3));
        when(daoFactory.getVmMemoryStatDAO()).thenReturn(vmMemoryStatDAO);
    }

    private Space newSpace(String name, long maxCapacity, long capacity, long used, int index) {
        VmMemoryStat.Space space = new VmMemoryStat.Space();
        space.name = name;
        space.maxCapacity = maxCapacity;
        space.capacity = capacity;
        space.used = used;
        space.index = index;
        return space;
    }

    private Generation newGeneration(String name, String collector, long maxCapacity, long capacity, List<Space> spaces) {
        VmMemoryStat.Generation gen = new VmMemoryStat.Generation();
        gen.name = name;
        gen.collector = collector;
        gen.maxCapacity = capacity;
        gen.spaces = spaces;
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
    public void testName() {
        assertEquals("vm-stat", cmd.getName());
    }

    @Test
    public void testDescription() {
        assertEquals("show various statistics about a VM", cmd.getDescription());
    }

    @Test
    public void testUsage() {
        assertEquals("show various statistics about a VM", cmd.getUsage());
    }

    @Test
    public void testAcceptedArguments() {
        Collection<ArgumentSpec> args = cmd.getAcceptedArguments();
        assertNotNull(args);
        assertEquals(2, args.size());
        assertTrue(args.contains(new SimpleArgumentSpec("vmId", "the ID of the VM to monitor", true, true)));
        assertTrue(args.contains(new SimpleArgumentSpec("hostId", "the ID of the host to monitor", true, true)));
    }

    @Test
    public void testStorageRequired() {
        assertTrue(cmd.isStorageRequired());
    }
}
