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

package com.redhat.thermostat.vm.byteman.common.internal;

import java.util.List;
import java.util.logging.Logger;

import com.redhat.thermostat.common.utils.LoggingUtils;
import com.redhat.thermostat.storage.core.AgentId;
import com.redhat.thermostat.storage.core.Key;
import com.redhat.thermostat.storage.core.PreparedStatement;
import com.redhat.thermostat.storage.core.Storage;
import com.redhat.thermostat.storage.core.VmId;
import com.redhat.thermostat.storage.core.VmTimeIntervalPojoListGetter;
import com.redhat.thermostat.storage.dao.AbstractDao;
import com.redhat.thermostat.storage.dao.AbstractDaoStatement;
import com.redhat.thermostat.vm.byteman.common.BytemanMetric;
import com.redhat.thermostat.vm.byteman.common.VmBytemanMetricDAO;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;

@Component
@Service(value = VmBytemanMetricDAO.class)
public class VmBytemanMetricDAOImpl extends AbstractDao implements VmBytemanMetricDAO {
    
    static final String STATEMENT_DESCRIPTOR = "ADD " + CATEGORY.getName() +
            " SET '" + Key.AGENT_ID.getName() + "' = ?s , " +
                 "'" + Key.VM_ID.getName() + "' = ?s , " +
                 "'" + Key.TIMESTAMP.getName() + "' = ?l , " +
                 "'" + MARKER.getName() + "' = ?s , " +
                 "'" + DATA.getName() + "' = ?s";
    
    @Reference
    private Storage storage;
    private VmTimeIntervalPojoListGetter<BytemanMetric> intervalGetter;
    
    public VmBytemanMetricDAOImpl() {
        // Default constructor for DS
    }
    
    VmBytemanMetricDAOImpl(Storage storage) {
        bindStorage(storage);
    }
    
    protected void bindStorage(Storage storage) {
        this.storage = storage;
    }
    
    protected void unbindStorage(Storage storage) {
        this.storage = null;
    }
    
    @Activate
    private void activate() {
        storage.registerCategory(CATEGORY);
        intervalGetter = new VmTimeIntervalPojoListGetter<>(storage, CATEGORY);
    }

    @Override
    public void putMetric(final BytemanMetric metric) {
        executeStatement(new AbstractDaoStatement<BytemanMetric>(storage, CATEGORY, STATEMENT_DESCRIPTOR) {

            @Override
            public PreparedStatement<BytemanMetric> customize(PreparedStatement<BytemanMetric> preparedStatement) {
                preparedStatement.setString(0, metric.getAgentId());
                preparedStatement.setString(1, metric.getVmId());
                preparedStatement.setLong(2, metric.getTimeStamp());
                preparedStatement.setString(3, metric.getMarker());
                preparedStatement.setString(4, metric.getData());
                return preparedStatement;
            }
        });

    }

    @Override
    public List<BytemanMetric> findBytemanMetrics(long from, long to,
            VmId vmId, AgentId agentId) {
        return intervalGetter.getLatest(agentId, vmId, from, to);
    }

    @Override
    protected Logger getLogger() {
        return LoggingUtils.getLogger(VmBytemanMetricDAOImpl.class);
    }

}
