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

package com.redhat.thermostat.client.cli.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Arrays;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.redhat.thermostat.common.cli.CommandContext;
import com.redhat.thermostat.common.cli.CommandException;
import com.redhat.thermostat.common.cli.SimpleArguments;
import com.redhat.thermostat.shared.locale.Translate;
import com.redhat.thermostat.storage.core.HostRef;
import com.redhat.thermostat.storage.core.VmRef;
import com.redhat.thermostat.storage.dao.HostInfoDAO;
import com.redhat.thermostat.storage.dao.VmInfoDAO;
import com.redhat.thermostat.storage.model.VmInfo;
import com.redhat.thermostat.test.TestCommandContextFactory;
import com.redhat.thermostat.testutils.StubBundleContext;

public class ListVMsCommandTest {
    
    private static final Translate<LocaleResources> translator = LocaleResources.createLocalizer();

    private ListVMsCommand cmd;
    private TestCommandContextFactory cmdCtxFactory;
    private HostInfoDAO hostsDAO;
    private VmInfoDAO vmsDAO;
    private StubBundleContext context;

    @Before
    public void setUp() {
        setupCommandContextFactory();
        context = new StubBundleContext();

        cmd = new ListVMsCommand(context);

        hostsDAO = mock(HostInfoDAO.class);
        vmsDAO = mock(VmInfoDAO.class);
    }

    private void setupCommandContextFactory() {
        cmdCtxFactory = new TestCommandContextFactory();
    }

    @After
    public void tearDown() {
        vmsDAO = null;
        hostsDAO = null;
        cmdCtxFactory = null;
        cmd = null;
    }

    @Test
    public void verifyOutputFormatOneLine() throws CommandException {
        context.registerService(HostInfoDAO.class, hostsDAO, null);
        context.registerService(VmInfoDAO.class, vmsDAO, null);

        HostRef host1 = new HostRef("123", "h1");
        String vmId = "vmId";
        VmRef vm1 = new VmRef(host1, vmId, 1, "n");
        VmInfo vm1Info = new VmInfo("foo", vmId, 1, 0, 1, "", "", "", "", "", "", "", "", null, null, null, -1, null);
        when(hostsDAO.getHosts()).thenReturn(Arrays.asList(host1));
        when(vmsDAO.getVMs(host1)).thenReturn(Arrays.asList(vm1));
        when(vmsDAO.getVmInfo(eq(vm1))).thenReturn(vm1Info);

        SimpleArguments args = new SimpleArguments();
        args.addArgument("--dbUrl", "fluff");
        CommandContext ctx = cmdCtxFactory.createContext(args);

        cmd.run(ctx);

        String output = cmdCtxFactory.getOutput();
        assertEquals("HOST_ID HOST VM_ID VM_PID STATUS VM_NAME\n" +
                     "123     h1   vmId  1      EXITED n\n", output);
    }

    @Test
    public void verifyOutputFormatMultiLines() throws CommandException {
        context.registerService(HostInfoDAO.class, hostsDAO, null);
        context.registerService(VmInfoDAO.class, vmsDAO, null);

        HostRef host1 = new HostRef("123", "h1");
        HostRef host2 = new HostRef("456", "longhostname");
        when(hostsDAO.getHosts()).thenReturn(Arrays.asList(host1, host2));

        VmRef vm1 = new VmRef(host1, "vm1", 1, "n");
        VmRef vm2 = new VmRef(host1, "vm2", 2, "n1");
        VmRef vm3 = new VmRef(host2, "vm3", 123456, "longvmname");

        VmInfo vmInfo = new VmInfo("foo", "vm1", 1, 0, 1, "", "", "", "", "", "", "", "", null, null, null, -1, null);

        when(vmsDAO.getVMs(host1)).thenReturn(Arrays.asList(vm1, vm2));
        when(vmsDAO.getVMs(host2)).thenReturn(Arrays.asList(vm3));

        when(vmsDAO.getVmInfo(isA(VmRef.class))).thenReturn(vmInfo);

        SimpleArguments args = new SimpleArguments();
        args.addArgument("--dbUrl", "fluff");
        CommandContext ctx = cmdCtxFactory.createContext(args);

        cmd.run(ctx);

        String output = cmdCtxFactory.getOutput();
        assertEquals("HOST_ID HOST         VM_ID VM_PID STATUS VM_NAME\n" +
                     "123     h1           vm1   1      EXITED n\n" +
                     "123     h1           vm2   2      EXITED n1\n" +
                     "456     longhostname vm3   123456 EXITED longvmname\n", output);
    }
    
    @Test
    public void testNeedHostInfoDAO() throws CommandException {
        context.registerService(VmInfoDAO.class, vmsDAO, null);

        SimpleArguments args = new SimpleArguments();
        args.addArgument("--dbUrl", "fluff");
        CommandContext ctx = cmdCtxFactory.createContext(args);

        try {
            cmd.run(ctx);
            fail();
        } catch (CommandException e) {
            assertEquals(translator.localize(LocaleResources.HOST_SERVICE_UNAVAILABLE).getContents(), e.getMessage());
        }
    }
    
    @Test
    public void testNeedVmInfoDAO() throws CommandException {
        context.registerService(HostInfoDAO.class, hostsDAO, null);

        SimpleArguments args = new SimpleArguments();
        args.addArgument("--dbUrl", "fluff");
        CommandContext ctx = cmdCtxFactory.createContext(args);

        try {
            cmd.run(ctx);
            fail();
        } catch (CommandException e) {
            assertEquals(translator.localize(LocaleResources.VM_SERVICE_UNAVAILABLE).getContents(), e.getMessage());
        }
    }

}

