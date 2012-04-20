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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collection;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.redhat.thermostat.cli.AppContextSetup;
import com.redhat.thermostat.cli.ArgumentSpec;
import com.redhat.thermostat.cli.CommandContext;
import com.redhat.thermostat.cli.CommandContextFactory;
import com.redhat.thermostat.cli.CommandException;
import com.redhat.thermostat.cli.SimpleArgumentSpec;
import com.redhat.thermostat.cli.SimpleArguments;
import com.redhat.thermostat.common.appctx.ApplicationContext;
import com.redhat.thermostat.common.appctx.ApplicationContextUtil;
import com.redhat.thermostat.common.dao.DAOFactory;
import com.redhat.thermostat.common.dao.HostInfoDAO;
import com.redhat.thermostat.common.dao.HostRef;
import com.redhat.thermostat.common.dao.VmInfoDAO;
import com.redhat.thermostat.common.dao.VmRef;
import com.redhat.thermostat.test.TestCommandContextFactory;

public class ListVMsCommandTest {

    private ListVMsCommand cmd;
    private AppContextSetup appContextSetup;
    private TestCommandContextFactory cmdCtxFactory;
    private HostInfoDAO hostsDAO;
    private VmInfoDAO vmsDAO;

    @Before
    public void setUp() {
        ApplicationContextUtil.resetApplicationContext();
        setupCommandContextFactory();

        cmd = new ListVMsCommand();

        setupDAOs();

    }

    private void setupCommandContextFactory() {
        appContextSetup = mock(AppContextSetup.class);
        cmdCtxFactory = new TestCommandContextFactory() {
            @Override
            protected AppContextSetup getAppContextSetup() {
                return appContextSetup;
            }
        };
        CommandContextFactory.setInstance(cmdCtxFactory);
    }

    private void setupDAOs() {
        hostsDAO = mock(HostInfoDAO.class);
        vmsDAO = mock(VmInfoDAO.class);
        DAOFactory daoFactory = mock(DAOFactory.class);
        when(daoFactory.getHostInfoDAO()).thenReturn(hostsDAO);
        when(daoFactory.getVmInfoDAO()).thenReturn(vmsDAO);
        ApplicationContext.getInstance().setDAOFactory(daoFactory);
    }

    @After
    public void tearDown() {
        vmsDAO = null;
        hostsDAO = null;
        cmdCtxFactory = null;
        cmd = null;
        appContextSetup = null;
        CommandContextFactory.setInstance(new CommandContextFactory());
        ApplicationContextUtil.resetApplicationContext();
    }

    @Test
    public void verifyOutputFormatOneLine() throws CommandException {

        HostRef host1 = new HostRef("123", "h1");
        when(hostsDAO.getHosts()).thenReturn(Arrays.asList(host1));
        when(vmsDAO.getVMs(host1)).thenReturn(Arrays.asList(new VmRef(host1, 1, "n")));

        SimpleArguments args = new SimpleArguments();
        args.addArgument("--dbUrl", "fluff");
        CommandContext ctx = cmdCtxFactory.createContext(args);

        cmd.run(ctx);

        String output = cmdCtxFactory.getOutput();
        assertEquals("HOST VM_ID VM_NAME\n" +
                     "h1   1     n\n", output);
    }

    @Test
    public void verifyOutputFormatMultiLines() throws CommandException {

        HostRef host1 = new HostRef("123", "h1");
        HostRef host2 = new HostRef("456", "longhostname");
        when(hostsDAO.getHosts()).thenReturn(Arrays.asList(host1, host2));

        when(vmsDAO.getVMs(host1)).thenReturn(Arrays.asList(new VmRef(host1, 1, "n"), new VmRef(host1, 2, "n1")));
        when(vmsDAO.getVMs(host2)).thenReturn(Arrays.asList(new VmRef(host2, 123456, "longvmname")));

        SimpleArguments args = new SimpleArguments();
        args.addArgument("--dbUrl", "fluff");
        CommandContext ctx = cmdCtxFactory.createContext(args);

        cmd.run(ctx);

        String output = cmdCtxFactory.getOutput();
        assertEquals("HOST         VM_ID  VM_NAME\n" +
                     "h1           1      n\n" +
                     "h1           2      n1\n" +
                     "longhostname 123456 longvmname\n", output);
    }

    @Test
    public void testName() {
        assertEquals("list-vms", cmd.getName());
    }

    @Test
    public void testDescription() {
        assertEquals("lists all currently monitored VMs", cmd.getDescription());
    }

    @Test
    public void testUsage() {
        String expected = "lists all currently monitored VMs";

        assertEquals(expected, cmd.getUsage());
    }

    @Test
    public void testAcceptedArguments() {
        Collection<ArgumentSpec> args = cmd.getAcceptedArguments();
        assertNotNull(args);
        assertTrue(args.isEmpty());
    }
}
