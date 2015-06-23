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

package com.redhat.thermostat.itest;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public class WebStorageUsingIntegrationTest extends IntegrationTest {

    protected static final String THERMOSTAT_USERS_FILE = getConfigurationDir() + "/thermostat-users.properties";
    protected static final String THERMOSTAT_ROLES_FILE = getConfigurationDir() + "/thermostat-roles.properties";
    protected static final String THERMOSTAT_WEB_AUTH_FILE = getConfigurationDir() + "/web.auth";
    
    private static Path backupUsers;
    private static Path backupRoles;
    private static Path backupWebAuth;
    
    protected static void backupOriginalCredentialsFiles() throws IOException {
        backupUsers = Files.createTempFile("itest-backup-thermostat-users", "");
        backupRoles = Files.createTempFile("itest-backup-thermostat-roles", "");
        backupWebAuth = Files.createTempFile("itest-backup-webapp-auth", "");
        backupRoles.toFile().deleteOnExit();
        backupUsers.toFile().deleteOnExit();
        backupWebAuth.toFile().deleteOnExit();
        Files.copy(new File(THERMOSTAT_USERS_FILE).toPath(), backupUsers, StandardCopyOption.REPLACE_EXISTING);
        Files.copy(new File(THERMOSTAT_ROLES_FILE).toPath(), backupRoles, StandardCopyOption.REPLACE_EXISTING);
        Files.copy(new File(THERMOSTAT_WEB_AUTH_FILE).toPath(), backupWebAuth, StandardCopyOption.REPLACE_EXISTING);
    }
    
    protected static void restoreBackedUpCredentialsFiles() throws IOException {
        Files.copy(backupUsers, new File(THERMOSTAT_USERS_FILE).toPath(), StandardCopyOption.REPLACE_EXISTING);
        Files.copy(backupRoles, new File(THERMOSTAT_ROLES_FILE).toPath(), StandardCopyOption.REPLACE_EXISTING);
        Files.copy(backupWebAuth, new File(THERMOSTAT_WEB_AUTH_FILE).toPath(), StandardCopyOption.REPLACE_EXISTING);
    }
}
