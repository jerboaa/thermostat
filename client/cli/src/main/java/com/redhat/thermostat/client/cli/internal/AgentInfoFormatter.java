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

import java.io.PrintStream;
import java.text.DateFormat;
import java.util.Date;
import java.util.List;

import com.redhat.thermostat.common.cli.TableRenderer;
import com.redhat.thermostat.shared.locale.Translate;
import com.redhat.thermostat.storage.model.AgentInformation;
import com.redhat.thermostat.storage.model.BackendInformation;

public class AgentInfoFormatter {
    private static final Translate<LocaleResources> translator = LocaleResources.createLocalizer();
    private static final int agentColumns = 2;
    private static final int backendColumns = 3;

    private final TableRenderer agentTable = new TableRenderer(agentColumns);
    private final TableRenderer backendTable = new TableRenderer(backendColumns);
    private final DateFormat dateFormat = DateFormat.getDateTimeInstance();

    private static final String AGENT_ID = translator.localize(LocaleResources.AGENT_ID).getContents();
    private static final String CONFIG_LISTEN_ADDRESS = translator.localize(LocaleResources.CONFIG_LISTEN_ADDRESS).getContents();
    private static final String START_TIME = translator.localize(LocaleResources.START_TIME).getContents();
    private static final String STOP_TIME = translator.localize(LocaleResources.STOP_TIME).getContents();

    private static final String BACKEND = translator.localize(LocaleResources.BACKEND).getContents();
    private static final String STATUS = translator.localize(LocaleResources.STATUS).getContents();
    private static final String DESCIRPTION = translator.localize(LocaleResources.DESCRIPTION).getContents();

    void format(PrintStream output) {
        agentTable.render(output);
        backendTable.render(output);
    }

    void addAgent(AgentInformation info, List<BackendInformation> backendList) {
        printAgent(info, backendList);
    }

    private void printAgent(AgentInformation info, List<BackendInformation> backendList) {
        printRow(AGENT_ID, info.getAgentId());
        printRow(CONFIG_LISTEN_ADDRESS, info.getConfigListenAddress());
        printRow(START_TIME, dateFormat.format(new Date(info.getStartTime())));
        printRow(STOP_TIME, getStopString(info.getStopTime()));

        if (!backendList.isEmpty()) {
            printEmptyRow();

            printRow(BACKEND, STATUS, DESCIRPTION);

            for (BackendInformation backend : backendList) {
                printRow(backend.getName(),
                        backend.isActive() ?
                                translator.localize(LocaleResources.AGENT_INFO_BACKEND_STATUS_ACTIVE).getContents()
                                : translator.localize(LocaleResources.AGENT_INFO_BACKEND_STATUS_INACTIVE).getContents(),
                        backend.getDescription()
                );
            }
        }
    }

    private void printEmptyRow() {
        printRow("", "");
    }

    private String getStopString(long stopTime) {
        String stopString;
        if (stopTime == 0) {
            stopString = translator.localize(LocaleResources.AGENT_ACTIVE).getContents();
        } else {
            stopString = dateFormat.format(new Date(stopTime));
        }

        return stopString;
    }

    private void printRow(String title, String content) {
        agentTable.printLine(title, content);
    }

    private void printRow(String title, String status, String description) {
        backendTable.printLine(title, status, description);
    }
}
