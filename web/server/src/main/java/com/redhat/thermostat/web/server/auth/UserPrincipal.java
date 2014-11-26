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

import java.io.Serializable;
import java.security.Principal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import com.redhat.thermostat.storage.core.StatementDescriptor;
import com.redhat.thermostat.storage.model.Pojo;
import com.redhat.thermostat.storage.query.Expression;
import com.redhat.thermostat.web.server.auth.FilterResult.ResultType;

/**
 * Class representing thermostat users
 *
 */
public class UserPrincipal implements Serializable, Principal {
    
    private static final RolePrincipal GRANT_READ_ALL = new RolePrincipal(Roles.GRANT_READ_ALL);
    
    private static final Set<BasicRole> EMPTY_SET = new HashSet<>(0);
    private static final long serialVersionUID = 2646753284881445421L;
    // The set of roles this user is a member of (they may be nested)
    private Set<BasicRole> roles;
    // The name of this principal
    private String name;
    
    /**
     * Creates a new user principal with the given name
     * 
     * @param name The user name.
     * @throws NullPointerException if name is null.
     */
    public UserPrincipal(String name) {
        this.name = Objects.requireNonNull(name);
    }

    /**
     * 
     * @return The set of roles this principal is a member of. An empty set
     *         if this user has no role memberships.
     */
    public final Set<BasicRole> getRoles() {
        if (roles == null) {
            return EMPTY_SET;
        }
        return roles;
    }

    /**
     * Sets the set of roles which this principal is a member of.
     * 
     * @param roles
     * @throws NullPointerException If the given role set was null.
     */
    public void setRoles(Set<BasicRole> roles) {
        this.roles = Objects.requireNonNull(roles);
    }
    
    /**
     * Prepare a read filter for this user which can be applied prior executing
     * trusted prepared queries.
     * 
     * @param desc
     *            The descriptor for which to get the filter.
     * @param metaData
     *            Metadata for the provided descriptor.
     * 
     * @return An {@link FilterResult} which can be used to make a decision on
     *         which records to return.
     */
    public <T extends Pojo> FilterResult getReadFilter(StatementDescriptor<T> desc) {
        if (getRoles().contains(GRANT_READ_ALL)) {
            // user can see everything, no filtering is happening at all.
            return new FilterResult(ResultType.ALL);
        }
        List<StatementFilter<T>> filters = buildFilters();
        
        // perform filtering using our list of filters
        Expression parentExpression = null;
        FilterResult overallResult = null;
        for (StatementFilter<T> filter: filters) {
            overallResult = filter.applyFilter(desc, parentExpression);
            switch (overallResult.getType()) {
            case ALL: // fall-through, expression == null
            case QUERY_EXPRESSION:
                // continue filtering
                parentExpression = overallResult.getFilterExpression();
                break;
            case EMPTY:
                // no point continuing, already nothing
                return overallResult;
            default:
                throw new IllegalStateException("Unknown result type!");
            }
        }
        // done filtering
        return overallResult;
    }

    /*
     * Filters are applied in order. Passing through one which didn't return
     * empty continues the filter chain. Currently, we filter by:
     * agent IDs -> hostnames -> vm IDs -> vm usernames
     */
    private <T extends Pojo> List<StatementFilter<T>> buildFilters() {
        List<StatementFilter<T>> filters = new ArrayList<>();
        Set<BasicRole> roles = getRoles();
        AgentIdFilter<T> agentIdFilter = new AgentIdFilter<>(roles);
        HostnameFilter<T> hostnameFilter = new HostnameFilter<>(roles);
        VmIdFilter<T> vmIdFilter = new VmIdFilter<>(roles);
        VmUsernameFilter<T> vmUsernameFilter = new VmUsernameFilter<>(roles);
        filters.add(agentIdFilter);
        filters.add(hostnameFilter);
        filters.add(vmIdFilter);
        filters.add(vmUsernameFilter);
        return filters;
    }
    
    @Override
    public String getName() {
        return name;
    }
    
    @Override
    public boolean equals(Object other) {
        if (!(other instanceof Principal)) {
            return false;
        }
        String otherName = ((Principal) other).getName();
        boolean equals = false;
        if (name == null) {
            equals = otherName == null;
        } else {
            equals = name.equals(otherName);
        }
        return equals;
    }
    
    @Override
    public int hashCode() {
        return (name == null ? 0 : name.hashCode());
    }
    
    @Override
    public String toString() {
        return this.getClass().getName() + ": " + name;
    }
}

