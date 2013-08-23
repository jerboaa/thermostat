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

package com.redhat.thermostat.numa.common.internal;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.redhat.thermostat.common.utils.LoggingUtils;
import com.redhat.thermostat.numa.common.NumaDAO;
import com.redhat.thermostat.numa.common.NumaHostInfo;
import com.redhat.thermostat.numa.common.NumaStat;
import com.redhat.thermostat.storage.core.Add;
import com.redhat.thermostat.storage.core.Cursor;
import com.redhat.thermostat.storage.core.DescriptorParsingException;
import com.redhat.thermostat.storage.core.HostLatestPojoListGetter;
import com.redhat.thermostat.storage.core.HostRef;
import com.redhat.thermostat.storage.core.Key;
import com.redhat.thermostat.storage.core.PreparedStatement;
import com.redhat.thermostat.storage.core.StatementDescriptor;
import com.redhat.thermostat.storage.core.StatementExecutionException;
import com.redhat.thermostat.storage.core.Storage;

public class NumaDAOImpl implements NumaDAO {

    private static final Logger logger = LoggingUtils.getLogger(NumaDAOImpl.class);
    static final String QUERY_NUMA_INFO = "QUERY "
            + numaHostCategory.getName() + " WHERE '" 
            + Key.AGENT_ID.getName() + "' = ?s LIMIT 1";
    
    private final Storage storage;
    private final HostLatestPojoListGetter<NumaStat> getter;

    NumaDAOImpl(Storage storage) {
        this.storage = storage;
        storage.registerCategory(numaStatCategory);
        storage.registerCategory(numaHostCategory);
        this.getter = new HostLatestPojoListGetter<>(storage, numaStatCategory);
    }

    @Override
    public void putNumaStat(NumaStat stat) {
        Add<NumaStat> add = storage.createAdd(numaStatCategory);
        add.setPojo(stat);
        add.apply();
    }

    @Override
    public List<NumaStat> getLatestNumaStats(HostRef ref, long lastTimeStamp) {
        return getter.getLatest(ref, lastTimeStamp);
    }

    @Override
    public void putNumberOfNumaNodes(int numNodes) {
        Add<NumaHostInfo> replace = storage.createAdd(numaHostCategory);
        NumaHostInfo numaHostInfo = new NumaHostInfo();
        numaHostInfo.setNumNumaNodes(numNodes);
        replace.setPojo(numaHostInfo);
        replace.apply();
    }

    @Override
    public int getNumberOfNumaNodes(HostRef ref) {
        StatementDescriptor<NumaHostInfo> desc = new StatementDescriptor<>(numaHostCategory, QUERY_NUMA_INFO);
        PreparedStatement<NumaHostInfo> stmt;
        Cursor<NumaHostInfo> cursor;
        try {
            stmt = storage.prepareStatement(desc);
            stmt.setString(0, ref.getAgentId());
            cursor = stmt.executeQuery();
        } catch (DescriptorParsingException e) {
            // should not happen, but if it *does* happen, at least log it
            logger.log(Level.SEVERE, "Preparing query '" + desc + "' failed!", e);
            return 0;
        } catch (StatementExecutionException e) {
            // should not happen, but if it *does* happen, at least log it
            logger.log(Level.SEVERE, "Executing query '" + desc + "' failed!", e);
            return 0;
        }
        
        if (cursor.hasNext()) {
            return cursor.next().getNumNumaNodes();
        } else {
            return 0;
        }
    }
}

