/*
 * Copyright 2012-2017 Red Hat, Inc.
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

import java.security.Principal;
import java.security.acl.Group;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Objects;
import java.util.Vector;

import static com.redhat.thermostat.common.utils.IteratorUtils.asList;

/**
 * Class representing a simple thermostat role. Roles can be nested.
 * 
 * @see Group
 */
public class RolePrincipal extends BasicRole {

    private static final long serialVersionUID = -7366668253791828610L;
    // the set of nested roles if any
    private final HashSet<Group> roles = new HashSet<>();

    /**
     * Creates a role principal with the specified role name containing no roles
     * within it.
     * 
     * @param roleName
     * @throws NullPointerException if roleName was null.
     */
    public RolePrincipal(String roleName) {
        super(Objects.requireNonNull(roleName));
    }

    /**
     * Adds a role this role principal.
     * 
     * @return true if and only if the role has been successfully added.
     * @throws IllegalArgumentException
     *             if the principal to be added is not a {@link Group}
     *         NullPointerException If the role was null.
     */
    @Override
    public boolean addMember(Principal role) {
        if (!(Objects.requireNonNull(role) instanceof Group)) {
            throw new IllegalArgumentException("principal not a group");
        }
        return roles.add((Group) role);
    }

    /**
     * Removes a role from this role principal.
     * 
     * @return true if the principal was successfully removed.
     */
    @Override
    public boolean removeMember(Principal role) {
        // will return false if role not a member, omit check for Group
        return roles.remove(role);
    }

    @Override
    public boolean isMember(Principal member) {
        if (roles.contains(member)) {
            return true;
        }
        // recursive case
        boolean isMember = false;
        for (Group group : roles) {
            isMember = isMember || group.isMember(member);
            if (!isMember) {
                break;
            }
        }
        return isMember;
    }

    @Override
    public Enumeration<? extends Principal> members() {
        Vector<Principal> vector = new Vector<>();
        for (Group group : roles) {
            vector.add(group);
        }
        return vector.elements();
    }

}