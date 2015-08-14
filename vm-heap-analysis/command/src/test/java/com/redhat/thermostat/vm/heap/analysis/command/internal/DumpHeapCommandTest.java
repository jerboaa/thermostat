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

package com.redhat.thermostat.vm.heap.analysis.command.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
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

import com.redhat.thermostat.client.cli.VmArgument;
import com.redhat.thermostat.client.command.RequestQueue;
import com.redhat.thermostat.common.cli.CommandException;
import com.redhat.thermostat.common.cli.SimpleArguments;
import com.redhat.thermostat.shared.locale.Translate;
import com.redhat.thermostat.storage.core.AgentId;
import com.redhat.thermostat.storage.core.VmId;
import com.redhat.thermostat.storage.dao.AgentInfoDAO;
import com.redhat.thermostat.storage.dao.VmInfoDAO;
import com.redhat.thermostat.storage.model.VmInfo;
import com.redhat.thermostat.test.TestCommandContextFactory;
import com.redhat.thermostat.vm.heap.analysis.command.locale.LocaleResources;

public class DumpHeapCommandTest {

    private static final Translate<LocaleResources> TRANSLATOR = LocaleResources
            .createLocalizer();

    /**
     * The GUI heap dump relies on this command with particular input. Any changes to DumpHeapCommand
     * should also be checked against the HeapDumper.
     *
     * See com.redhat.thermostat.vm.heap.analysis.client.core.internal.HeapDumper.dump()
     */
    @Test
    public void verifyActuallyCallsWorker() throws CommandException {
        VmInfoDAO vmInfoDao = mock(VmInfoDAO.class);
        AgentInfoDAO agentInfoDao = mock(AgentInfoDAO.class);
        RequestQueue queue = mock(RequestQueue.class);
        VmInfo vmInfo = new VmInfo("myAgent", "foo", 123, 0, 0, null, null, null, null, null, null, null, null, null,
                null, null,0, "myUsername");
        when(vmInfoDao.getVmInfo(new VmId("foo"))).thenReturn(vmInfo);

        DumpHeapHelper impl = mock(DumpHeapHelper.class);
        final ArgumentCaptor<Runnable> successHandler = ArgumentCaptor
                .forClass(Runnable.class);
        doAnswer(new Answer<Void>() {

            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                successHandler.getValue().run();
                return null;
            }
        }).when(impl).execute(eq(vmInfoDao), eq(agentInfoDao), any(AgentId.class), any(VmId.class), eq(queue),
                successHandler.capture(), any(Runnable.class));

        DumpHeapCommand command = new DumpHeapCommand(impl);

        command.setVmInfoDAO(vmInfoDao);
        command.setAgentInfoDAO(agentInfoDao);
        command.setRequestQueue(queue);

        TestCommandContextFactory factory = new TestCommandContextFactory();

        SimpleArguments args = new SimpleArguments();
        args.addArgument(VmArgument.ARGUMENT_NAME, "foo");

        command.run(factory.createContext(args));

        verify(impl).execute(eq(vmInfoDao), eq(agentInfoDao), isA(AgentId.class), isA(VmId.class), eq(queue),
                any(Runnable.class), any(Runnable.class));
        assertEquals("Done\n", factory.getOutput());
    }

    @Test
    public void verifyNeedsVmId() throws CommandException {
        VmInfoDAO vmInfoDao = mock(VmInfoDAO.class);
        AgentInfoDAO agentInfoDao = mock(AgentInfoDAO.class);
        RequestQueue queue = mock(RequestQueue.class);

        DumpHeapHelper impl = mock(DumpHeapHelper.class);
        DumpHeapCommand command = new DumpHeapCommand(impl);

        command.setVmInfoDAO(vmInfoDao);
        command.setAgentInfoDAO(agentInfoDao);
        command.setRequestQueue(queue);

        TestCommandContextFactory factory = new TestCommandContextFactory();

        SimpleArguments args = new SimpleArguments();

        try {
            command.run(factory.createContext(args));
            assertTrue("should not reach here", false);
        } catch (CommandException ce) {
            assertEquals("A vmId is required", ce.getMessage());
        }
    }

    @Test
    public void verifyFailsIfAgentDaoIsNotAvailable() {
        VmInfoDAO vmInfoDao = mock(VmInfoDAO.class);
        RequestQueue queue = mock(RequestQueue.class);

        DumpHeapHelper impl = mock(DumpHeapHelper.class);
        DumpHeapCommand command = new DumpHeapCommand(impl);

        command.setVmInfoDAO(vmInfoDao);
        command.setRequestQueue(queue);

        TestCommandContextFactory factory = new TestCommandContextFactory();

        SimpleArguments args = new SimpleArguments();
        args.addArgument(VmArgument.ARGUMENT_NAME, "bar");

        try {
            command.run(factory.createContext(args));
            assertTrue("should not reach here", false);
        } catch (CommandException ce) {
            assertEquals(TRANSLATOR.localize(LocaleResources.AGENT_SERVICE_UNAVAILABLE).getContents(), ce.getMessage());
        }
    }
    
    @Test
    public void verifyFailsIfRequestQueueIsNotAvailable() {
        VmInfoDAO vmInfoDao = mock(VmInfoDAO.class);
        AgentInfoDAO agentInfoDao = mock(AgentInfoDAO.class);

        DumpHeapHelper impl = mock(DumpHeapHelper.class);
        DumpHeapCommand command = new DumpHeapCommand(impl);

        command.setVmInfoDAO(vmInfoDao);
        command.setAgentInfoDAO(agentInfoDao);

        TestCommandContextFactory factory = new TestCommandContextFactory();

        SimpleArguments args = new SimpleArguments();
        args.addArgument(VmArgument.ARGUMENT_NAME, "bar");
        args.addArgument("vmPid", "123");

        try {
            command.run(factory.createContext(args));
            fail();
        } catch (CommandException ce) {
            assertEquals(TRANSLATOR.localize(LocaleResources.REQUEST_QUEUE_UNAVAILABLE).getContents(), ce.getMessage());
        }
    }
    
    @Test
    public void verifyFailsIfVmDaoIsNotAvailable() {
        AgentInfoDAO agentInfoDao = mock(AgentInfoDAO.class);
        RequestQueue queue = mock(RequestQueue.class);

        DumpHeapHelper impl = mock(DumpHeapHelper.class);
        DumpHeapCommand command = new DumpHeapCommand(impl);

        command.setAgentInfoDAO(agentInfoDao);
        command.setRequestQueue(queue);

        TestCommandContextFactory factory = new TestCommandContextFactory();

        SimpleArguments args = new SimpleArguments();
        args.addArgument(VmArgument.ARGUMENT_NAME, "bar");

        try {
            command.run(factory.createContext(args));
            fail();
        } catch (CommandException ce) {
            assertEquals(TRANSLATOR.localize(LocaleResources.VM_SERVICE_UNAVAILABLE).getContents(), ce.getMessage());
        }
    }

    @Test
    public void verifyErrorMessage() {
        final String AGENT_ID = "myAgent";
        final String VM_ID = "myVm";
        VmInfoDAO vmInfoDao = mock(VmInfoDAO.class);
        AgentInfoDAO agentInfoDao = mock(AgentInfoDAO.class);
        RequestQueue queue = mock(RequestQueue.class);
        VmInfo vmInfo = new VmInfo(AGENT_ID, VM_ID, 123, 0, 0, null, null, null, null, null, null, null, null,
                null, null, null, 0, null);
        VmId vmId = new VmId(VM_ID);
        AgentId agentId = new AgentId(AGENT_ID);

        when(vmInfoDao.getVmInfo(vmId)).thenReturn(vmInfo);

        DumpHeapHelper impl = mock(DumpHeapHelper.class);
        DumpHeapCommand command = new DumpHeapCommand(impl);

        final ArgumentCaptor<Runnable> errorHandler = ArgumentCaptor.forClass(Runnable.class);

        doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                errorHandler.getValue().run();
                return null;
            }
        }).when(impl).execute(eq(vmInfoDao),
                              eq(agentInfoDao),
                              eq(agentId),
                              eq(vmId),
                              eq(queue),
                              any(Runnable.class),
                              errorHandler.capture());

        command.setVmInfoDAO(vmInfoDao);
        command.setAgentInfoDAO(agentInfoDao);
        command.setRequestQueue(queue);

        TestCommandContextFactory factory = new TestCommandContextFactory();

        SimpleArguments args = new SimpleArguments();
        args.addArgument(VmArgument.ARGUMENT_NAME, VM_ID);

        try {
            command.run(factory.createContext(args));
            fail("CommandException expected");
        } catch (CommandException e) {
            assertEquals(TRANSLATOR.localize(LocaleResources.HEAP_DUMP_ERROR,
                    AGENT_ID, VM_ID).getContents(), e.getMessage());
        }
    }

}

