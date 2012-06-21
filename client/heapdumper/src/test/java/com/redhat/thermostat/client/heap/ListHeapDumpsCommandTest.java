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

package com.redhat.thermostat.client.heap;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.TimeZone;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.redhat.thermostat.common.appctx.ApplicationContext;
import com.redhat.thermostat.common.appctx.ApplicationContextUtil;
import com.redhat.thermostat.common.cli.ArgumentSpec;
import com.redhat.thermostat.common.cli.Command;
import com.redhat.thermostat.common.cli.CommandException;
import com.redhat.thermostat.common.dao.DAOFactory;
import com.redhat.thermostat.common.dao.HeapDAO;
import com.redhat.thermostat.common.dao.HostInfoDAO;
import com.redhat.thermostat.common.dao.HostRef;
import com.redhat.thermostat.common.dao.VmInfoDAO;
import com.redhat.thermostat.common.dao.VmRef;
import com.redhat.thermostat.common.model.HeapInfo;
import com.redhat.thermostat.test.TestCommandContextFactory;

public class ListHeapDumpsCommandTest {

    private static TimeZone defaultTimezone;

    @BeforeClass
    public static void setUpClass() {
        defaultTimezone = TimeZone.getDefault();
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
    }

    @AfterClass
    public static void tearDownClass() {
        TimeZone.setDefault(defaultTimezone);
    }

    @Before
    public void setUp() {
        ApplicationContextUtil.resetApplicationContext();
    }

    @After
    public void tearDown() {
        ApplicationContextUtil.resetApplicationContext();

    }

    @Test
    public void verifyBasics() {
        Command command = new ListHeapDumpsCommand();
        assertEquals("list-heap-dumps", command.getName());
        assertNotNull(command.getDescription());
        assertNotNull(command.getUsage());
    }

    @Test
    public void verifyArguments() {
        Command command = new ListHeapDumpsCommand();
        List<ArgumentSpec> arguments = new ArrayList<>(command.getAcceptedArguments());
        assertTrue(arguments.isEmpty());
    }

    @Test
    public void verifyWorksWithoutAnyInformation() throws CommandException {
        HostInfoDAO hostInfo = mock(HostInfoDAO.class);

        VmInfoDAO vmInfo = mock(VmInfoDAO.class);

        DAOFactory daoFactory = mock(DAOFactory.class);
        when(daoFactory.getHostInfoDAO()).thenReturn(hostInfo);
        when(daoFactory.getVmInfoDAO()).thenReturn(vmInfo);

        ApplicationContext.getInstance().setDAOFactory(daoFactory);

        Command command = new ListHeapDumpsCommand();
        TestCommandContextFactory factory = new TestCommandContextFactory();
        command.run(factory.createContext(null));
        assertEquals("HOST ID VM ID HEAP ID TIMESTAMP\n", factory.getOutput());
    }

    @Test
    public void verifyWorks() throws CommandException {
        HostRef hostRef = mock(HostRef.class);
        when(hostRef.getStringID()).thenReturn("host-id");
        VmRef vmRef = mock(VmRef.class);
        when(vmRef.getStringID()).thenReturn("vm-id");

        HeapInfo heapInfo = mock(HeapInfo.class);
        Calendar timestamp = Calendar.getInstance();
        timestamp.set(2012, 5, 7, 15, 32, 0);
        when(heapInfo.getTimestamp()).thenReturn(timestamp.getTimeInMillis());
        when(heapInfo.getHeapDumpId()).thenReturn("0001");

        HeapDAO heapDao = mock(HeapDAO.class);

        VmInfoDAO vmInfo = mock(VmInfoDAO.class);
        when(vmInfo.getVMs(hostRef)).thenReturn(Arrays.asList(vmRef));

        HostInfoDAO hostInfo = mock(HostInfoDAO.class);
        when(hostInfo.getHosts()).thenReturn(Arrays.asList(hostRef));

        when(heapDao.getAllHeapInfo(vmRef)).thenReturn(Arrays.asList(heapInfo));

        DAOFactory daoFactory = mock(DAOFactory.class);
        when(daoFactory.getHostInfoDAO()).thenReturn(hostInfo);
        when(daoFactory.getVmInfoDAO()).thenReturn(vmInfo);
        when(daoFactory.getHeapDAO()).thenReturn(heapDao);

        ApplicationContext.getInstance().setDAOFactory(daoFactory);

        Command command = new ListHeapDumpsCommand();
        TestCommandContextFactory factory = new TestCommandContextFactory();
        command.run(factory.createContext(null));

        String expected = "HOST ID VM ID HEAP ID TIMESTAMP\n" +
                          "host-id vm-id 0001    Thu Jun 07 15:32:00 UTC 2012\n";

        assertEquals(expected, factory.getOutput());
    }
}
