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

package com.redhat.thermostat.web.server.auth.spi;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.redhat.thermostat.common.utils.LoggingUtils;
import com.redhat.thermostat.shared.config.InvalidConfigurationException;
import com.redhat.thermostat.shared.config.internal.CommonPathsImpl;
import com.redhat.thermostat.web.server.ConfigurationFinder;

/**
 * 
 * Validates users against an internal, properties based, user database.
 *
 */
class PropertiesUserValidator implements UserValidator {
    
    private static final Logger logger = LoggingUtils.getLogger(PropertiesUserValidator.class);
    private static final String DEFAULT_USERS_FILE = "thermostat-users.properties";
    private Properties users;
    
    PropertiesUserValidator() {
        // this is the default configuration. it should be overriden through different means
        // see javadoc of PropertiesUsernameRolesLoginModule
        this(new ConfigurationFinder(new CommonPathsImpl()).getConfiguration(DEFAULT_USERS_FILE));
    }
    
    PropertiesUserValidator(String usersFile) {
        this(new File(usersFile));
    }

    PropertiesUserValidator(File file) {
        loadUsers(file);
    }

    @Override
    public synchronized void authenticate(String username, char[] password)
            throws UserValidationException {
        if (users == null) {
            throw new UserValidationException("No user database");
        }
        String tmp = users.getProperty(username);
        if (tmp == null) {
            throw new UserValidationException("User '" + username + "' not found");
        }
        // We have an entry in our user db for the requested username.
        char refPassWd[] = tmp.toCharArray();
        try {
            validate(password, refPassWd);
        } finally {
            // clear our password
            for (int i = 0; i < refPassWd.length; i++) {
                refPassWd[i] = '\0';
            }
        }
    }
    
    private void validate(char[] theirPwd, char[] ourPwd) throws UserValidationException {
        String failureMessage = "Login failed!";
        if (theirPwd.length != ourPwd.length) {
            throw new UserValidationException(failureMessage);
        }
        for (int i = 0; i < theirPwd.length; i++) {
            if (theirPwd[i] != ourPwd[i]) {
                throw new UserValidationException(failureMessage);
            }
        }
    }

    private void loadUsers(File userDB) {
        if (users == null) {
            Properties users = new Properties();
            try (FileInputStream stream = new FileInputStream(userDB)) {
                users.load(stream);
                this.users = users;
            } catch (IOException e) {
                String msg = "Unable to load user database";
                logger.log(Level.WARNING, msg, e);
                throw new InvalidConfigurationException(msg);
            }
        }
    }

    @Override
    public Set<Object> getAllKnownUsers() throws IllegalStateException {
        if (users == null) {
            throw new IllegalStateException("No user database");
        }
        return Collections.unmodifiableSet(users.keySet());
    }
}

