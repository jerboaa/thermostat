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

import java.io.Serializable;
import java.security.Principal;
import java.security.acl.Group;
import java.util.Objects;

/**
 * Base class for all thermostat roles.
 * 
 * To be precise, every {@link Principal} which does NOT have the same name
 * as the currently logged in user, are BasicRole principals. The name of a
 * Principal is defined by {@link Principal#getName()}.
 *
 */
public abstract class BasicRole implements Group, Serializable {
    
    private static final long serialVersionUID = -4572772782292794645L;
    protected String name;
    
    public BasicRole(String name) {
        this.name = name;
    }

    /**
     * Compares two BasicRoles.
     * 
     * @return true if and only if other is a BasicRole and its name is the same
     *         as this role.
     */
    @Override
    public boolean equals(Object other) {
        if (!(other instanceof BasicRole)) {
            return false;
        }
        String otherName = ((Principal) other).getName();
        return Objects.equals(name, otherName);
    }
    
    @Override
    public int hashCode() {
        return (name == null ? 0 : name.hashCode());
    }
    
    @Override
    public String getName() {
        return name;
    }
}

