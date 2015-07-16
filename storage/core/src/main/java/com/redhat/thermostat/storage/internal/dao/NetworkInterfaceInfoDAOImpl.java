/*
 * Copyright 2012-2015 Red Hat, Inc.
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

import java.util.List;
import java.util.logging.Logger;

import com.redhat.thermostat.common.utils.LoggingUtils;
import com.redhat.thermostat.storage.core.HostRef;
import com.redhat.thermostat.storage.core.Key;
import com.redhat.thermostat.storage.core.PreparedStatement;
import com.redhat.thermostat.storage.core.Storage;
import com.redhat.thermostat.storage.dao.AbstractDao;
import com.redhat.thermostat.storage.dao.AbstractDaoQuery;
import com.redhat.thermostat.storage.dao.AbstractDaoStatement;
import com.redhat.thermostat.storage.dao.NetworkInterfaceInfoDAO;
import com.redhat.thermostat.storage.model.NetworkInterfaceInfo;

public class NetworkInterfaceInfoDAOImpl extends AbstractDao implements NetworkInterfaceInfoDAO {

    private static final Logger logger = LoggingUtils.getLogger(NetworkInterfaceInfoDAOImpl.class);
    static final String QUERY_NETWORK_INFO = "QUERY "
            + networkInfoCategory.getName() + " WHERE '"
            + Key.AGENT_ID.getName() + "' = ?s";
    // REPLACE network-info SET 'agentId' = ?s , \
    //                          'interfaceName' = ?s , \
    //                          'ip4Addr' = ?s , \
    //                          'ip6Addr' = ?s
    //                      WHERE 'agentId' = ?s AND 'interfaceName' = ?s
    static final String DESC_REPLACE_NETWORK_INFO = "REPLACE " +
                            networkInfoCategory.getName() +
                 " SET " +
                    "'" + Key.AGENT_ID.getName() + "' = ?s , " +
                    "'" + ifaceKey.getName() + "' = ?s , " +
                    "'" + ip4AddrKey.getName() + "' = ?s , " +
                    "'" + ip6AddrKey.getName() + "' = ?s " +
                 "WHERE '" + Key.AGENT_ID.getName() + "' = ?s AND " +
                       "'" + ifaceKey.getName() + "' = ?s"; 
                                

    private final Storage storage;

    public NetworkInterfaceInfoDAOImpl(Storage storage) {
        this.storage = storage;
        storage.registerCategory(networkInfoCategory);
    }

    @Override
    public List<NetworkInterfaceInfo> getNetworkInterfaces(final HostRef ref) {
        return executeQuery(
                new AbstractDaoQuery<NetworkInterfaceInfo>(storage, networkInfoCategory, QUERY_NETWORK_INFO) {
                    @Override
                    public PreparedStatement<NetworkInterfaceInfo> customize(PreparedStatement<NetworkInterfaceInfo> preparedStatement) {
                        preparedStatement.setString(0, ref.getAgentId());
                        return preparedStatement;
                    }
                }).asList();
    }

    @Override
    public void putNetworkInterfaceInfo(final NetworkInterfaceInfo info) {
        executeStatement(
                new AbstractDaoStatement<NetworkInterfaceInfo>(storage, networkInfoCategory, DESC_REPLACE_NETWORK_INFO) {
                    @Override
                    public PreparedStatement<NetworkInterfaceInfo> customize(PreparedStatement<NetworkInterfaceInfo> preparedStatement) {
                        // SET params.
                        preparedStatement.setString(0, info.getAgentId());
                        preparedStatement.setString(1, info.getInterfaceName());
                        preparedStatement.setString(2, info.getIp4Addr());
                        preparedStatement.setString(3, info.getIp6Addr());
                        // WHERE params.
                        preparedStatement.setString(4, info.getAgentId());
                        preparedStatement.setString(5, info.getInterfaceName());
                        return preparedStatement;
                    }
                });
    }

    @Override
    protected Logger getLogger() {
        return logger;
    }
}

