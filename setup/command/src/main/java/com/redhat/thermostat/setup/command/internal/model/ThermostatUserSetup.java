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

package com.redhat.thermostat.setup.command.internal.model;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.Writer;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.redhat.thermostat.common.utils.LoggingUtils;

class ThermostatUserSetup implements UserSetup, RoleSetup {
    
    private static final Logger logger = LoggingUtils.getLogger(ThermostatUserSetup.class);
    private final UserCredsValidator validator;
    private final UserPropertiesFinder propsFileFinder;
    private final CredentialsFileCreator fileCreator;
    private final StampFiles stampFiles;
    private final Map<String, String> roleComments = new HashMap<>();
    private final Map<String, String> userComments = new HashMap<>();
    private final Map<String, String[]> roles = new HashMap<>();
    private final Map<String, char[]> userCreds = new HashMap<>();
    
    ThermostatUserSetup(UserPropertiesFinder propsFileFinder, UserCredsValidator validator, CredentialsFileCreator creator, StampFiles stampFiles) {
        this.propsFileFinder = propsFileFinder;
        this.validator = validator;
        this.fileCreator = creator;
        this.stampFiles = stampFiles;
    }

    @Override
    public void commit() throws IOException {
        Map<String, CommentedRolePropertyValue> roleProps = buildRoleProperties();
        Map<String, CommentedCredsPropertyValue> userProps = buildUserProperties();
        writeUsers(userProps);
        writeRoles(roleProps);
        writeSetupCompleteFile();
    }
    
    private void writeSetupCompleteFile() throws IOException {
        String completeTime = ThermostatSetup.DATE_FORMAT.format(new Date());
        String contents = "Created by '" + ThermostatSetup.PROGRAM_NAME + "' on " + completeTime + "\n";
        stampFiles.createSetupCompleteStamp(contents);
    }
    
    // package-private for testing
    void writeRoles(Map<String, CommentedRolePropertyValue> roleProps) throws IOException {
        File rolePropsFile = propsFileFinder.getRolesProperties();
        fileCreator.create(rolePropsFile);
        Properties existingProperties = readPropsFromFile(rolePropsFile);
        try (FileOutputStream roleStream = new FileOutputStream(rolePropsFile, true);
                Writer rolesWriter = new PropertiesWriter(roleStream)) {
            for (String key : roleProps.keySet()) {
                Properties p = new Properties();
                CommentedRolePropertyValue val = roleProps.get(key);
                // prevent duplicate entries from being written
                if (existingProperties.get(key) == null) {
                    p.put(key, val.getValue());
                    p.store(rolesWriter, val.getComment());
                } else {
                    logger.info("Skipping already existing key '" + key + "' in file " + rolePropsFile.toString());
                }
            }
        }
        
    }

    // package-private for testing
    Properties readPropsFromFile(File propsFile) {
        Properties props = new Properties();
        try (FileInputStream inStream = new FileInputStream(propsFile)) {
            props.load(inStream);
        } catch (IOException e) {
            logger.log(Level.INFO, "Failed to read existing properties file " + propsFile.toString());
        }
        return props;
    }

    // package-private for testing
    void writeUsers(Map<String, CommentedCredsPropertyValue> userProps) throws IOException {
        File userPropsFile = propsFileFinder.getUserProperties();
        fileCreator.create(userPropsFile);
        Properties existingProperties = readPropsFromFile(userPropsFile);
        try (FileOutputStream userStream = new FileOutputStream(userPropsFile, true)) {
            for (String key : userProps.keySet()) {
                Properties p = new Properties();
                CommentedCredsPropertyValue val = userProps.get(key);
                // prevent duplicate entries from being written
                if (existingProperties.get(key) == null) {
                    p.put(key, String.valueOf(val.getValue()));
                    p.store(userStream, val.getComment());
                } else {
                    logger.info("Skipping already existing key '" + key + "' in file " + userPropsFile.toString());
                }
            }
        }
    }

    // package-private for testing
    Map<String, CommentedCredsPropertyValue> buildUserProperties() {
        Map<String, CommentedCredsPropertyValue> userProps = new HashMap<>();
        for (String user: userCreds.keySet()) {
            userProps.put(user, new CommentedCredsPropertyValue(userCreds.get(user), userComments.get(user)));
        }
        return userProps;
    }

    // package-private for testing
    Map<String, CommentedRolePropertyValue> buildRoleProperties() {
        Map<String, CommentedRolePropertyValue> roleProps = new HashMap<>();
        for (String roleKey: roles.keySet()) {
            String value = roleSetToString(roles.get(roleKey));
            CommentedRolePropertyValue roleVal = new CommentedRolePropertyValue(value, roleComments.get(roleKey));
            roleProps.put(roleKey, roleVal);
        }
        return roleProps;
    }

    @Override
    public void assignRolesToUser(String username, String[] roles, String comment) {
        validator.validateUsername(username);
        validateRoles(roles);
        this.roles.put(username, roles);
        addRoleComment(username, comment);
    }

    @Override
    public void createRecursiveRole(String name, String[] rolePrimitives, String comment) {
        validator.validateUsername(name);
        validateRoles(rolePrimitives);
        this.roles.put(name, rolePrimitives);
        addRoleComment(name, comment);
    }
    
    private void validateRoles(String[] roles) {
        if (roles == null || roles.length == 0) {
            throw new IllegalArgumentException("Roles must not be null or empty");
        }
    }

    @Override
    public void createUser(String username, char[] password, String comment) {
        validator.validateUsername(username);
        validator.validatePassword(password);
        this.userCreds.put(username, password);
        addUserComment(username, comment);
    }
    
    void addRoleComment(String property, String comment) {
        if (comment != null) {
            roleComments.put(Objects.requireNonNull(property), comment);
        }
    }
    
    void addUserComment(String property, String comment) {
        if (comment != null) {
            userComments.put(Objects.requireNonNull(property), comment);
        }
    }
    
    String roleSetToString(String[] roles) {
        if (roles.length == 0) {
            return "";
        }
        StringBuilder rolesBuilder = new StringBuilder();
        for (int i = 0; i < roles.length - 1; i++) {
            rolesBuilder.append(roles[i] + ", " + System.getProperty("line.separator"));
        }
        rolesBuilder.append(roles[roles.length - 1]);
        return rolesBuilder.toString();
    }
    
    static class CommentedPropertyValue {
        private final String comment;
        
        CommentedPropertyValue(String comment) {
            this.comment = comment;
        }

        String getComment() {
            return comment;
        }

    }
    
    static class CommentedRolePropertyValue extends CommentedPropertyValue {
        
        private final String value;
        
        CommentedRolePropertyValue(String value, String comment) {
            super(comment);
            this.value = value;
        }

        String getValue() {
            return value;
        }
    }
    
    static class CommentedCredsPropertyValue extends CommentedPropertyValue {
        
        private final char[] value;
        
        CommentedCredsPropertyValue(char[] value, String comment) {
            super(comment);
            this.value = value;
        }

        char[] getValue() {
            return value;
        }
    }

}
