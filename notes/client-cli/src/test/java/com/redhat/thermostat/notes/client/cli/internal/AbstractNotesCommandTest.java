/*
 * Copyright 2012-2017 Red Hat, Inc.
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
import com.redhat.thermostat.common.cli.CommandContext;
import com.redhat.thermostat.common.cli.CommandException;
import com.redhat.thermostat.common.internal.test.TestCommandContextFactory;
import com.redhat.thermostat.notes.client.cli.locale.LocaleResources;
import com.redhat.thermostat.notes.common.HostNoteDAO;
import com.redhat.thermostat.notes.common.VmNoteDAO;
import com.redhat.thermostat.shared.locale.Translate;
import com.redhat.thermostat.storage.core.AgentId;
import com.redhat.thermostat.storage.core.HostRef;
import com.redhat.thermostat.storage.core.VmId;
import com.redhat.thermostat.storage.core.VmRef;
import com.redhat.thermostat.storage.dao.AgentInfoDAO;
import com.redhat.thermostat.storage.dao.HostInfoDAO;
import com.redhat.thermostat.storage.dao.VmInfoDAO;
import com.redhat.thermostat.storage.model.AgentInformation;
import com.redhat.thermostat.storage.model.HostInfo;
import com.redhat.thermostat.storage.model.VmInfo;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.junit.matchers.JUnitMatchers.containsString;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public abstract class AbstractNotesCommandTest<T extends NotesSubcommand> {

    private static final Translate<LocaleResources> translator = LocaleResources.createLocalizer();

    private static final String INVALID_AGENTID_MSG = translator.localize(LocaleResources.INVALID_AGENTID).getContents().replaceAll("\\{0\\}", "");
    private static final String INVALID_VMID_MSG = translator.localize(LocaleResources.INVALID_VMID).getContents().replaceAll("\\{0\\}", "");

    VmInfoDAO vmInfoDAO;
    HostInfoDAO hostInfoDAO;
    AgentInfoDAO agentInfoDAO;
    VmNoteDAO vmNoteDAO;
    HostNoteDAO hostNoteDAO;

    TestCommandContextFactory contextFactory;

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
        command.bindVmInfoDao(vmInfoDAO);
        command.bindHostInfoDao(hostInfoDAO);
        command.bindAgentInfoDao(agentInfoDAO);
        command.bindVmNoteDao(vmNoteDAO);
        command.bindHostNoteDao(hostNoteDAO);
    }

    @Test
    public void testAssertExpectedArgsSucceedsWithExpectedInput1() throws CommandException {
        Arguments args = mock(Arguments.class);
        when(args.hasArgument(VmArgument.ARGUMENT_NAME)).thenReturn(true);
        when(args.hasArgument(AgentArgument.ARGUMENT_NAME)).thenReturn(false);
        NotesSubcommand.assertExpectedAgentAndVmArgsProvided(args);
    }

    @Test
    public void testAssertExpectedArgsSucceedsWithExpectedInput2() throws CommandException {
        Arguments args = mock(Arguments.class);
        when(args.hasArgument(VmArgument.ARGUMENT_NAME)).thenReturn(false);
        when(args.hasArgument(AgentArgument.ARGUMENT_NAME)).thenReturn(true);
        NotesSubcommand.assertExpectedAgentAndVmArgsProvided(args);
    }

    @Test(expected = CommandException.class)
    public void testAssertExpectedArgsFailsWithNeitherArg() throws CommandException {
        Arguments args = mock(Arguments.class);
        when(args.hasArgument(VmArgument.ARGUMENT_NAME)).thenReturn(false);
        when(args.hasArgument(AgentArgument.ARGUMENT_NAME)).thenReturn(false);
        NotesSubcommand.assertExpectedAgentAndVmArgsProvided(args);
    }

    @Test(expected = CommandException.class)
    public void testAssertExpectedArgsFailsWithBothArgs() throws CommandException {
        Arguments args = mock(Arguments.class);
        when(args.hasArgument(VmArgument.ARGUMENT_NAME)).thenReturn(true);
        when(args.hasArgument(AgentArgument.ARGUMENT_NAME)).thenReturn(true);
        NotesSubcommand.assertExpectedAgentAndVmArgsProvided(args);
    }

    @Test
    public void testAssertVmExistsWithValidVmId() {
        VmInfo vmInfo = new VmInfo();
        vmInfo.setAgentId("foo-agentid");
        vmInfo.setVmId("foo-vmid");
        vmInfo.setVmPid(100);
        vmInfo.setVmName("foo-vmname");
        when(vmInfoDAO.getVmInfo(any(VmId.class))).thenReturn(vmInfo);
        try {
            command.checkVmExists(new VmId("foo-vmid"));
        } catch (CommandException unexpected) {
            fail("Did not expect exception: " + unexpected.toString());
        }
    }

    @Test(expected = CommandException.class)
    public void testAssertVmExistsWithNullVmId() throws CommandException {
        command.checkVmExists(null);
    }

    @Test(expected = CommandException.class)
    public void testAssertVmExistsWithInvalidVmId() throws CommandException {
        when(vmInfoDAO.getVmInfo(any(VmId.class))).thenReturn(null);
        command.checkVmExists(new VmId("foo-vmid"));
    }

    @Test
    public void testAssertAgentExistsWithValidAgentId() {
        AgentInformation agentInfo = new AgentInformation();
        agentInfo.setAgentId("foo-agentid");
        agentInfo.setAlive(true);
        agentInfo.setConfigListenAddress("127.0.0.1");
        agentInfo.setStartTime(100L);
        agentInfo.setStopTime(0L);
        when(agentInfoDAO.getAgentInformation(any(AgentId.class))).thenReturn(agentInfo);
        try {
            command.checkAgentExists(new AgentId("foo-agentid"));
        } catch (CommandException unexpected) {
            fail("Did not expect exception: " + unexpected.toString());
        }
    }

    @Test(expected = CommandException.class)
    public void testAssertAgentExistsWithNullAgentId() throws CommandException {
        command.checkAgentExists(null);
    }

    @Test(expected = CommandException.class)
    public void testAssertAgentExistsWithInvalidAgentId() throws CommandException {
        when(agentInfoDAO.getAgentInformation(any(AgentId.class))).thenReturn(null);
        command.checkAgentExists(new AgentId("foo-agentid"));
    }

    @Test
    public void testGetVmRefFromVmId() throws CommandException {
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
    public void testGetHostRefFromAgentId() throws CommandException {
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
        NotesSubcommand.getNoteId(args);
    }

    @Test
    public void testGetNoteId() throws CommandException {
        Arguments args = mock(Arguments.class);
        when(args.hasArgument(NotesSubcommand.NOTE_ID_ARGUMENT)).thenReturn(true);
        when(args.getArgument(NotesSubcommand.NOTE_ID_ARGUMENT)).thenReturn("foo-noteid");
        String result = NotesSubcommand.getNoteId(args);
        assertThat(result, is("foo-noteid"));
    }

    @Test
    public void testGetNoteContent() throws CommandException {
        Arguments args = mock(Arguments.class);
        when(args.hasArgument(NotesSubcommand.NOTE_CONTENT_ARGUMENT)).thenReturn(true);
        when(args.getArgument(NotesSubcommand.NOTE_CONTENT_ARGUMENT)).thenReturn("this is a note");
        when(args.getNonOptionArguments()).thenReturn(Collections.<String>emptyList());
        String result = NotesSubcommand.getNoteContent(args);
        assertThat(result, is("this is a note"));
    }

    @Test
    public void testGetNoteContentWithStrangeInput() throws CommandException {
        Arguments args = mock(Arguments.class);
        when(args.hasArgument(NotesSubcommand.NOTE_CONTENT_ARGUMENT)).thenReturn(true);
        when(args.getArgument(NotesSubcommand.NOTE_CONTENT_ARGUMENT)).thenReturn("this is a note");
        when(args.getNonOptionArguments()).thenReturn(Arrays.asList("here", "is", "another", "--noteContent", "strange", "--noteContent", "--input"));
        String result = NotesSubcommand.getNoteContent(args);
        assertThat(result, is("this is a note"));
    }

    void doInvalidAgentIdTest(Arguments args) {
        doInvalidIdTest(args, INVALID_AGENTID_MSG);
    }

    void doInvalidVmIdTest(Arguments args) {
        doInvalidIdTest(args, INVALID_VMID_MSG);
    }

    private void doInvalidIdTest(Arguments args, String msg) {
        CommandContext ctx = contextFactory.createContext(args);
        Exception ex = null;
        try {
            command.run(ctx);
        } catch (Exception e) {
            ex = e;
        }
        assertThat(ex, is(not(equalTo(null))));
        assertThat(ex.getMessage(), containsString(msg));
    }

    public abstract T createCommand();

}
