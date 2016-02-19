/*
 * Copyright 2012-2016 Red Hat, Inc.
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

package com.redhat.thermostat.notes.client.cli.internal;

import com.redhat.thermostat.client.cli.AgentArgument;
import com.redhat.thermostat.client.cli.VmArgument;
import com.redhat.thermostat.common.cli.Arguments;
import com.redhat.thermostat.common.cli.CommandException;
import com.redhat.thermostat.notes.common.HostNoteDAO;
import com.redhat.thermostat.notes.common.VmNoteDAO;
import com.redhat.thermostat.storage.core.AgentId;
import com.redhat.thermostat.storage.core.HostRef;
import com.redhat.thermostat.storage.core.VmId;
import com.redhat.thermostat.storage.core.VmRef;
import com.redhat.thermostat.storage.dao.AgentInfoDAO;
import com.redhat.thermostat.storage.dao.HostInfoDAO;
import com.redhat.thermostat.storage.dao.VmInfoDAO;
import com.redhat.thermostat.storage.model.HostInfo;
import com.redhat.thermostat.storage.model.VmInfo;
import com.redhat.thermostat.test.TestCommandContextFactory;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public abstract class AbstractNotesCommandTest<T extends AbstractNotesCommand> {

    protected VmInfoDAO vmInfoDAO;
    protected HostInfoDAO hostInfoDAO;
    protected AgentInfoDAO agentInfoDAO;
    protected VmNoteDAO vmNoteDAO;
    protected HostNoteDAO hostNoteDAO;

    protected TestCommandContextFactory contextFactory;

    protected T command;

    @Before
    public void setup() {
        vmInfoDAO = mock(VmInfoDAO.class);
        hostInfoDAO = mock(HostInfoDAO.class);
        agentInfoDAO = mock(AgentInfoDAO.class);
        vmNoteDAO = mock(VmNoteDAO.class);
        hostNoteDAO = mock(HostNoteDAO.class);

        contextFactory = new TestCommandContextFactory();

        command = createCommand();
        setAllServices();
    }

    protected void setAllServices() {
        command.setVmInfoDao(vmInfoDAO);
        command.setHostInfoDao(hostInfoDAO);
        command.setAgentInfoDao(agentInfoDAO);
        command.setVmNoteDao(vmNoteDAO);
        command.setHostNoteDao(hostNoteDAO);
        try {
            command.setupServices();
        } catch (CommandException e) {
            fail("Service setup should not fail here!");
        }
    }

    @Test
    public void testSetupServicesSucceedsWhenAllPresent() throws CommandException {
        command.setupServices();
    }

    @Test(expected = CommandException.class)
    public void testSetupServicesFailsWhenVmInfoMissing() throws CommandException {
        command.servicesUnavailable();
        command.setHostInfoDao(hostInfoDAO);
        command.setAgentInfoDao(agentInfoDAO);
        command.setVmNoteDao(vmNoteDAO);
        command.setHostNoteDao(hostNoteDAO);
        command.setupServices();
    }

    @Test(expected = CommandException.class)
    public void testSetupServicesFailsWhenHostInfoMissing() throws CommandException {
        command.servicesUnavailable();
        command.setVmInfoDao(vmInfoDAO);
        command.setAgentInfoDao(agentInfoDAO);
        command.setVmNoteDao(vmNoteDAO);
        command.setHostNoteDao(hostNoteDAO);
        command.setupServices();
    }

    @Test(expected = CommandException.class)
    public void testSetupServicesFailsWhenAgentInfoMissing() throws CommandException {
        command.servicesUnavailable();
        command.setVmInfoDao(vmInfoDAO);
        command.setHostInfoDao(hostInfoDAO);
        command.setVmNoteDao(vmNoteDAO);
        command.setHostNoteDao(hostNoteDAO);
        command.setupServices();
    }

    @Test(expected = CommandException.class)
    public void testSetupServicesFailsWhenVmNoteMissing() throws CommandException {
        command.servicesUnavailable();
        command.setVmInfoDao(vmInfoDAO);
        command.setHostInfoDao(hostInfoDAO);
        command.setAgentInfoDao(agentInfoDAO);
        command.setHostNoteDao(hostNoteDAO);
        command.setupServices();
    }

    @Test(expected = CommandException.class)
    public void testSetupServicesFailsWhenHostNoteMissing() throws CommandException {
        command.servicesUnavailable();
        command.setVmInfoDao(vmInfoDAO);
        command.setHostInfoDao(hostInfoDAO);
        command.setAgentInfoDao(agentInfoDAO);
        command.setVmNoteDao(vmNoteDAO);
        command.setupServices();
    }

    @Test
    public void testAssertExpectedArgsSucceedsWithExpectedInput1() throws CommandException {
        Arguments args = mock(Arguments.class);
        when(args.hasArgument(VmArgument.ARGUMENT_NAME)).thenReturn(true);
        when(args.hasArgument(AgentArgument.ARGUMENT_NAME)).thenReturn(false);
        AbstractNotesCommand.assertExpectedAgentAndVmArgsProvided(args);
    }

    @Test
    public void testAssertExpectedArgsSucceedsWithExpectedInput2() throws CommandException {
        Arguments args = mock(Arguments.class);
        when(args.hasArgument(VmArgument.ARGUMENT_NAME)).thenReturn(false);
        when(args.hasArgument(AgentArgument.ARGUMENT_NAME)).thenReturn(true);
        AbstractNotesCommand.assertExpectedAgentAndVmArgsProvided(args);
    }

    @Test(expected = CommandException.class)
    public void testAssertExpectedArgsFailsWithNeitherArg() throws CommandException {
        Arguments args = mock(Arguments.class);
        when(args.hasArgument(VmArgument.ARGUMENT_NAME)).thenReturn(false);
        when(args.hasArgument(AgentArgument.ARGUMENT_NAME)).thenReturn(false);
        AbstractNotesCommand.assertExpectedAgentAndVmArgsProvided(args);
    }

    @Test(expected = CommandException.class)
    public void testAssertExpectedArgsFailsWithBothArgs() throws CommandException {
        Arguments args = mock(Arguments.class);
        when(args.hasArgument(VmArgument.ARGUMENT_NAME)).thenReturn(true);
        when(args.hasArgument(AgentArgument.ARGUMENT_NAME)).thenReturn(true);
        AbstractNotesCommand.assertExpectedAgentAndVmArgsProvided(args);
    }

    @Test
    public void testGetVmRefFromVmId() {
        VmInfo vmInfo = new VmInfo();
        vmInfo.setAgentId("foo-agentid");
        vmInfo.setVmId("foo-vmid");
        vmInfo.setVmPid(100);
        vmInfo.setVmName("foo-vmname");
        when(vmInfoDAO.getVmInfo(any(VmId.class))).thenReturn(vmInfo);
        HostInfo hostInfo = mock(HostInfo.class);
        when(hostInfo.getHostname()).thenReturn("foo-hostname");
        when(hostInfoDAO.getHostInfo(any(AgentId.class))).thenReturn(hostInfo);
        VmRef vmRef = command.getVmRefFromVmId(new VmId("foo-vmid"));

        assertThat(vmRef.getVmId(), is(vmInfo.getVmId()));
        assertThat(vmRef.getPid(), is(vmInfo.getVmPid()));
        assertThat(vmRef.getHostRef().getAgentId(), is(vmInfo.getAgentId()));
        assertThat(vmRef.getHostRef().getHostName(), is(hostInfo.getHostname()));
    }

    @Test
    public void testGetHostRefFromAgentId() {
        HostInfo hostInfo = mock(HostInfo.class);
        when(hostInfo.getHostname()).thenReturn("foo-hostname");
        when(hostInfoDAO.getHostInfo(any(AgentId.class))).thenReturn(hostInfo);
        AgentId agentId = mock(AgentId.class);
        when(agentId.get()).thenReturn("foo-agentid");
        HostRef hostRef = command.getHostRefFromAgentId(agentId);

        assertThat(hostRef.getHostName(), is(hostInfo.getHostname()));
        assertThat(hostRef.getAgentId(), is(agentId.get()));
    }

    @Test(expected = CommandException.class)
    public void testGetNoteIdFailsWhenArgNotProvided() throws CommandException {
        Arguments args = mock(Arguments.class);
        AbstractNotesCommand.getNoteId(args);
    }

    @Test
    public void testGetNoteId() throws CommandException {
        Arguments args = mock(Arguments.class);
        when(args.hasArgument(NotesCommand.NOTE_ID_ARGUMENT)).thenReturn(true);
        when(args.getArgument(NotesCommand.NOTE_ID_ARGUMENT)).thenReturn("foo-noteid");
        String result = AbstractNotesCommand.getNoteId(args);
        assertThat(result, is("foo-noteid"));
    }

    @Test
    public void testGetNoteContentWithNoFlagAndNoNonOptionArgs() {
        Arguments args = mock(Arguments.class);
        when(args.hasArgument(NotesCommand.NOTE_CONTENT_ARGUMENT)).thenReturn(false);
        when(args.getNonOptionArguments()).thenReturn(Collections.<String>emptyList());
        String result = AbstractNotesCommand.getNoteContent(args);
        assertThat(result, is(""));
    }

    @Test
    public void testGetNoteContentWithNonOptionArgs() {
        Arguments args = mock(Arguments.class);
        when(args.hasArgument(NotesCommand.NOTE_CONTENT_ARGUMENT)).thenReturn(false);
        when(args.getNonOptionArguments()).thenReturn(Arrays.asList("this", "is", "a", "note"));
        String result = AbstractNotesCommand.getNoteContent(args);
        assertThat(result, is("this is a note"));
    }

    @Test
    public void testGetNoteContentWithFlag() {
        Arguments args = mock(Arguments.class);
        when(args.hasArgument(NotesCommand.NOTE_CONTENT_ARGUMENT)).thenReturn(true);
        when(args.getArgument(NotesCommand.NOTE_CONTENT_ARGUMENT)).thenReturn("this is a note");
        when(args.getNonOptionArguments()).thenReturn(Collections.<String>emptyList());
        String result = AbstractNotesCommand.getNoteContent(args);
        assertThat(result, is("this is a note"));
    }

    @Test
    public void testGetNoteContentWithFlagAndNonOptionArgs() {
        Arguments args = mock(Arguments.class);
        when(args.hasArgument(NotesCommand.NOTE_CONTENT_ARGUMENT)).thenReturn(true);
        when(args.getArgument(NotesCommand.NOTE_CONTENT_ARGUMENT)).thenReturn("this is a note");
        when(args.getNonOptionArguments()).thenReturn(Arrays.asList("here", "is", "another"));
        String result = AbstractNotesCommand.getNoteContent(args);
        assertThat(result, is("this is a note"));
    }

    @Test
    public void testGetNoteContentWithStrangeInput() {
        Arguments args = mock(Arguments.class);
        when(args.hasArgument(NotesCommand.NOTE_CONTENT_ARGUMENT)).thenReturn(true);
        when(args.getArgument(NotesCommand.NOTE_CONTENT_ARGUMENT)).thenReturn("this is a note");
        when(args.getNonOptionArguments()).thenReturn(Arrays.asList("here", "is", "another", "--noteContent", "strange", "--noteContent", "--input"));
        String result = AbstractNotesCommand.getNoteContent(args);
        assertThat(result, is("this is a note"));
    }

    public abstract T createCommand();

}
