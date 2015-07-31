/*
 * Copyright 2012-2015 Red Hat, Inc.
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

package com.redhat.thermostat.setup.command.internal;

import java.io.IOException;

public interface ThermostatSetup {

    /**
     * Provided a username and password,
     * creates a MongodbUser
     *
     * @param username
     * @param password
     * @throws MongodbUserSetupException
     */
    void createMongodbUser(String username, char[] password) throws MongodbUserSetupException;

    /**
     * Creates web.auth file and sets
     * users.properties and
     * roles.properties for a user
     *
     * @param username
     * @param password
     * @param roles
     * @throws IOException
     */
    void createThermostatUser(String username, char[] password, String[] roles) throws IOException;

    /**
     * Checks if webapp is installed on the system
     *
     * @return true if webapp is installed, false otherwise
     */
    boolean isWebAppInstalled();
}
