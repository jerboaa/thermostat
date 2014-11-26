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

package com.redhat.thermostat.web.server.auth;

import java.util.Set;

import com.redhat.thermostat.storage.core.Category;
import com.redhat.thermostat.storage.core.Key;
import com.redhat.thermostat.storage.core.StatementDescriptor;
import com.redhat.thermostat.storage.model.Pojo;
import com.redhat.thermostat.storage.query.Expression;
import com.redhat.thermostat.storage.query.ExpressionFactory;
import com.redhat.thermostat.web.server.auth.FilterResult.ResultType;

/**
 * Filters based on granted VM IDs.
 * 
 * @see also {@link Roles#GRANT_VMS_READ_BY_VM_ID_ALL}
 */
class VmIdFilter<T extends Pojo> extends AbstractFilter<T> {
    
    static final RolePrincipal GRANT_VMS_BY_ID_READ_ALL = new RolePrincipal(Roles.GRANT_VMS_READ_BY_VM_ID_ALL);
    static final String VMS_BY_VM_ID_GRANT_ROLE_PREFIX = "thermostat-vms-grant-read-vmId-";
    
    VmIdFilter(Set<BasicRole> userRoles) {
        super(userRoles);
    }

    @Override
    public FilterResult applyFilter(StatementDescriptor<T> desc,
                                    Expression parentExpression) {
        if (userRoles.contains(GRANT_VMS_BY_ID_READ_ALL)) {
            return allWithExpression(parentExpression);
        }
        // perform filtering on vmId
        Category<T> category = desc.getCategory();
        if (category.getKey(Key.VM_ID.getName()) != null) {
            // add vmId IN clause
            ExpressionFactory factory = new ExpressionFactory();
            Set<String> vmIds = getGrantedVmsByVmId();
            Expression filterExpression = factory.in(Key.VM_ID, vmIds, String.class);
            if (parentExpression != null) {
                filterExpression = factory.and(parentExpression, filterExpression);
            }
            FilterResult result = new FilterResult(ResultType.QUERY_EXPRESSION);
            result.setFilterExpression(filterExpression);
            return result;
        } else {
            // can't do much
            return allWithExpression(parentExpression);
        }
    }
    
    private Set<String> getGrantedVmsByVmId() {
        return getGranted(VMS_BY_VM_ID_GRANT_ROLE_PREFIX);
    }

}

