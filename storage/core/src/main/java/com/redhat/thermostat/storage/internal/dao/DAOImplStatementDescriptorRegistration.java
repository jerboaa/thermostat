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

package com.redhat.thermostat.storage.internal.dao;

import java.util.HashSet;
import java.util.Set;

import com.redhat.thermostat.storage.core.PreparedParameter;
import com.redhat.thermostat.storage.core.auth.StatementDescriptorRegistration;

/**
 * Registers prepared queries issued by this maven module
 * via various DAOs.
 *
 */
public class DAOImplStatementDescriptorRegistration implements
        StatementDescriptorRegistration {
    
    @Override
    public Set<String> getStatementDescriptors() {
        Set<String> daoDescs = new HashSet<>();
        daoDescs.add(AgentInfoDAOImpl.QUERY_AGENT_INFO);
        daoDescs.add(AgentInfoDAOImpl.QUERY_ALIVE_AGENTS);
        daoDescs.add(AgentInfoDAOImpl.QUERY_ALL_AGENTS);
        daoDescs.add(AgentInfoDAOImpl.AGGREGATE_COUNT_ALL_AGENTS);
        daoDescs.add(AgentInfoDAOImpl.DESC_ADD_AGENT_INFO);
        daoDescs.add(AgentInfoDAOImpl.DESC_REMOVE_AGENT_INFO);
        daoDescs.add(AgentInfoDAOImpl.DESC_UPDATE_AGENT_INFO);
        daoDescs.add(BackendInfoDAOImpl.QUERY_BACKEND_INFO);
        daoDescs.add(BackendInfoDAOImpl.DESC_ADD_BACKEND_INFO);
        daoDescs.add(BackendInfoDAOImpl.DESC_REMOVE_BACKEND_INFO);
        daoDescs.add(HostInfoDAOImpl.QUERY_HOST_INFO);
        daoDescs.add(HostInfoDAOImpl.QUERY_ALL_HOSTS);
        daoDescs.add(HostInfoDAOImpl.AGGREGATE_COUNT_ALL_HOSTS);
        daoDescs.add(HostInfoDAOImpl.DESC_ADD_HOST_INFO);
        daoDescs.add(NetworkInterfaceInfoDAOImpl.QUERY_NETWORK_INFO);
        daoDescs.add(NetworkInterfaceInfoDAOImpl.DESC_REPLACE_NETWORK_INFO);
        daoDescs.add(VmInfoDAOImpl.QUERY_ALL_VMS_FOR_HOST);
        daoDescs.add(VmInfoDAOImpl.QUERY_ALL_VMS);
        daoDescs.add(VmInfoDAOImpl.QUERY_VM_INFO);
        daoDescs.add(VmInfoDAOImpl.AGGREGATE_COUNT_ALL_VMS);
        daoDescs.add(VmInfoDAOImpl.DESC_ADD_VM_INFO);
        daoDescs.add(VmInfoDAOImpl.DESC_UPDATE_VM_STOP_TIME);
        daoDescs.add(VmInfoDAOImpl.QUERY_VM_FROM_ID);
        daoDescs.add(SchemaInfoDAOImpl.QUERY_ALL_COLLECTIONS);

        return daoDescs;
    }

}

