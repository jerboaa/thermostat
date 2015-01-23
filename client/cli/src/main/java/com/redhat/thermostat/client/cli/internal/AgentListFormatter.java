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

import java.io.PrintStream;
import java.text.DateFormat;
import java.util.Date;

import com.redhat.thermostat.common.cli.TableRenderer;
import com.redhat.thermostat.shared.locale.Translate;
import com.redhat.thermostat.storage.model.AgentInformation;


public class AgentListFormatter{
    private static final Translate<LocaleResources> translator = LocaleResources.createLocalizer();
    private static final DateFormat dateFormat = DateFormat.getDateTimeInstance();
    private static final int NUM_COLUMNS = 4;

    private static final String AGENT_ID = translator.localize(LocaleResources.AGENT_ID).getContents();
    private static final String CONFIG_LISTEN_ADDRESS = translator.localize(LocaleResources.CONFIG_LISTEN_ADDRESS).getContents();
    private static final String START_TIME = translator.localize(LocaleResources.START_TIME).getContents();
    private static final String STOP_TIME = translator.localize(LocaleResources.STOP_TIME).getContents();

    private final TableRenderer tableRenderer = new TableRenderer(NUM_COLUMNS);

    void addHeader() {
        printLine(AGENT_ID, CONFIG_LISTEN_ADDRESS, START_TIME, STOP_TIME);
    }

    void addAgent(AgentInformation info) {
        printAgent(info);
    }

    void format(PrintStream out) {
        tableRenderer.render(out);
    }

    private void printAgent(AgentInformation info) {
        String startTime = dateFormat.format(new Date(info.getStartTime()));
        String stopTime = getStopTimeMessage(info.getStopTime());

        printLine(info.getAgentId(),
                info.getConfigListenAddress(),
                startTime,
                stopTime);
    }

    private String getStopTimeMessage(long stopTime) {
        if (stopTime == 0) {
            return translator.localize(LocaleResources.AGENT_ACTIVE).getContents();
        } else {
            return dateFormat.format(new Date(stopTime));
        }
    }

    private void printLine(String agentId, String address, String startTime, String stopTime) {
        tableRenderer.printLine(agentId, address, startTime, stopTime);
    }

}
