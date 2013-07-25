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

package com.redhat.thermostat.web.server.auth;

import java.util.Objects;
import java.util.Set;

import com.redhat.thermostat.storage.core.Category;
import com.redhat.thermostat.storage.core.Key;
import com.redhat.thermostat.storage.core.StatementDescriptor;
import com.redhat.thermostat.storage.core.auth.DescriptorMetadata;
import com.redhat.thermostat.storage.model.Pojo;
import com.redhat.thermostat.storage.query.Expression;
import com.redhat.thermostat.storage.query.ExpressionFactory;
import com.redhat.thermostat.web.server.auth.FilterResult.ResultType;

/**
 * Filters queries based on granted agent IDs.
 * 
 * @see also {@link Roles#GRANT_AGENTS_READ_ALL}
 */
class AgentIdFilter<T extends Pojo> extends AbstractFilter<T> {
    
    static final RolePrincipal GRANT_AGENTS_READ_ALL = new RolePrincipal(Roles.GRANT_AGENTS_READ_ALL);
    static final String AGENTS_BY_AGENT_ID_GRANT_ROLE_PREFIX = "thermostat-agents-grant-read-agentId-";
    
    AgentIdFilter(Set<BasicRole> userRoles) {
        super(userRoles);
    }

    @Override
    public FilterResult applyFilter(StatementDescriptor<T> desc,
            DescriptorMetadata metaData, Expression parentExpression) {
        if (userRoles.contains(GRANT_AGENTS_READ_ALL)) {
            return allWithExpression(parentExpression);
        }
        Category<T> category = desc.getCategory();
        // user cannot read all agents
        if (category.getKey(Key.AGENT_ID.getName()) != null) {
            if (metaData.hasAgentId()) {
                // if given agent ID not in granted list, return empty
                String agentId = Objects.requireNonNull(metaData.getAgentId());
                RolePrincipal agentIdGrantRole = new RolePrincipal(AGENTS_BY_AGENT_ID_GRANT_ROLE_PREFIX + agentId);
                if (!userRoles.contains(agentIdGrantRole)) {
                    return new FilterResult(ResultType.EMPTY);
                } else {
                    // agentId allowed
                    return allWithExpression(parentExpression);
                }
            } else {
                // tag on in clause for agentId
                ExpressionFactory factory = new ExpressionFactory();
                Set<String> agentIds = getGrantedAgentsByAgentId();
                Expression filterExpression = factory.in(Key.AGENT_ID, agentIds, String.class);
                FilterResult result = new FilterResult(ResultType.QUERY_EXPRESSION);
                if (parentExpression != null) {
                    filterExpression = factory.and(parentExpression, filterExpression);
                }
                result.setFilterExpression(filterExpression);
                return result;
            }
        } else {
            // can't do anything here, let it through for next stage.
            return allWithExpression(parentExpression);
        }
    }

    private Set<String> getGrantedAgentsByAgentId() {
        return getGranted(AGENTS_BY_AGENT_ID_GRANT_ROLE_PREFIX);
    }
}
