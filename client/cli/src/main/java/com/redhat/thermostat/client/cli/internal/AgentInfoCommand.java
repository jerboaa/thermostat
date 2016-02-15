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
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import com.redhat.thermostat.common.cli.AbstractCommand;
import com.redhat.thermostat.common.cli.CommandContext;
import com.redhat.thermostat.common.cli.CommandException;
import com.redhat.thermostat.common.cli.CommandLineArgumentParseException;
import com.redhat.thermostat.shared.locale.Translate;
import com.redhat.thermostat.storage.core.AgentId;
import com.redhat.thermostat.storage.dao.AgentInfoDAO;
import com.redhat.thermostat.storage.dao.BackendInfoDAO;
import com.redhat.thermostat.storage.model.AgentInformation;
import com.redhat.thermostat.storage.model.BackendInformation;

public class AgentInfoCommand extends AbstractCommand {

    private static final Translate<LocaleResources> translator = LocaleResources.createLocalizer();

    private AgentInfoDAO agentInfoDAO;
    private BackendInfoDAO backendInfoDAO;

    private Semaphore servicesAvailable = new Semaphore(0);

    @Override
    public void run(CommandContext ctx) throws CommandException {
        waitForServices(500l);

        requireNonNull(agentInfoDAO, translator.localize(LocaleResources.AGENT_SERVICE_UNAVAILABLE));
        requireNonNull(backendInfoDAO, translator.localize(LocaleResources.BACKEND_SERVICE_UNAVAILABLE));

        String agentId = ctx.getArguments().getArgument("agentId");
        if (agentId == null) {
            throw new CommandLineArgumentParseException(translator.localize(LocaleResources.AGENTID_REQUIRED));
        }

        displayAgentInfo(ctx.getConsole().getOutput(), agentId);
    }

    private void waitForServices(long timeout) {
        try {
            servicesAvailable.tryAcquire(timeout, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            translator.localize(LocaleResources.COMMAND_INTERRUPTED);
        }
    }

    private void displayAgentInfo(PrintStream out, String agentId) throws CommandException {
        AgentId agent = new AgentId(agentId);
        AgentInformation info = agentInfoDAO.getAgentInformation(agent);
        requireNonNull(info, translator.localize(LocaleResources.AGENT_NOT_FOUND, agentId));

        List<BackendInformation> backendList = backendInfoDAO.getBackendInformation(agent);

        AgentInfoFormatter formatter = new AgentInfoFormatter();

        formatter.addAgent(info, backendList);
        formatter.format(out);
    }

    public void setServices(AgentInfoDAO agentInfoDAO, BackendInfoDAO backendInfoDAO) {
        this.agentInfoDAO = agentInfoDAO;
        this.backendInfoDAO = backendInfoDAO;
        if (agentInfoDAO == null || backendInfoDAO ==  null) {
            servicesUnavailable();
        } else {
            serviceAvailable();
        }
    }

    private void serviceAvailable() {
        this.servicesAvailable.release();
    }

    private void servicesUnavailable() {
        this.servicesAvailable.drainPermits();
    }


}
