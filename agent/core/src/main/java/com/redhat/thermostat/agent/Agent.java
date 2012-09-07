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
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;

import com.redhat.thermostat.agent.config.AgentStartupConfiguration;
import com.redhat.thermostat.backend.Backend;
import com.redhat.thermostat.backend.BackendRegistry;
import com.redhat.thermostat.common.LaunchException;
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

    private AgentInformation agentInfo;
    
    private List<BackendInformation> backendInfos;

    private Storage storage;
    private AgentInfoDAO agentDao;
    private BackendInfoDAO backendDao;
    private boolean started = false;

    public Agent(BackendRegistry backendRegistry, AgentStartupConfiguration config, DAOFactory daos) {
        this(backendRegistry, UUID.randomUUID(), config, daos);
    }

    public Agent(BackendRegistry registry, UUID agentId, AgentStartupConfiguration config, DAOFactory daos) {
        this.id = agentId;
        this.backendRegistry = registry;
        this.config = config;
        this.storage = daos.getStorage();
        this.storage.setAgentId(agentId);
        this.agentDao = daos.getAgentInfoDAO();
        this.backendDao = daos.getBackendInfoDAO();
    }

    private void startBackends() throws LaunchException {
        for (Backend be : backendRegistry.getAll()) {
            logger.fine("Attempting to start backend: " + be.getName());
            if (!be.activate()) {
                logger.warning("Issue while starting backend: " + be.getName());
                // When encountering issues during startup, we should not attempt to continue activating.
                stopBackends();
                throw new LaunchException("Could not activate backend: " + be.getName());
            }
        }
    }

    private void stopBackends() {
        for (Backend be : backendRegistry.getAll()) {
            logger.fine("Attempting to stop backend: " +be.getName());
            if (!be.deactivate()) {
                // When encountering issues during shutdown, we should attempt to shut down remaining backends.
                logger.warning("Issue while deactivating backend: " + be.getName());
            }
        }
    }

    public synchronized void start() throws LaunchException {
        if (!started) {
            startBackends();
            agentInfo = createAgentInformation();
            agentDao.addAgentInformation(agentInfo);

            backendInfos = createBackendInformation();
            for (BackendInformation backendInfo : backendInfos) {
                backendDao.addBackendInformation(backendInfo);
            }
            started = true;
        } else {
            logger.warning("Attempt to start agent when already started.");
        }
    }

    private AgentInformation createAgentInformation() {
        AgentInformation agentInfo = new AgentInformation();
        agentInfo.setStartTime(config.getStartTime());
        agentInfo.setAlive(true);
        agentInfo.setConfigListenAddress(config.getConfigListenAddress());
        return agentInfo;
    }

    private List<BackendInformation> createBackendInformation() {
        List<BackendInformation> results = new ArrayList<>();

        for (Backend backend : backendRegistry.getAll()) {
            BackendInformation backendInfo = new BackendInformation();
            backendInfo.setName(backend.getName());
            backendInfo.setDescription(backend.getDescription());
            backendInfo.setObserveNewJvm(backend.getObserveNewJvm());
            backendInfo.setActive(true);
            backendInfo.setPids(new ArrayList<Integer>());

            results.add(backendInfo);

        }
        return results;
    }

    public synchronized void stop() {
        if (started) {

            stopBackends();
            removeBackendInformation();
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

    private void removeBackendInformation() {
        for (BackendInformation info : backendInfos) {
            backendDao.removeBackendInformation(info);
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
