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

package com.redhat.thermostat.tools;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.redhat.thermostat.agent.AgentApplication;
import com.redhat.thermostat.common.ActionEvent;
import com.redhat.thermostat.common.ActionListener;
import com.redhat.thermostat.common.ActionNotifier;
import com.redhat.thermostat.common.NotImplementedException;
import com.redhat.thermostat.common.config.InvalidConfigurationException;
import com.redhat.thermostat.common.config.StartupConfiguration;
import com.redhat.thermostat.tools.db.DBService;

/**
 * Simple service that allows starting Agent and DB Backend
 * in a single step.
 */
public class ThermostatService implements Application, ActionListener<ApplicationState> {
    
    private Application database;
    private AgentApplication agent;
    
    private ActionNotifier<ApplicationState> notifier;
    
    public ThermostatService() {
        database = new DBService();
        agent = new AgentApplication();
        notifier = new ActionNotifier<>(this);
    }
    
    @Override
    public void parseArguments(List<String> args) throws InvalidConfigurationException {
        // Currently, all the arguments are for the db. We only configure the
        // agent accordingly to the database settings.
        // so nothing else is done here at this stage
        database.parseArguments(args);
        database.getNotifier().addActionListener(this);
        agent.getNotifier().addActionListener(this);
    }

    @Override
    public void run() {
        // just run the database, if the database is successful, let the
        // listeners start the agent for us.
        database.run();
    }

    @Override
    public void printHelp() {
        // TODO, no, really, seriously
    }

    @Override
    public ActionNotifier<ApplicationState> getNotifier() {
        return notifier;
    }

    @Override
    public StartupConfiguration getConfiguration() {
        throw new NotImplementedException("NYI");
    }
    
    public static void main(String[] args) throws IOException, InvalidConfigurationException {
        ThermostatService service = new ThermostatService();
        // TODO: other than failing, this should really print the help
        // from the appropriate application instead, see the printHelp comment
        // too.
        service.parseArguments(Arrays.asList(args));
        service.run();
    }

    @Override
    public void actionPerformed(ActionEvent<ApplicationState> actionEvent) {
        if (actionEvent.getSource().equals(database)) {
            switch (actionEvent.getActionId()) {
            // we are only interested in starting the agent if
            // we started the database ourselves
            case START:
                String dbUrl = database.getConfiguration().getDBConnectionString();
                List<String> args = new ArrayList<>();
                args.add("--dbUrl");
                args.add(dbUrl);
                try {
                    agent.parseArguments(args);
                    System.err.println("starting agent now...");
                    agent.run();
                } catch (InvalidConfigurationException e) {
                    notifier.fireAction(ApplicationState.FAIL);
                }
                break;
            }
        } else if (actionEvent.getSource().equals(agent)) {
            // agent is running
            switch (actionEvent.getActionId()) {
            case START:
                notifier.fireAction(ApplicationState.SUCCESS);
                break;
            }
        }
    }
}
