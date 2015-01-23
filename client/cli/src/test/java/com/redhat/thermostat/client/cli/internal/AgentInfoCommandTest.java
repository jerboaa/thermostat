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

package com.redhat.thermostat.client.cli.internal;


import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.text.DateFormat;
import java.util.Arrays;
import java.util.Date;

import org.junit.Before;
import org.junit.Test;

import com.redhat.thermostat.common.cli.CommandContext;
import com.redhat.thermostat.common.cli.CommandException;
import com.redhat.thermostat.common.cli.CommandLineArgumentParseException;
import com.redhat.thermostat.common.cli.SimpleArguments;
import com.redhat.thermostat.shared.locale.Translate;
import com.redhat.thermostat.storage.core.HostRef;
import com.redhat.thermostat.storage.dao.AgentInfoDAO;
import com.redhat.thermostat.storage.dao.BackendInfoDAO;
import com.redhat.thermostat.storage.model.AgentInformation;
import com.redhat.thermostat.storage.model.BackendInformation;
import com.redhat.thermostat.test.TestCommandContextFactory;

public class AgentInfoCommandTest {
    private static final Translate<LocaleResources> translator = LocaleResources.createLocalizer();

    private AgentInfoCommand command;

    private TestCommandContextFactory cmdCtxFactory;
    private AgentInfoDAO agentInfoDAO;
    private BackendInfoDAO backendInfoDAO;

    private HostRef hostRef;

    private DateFormat dateFormat;

    @Before
    public void setup() {
        dateFormat = DateFormat.getDateTimeInstance();

        cmdCtxFactory = new TestCommandContextFactory();
        agentInfoDAO = mock(AgentInfoDAO.class);
        backendInfoDAO = mock(BackendInfoDAO.class);

        hostRef = new HostRef("liveAgent", "dummy");
        command = new AgentInfoCommand();
    }

    @Test
    public void testGetAgentInfo() throws CommandException {
        String idOne = "liveAgent";
        String address = "configListenAddress";
        long startTime = 0;
        long stopTime = 1;

        AgentInformation agentOne = new AgentInformation();
        agentOne.setAgentId(idOne);
        agentOne.setConfigListenAddress(address);
        agentOne.setStartTime(startTime);
        agentOne.setStopTime(stopTime);

        when(agentInfoDAO.getAgentInformation(hostRef)).thenReturn(agentOne);

        String backendName = "backendInfo";
        boolean status = true;
        String backendDescription = "description";

        BackendInformation backendInformation = mock(BackendInformation.class);
        when(backendInformation.getName()).thenReturn(backendName);
        when(backendInformation.isActive()).thenReturn(status);
        when(backendInformation.getDescription()).thenReturn(backendDescription);
        when(backendInfoDAO.getBackendInformation(hostRef)).thenReturn(Arrays.asList(new BackendInformation[] {backendInformation}));

        setupServices();
        CommandContext context = setupArgs(idOne);

        command.run(context);

        String output = cmdCtxFactory.getOutput();

        verifyTitles(output);
        verifyAgentContent(output, idOne, address, startTime, stopTime);
        verifyBackendContent(output, backendName, status, backendDescription);
    }

    private void verifyTitles(String output) {
        String AGENT_ID = translator.localize(LocaleResources.AGENT_ID).getContents();
        String CONFIG_LISTEN_ADDRESS = translator.localize(LocaleResources.CONFIG_LISTEN_ADDRESS).getContents();
        String START_TIME = translator.localize(LocaleResources.START_TIME).getContents();
        String STOP_TIME = translator.localize(LocaleResources.STOP_TIME).getContents();

        String BACKEND = translator.localize(LocaleResources.BACKEND).getContents();
        String STATUS = translator.localize(LocaleResources.STATUS).getContents();


        assertTrue(output.contains(AGENT_ID));
        assertTrue(output.contains(CONFIG_LISTEN_ADDRESS));
        assertTrue(output.contains(START_TIME));
        assertTrue(output.contains(STOP_TIME));

        assertTrue(output.contains(BACKEND));
        assertTrue(output.contains(STATUS));
    }

    private void verifyAgentContent(String output, String agentId, String address, long startTime, long stopTime) {
        assertTrue(output.contains(agentId));
        assertTrue(output.contains(address));
        assertTrue(output.contains(dateFormat.format(new Date(startTime))));
        assertTrue(output.contains(dateFormat.format(new Date(stopTime))));
    }

    private void verifyBackendContent(String output, String name, boolean status, String description) {
        String statusString = status ?
                translator.localize(LocaleResources.AGENT_INFO_BACKEND_STATUS_ACTIVE).getContents()
                : translator.localize(LocaleResources.AGENT_INFO_BACKEND_STATUS_INACTIVE).getContents();

        assertTrue(output.contains(name));
        assertTrue(output.contains(statusString));
        assertTrue(output.contains(description));
    }

    @Test(expected = CommandException.class)
    public void testGetNonexistentAgentInfo() throws CommandException {
        String agentId = "nonexistentAgent";

        setupServices();
        CommandContext context = setupArgs(agentId);

        command.run(context);
    }

    @Test(expected = CommandLineArgumentParseException.class)
    public void testNoArgumentCommand() throws CommandException {
        setupServices();

        command.run(cmdCtxFactory.createContext(new SimpleArguments()));
    }

    @Test(expected = CommandException.class)
    public void testListAgentsWithoutServices() throws CommandException {
        CommandContext context = cmdCtxFactory.createContext(new SimpleArguments());

        command.run(context);
    }


    private void setupServices() {
        command.setServices(agentInfoDAO, backendInfoDAO);
    }

    private CommandContext setupArgs(String agentId) {
        SimpleArguments args = new SimpleArguments();
        args.addArgument("agentId", agentId);

        return cmdCtxFactory.createContext(args);
    }
}
