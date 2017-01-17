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

package com.redhat.thermostat.agent;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.redhat.thermostat.agent.config.AgentStartupConfiguration;
import com.redhat.thermostat.backend.Backend;
import com.redhat.thermostat.backend.BackendRegistry;
import com.redhat.thermostat.common.ActionEvent;
import com.redhat.thermostat.common.ActionListener;
import com.redhat.thermostat.common.LaunchException;
import com.redhat.thermostat.common.ThermostatExtensionRegistry;
import com.redhat.thermostat.common.utils.HostPortPair;
import com.redhat.thermostat.common.utils.LoggingUtils;
import com.redhat.thermostat.storage.core.Storage;
import com.redhat.thermostat.storage.core.WriterID;
import com.redhat.thermostat.storage.dao.AgentInfoDAO;
import com.redhat.thermostat.storage.dao.BackendInfoDAO;
import com.redhat.thermostat.storage.model.AgentInformation;
import com.redhat.thermostat.storage.model.BackendInformation;
import com.redhat.thermostat.utils.management.internal.MXBeanConnectionPoolControl;
import com.redhat.thermostat.utils.management.internal.MXBeanConnectionPoolTracker;

/**
 * Represents the Agent running on a host.
 */
public class Agent {

    private static final Logger logger = LoggingUtils.getLogger(Agent.class);

    private final BackendRegistry backendRegistry;
    private final AgentStartupConfiguration config;
    private final Map<Backend, BackendInformation> backendInfos;
    private final Storage storage;
    private final AgentInfoDAO agentDao;
    private final BackendInfoDAO backendDao;
    private final WriterID writerID;
    private final MXBeanConnectionPoolTracker poolTracker;
    
    private MXBeanConnectionPoolControl pool;
    private AgentInformation agentInfo;
    private boolean started = false;
    

    private ActionListener<ThermostatExtensionRegistry.Action> backendRegistryListener =
            new ActionListener<ThermostatExtensionRegistry.Action>()
    {
        @Override
        public void actionPerformed(ActionEvent<ThermostatExtensionRegistry.Action> actionEvent) {
            Backend backend = (Backend) actionEvent.getPayload();
            
            switch (actionEvent.getActionId()) {

                case SERVICE_ADDED: {
                    if (!backendInfos.containsKey(backend)) {

                        logger.info("Adding backend: " + backend);

                        backend.activate();

                        BackendInformation info = AgentHelper.createBackendInformation(backend, getId());
                        backendDao.addBackendInformation(info);
                        backendInfos.put(backend, info);                    
                    } else {
                        logger.warning("Backend registered that agent already knows about:" + backend);
                    }
                    break;
                }

                case SERVICE_REMOVED: {
                    BackendInformation info = backendInfos.get(backend);
                    if (info != null) {
                        logger.info("removing backend: " + backend);

                        backend.deactivate();

                        backendDao.removeBackendInformation(info);
                        backendInfos.remove(backend); 
                    }
                    break;
                }

                default: {
                    logger.log(Level.WARNING, "received unknown event from BackendRegistry: " + actionEvent.getActionId());
                    break;
                }
            }
        }
    };
    
    public Agent(BackendRegistry registry, AgentStartupConfiguration config, Storage storage,
            AgentInfoDAO agentInfoDao, BackendInfoDAO backendInfoDao, WriterID writerId) {
        this(registry, config, storage, agentInfoDao, backendInfoDao, writerId, new MXBeanConnectionPoolTracker());
    }
    
    Agent(BackendRegistry registry, AgentStartupConfiguration config, Storage storage,
            AgentInfoDAO agentInfoDao, BackendInfoDAO backendInfoDao, WriterID writerId,
            MXBeanConnectionPoolTracker poolTracker) {
        this.backendRegistry = registry;
        this.config = config;
        this.storage = storage;
        this.agentDao = agentInfoDao;
        this.backendDao = backendInfoDao;
        this.writerID = writerId;
        // Need MXBeanConnectionPool without breaking 1.x API
        this.poolTracker = poolTracker;
        poolTracker.open();
        
        backendInfos = new ConcurrentHashMap<>();
        
        backendRegistry.addActionListener(backendRegistryListener);
    }

    public synchronized void start() throws LaunchException {
        if (!started) {
            agentInfo = createAgentInformation();
            agentInfo.setAgentId(getId());
            agentDao.addAgentInformation(agentInfo);
            
            backendRegistry.start();
            try {
                pool = poolTracker.getPoolWithTimeout();
                pool.start();
                started = true;
            } catch (IOException e) {
                throw new LaunchException("Failed to start JMX services for agent", e);
            }
        } else {
            logger.warning("Attempt to start agent when already started.");
        }
    }

    Map<Backend, BackendInformation> getBackendInfos() {
        return backendInfos;
    }
    
    private AgentInformation createAgentInformation() {
        String writerId = getId();
        AgentInformation agentInfo = new AgentInformation(writerId);
        agentInfo.setStartTime(config.getStartTime());
        agentInfo.setAlive(true);
        // Report the configured publish address if any. Otherwise,
        // defaults to (agent-local) configured listen address.
        HostPortPair publishAddress = config.getConfigPublishAddress();
        agentInfo.setConfigListenAddress(publishAddress.toExternalForm());
        return agentInfo;
    }


    public synchronized void stop() {
        if (started) {
            
            backendRegistry.stop();
            
            if (config.purge()) {
                removeAllAgentRelatedInformation();
            } else {
                updateAgentStatusToStopped();
            }
            
            if (pool.isStarted()) {
                try {
                    pool.shutdown();
                } catch (IOException e) {
                    logger.log(Level.WARNING, "Failed to cleanly shut down JMX services", e);
                }
            }
            poolTracker.close();
            started = false;
        } else {
            logger.warning("Attempt to stop agent which is not active");
        }
    }

    private void removeAllAgentRelatedInformation() {
        System.out.println("purging database");
        logger.info("purging database");
        agentDao.removeAgentInformation(agentInfo);
        storage.purge(agentInfo.getAgentId());
    }

    private void updateAgentStatusToStopped() {
        agentInfo.setStopTime(System.currentTimeMillis());
        agentInfo.setAlive(false);
        agentDao.updateAgentInformation(agentInfo);
    }
    
    public String getId() {
        return writerID.getWriterID();
    }

}

