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

package com.redhat.thermostat.agent;

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.redhat.thermostat.agent.config.StartupConfiguration;
import com.redhat.thermostat.backend.BackendLoadException;
import com.redhat.thermostat.backend.BackendRegistry;
import com.redhat.thermostat.common.Constants;
import com.redhat.thermostat.common.LaunchException;
import com.redhat.thermostat.common.storage.MongoStorage;
import com.redhat.thermostat.common.storage.Storage;
import com.redhat.thermostat.common.utils.LoggedExternalProcess;
import com.redhat.thermostat.common.utils.LoggingUtils;

public final class Main {

    private Main() {
        throw new IllegalStateException("Should not be instantiated");
    }

    public static void main(String[] args) {
        long startTimestamp = System.currentTimeMillis();

        List<String> argsAsList = new ArrayList<String>(Arrays.asList(args));
        while (argsAsList.contains(Constants.AGENT_ARGUMENT_DEVEL)) {
            argsAsList.remove(Constants.AGENT_ARGUMENT_DEVEL);
            LoggingUtils.useDevelConsole();
        }

        LoggingUtils.setGlobalLogLevel(Level.ALL);
        Logger logger = LoggingUtils.getLogger(Main.class);

        StartupConfiguration config = null;
        try {
            config = new StartupConfiguration(startTimestamp, argsAsList.toArray(new String[0]));
        } catch (LaunchException le) {
            logger.log(Level.SEVERE,
                    "Unable to instantiate startup configuration.",
                    le);
            System.exit(Constants.EXIT_CONFIGURATION_ERROR);
        }

        LoggingUtils.setGlobalLogLevel(config.getLogLevel());

        String mongoScript = config.getMongoLaunchScript();
        if (config.getLocalMode()) {
            try {
                logger.fine("Starting private mongod instance.");
                logger.finest("Mongo launch script at: " + mongoScript);
                int result = new LoggedExternalProcess(new String[] { mongoScript, "start" }).runAndReturnResult();
                if (result != 0) {
                    logger.severe("Error starting local mongod instance.");
                    System.exit(Constants.EXIT_UNABLE_TO_CONNECT_TO_DATABASE);
                }
            } catch (IOException e) {
                logger.log(Level.SEVERE, "Unable to execute script to start local mongod instance.", e);
                System.exit(Constants.EXIT_UNABLE_TO_CONNECT_TO_DATABASE);
            } catch (InterruptedException e) {
                logger.log(Level.SEVERE, "Interrupted while starting local mongod instance.", e);
                System.exit(Constants.EXIT_UNABLE_TO_CONNECT_TO_DATABASE);
            }
        }
        Storage storage = new MongoStorage();
        try {
            storage.connect(config.getDatabaseURIAsString());
            logger.fine("Storage configured with database URI.");
        } catch (UnknownHostException uhe) {
            logger.log(Level.SEVERE, "Could not initialize storage layer.", uhe);
            System.exit(Constants.EXIT_UNABLE_TO_CONNECT_TO_DATABASE);
        }

        BackendRegistry backendRegistry = null;
        try {
            backendRegistry = new BackendRegistry(config, storage);
        } catch (BackendLoadException ble) {
            logger.log(Level.SEVERE, "Could not get BackendRegistry instance.", ble);
            System.exit(Constants.EXIT_BACKEND_LOAD_ERROR);
        }

        Agent agent = new Agent(backendRegistry, config, storage);
        storage.setAgentId(agent.getId());
        try {
            logger.fine("Starting agent.");
            agent.start();
        } catch (LaunchException le) {
            logger.log(Level.SEVERE,
                    "Agent could not start, probably because a configured backend could not be activated.",
                    le);
            System.exit(Constants.EXIT_BACKEND_START_ERROR);
        }
        logger.fine("Agent started.");

        try {
            System.in.read();
        } catch (IOException e) {
            e.printStackTrace();
        }

        agent.stop();
        logger.fine("Agent stopped.");
        if (config.getLocalMode()) {
            logger.fine("Stopping private mongod instance.");
            try {
                int result = new LoggedExternalProcess(new String[] { mongoScript, "stop" }).runAndReturnResult();
                if (result != 0) {
                    logger.severe("Error stopping local mongod instance.");
                }
            } catch (IOException e) {
                logger.log(Level.WARNING, "Unable to execute script to stop private mongod instance.", e);
            } catch (InterruptedException e) {
                logger.log(Level.SEVERE, "Interrupted while stopping local mongod instance.", e);
            }
        }
    }
}
