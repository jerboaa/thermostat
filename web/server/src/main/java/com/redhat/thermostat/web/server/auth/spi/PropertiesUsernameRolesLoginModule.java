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

package com.redhat.thermostat.web.server.auth.spi;

import java.security.Principal;
import java.security.acl.Group;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.security.auth.Subject;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.login.LoginException;

import com.redhat.thermostat.common.utils.LoggingUtils;
import com.redhat.thermostat.web.server.auth.BasicRole;
import com.redhat.thermostat.web.server.auth.RolePrincipal;
import com.redhat.thermostat.web.server.auth.UserPrincipal;

/**
 * <p>
 * A login module which uses properties files for validating users and for
 * amending authenticated users with appropriate roles.
 * </p>
 * <p>
 * The properties file which will be used for user credential validation can be
 * specified via the <code>users.properties</code> option and defaults to
 * <code>$THERMOSTAT_HOME/thermostat-users.properties</code> if no such option
 * has been specified. Mappings for users to roles come from a file as specified
 * by the <code>roles.properties</code> option and defaults to
 * <code>$THERMOSTAT_HOME/thermostat-roles.properties</code> if no such option
 * has been provided.
 * </p>
 */
public class PropertiesUsernameRolesLoginModule extends AbstractLoginModule {
    
    private static final Logger logger = LoggingUtils.getLogger(PropertiesUsernameRolesLoginModule.class);
    
    // The validator to use for authentication
    private UserValidator validator;
    private RolesAmender amender;
    private String username;
    private boolean loginOK = false;
    

    @Override
    public void initialize(Subject subject, CallbackHandler callbackHandler,
            Map<String, ?> sharedState, Map<String, ?> options) {
        super.initialize(subject, callbackHandler, sharedState, options);
        this.validator = getValidator((String) options.get("users.properties"));
        this.amender = getRolesAmender((String) options.get("roles.properties"),
                validator.getAllKnownUsers());
    }

    @Override
    public boolean login() throws LoginException {
        if (debug) {
            logger.log(Level.FINEST, "Logging in ...");
        }
        loginOK = false;
        char[] password = null;
        try {
            Object[] creds = super.getUsernamePasswordFromCallBack();
            username = (String)creds[0];
            password = (char[])creds[1];
            validator.authenticate(username, password);
            loginOK = true;
            if (debug) {
                logger.log(Level.FINEST, "Logged in successfully: user == '" + username + "'");
            }
        } catch (UserValidationException e) {
            if (debug) {
                logger.log(Level.INFO, "Authentication failed for user '" + username + "'");
            }
            throw new LoginException(e.getMessage());
        } finally {
            clearPassword(password);
        }
        return loginOK;
    }

    @Override
    public boolean commit() throws LoginException {
        if (loginOK == false) {
            return false;
        }
        if (debug) {
            logger.log(Level.FINEST, "Committing principals for user '" + username + "'");
        }
        Set<Principal> principals = subject.getPrincipals();
        // Tomcat uses classes as specified by the LoginModule config
        // in order to distinguish between user principals and role principals
        // JBoss on the other hand uses string based name matching for the
        // user principal
        principals.add(new UserPrincipal(username));
        Set<BasicRole> roles = null;
        try {
            roles = amender.getRoles(username);
        } catch (IllegalStateException e) {
            if (debug) {
                logger.log(Level.INFO, "Failed to commit" + e.getMessage());
            }
            throw new LoginException();
        }
        principals.addAll(roles);
        // JBoss uses the Group "Roles" as the principal
        // for role matching
        Group rolesRole = new RolePrincipal("Roles");
        Iterator<BasicRole> it = roles.iterator();
        while (it.hasNext()) {
            BasicRole r = it.next();
            rolesRole.addMember(r);
        }
        principals.add(rolesRole);
        return true;
    }

    @Override
    public boolean abort() throws LoginException {
        clearPrincipals();
        if (debug) {
            logger.log(Level.FINEST, "Login aborted!");
        }
        return true;
    }

    @Override
    public boolean logout() throws LoginException {
        clearPrincipals();
        if (debug) {
            logger.log(Level.FINEST, "Logged out!");
        }
        return true;
    }
    
    private void clearPassword(char[] password) {
        if (password == null) {
            return;
        }
        for (int i= 0; i < password.length; i++) {
            password[i] = '\0';
        }
    }

    private void clearPrincipals() {
        Set<Principal> principals = subject.getPrincipals();
        principals.clear();
    }

    private UserValidator getValidator(final String usersProperties) {
        UserValidator validator = null;
        try {
            if (usersProperties == null) {
                if (debug) {
                    logger.log(Level.FINEST, "Using default user database");
                }
                validator = new PropertiesUserValidator();
            } else {
                if (debug) {
                    logger.log(Level.FINEST, "Using user database as defined in file '" + usersProperties + "'");
                }
                validator = new PropertiesUserValidator(usersProperties);
            }
        } catch (Throwable e) {
            // Can't continue at this point, since we need this for
            // authentication.
            String msg = "Fatal: Failed to initialize user database";
            logger.log(Level.SEVERE,  msg, e);
            throw new RuntimeException(msg);
        }
        return validator;
    }

    private RolesAmender getRolesAmender(final String rolesProperties, final Set<Object> users) {
        RolesAmender roleAmender = null;
        try {
            if (rolesProperties == null) {
                if (debug) {
                    logger.log(Level.FINEST, "Using default roles database");
                }
                roleAmender = new RolesAmender(users);
            } else {
                if (debug) {
                    logger.log(Level.FINEST, "Using roles database as defined in file '" + rolesProperties + "'");
                }
                roleAmender = new RolesAmender(rolesProperties, users);
            }
        } catch (Throwable e) {
            // Can't continue at this point, since we need this for
            // authentication.
            String msg = "Fatal: Failed to initialize role/user mapping database";
            logger.log(Level.SEVERE, msg, e);
            throw new RuntimeException(msg, e);
        }
        return roleAmender;
    }

}
