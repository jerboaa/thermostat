/*
 * Copyright 2012-2014 Red Hat, Inc.
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

package com.redhat.thermostat.thread.dao.impl;

import com.redhat.thermostat.storage.core.PreparedParameter;
import com.redhat.thermostat.storage.core.auth.DescriptorMetadata;
import com.redhat.thermostat.storage.core.auth.StatementDescriptorMetadataFactory;
import com.redhat.thermostat.storage.core.auth.StatementDescriptorRegistration;

import java.util.HashSet;
import java.util.Set;

/**
 * Registers prepared queries issued by this maven module via
 * via {@link ThreadDaoImpl}.
 *
 */
public class ThreadDaoImplStatementDescriptorRegistration implements
        StatementDescriptorRegistration, StatementDescriptorMetadataFactory {

    private final Set<String> descs;
    
    public ThreadDaoImplStatementDescriptorRegistration() {
        descs = new HashSet<>();
        descs.add(ThreadDaoImpl.QUERY_LATEST_DEADLOCK_INFO);
        descs.add(ThreadDaoImpl.QUERY_LATEST_HARVESTING_STATUS);

        // TODO: this needs to go in an helper class
        descs.addAll(ThreadDaoImpl.SUMMARY.describeStatements());
        descs.addAll(ThreadDaoImpl.SESSIONS.describeStatements());

        descs.add(ThreadDaoImpl.DESC_ADD_THREAD_DEADLOCK_DATA);
        descs.add(ThreadDaoImpl.DESC_ADD_THREAD_HARVESTING_STATUS);

        descs.add(ThreadDaoImpl.ADD_THREAD_HEADER);
        descs.add(ThreadDaoImpl.QUERY_THREAD_HEADER);
        descs.add(ThreadDaoImpl.QUERY_ALL_THREAD_HEADERS);

        descs.add(ThreadDaoImpl.ADD_THREAD_STATE);
        descs.add(ThreadDaoImpl.QUERY_LATEST_THREAD_STATE_FOR_THREAD);
        descs.add(ThreadDaoImpl.QUERY_FIRST_THREAD_STATE_FOR_THREAD);

        descs.add(ThreadDaoImpl.QUERY_OLDEST_THREAD_STATE);
        descs.add(ThreadDaoImpl.QUERY_LATEST_THREAD_STATE);

        descs.add(ThreadDaoImpl.QUERY_THREAD_STATE_PER_THREAD);

        descs.add(ThreadDaoImpl.ADD_CONTENTION_SAMPLE);
        descs.add(ThreadDaoImpl.GET_LATEST_CONTENTION_SAMPLE);
        descs.add(ThreadDaoImpl.DESC_UPDATE_THREAD_STATE);
    }
    
    @Override
    public Set<String> getStatementDescriptors() {
        return descs;
    }

    @Override
    public DescriptorMetadata getDescriptorMetadata(String descriptor,
            PreparedParameter[] params) {
        if (descriptor.equals(ThreadDaoImpl.QUERY_THREAD_STATE_PER_THREAD) ||
                descriptor.equals(ThreadDaoImpl.GET_LATEST_CONTENTION_SAMPLE)) {
            // no agent/vm ids in statement.
            return new DescriptorMetadata();
        } else if (descriptor.equals(ThreadDaoImpl.QUERY_LATEST_THREAD_STATE_FOR_THREAD) ||
                descriptor.equals(ThreadDaoImpl.QUERY_FIRST_THREAD_STATE_FOR_THREAD)) {
            String agentId = (String)params[0].getValue();
            return new DescriptorMetadata(agentId);
        } else if (descs.contains(descriptor)) {
            // All other queries have agentId/vmId parameters
            String agentId = (String)params[0].getValue();
            String vmId = (String)params[1].getValue();
            DescriptorMetadata metadata = new DescriptorMetadata(agentId, vmId);
            return metadata;
        } else {
            throw new IllegalArgumentException("Unknown statement ->" + descriptor + "<-");
        }
    }

}

