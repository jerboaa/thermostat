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

package com.redhat.thermostat.vm.classstat.common.internal;

import java.util.HashSet;
import java.util.Set;

import com.redhat.thermostat.storage.core.PreparedParameter;
import com.redhat.thermostat.storage.core.VmLatestPojoListGetter;
import com.redhat.thermostat.storage.core.auth.DescriptorMetadata;
import com.redhat.thermostat.storage.core.auth.StatementDescriptorRegistration;
import com.redhat.thermostat.vm.classstat.common.VmClassStatDAO;

/**
 * Registers the prepared query issued by this maven module via
 * {@link VmLatestPojoListGetter}.
 *
 */
public class VmClassStatDAOImplStatementDescriptorRegistration implements
        StatementDescriptorRegistration {
    
    static final String QUERY = String.format(VmLatestPojoListGetter.VM_LATEST_QUERY_FORMAT,
            VmClassStatDAO.vmClassStatsCategory.getName());

    @Override
    public Set<String> getStatementDescriptors() {
        Set<String> descs = new HashSet<>(1);
        descs.add(QUERY);
        descs.add(VmClassStatDAOImpl.DESC_ADD_VM_CLASS_STAT);
        return descs;
    }

    @Override
    public DescriptorMetadata getDescriptorMetadata(String descriptor,
            PreparedParameter[] params) {
        if (descriptor.equals(QUERY)) {
            String agentId = (String)params[0].getValue();
            String vmId = (String)params[1].getValue();
            DescriptorMetadata metadata = new DescriptorMetadata(agentId, vmId);
            return metadata;
        } else {
            throw new IllegalArgumentException("Unknown descriptor ->" + descriptor + "<-");
        }
    }

}
