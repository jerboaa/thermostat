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

package com.redhat.thermostat.launcher.internal;

import com.redhat.thermostat.common.cli.CompletionInfo;
import com.redhat.thermostat.storage.core.AgentId;
import com.redhat.thermostat.storage.core.VmId;
import com.redhat.thermostat.storage.dao.AgentInfoDAO;
import com.redhat.thermostat.storage.dao.VmInfoDAO;
import com.redhat.thermostat.storage.model.AgentInformation;
import com.redhat.thermostat.storage.model.VmInfo;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

@Component
@Service
public class VmIdsFinderImpl implements VmIdsFinder {

    @Reference
    private VmInfoDAO vmInfoDao;

    @Reference
    private AgentInfoDAO agentInfoDao;

    @Override
    public List<CompletionInfo> findCompletions() {
        if (vmInfoDao == null || agentInfoDao == null) {
            return Collections.emptyList();
        }

        List<CompletionInfo> vmIds = new ArrayList<>();
        for (AgentId agentId : agentInfoDao.getAgentIds()) {
            AgentInformation agentInfo = agentInfoDao.getAgentInformation(agentId);
            if (agentInfo != null) {
                Collection<VmId> vms = vmInfoDao.getVmIds(agentId);
                for (VmId vm : vms) {
                    VmInfo info = vmInfoDao.getVmInfo(vm);
                    vmIds.add(new CompletionInfo(info.getVmId(), getUserVisibleText(info, agentInfo)));
                }
            }
        }
        return vmIds;
    }

    private String getUserVisibleText(VmInfo info, AgentInformation agentInfo) {
        return info.getMainClass() + "(" + info.isAlive(agentInfo).toString() + ")";
    }

    void bindVmInfoDao(VmInfoDAO vmInfoDAO) {
        this.vmInfoDao = vmInfoDAO;
    }

    void unbindVmInfoDao(VmInfoDAO vmInfoDAO) {
        this.vmInfoDao = null;
    }

    void bindAgentInfoDao(AgentInfoDAO agentInfoDAO) {
        this.agentInfoDao = agentInfoDAO;
    }

    void unindAgentInfoDao(AgentInfoDAO agentInfoDAO) {
        this.agentInfoDao = null;
    }

}
