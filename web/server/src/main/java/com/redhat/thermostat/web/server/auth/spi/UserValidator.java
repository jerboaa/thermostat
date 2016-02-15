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

package com.redhat.thermostat.web.server.auth.spi;

import java.util.Set;

/**
 * Validates user's password credentials against an internal user database.
 *
 */
public interface UserValidator {

    /**
     * Validates the given username/password combination. If no
     * {@link UserValidationException} is thrown validation was successful.
     * 
     * @param username The user to validate against the internal database.
     * @param password The password of <code>username</code>
     * @throws UserValidationException If the user could not be validated. Reasons
     *  as to why this exception may be thrown include:
     *  <ul>
     *    <li>The user does not exist in the database</li>
     *    <li>Invalid credentials were provided</li>
     *  </ul>
     */
    void authenticate(String username, char[] password) throws UserValidationException;

    /**
     * Get a read-only set of all users the system knows about. All members of
     * the set are Strings.
     * 
     * @return All known users.
     * @throws IllegalStateException If no user database is available.
     */
    Set<Object> getAllKnownUsers() throws IllegalStateException;
}

