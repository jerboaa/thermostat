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

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;

import com.redhat.thermostat.common.model.Range;
import com.redhat.thermostat.common.utils.LoggingUtils;
import com.redhat.thermostat.storage.core.AgentId;
import com.redhat.thermostat.storage.core.Category;
import com.redhat.thermostat.storage.core.Key;
import com.redhat.thermostat.storage.core.PreparedStatement;
import com.redhat.thermostat.storage.core.Storage;
import com.redhat.thermostat.storage.core.VmId;
import com.redhat.thermostat.storage.core.VmTimeIntervalPojoListGetter;
import com.redhat.thermostat.storage.dao.AbstractDao;
import com.redhat.thermostat.storage.dao.AbstractDaoQuery;
import com.redhat.thermostat.storage.dao.AbstractDaoStatement;
import com.redhat.thermostat.vm.byteman.common.BytemanMetric;
import com.redhat.thermostat.vm.byteman.common.VmBytemanDAO;
import com.redhat.thermostat.vm.byteman.common.VmBytemanStatus;

@Component
@Service(value = VmBytemanDAO.class)
public class VmBytemanDAOImpl extends AbstractDao implements VmBytemanDAO {
    
    static final Key<String> MARKER = new Key<>("marker");
    static final Key<String> DATA = new Key<>("data");
    static final Key<String> RULE = new Key<>("rule");
    static final Key<Integer> PORT = new Key<>("listenPort");
    
    static final Category<BytemanMetric> VM_BYTEMAN_METRICS_CATEGORY = new Category<>(
            "vm-byteman-metrics",
            BytemanMetric.class,
            Key.AGENT_ID, Key.VM_ID, Key.TIMESTAMP,
            MARKER, DATA);
    
    static final Category<VmBytemanStatus> VM_BYTEMAN_STATUS_CATEGORY = new Category<>(
            "vm-byteman-status",
            VmBytemanStatus.class,
            Key.AGENT_ID, Key.VM_ID, Key.TIMESTAMP,
            RULE, PORT);
    
    static final String REPLACE_OR_ADD_STATUS_DESC = "REPLACE " + VM_BYTEMAN_STATUS_CATEGORY.getName() +
            " SET '" + Key.AGENT_ID.getName() + "' = ?s , " +
                 "'" + Key.VM_ID.getName() + "' = ?s , " +
                 "'" + Key.TIMESTAMP.getName() + "' = ?l , " +
                 "'" + RULE.getName() + "' = ?s , " +
                 "'" + PORT.getName() + "' = ?i WHERE "
                         + "'" + Key.VM_ID.getName() + "' = ?s";
    
    static final String ADD_METRIC_DESC = "ADD " + VM_BYTEMAN_METRICS_CATEGORY.getName() +
            " SET '" + Key.AGENT_ID.getName() + "' = ?s , " +
                 "'" + Key.VM_ID.getName() + "' = ?s , " +
                 "'" + Key.TIMESTAMP.getName() + "' = ?l , " +
                 "'" + MARKER.getName() + "' = ?s , " +
                 "'" + DATA.getName() + "' = ?s";
    
    static final String QUERY_VM_BYTEMAN_STATUS = "QUERY " + VM_BYTEMAN_STATUS_CATEGORY.getName() +
            " WHERE '" + Key.VM_ID.getName() + "' = ?s LIMIT 1";
    
    @Reference
    private Storage storage;
    private VmTimeIntervalPojoListGetter<BytemanMetric> intervalGetter;
    
    public VmBytemanDAOImpl() {
        // Default constructor for DS
    }
    
    VmBytemanDAOImpl(Storage storage) {
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
        storage.registerCategory(VM_BYTEMAN_METRICS_CATEGORY);
        storage.registerCategory(VM_BYTEMAN_STATUS_CATEGORY);
        intervalGetter = new VmTimeIntervalPojoListGetter<>(storage, VM_BYTEMAN_METRICS_CATEGORY);
    }

    @Override
    public void addMetric(final BytemanMetric metric) {
        executeStatement(new AbstractDaoStatement<BytemanMetric>(storage, VM_BYTEMAN_METRICS_CATEGORY, ADD_METRIC_DESC) {

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
    public List<BytemanMetric> findBytemanMetrics(Range<Long> timeRange,
            VmId vmId, AgentId agentId) {
        return intervalGetter.getLatest(agentId, vmId, timeRange.getMin(), timeRange.getMax());
    }

    @Override
    protected Logger getLogger() {
        return LoggingUtils.getLogger(VmBytemanDAOImpl.class);
    }

    @Override
    public void addOrReplaceBytemanStatus(final VmBytemanStatus status) {
        executeStatement(new AbstractDaoStatement<VmBytemanStatus>(storage, VM_BYTEMAN_STATUS_CATEGORY, REPLACE_OR_ADD_STATUS_DESC) {

            @Override
            public PreparedStatement<VmBytemanStatus> customize(PreparedStatement<VmBytemanStatus> preparedStatement) {
                preparedStatement.setString(0, status.getAgentId());
                preparedStatement.setString(1, status.getVmId());
                preparedStatement.setLong(2, status.getTimeStamp());
                preparedStatement.setString(3, status.getRule());
                preparedStatement.setInt(4, status.getListenPort());
                preparedStatement.setString(5, status.getVmId());
                return preparedStatement;
            }
        });
        
    }

    @Override
    public VmBytemanStatus findBytemanStatus(final VmId vmId) {
        List<VmBytemanStatus> result = executeQuery(new AbstractDaoQuery<VmBytemanStatus>(storage, VM_BYTEMAN_STATUS_CATEGORY, QUERY_VM_BYTEMAN_STATUS) {

            @Override
            public PreparedStatement<VmBytemanStatus> customize(PreparedStatement<VmBytemanStatus> preparedStatement) {
                preparedStatement.setString(0, vmId.get());
                return preparedStatement;
            }
        }).asList();
        if (result.isEmpty()) {
            return null;
        }
        return result.get(0);
    }

}
