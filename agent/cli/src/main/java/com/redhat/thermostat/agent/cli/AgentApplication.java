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

package com.redhat.thermostat.agent.cli;

import java.util.concurrent.CountDownLatch;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.cli.Options;

import com.redhat.thermostat.agent.Agent;
import com.redhat.thermostat.agent.command.ConfigurationServer;
import com.redhat.thermostat.agent.config.AgentConfigsUtils;
import com.redhat.thermostat.agent.config.AgentOptionParser;
import com.redhat.thermostat.agent.config.AgentStartupConfiguration;
import com.redhat.thermostat.backend.BackendLoadException;
import com.redhat.thermostat.backend.BackendRegistry;
import com.redhat.thermostat.common.Constants;
import com.redhat.thermostat.common.LaunchException;
import com.redhat.thermostat.common.ThreadPoolTimerFactory;
import com.redhat.thermostat.common.TimerFactory;
import com.redhat.thermostat.common.appctx.ApplicationContext;
import com.redhat.thermostat.common.cli.Arguments;
import com.redhat.thermostat.common.cli.CommandContext;
import com.redhat.thermostat.common.cli.CommandException;
import com.redhat.thermostat.common.config.InvalidConfigurationException;
import com.redhat.thermostat.common.dao.AgentInfoDAO;
import com.redhat.thermostat.common.dao.BackendInfoDAO;
import com.redhat.thermostat.common.dao.CpuStatDAO;
import com.redhat.thermostat.common.dao.DAOFactory;
import com.redhat.thermostat.common.dao.HeapDAO;
import com.redhat.thermostat.common.dao.HostInfoDAO;
import com.redhat.thermostat.common.dao.MemoryStatDAO;
import com.redhat.thermostat.common.dao.MongoDAOFactory;
import com.redhat.thermostat.common.dao.NetworkInterfaceInfoDAO;
import com.redhat.thermostat.common.dao.VmClassStatDAO;
import com.redhat.thermostat.common.dao.VmCpuStatDAO;
import com.redhat.thermostat.common.dao.VmGcStatDAO;
import com.redhat.thermostat.common.dao.VmInfoDAO;
import com.redhat.thermostat.common.dao.VmMemoryStatDAO;
import com.redhat.thermostat.common.storage.Connection;
import com.redhat.thermostat.common.storage.Connection.ConnectionListener;
import com.redhat.thermostat.common.storage.Connection.ConnectionStatus;
import com.redhat.thermostat.common.storage.MongoStorageProvider;
import com.redhat.thermostat.common.storage.Storage;
import com.redhat.thermostat.common.storage.StorageProvider;
import com.redhat.thermostat.common.tools.BasicCommand;
import com.redhat.thermostat.common.utils.LoggingUtils;
import com.redhat.thermostat.common.utils.OSGIUtils;

import sun.misc.Signal;
import sun.misc.SignalHandler;

@SuppressWarnings("restriction")
public final class AgentApplication extends BasicCommand {

    private static final String NAME = "agent";

    // TODO: Use LocaleResources for i18n-ized strings.

    private static final String USAGE = "thermostat agent -d <url> [-u <user> -p <password>] [-s] [--debug]";

    private AgentStartupConfiguration configuration;
    private AgentOptionParser parser;
    
    private void parseArguments(Arguments args) throws InvalidConfigurationException {
        configuration = AgentConfigsUtils.createAgentConfigs();
        parser = new AgentOptionParser(configuration, args);
        parser.parse();
    }

    @Override
    public AgentStartupConfiguration getConfiguration() {
        return configuration;
    }
    
    private void runAgent(CommandContext ctx) {
        long startTime = System.currentTimeMillis();
        configuration.setStartTime(startTime);
        
        if (configuration.isDebugConsole()) {
            LoggingUtils.useDevelConsole();
        }
        final Logger logger = LoggingUtils.getLogger(AgentApplication.class);

        StorageProvider connProv = new MongoStorageProvider(configuration);
        final DAOFactory daoFactory = new MongoDAOFactory(connProv);
        ApplicationContext.getInstance().setDAOFactory(daoFactory);
        TimerFactory timerFactory = new ThreadPoolTimerFactory(1);
        ApplicationContext.getInstance().setTimerFactory(timerFactory);

        Connection connection = daoFactory.getConnection();
        ConnectionListener connectionListener = new ConnectionListener() {
            @Override
            public void changed(ConnectionStatus newStatus) {
                switch (newStatus) {
                case DISCONNECTED:
                    logger.warning("Unexpected disconnect event.");
                    break;
                case CONNECTING:
                    logger.fine("Connecting to storage.");
                    break;
                case CONNECTED:
                    logger.fine("Connected to storage, registering storage as service");
                    registerDAOsAndStorageAsOSGiServices(daoFactory);
                    break;
                case FAILED_TO_CONNECT:
                    logger.warning("Could not connect to storage.");
                    System.exit(Constants.EXIT_UNABLE_TO_CONNECT_TO_DATABASE);
                default:
                    logger.warning("Unfamiliar ConnectionStatus value");
                }
            }
        };

        connection.addListener(connectionListener);
        connection.connect();
        logger.fine("Connecting to storage...");

        final ConfigurationServer configServer = OSGIUtils.getInstance().getService(ConfigurationServer.class);
        configServer.startListening(configuration.getConfigListenAddress());

        BackendRegistry backendRegistry = null;
        try {
            backendRegistry = new BackendRegistry(configuration);
        } catch (BackendLoadException ble) {
            logger.log(Level.SEVERE, "Could not get BackendRegistry instance.", ble);
            System.exit(Constants.EXIT_BACKEND_LOAD_ERROR);
        }

        final Agent agent = new Agent(backendRegistry, configuration, daoFactory, daoFactory.getAgentInfoDAO(), daoFactory.getBackendInfoDAO());
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

        ctx.getConsole().getOutput().println("Agent id: " + agent.getId());
        ctx.getConsole().getOutput().println("agent started.");
        logger.fine("Agent id: " + agent.getId());

        final CountDownLatch shutdownLatch = new CountDownLatch(1);
        SignalHandler handler = new CustomSignalHandler(agent, configServer, logger, shutdownLatch);
        Signal.handle(new Signal("INT"), handler);
        Signal.handle(new Signal("TERM"), handler);
        try {
            // Wait for either SIGINT or SIGTERM
            shutdownLatch.await();
            logger.fine("terminating agent cmd");
        } catch (InterruptedException e) {
            return;
        }
    }
    
    private void registerDAOsAndStorageAsOSGiServices(DAOFactory daoFactory) {
        OSGIUtils registerer = OSGIUtils.getInstance();

        registerer.registerService(Storage.class, daoFactory.getStorage());

        registerer.registerService(AgentInfoDAO.class, daoFactory.getAgentInfoDAO());
        registerer.registerService(BackendInfoDAO.class, daoFactory.getBackendInfoDAO());

        registerer.registerService(HostInfoDAO.class, daoFactory.getHostInfoDAO());
        registerer.registerService(NetworkInterfaceInfoDAO.class, daoFactory.getNetworkInterfaceInfoDAO());
        registerer.registerService(CpuStatDAO.class, daoFactory.getCpuStatDAO());
        registerer.registerService(MemoryStatDAO.class, daoFactory.getMemoryStatDAO());

        registerer.registerService(VmInfoDAO.class, daoFactory.getVmInfoDAO());
        registerer.registerService(VmClassStatDAO.class, daoFactory.getVmClassStatsDAO());
        registerer.registerService(VmCpuStatDAO.class, daoFactory.getVmCpuStatDAO());
        registerer.registerService(VmGcStatDAO.class, daoFactory.getVmGcStatDAO());
        registerer.registerService(VmMemoryStatDAO.class, daoFactory.getVmMemoryStatDAO());
        registerer.registerService(HeapDAO.class, daoFactory.getHeapDAO());
    }

    @Override
    public void run(CommandContext ctx) throws CommandException {
        try {
            parseArguments(ctx.getArguments());
            if (!parser.isHelp()) {
                runAgent(ctx);
            }
        } catch (InvalidConfigurationException ex) {
            throw new CommandException(ex);
        }
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public String getUsage() {
        return USAGE;
    }

    @Override
    public Options getOptions() {
        return AgentOptionParser.getOptions();
    }
    
    // Does not need a reference of the enclosing type so lets declare this class static
    private static class CustomSignalHandler implements SignalHandler {

        private Agent agent;
        private ConfigurationServer configServer;
        private Logger logger;
        private CountDownLatch shutdownLatch;
        
        CustomSignalHandler(Agent agent, ConfigurationServer configServer, Logger logger, CountDownLatch latch) {
            this.agent = agent;
            this.configServer = configServer;
            this.logger = logger;
            this.shutdownLatch = latch;
        }
        
        @Override
        public void handle(Signal arg0) {
            configServer.stopListening();
            agent.stop();
            logger.fine("Agent stopped.");       
            shutdownLatch.countDown();
        }
        
    }

}
