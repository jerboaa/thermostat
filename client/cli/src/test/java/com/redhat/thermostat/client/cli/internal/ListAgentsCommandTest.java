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
import com.redhat.thermostat.common.cli.SimpleArguments;
import com.redhat.thermostat.common.internal.test.TestCommandContextFactory;
import com.redhat.thermostat.shared.locale.Translate;
import com.redhat.thermostat.storage.dao.AgentInfoDAO;
import com.redhat.thermostat.storage.model.AgentInformation;

public class ListAgentsCommandTest {
    private static final Translate<LocaleResources> translator = LocaleResources.createLocalizer();

    private ListAgentsCommand command;

    private TestCommandContextFactory cmdCtxFactory;
    private AgentInfoDAO agentInfoDAO;

    private DateFormat dateFormat;


    @Before
    public void setup() {
        dateFormat = DateFormat.getDateTimeInstance();

        cmdCtxFactory = new TestCommandContextFactory();
        agentInfoDAO = mock(AgentInfoDAO.class);

        command = new ListAgentsCommand();

    }

    @Test
    public void testListAgents() throws CommandException {
        String idOne = "agentOne";
        String address = "configListenAddress";
        long startTime = 0;
        long stopTime = 1;

        AgentInformation agentOne = new AgentInformation();
        agentOne.setAgentId(idOne);
        agentOne.setConfigListenAddress(address);
        agentOne.setStartTime(startTime);
        agentOne.setStopTime(stopTime);


        String idTwo = "agentTwo";
        AgentInformation agentTwo = new AgentInformation();
        agentTwo.setAgentId(idTwo);
        agentTwo.setConfigListenAddress(address);
        agentTwo.setStartTime(startTime);
        agentTwo.setStopTime(stopTime);

        when(agentInfoDAO.getAllAgentInformation()).thenReturn(Arrays.asList(new AgentInformation[]{agentOne, agentTwo}));

        command.setAgentInfoDAO(agentInfoDAO);

        CommandContext context = cmdCtxFactory.createContext(new SimpleArguments());

        command.run(context);

        String output = cmdCtxFactory.getOutput();

        verifyHeader(output);
        verifyAgentPrinted(output, idOne, address, startTime, stopTime);
        verifyAgentPrinted(output, idTwo, address, startTime, stopTime);
    }

    private void verifyAgentPrinted(String output, String agentId, String address, long startTime, long stopTime) {
        assertTrue(output.contains(agentId));
        assertTrue(output.contains(address));
        assertTrue(output.contains(dateFormat.format(new Date(startTime))));
        assertTrue(output.contains(dateFormat.format(new Date(stopTime))));
    }

    private void verifyHeader(String output) {
        String AGENT_ID = translator.localize(LocaleResources.AGENT_ID).getContents();
        String CONFIG_LISTEN_ADDRESS = translator.localize(LocaleResources.CONFIG_LISTEN_ADDRESS).getContents();
        String START_TIME = translator.localize(LocaleResources.START_TIME).getContents();
        String STOP_TIME = translator.localize(LocaleResources.STOP_TIME).getContents();


        assertTrue(output.contains(AGENT_ID));
        assertTrue(output.contains(CONFIG_LISTEN_ADDRESS));
        assertTrue(output.contains(START_TIME));
        assertTrue(output.contains(STOP_TIME));
    }

    @Test(expected = CommandException.class)
    public void testListAgentsWithoutServices() throws CommandException {
        CommandContext context = cmdCtxFactory.createContext(new SimpleArguments());

        command.run(context);
    }
}
