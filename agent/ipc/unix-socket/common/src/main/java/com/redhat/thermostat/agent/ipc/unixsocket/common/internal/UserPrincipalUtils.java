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

package com.redhat.thermostat.agent.ipc.unixsocket.common.internal;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.UserPrincipal;

public class UserPrincipalUtils {
    
    private final SystemHelper helper;
    
    public UserPrincipalUtils() {
        this(new SystemHelper());
    }
    
    UserPrincipalUtils(SystemHelper helper) {
        this.helper = helper;
    }
    
    public UserPrincipal getCurrentUser() throws IOException {
        // Use OS means to create a file and determine the owner after.
        // This matches what we do in the agent process itself. There is a
        // problem otherwise if Thermostat agent runs as user X, but
        // the "user.name" property is set to user Y.
        UserPrincipal principal = null;
        Path fileToTest = null;
        String usernameCandidate = helper.getSystemProperty("user.name");
        try {
            fileToTest = helper.createTempFile("thermostat_uds", usernameCandidate);
            principal = helper.getOwner(fileToTest);
        } catch (Exception e) {
            throw new IOException("Failed to determine current user via OS. User was: " + usernameCandidate, e);
        } finally {
            if (fileToTest != null) {
                // Clean up temp file once we have the UserPrincipal
                helper.deleteFile(fileToTest);
            }
        }
        
        if (principal == null) {
            throw new IOException("Failed to determine current user via OS. User was: " + usernameCandidate);
        }
        return principal;
    }
    
    // Helper class for testing static methods that cannot be easily mocked
    static class SystemHelper {

        UserPrincipal getOwner(Path fileToTest) throws IOException {
            return Files.getOwner(fileToTest);
        }
        
        Path createTempFile(String prefix, String suffix) throws IOException {
            return Files.createTempFile(prefix, suffix);
        }
        
        void deleteFile(Path file) throws IOException {
            Files.delete(file);
        }
        
        String getSystemProperty(String name) {
            return System.getProperty(name);
        }
        
    }

}
