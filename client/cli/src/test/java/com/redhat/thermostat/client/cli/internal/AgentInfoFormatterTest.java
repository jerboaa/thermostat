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

package com.redhat.thermostat.client.cli.internal;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.text.DateFormat;
import java.util.Arrays;
import java.util.Date;

import org.junit.Test;

import com.redhat.thermostat.shared.locale.Translate;
import com.redhat.thermostat.storage.model.AgentInformation;
import com.redhat.thermostat.storage.model.BackendInformation;

public class AgentInfoFormatterTest {
    private static final Translate<LocaleResources> translator = LocaleResources.createLocalizer();

    private final String AGENT_ID = translator.localize(LocaleResources.AGENT_ID).getContents();
    private final String CONFIG_LISTEN_ADDRESS = translator.localize(LocaleResources.CONFIG_LISTEN_ADDRESS).getContents();
    private final String START_TIME = translator.localize(LocaleResources.START_TIME).getContents();
    private final String STOP_TIME = translator.localize(LocaleResources.STOP_TIME).getContents();

    private final String BACKEND = translator.localize(LocaleResources.BACKEND).getContents();
    private final String STATUS = translator.localize(LocaleResources.STATUS).getContents();

    private final AgentInfoFormatter formatter = new AgentInfoFormatter();

    private final ByteArrayOutputStream baos = new ByteArrayOutputStream();
    private final PrintStream out = new PrintStream(baos);


    @Test
    public void testPrintAgentInfo() {
        String agentId = "liveAgent";
        String address = "configListenAddress";
        long startTime = 0;
        long stopTime = 1;

        AgentInformation info = new AgentInformation();
        info.setAgentId(agentId);
        info.setConfigListenAddress(address);
        info.setStartTime(startTime);
        info.setStopTime(stopTime);

        String backendName = "backendInfo";
        boolean status = true;
        String backendDescription = "description";
        BackendInformation backendInformation = mock(BackendInformation.class);
        when(backendInformation.getName()).thenReturn(backendName);
        when(backendInformation.isActive()).thenReturn(status);
        when(backendInformation.getDescription()).thenReturn(backendDescription);

        formatter.addAgent(info, Arrays.asList(new BackendInformation[] {backendInformation}));
        formatter.format(out);

        String output = new String(baos.toByteArray());

        DateFormat dateFormat = DateFormat.getDateTimeInstance();

        assertTrue(output.contains(AGENT_ID));
        assertTrue(output.contains(CONFIG_LISTEN_ADDRESS));
        assertTrue(output.contains(START_TIME));
        assertTrue(output.contains(STOP_TIME));

        assertTrue(output.contains(BACKEND));
        assertTrue(output.contains(STATUS));


        assertTrue(output.contains(agentId));
        assertTrue(output.contains(address));
        assertTrue(output.contains(dateFormat.format(new Date(startTime))));
        assertTrue(output.contains(dateFormat.format(new Date(stopTime))));

        String statusString = status ?
                translator.localize(LocaleResources.AGENT_INFO_BACKEND_STATUS_ACTIVE).getContents()
                : translator.localize(LocaleResources.AGENT_INFO_BACKEND_STATUS_INACTIVE).getContents();

        assertTrue(output.contains(backendName));
        assertTrue(output.contains(statusString));
        assertTrue(output.contains(backendDescription));
    }


}
