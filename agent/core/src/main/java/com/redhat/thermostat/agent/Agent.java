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

import java.util.ArrayList;
import java.util.Map;
import java.util.UUID;
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
import com.redhat.thermostat.common.dao.AgentInfoDAO;
import com.redhat.thermostat.common.dao.BackendInfoDAO;
import com.redhat.thermostat.common.dao.DAOFactory;
import com.redhat.thermostat.common.model.AgentInformation;
import com.redhat.thermostat.common.model.BackendInformation;
import com.redhat.thermostat.common.storage.Storage;
import com.redhat.thermostat.common.utils.LoggingUtils;

/**
 * Represents the Agent running on a host.
 */
public class Agent {

    private static final Logger logger = LoggingUtils.getLogger(Agent.class);

    private final UUID id;
    private final BackendRegistry backendRegistry;
    private final AgentStartupConfiguration config;

    private final DAOFactory daoFactory;
    
    private AgentInformation agentInfo;
    
    private Map<Backend, BackendInformation> backendInfos;

    private Storage storage;
    private AgentInfoDAO agentDao;
    private BackendInfoDAO backendDao;
    private boolean started = false;

    private ActionListener<ThermostatExtensionRegistry.Action> backendRegistryListener =
            new ActionListener<ThermostatExtensionRegistry.Action>()
    {
        @Override
        public void actionPerformed(ActionEvent<ThermostatExtensionRegistry.Action> actionEvent) {
            Backend backend = (Backend) actionEvent.getPayload();
            
            switch (actionEvent.getActionId()) {
            case SERVICE_ADDED: {
                // TODO: this backed has been already added, we should
                // probably signal the user this
                if (!backendInfos.containsKey(backend)) {

                    logger.info("Adding backend: " + backend);
                    
                    backend.setDAOFactory(daoFactory);
                    backend.activate();

                    BackendInformation info = createBackendInformation(backend);
                    backendDao.addBackendInformation(info);
                    backendInfos.put(backend, info);                    
                }
            }
            break;

            case SERVICE_REMOVED: {
                BackendInformation info = backendInfos.get(backend);
                if (info != null) {
                    logger.info("removing backend: " + backend);
                    
                    backend.deactivate();
                    
                    backendDao.removeBackendInformation(info);
                    backendInfos.remove(backend); 
                }
            }
            break;
                
            default:
                logger.log(Level.WARNING, "received unknown event from BackendRegistry: " + actionEvent.getActionId());
                break;
            }
        }
    };
    
    public Agent(BackendRegistry backendRegistry, AgentStartupConfiguration config, DAOFactory daoFactory)
    {
        this(backendRegistry, UUID.randomUUID(), config, daoFactory);
    }

    public Agent(BackendRegistry registry, UUID agentId, AgentStartupConfiguration config, DAOFactory daoFactory)
    {
        this.id = agentId;
        this.backendRegistry = registry;
        this.config = config;
        this.storage = daoFactory.getStorage();
        this.storage.setAgentId(agentId);
        this.agentDao = daoFactory.getAgentInfoDAO();
        this.backendDao = daoFactory.getBackendInfoDAO();
        
        this.daoFactory = daoFactory;
        
        backendInfos = new ConcurrentHashMap<>();
        
        backendRegistry.addActionListener(backendRegistryListener);
    }

    public synchronized void start() throws LaunchException {
        if (!started) {
            agentInfo = createAgentInformation();
            agentDao.addAgentInformation(agentInfo);
            
            backendRegistry.start();
            
            started = true;
        } else {
            logger.warning("Attempt to start agent when already started.");
        }
    }

    Map<Backend, BackendInformation> getBackendInfos() {
        return backendInfos;
    }
    
    private AgentInformation createAgentInformation() {
        AgentInformation agentInfo = new AgentInformation();
        agentInfo.setStartTime(config.getStartTime());
        agentInfo.setAlive(true);
        agentInfo.setConfigListenAddress(config.getConfigListenAddress());
        return agentInfo;
    }

    private BackendInformation createBackendInformation(Backend backend) {

        BackendInformation backendInfo = new BackendInformation();
        backendInfo.setName(backend.getName());
        backendInfo.setDescription(backend.getDescription());
        backendInfo.setObserveNewJvm(backend.getObserveNewJvm());
        backendInfo.setActive(true);
        backendInfo.setPids(new int[0]);
        
        return backendInfo;
    }

    public synchronized void stop() {
        if (started) {
            
            backendRegistry.stop();
            
            if (config.purge()) {
                removeAllAgentRelatedInformation();
            } else {
                updateAgentStatusToStopped();
            }
            started = false;
        } else {
            logger.warning("Attempt to stop agent which is not active");
        }
    }

    private void removeAllAgentRelatedInformation() {
        System.out.println("purging database");
        logger.info("purging database");
        agentDao.removeAgentInformation(agentInfo);
        storage.purge();
    }

    private void updateAgentStatusToStopped() {
        agentInfo.setStopTime(System.currentTimeMillis());
        agentInfo.setAlive(false);
        agentDao.updateAgentInformation(agentInfo);
    }

    public UUID getId() {
        return id;
    }

}
