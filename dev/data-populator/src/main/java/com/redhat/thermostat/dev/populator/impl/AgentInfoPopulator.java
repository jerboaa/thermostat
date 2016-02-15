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

package com.redhat.thermostat.dev.populator.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.redhat.thermostat.dev.populator.config.ConfigItem;
import com.redhat.thermostat.dev.populator.dependencies.ProcessedRecords;
import com.redhat.thermostat.dev.populator.dependencies.SharedState;
import com.redhat.thermostat.storage.core.Storage;
import com.redhat.thermostat.storage.dao.AgentInfoDAO;
import com.redhat.thermostat.storage.internal.dao.AgentInfoDAOImpl;
import com.redhat.thermostat.storage.model.AgentInformation;

public class AgentInfoPopulator extends BasePopulator {
    
    private final AgentInfoDAO dao;
    
    public AgentInfoPopulator() {
        this(null);
    }
    
    // for testing
    AgentInfoPopulator(AgentInfoDAO dao) {
        this.dao = dao;
    }

    @Override
    public SharedState addPojos(Storage storage, ConfigItem item, SharedState state) {
        // Default to all alive, if unset
        int aliveItems = item.getAliveItems() == ConfigItem.UNSET ? item.getNumber() : item.getAliveItems();
        List<String> processedRecords = new ArrayList<>(); 
        AgentInfoDAO agentInfoDao = getDao(storage);
        long currentTime = System.currentTimeMillis();
        long countBefore = agentInfoDao.getCount();
        System.out.println("Populating "+ item.getNumber() + " " + item.getName() + " records");
        for (int i = 0; i < item.getNumber(); i++) {
            AgentInformation agentInfo = new AgentInformation();
            String agentId = UUID.randomUUID().toString();
            processedRecords.add(agentId);
            agentInfo.setAgentId(agentId);
            agentInfo.setAlive(getAliveValue(aliveItems, i));
            agentInfo.setConfigListenAddress(String.format("127.0.0.1:%d", i));
            agentInfo.setStartTime(currentTime);
            agentInfoDao.addAgentInformation(agentInfo);
            reportProgress(item, i);
        }
        doWaitUntilCount(agentInfoDao, countBefore + item.getNumber());
        state.addProcessedRecords("agentId", new ProcessedRecords<>(processedRecords));
        // FIXME: Why does HostInfoDAOImpl need AgentInfoDAO? See HostInfoPopulator
        state.addProperty("agent-info-dao", agentInfoDao);
        return state;
    }

    private boolean getAliveValue(int aliveItems, int i) {
        if (i < aliveItems) {
            return true;
        } else {
            return false;
        }
    }

    private AgentInfoDAO getDao(Storage storage) {
        if (dao == null) {
            return new AgentInfoDAOImpl(storage);
        } else {
            return dao;
        }
    }

    @Override
    public String getHandledCollection() {
        return AgentInfoDAO.CATEGORY.getName();
    }

}
