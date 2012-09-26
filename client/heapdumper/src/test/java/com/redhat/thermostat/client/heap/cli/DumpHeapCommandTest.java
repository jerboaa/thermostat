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

package com.redhat.thermostat.client.heap.cli;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import com.redhat.thermostat.common.cli.Command;
import com.redhat.thermostat.common.cli.CommandException;
import com.redhat.thermostat.common.cli.SimpleArguments;
import com.redhat.thermostat.common.dao.AgentInfoDAO;
import com.redhat.thermostat.common.dao.VmRef;
import com.redhat.thermostat.common.utils.OSGIUtils;
import com.redhat.thermostat.test.TestCommandContextFactory;

public class DumpHeapCommandTest {

    @Test
    public void testBasics() {
        Command command = new DumpHeapCommand();
        assertEquals("dump-heap", command.getName());
        assertNotNull(command.getDescription());
        assertNotNull(command.getUsage());
    }

    @Test
    public void verifyAcuallyCallsWorker() throws CommandException {
        AgentInfoDAO agentInfoDao = mock(AgentInfoDAO.class);
        OSGIUtils osgi = mock(OSGIUtils.class);
        when(osgi.getService(AgentInfoDAO.class)).thenReturn(agentInfoDao);

        HeapDumperCommand impl = mock(HeapDumperCommand.class);
        final ArgumentCaptor<Runnable> arg = ArgumentCaptor.forClass(Runnable.class);
        doAnswer(new Answer<Void>() {

            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                arg.getValue().run();
                return null;
            }
        }).when(impl).execute(eq(agentInfoDao), any(VmRef.class), arg.capture());

        DumpHeapCommand command = new DumpHeapCommand(osgi, impl);

        TestCommandContextFactory factory = new TestCommandContextFactory();

        SimpleArguments args = new SimpleArguments();
        args.addArgument("hostId", "foo");
        args.addArgument("vmId", "0");

        command.run(factory.createContext(args));

        verify(impl).execute(eq(agentInfoDao), isA(VmRef.class), any(Runnable.class));
        assertEquals("Done\n", factory.getOutput());
    }

    @Test
    public void verifyNeedsHostAndVmId() throws CommandException {
        AgentInfoDAO agentInfoDao = mock(AgentInfoDAO.class);
        OSGIUtils osgi = mock(OSGIUtils.class);
        when(osgi.getService(AgentInfoDAO.class)).thenReturn(agentInfoDao);

        HeapDumperCommand impl = mock(HeapDumperCommand.class);
        DumpHeapCommand command = new DumpHeapCommand(osgi, impl);

        TestCommandContextFactory factory = new TestCommandContextFactory();

        SimpleArguments args = new SimpleArguments();

        try {
            command.run(factory.createContext(args));
            assertTrue("should not reach here", false);
        } catch (CommandException ce) {
            assertEquals("a hostId is required", ce.getMessage());
        }
    }

    @Test
    public void verifyFailsIfAgentDaoIsNotAvailable() {
        OSGIUtils osgi = mock(OSGIUtils.class);

        HeapDumperCommand impl = mock(HeapDumperCommand.class);
        DumpHeapCommand command = new DumpHeapCommand(osgi, impl);

        TestCommandContextFactory factory = new TestCommandContextFactory();

        SimpleArguments args = new SimpleArguments();
        args.addArgument("hostId", "foo");
        args.addArgument("vmId", "0");

        try {
            command.run(factory.createContext(args));
            assertTrue("should not reach here", false);
        } catch (CommandException ce) {
            assertEquals("Unable to access agent information", ce.getMessage());
        }
    }

}
