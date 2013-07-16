/*
 * Copyright 2012, 2013 Red Hat, Inc.
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.redhat.thermostat.common.utils.LoggingUtils;
import com.redhat.thermostat.storage.core.Cursor;
import com.redhat.thermostat.storage.core.DescriptorParsingException;
import com.redhat.thermostat.storage.core.HostRef;
import com.redhat.thermostat.storage.core.Key;
import com.redhat.thermostat.storage.core.PreparedStatement;
import com.redhat.thermostat.storage.core.Put;
import com.redhat.thermostat.storage.core.StatementDescriptor;
import com.redhat.thermostat.storage.core.StatementExecutionException;
import com.redhat.thermostat.storage.core.Storage;
import com.redhat.thermostat.storage.dao.NetworkInterfaceInfoDAO;
import com.redhat.thermostat.storage.model.NetworkInterfaceInfo;

public class NetworkInterfaceInfoDAOImpl implements NetworkInterfaceInfoDAO {

    private static final Logger logger = LoggingUtils.getLogger(NetworkInterfaceInfoDAOImpl.class);
    static final String QUERY_NETWORK_INFO = "QUERY "
            + networkInfoCategory.getName() + " WHERE '"
            + Key.AGENT_ID.getName() + "' = ?s";

    private Storage storage;

    public NetworkInterfaceInfoDAOImpl(Storage storage) {
        this.storage = storage;
        storage.registerCategory(networkInfoCategory);
    }

    @Override
    public List<NetworkInterfaceInfo> getNetworkInterfaces(HostRef ref) {
        StatementDescriptor<NetworkInterfaceInfo> desc = new StatementDescriptor<>(networkInfoCategory, QUERY_NETWORK_INFO);
        PreparedStatement<NetworkInterfaceInfo> stmt;
        Cursor<NetworkInterfaceInfo> cursor;
        try {
            stmt = storage.prepareStatement(desc);
            stmt.setString(0, ref.getAgentId());
            cursor = stmt.executeQuery();
        } catch (DescriptorParsingException e) {
            // should not happen, but if it *does* happen, at least log it
            logger.log(Level.SEVERE, "Preparing query '" + desc + "' failed!", e);
            return Collections.emptyList();
        } catch (StatementExecutionException e) {
            // should not happen, but if it *does* happen, at least log it
            logger.log(Level.SEVERE, "Executing query '" + desc + "' failed!", e);
            return Collections.emptyList();
        }

        List<NetworkInterfaceInfo> result = new ArrayList<>();
        while (cursor.hasNext()) {
            NetworkInterfaceInfo stat = cursor.next();
            result.add(stat);
        }
        return result;
    }

    @Override
    public void putNetworkInterfaceInfo(NetworkInterfaceInfo info) {
        Put replace = storage.createReplace(networkInfoCategory);
        replace.setPojo(info);
        replace.apply();
    }

}

