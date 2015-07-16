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

package com.redhat.thermostat.storage.core;

import java.util.List;
import java.util.logging.Logger;

import com.redhat.thermostat.common.utils.LoggingUtils;
import com.redhat.thermostat.storage.dao.AbstractDao;
import com.redhat.thermostat.storage.dao.AbstractDaoQuery;
import com.redhat.thermostat.storage.model.TimeStampedPojo;

/**
 * Get a {@link List} of {@link TimeStampedPojo}s newer than a given time stamp.
 *
 * @see VmTimeIntervalPojoListGetter
 */
public class VmLatestPojoListGetter<T extends TimeStampedPojo> extends AbstractDao {
    
    public static final String VM_LATEST_QUERY_FORMAT = "QUERY %s WHERE '"
            + Key.AGENT_ID.getName() + "' = ?s AND '"
            + Key.VM_ID.getName() + "' = ?s AND '" 
            + Key.TIMESTAMP.getName() + "' > ?l SORT '"
            + Key.TIMESTAMP.getName() + "' DSC";
    private static final Logger logger = LoggingUtils.getLogger(VmLatestPojoListGetter.class);
    
    private final Storage storage;
    private final Category<T> cat;
    private final String queryLatest;

    public VmLatestPojoListGetter(Storage storage, Category<T> cat) {
        this.storage = storage;
        this.cat = cat;
        this.queryLatest = String.format(VM_LATEST_QUERY_FORMAT, cat.getName());
    }

    @Deprecated
    public List<T> getLatest(VmRef vmRef, long since) {
        return getLatest(new AgentId(vmRef.getHostRef().getAgentId()), new VmId(vmRef.getVmId()), since);
    }

    public List<T> getLatest(final AgentId agentId, final VmId vmId, final long since) {
        return executeQuery(new AbstractDaoQuery<T>(storage, cat, queryLatest) {
            @Override
            public PreparedStatement<T> customize(PreparedStatement<T> preparedStatement) {
                preparedStatement.setString(0, agentId.get());
                preparedStatement.setString(1, vmId.get());
                preparedStatement.setLong(2, since);
                return preparedStatement;
            }
        }).asList();
    }
    
    // package private for tests
    String getQueryLatestDesc() {
        return queryLatest;
    }

    @Override
    public Logger getLogger() {
        return logger;
    }

}

