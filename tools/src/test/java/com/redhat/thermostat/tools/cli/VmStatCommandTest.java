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
import com.redhat.thermostat.common.dao.VmRef;
import com.redhat.thermostat.common.model.VmCpuStat;
import com.redhat.thermostat.test.TestCommandContextFactory;

public class VmStatCommandTest {

    private static Locale defaultLocale;

    @BeforeClass
    public static void setUpClass() {
        defaultLocale = Locale.getDefault();
        Locale.setDefault(Locale.US);
    }

    @AfterClass
    public static void tearDownClass() {
        Locale.setDefault(defaultLocale);
    }

    private VMStatCommand cmd;
    private VmCpuStatDAO vmCpuStatDAO;
    private AppContextSetup appContextSetup;
    private TestCommandContextFactory cmdCtxFactory;

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
        VmCpuStat cpustat1 = new VmCpuStat(123, vmId, 50.123454);
        VmCpuStat cpustat2 = new VmCpuStat(123, vmId, 65);
        VmCpuStat cpustat3 = new VmCpuStat(123, vmId, 70);
        List<VmCpuStat> cpuStats = Arrays.asList(cpustat1, cpustat2, cpustat3);
        when(vmCpuStatDAO.getLatestVmCpuStats(vm)).thenReturn(cpuStats);
        DAOFactory daoFactory = mock(DAOFactory.class);
        when(daoFactory.getVmCpuStatDAO()).thenReturn(vmCpuStatDAO);
        ApplicationContext.getInstance().setDAOFactory(daoFactory);
    }

    @Test
    public void testBasicCPU() throws CommandException {
        SimpleArguments args = new SimpleArguments();
        args.addArgument("vmId", "234");
        args.addArgument("hostId", "123");
        cmd.run(cmdCtxFactory.createContext(args));
        String expected = "%CPU\n50.1\n65.0\n70.0\n";
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
