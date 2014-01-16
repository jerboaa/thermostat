/*
 * Copyright 2012-2014 Red Hat, Inc.
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

package com.redhat.thermostat.agent.cli.impl;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;

import sun.misc.Signal;
import sun.misc.SignalHandler;

import com.redhat.thermostat.agent.Agent;
import com.redhat.thermostat.agent.command.ConfigurationServer;
import com.redhat.thermostat.agent.config.AgentConfigsUtils;
import com.redhat.thermostat.agent.config.AgentOptionParser;
import com.redhat.thermostat.agent.config.AgentStartupConfiguration;
import com.redhat.thermostat.backend.BackendRegistry;
import com.redhat.thermostat.backend.BackendService;
import com.redhat.thermostat.common.ExitStatus;
import com.redhat.thermostat.common.LaunchException;
import com.redhat.thermostat.common.MultipleServiceTracker;
import com.redhat.thermostat.common.MultipleServiceTracker.Action;
import com.redhat.thermostat.common.cli.AbstractStateNotifyingCommand;
import com.redhat.thermostat.common.cli.Arguments;
import com.redhat.thermostat.common.cli.CommandContext;
import com.redhat.thermostat.common.cli.CommandException;
import com.redhat.thermostat.common.utils.LoggingUtils;
import com.redhat.thermostat.shared.config.InvalidConfigurationException;
import com.redhat.thermostat.storage.core.Connection.ConnectionListener;
import com.redhat.thermostat.storage.core.Connection.ConnectionStatus;
import com.redhat.thermostat.storage.core.ConnectionException;
import com.redhat.thermostat.storage.core.DbService;
import com.redhat.thermostat.storage.core.DbServiceFactory;
import com.redhat.thermostat.storage.core.Storage;
import com.redhat.thermostat.storage.core.WriterID;
import com.redhat.thermostat.storage.dao.AgentInfoDAO;
import com.redhat.thermostat.storage.dao.BackendInfoDAO;

@SuppressWarnings("restriction")
public final class AgentApplication extends AbstractStateNotifyingCommand {

    private static final Logger logger = LoggingUtils.getLogger(AgentApplication.class);
    
    private final BundleContext bundleContext;
    private final ConfigurationCreator configurationCreator;

    private AgentStartupConfiguration configuration;
    private AgentOptionParser parser;
    private DbServiceFactory dbServiceFactory;
    @SuppressWarnings("rawtypes")
    private ServiceTracker configServerTracker;
    private MultipleServiceTracker daoTracker;
    private final ExitStatus exitStatus;
    private final WriterID writerId;
    private CountDownLatch shutdownLatch;

    public AgentApplication(BundleContext bundleContext, ExitStatus exitStatus, WriterID writerId) {
        this(bundleContext, exitStatus, writerId, new ConfigurationCreator(), new DbServiceFactory());
    }

    AgentApplication(BundleContext bundleContext, ExitStatus exitStatus, WriterID writerId, ConfigurationCreator configurationCreator, DbServiceFactory dbServiceFactory) {
        this.bundleContext = bundleContext;
        this.configurationCreator = configurationCreator;
        this.dbServiceFactory = dbServiceFactory;
        this.exitStatus = exitStatus;
        this.writerId = writerId;
    }
    
    private void parseArguments(Arguments args) throws InvalidConfigurationException {
        parser = new AgentOptionParser(configuration, args);
        parser.parse();
    }
    
    @SuppressWarnings({ "rawtypes", "unchecked" })
    private void runAgent(CommandContext ctx) {
        long startTime = System.currentTimeMillis();
        configuration.setStartTime(startTime);
        

        final DbService dbService = dbServiceFactory.createDbService(
                configuration.getDBConnectionString());
        
        shutdownLatch = new CountDownLatch(1);
        
        configServerTracker = new ServiceTracker(bundleContext, ConfigurationServer.class.getName(), null) {
            @Override
            public Object addingService(ServiceReference reference) {
                final ConfigurationServer configServer = (ConfigurationServer) super.addingService(reference);
                String [] host = configuration.getConfigListenAddress().split(":");

                try {
                    configServer.startListening(host[0], Integer.valueOf(host[1]));

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
                                logger.fine("Connected to storage");
                                handleConnected(configServer);
                                break;
                            case FAILED_TO_CONNECT:
                                // ConnectionException will be thrown
                                break;
                            default:
                                logger.warning("Unfamiliar ConnectionStatus value: " + newStatus.toString());
                            }
                        }
                    };

                    dbService.addConnectionListener(connectionListener);
                    logger.fine("Connecting to storage...");
                
                    dbService.connect();
                } catch (IOException e) {
                    logger.log(Level.SEVERE, e.getMessage());
                    // log stack trace as info only
                    logger.log(Level.INFO, e.getMessage(), e);
                    shutdown();
                } catch (ConnectionException e) {
                    logger.log(Level.SEVERE, "Could not connect to storage (" + e.getMessage() + ")");
                    // log stack trace as info only
                    logger.log(Level.INFO, "Could nto connect to storage", e);
                    shutdown();
                }
                
                return configServer;
            }
            
            @Override
            public void removedService(ServiceReference reference, Object service) {
                if (shutdownLatch.getCount() > 0) {
                    // Lost config server while still running
                    logger.warning("ConfigurationServer unexpectedly became unavailable");
                }
                super.removedService(reference, service);
            }
        };
        configServerTracker.open();
        
        try {
            // Wait for either SIGINT or SIGTERM
            shutdownLatch.await();
            logger.fine("terminating agent cmd");
        } catch (InterruptedException e) {
            return;
        }
    }

    @Override
    public void run(CommandContext ctx) throws CommandException {
        configuration = configurationCreator.create();

        parseArguments(ctx.getArguments());
        if (!parser.isHelp()) {
            runAgent(ctx);
        }
    }
    
    public void shutdown() {
        // Exit application
        if (shutdownLatch != null) {
            shutdownLatch.countDown();
        }
        
        if (daoTracker != null) {
            daoTracker.close();
        }
        if (configServerTracker != null) {
            configServerTracker.close();
        }
    }
    
    private class CustomSignalHandler implements SignalHandler {
        
        private Agent agent;
        private ConfigurationServer configServer;

        public CustomSignalHandler(Agent agent, ConfigurationServer configServer) {
            this.agent = agent;
            this.configServer = configServer;
        }
        
        @Override
        public void handle(Signal arg0) {
            configServer.stopListening();
            try {
                agent.stop();
            } catch (Exception ex) {
                // We don't want any exception to hold back the signal handler, otherwise
                // there will be no way to actually stop Thermostat.
                ex.printStackTrace();
            }
            logger.fine("Agent stopped.");       
            shutdown();
        }
        
    }

    Agent startAgent(final Storage storage, AgentInfoDAO agentInfoDAO, BackendInfoDAO backendInfoDAO) {
        BackendRegistry backendRegistry = null;
        try {
            backendRegistry = new BackendRegistry(bundleContext);
            
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Could not get BackendRegistry instance.", e);
            exitStatus.setExitStatus(ExitStatus.EXIT_ERROR);
            shutdown();
            // Since this would throw NPE's down the line if we continue in this
            // method, let's fail right and early :)
            throw new RuntimeException(e);
        }

        final Agent agent = new Agent(backendRegistry, configuration, storage, agentInfoDAO, backendInfoDAO, writerId);
        try {
            logger.fine("Starting agent.");
            agent.start();
            
            bundleContext.registerService(BackendService.class, new BackendService(), null);
            
        } catch (LaunchException le) {
            logger.log(Level.SEVERE,
                    "Agent could not start, probably because a configured backend could not be activated.",
                    le);
            exitStatus.setExitStatus(ExitStatus.EXIT_ERROR);
            shutdown();
        }
        logger.fine("Agent started.");

        logger.info("Agent id: " + agent.getId());
        logger.info("agent started.");
        return agent;
    }
    
    private void handleConnected(final ConfigurationServer configServer) {
        Class<?>[] deps = new Class<?>[] {
                Storage.class,
                AgentInfoDAO.class,
                BackendInfoDAO.class
        };
        daoTracker = new MultipleServiceTracker(bundleContext, deps, new Action() {

            @Override
            public void dependenciesAvailable(Map<String, Object> services) {
                Storage storage = (Storage) services.get(Storage.class.getName());
                AgentInfoDAO agentInfoDAO = (AgentInfoDAO) services
                        .get(AgentInfoDAO.class.getName());
                BackendInfoDAO backendInfoDAO = (BackendInfoDAO) services
                        .get(BackendInfoDAO.class.getName());

                Agent agent = startAgent(storage, agentInfoDAO, backendInfoDAO);
                SignalHandler handler = new CustomSignalHandler(agent, configServer);
                Signal.handle(new Signal("INT"), handler);
                Signal.handle(new Signal("TERM"), handler);
            }

            @Override
            public void dependenciesUnavailable() {
                if (shutdownLatch.getCount() > 0) {
                    // In the rare case we lose one of our deps, gracefully shutdown
                    logger.severe("Storage unexpectedly became unavailable");
                    shutdown();
                }
            }
            
        });
        daoTracker.open();
    }

    static class ConfigurationCreator {
        public AgentStartupConfiguration create() throws InvalidConfigurationException {
            return AgentConfigsUtils.createAgentConfigs();
        }
    }

    @Override
    public boolean isStorageRequired() {
        return false;
    }
    
}

