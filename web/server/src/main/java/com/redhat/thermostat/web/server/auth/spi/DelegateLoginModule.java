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

package com.redhat.thermostat.web.server.auth.spi;

import java.security.Principal;
import java.security.acl.Group;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.security.auth.Subject;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;
import javax.security.auth.spi.LoginModule;
import javax.servlet.http.HttpServletRequest;

import com.redhat.thermostat.common.utils.LoggingUtils;
import com.redhat.thermostat.web.server.auth.BasicRole;
import com.redhat.thermostat.web.server.auth.RolePrincipal;
import com.redhat.thermostat.web.server.auth.UserPrincipal;
import com.redhat.thermostat.web.server.auth.WrappedRolePrincipal;

/**
 * LoginModule which delegates to the configured implementing
 * {@link LoginModule}. This is useful in order to be able to provide a 
 * guarrantee that a {@link UserPrincipal} is always returned for
 * {@link HttpServletRequest#getUserPrincipal()} if any.
 * 
 */
public final class DelegateLoginModule extends AbstractLoginModule {
    
    private static final Logger logger = LoggingUtils.getLogger(DelegateLoginModule.class);
    private static final String JAAS_DELEGATE_CONFIG_NAME = "ThermostatJAASDelegate";
    // the delegate
    private LoginContext delegateContext;
    private String username;
    /**
     * The config name to use. Defaults to {@linkplain DelegateLoginModule#JAAS_DELEGATE_CONFIG_NAME}
     */
    private String configName;
    
    /**
     * Default, no-arg constructor.
     */
    public DelegateLoginModule() {
        this.configName = JAAS_DELEGATE_CONFIG_NAME;
    }
    
    // used for testing
    DelegateLoginModule(String configName) {
        this.configName = configName;
    }
    
    
    @Override
    public void initialize(Subject subject, CallbackHandler callbackHandler,
            Map<String, ?> sharedState, Map<String, ?> options) {
        super.initialize(subject, callbackHandler, sharedState, options);
        /*
         * Create and initialize the delegate 
         */
        try {
            this.delegateContext = new LoginContext(configName, subject, callbackHandler);
            debugLog(Level.FINEST, "successfully created delegate login context");
        } catch (LoginException e) {
            // This only happens if there is no "ThermostatJAASDelegate" config
            // and also no configuration with the name "other", which is likely
            // always there for real application servers.
            String message = "Fatal: Could not initialize delegate. " +
                    "'ThermostatJAASDelegate' " +
                    "and 'other' login modules are both not configured!";
            logger.log(Level.SEVERE, message, e);
            throw new RuntimeException(message);
        }
    }

    @Override
    public boolean login() throws LoginException {
        try {
            username = super.getUsernameFromCallBack();
            delegateContext.login();
            debugLog(Level.FINEST, "Login succeeded for " + username + " using the delegate.");
        } catch (LoginException e) {
            // This only shows up if debug is turned on
            // since it's just a plain login failure.
            debugLog(Level.FINEST, "Login failed", e);
            throw e;
        }
        return true;
    }

    @Override
    public boolean commit() throws LoginException {
        /*
         * Make sure to retrieve principals from the authenticated subject,
         * wrap them in UserPrincipal/BasicRole principals and inform the
         * UserPrincipal about the roles it is a member of.
         */
        Set<Principal> principals = subject.getPrincipals();
        int size = principals.size();
        Set<Principal> wrappedPrincipals = new HashSet<>(size);
        // the user principal is not in the roles set
        Set<BasicRole> roles = new HashSet<>(size -1);
        UserPrincipal userPrincipal = null;
        for (Principal p : principals) {
            if (p.getName().equals(username)) {
                // add our user principal
                if (userPrincipal != null) {
                    logger.log(Level.SEVERE, "Fatal: > 1 user principals!");
                    throw new IllegalStateException("> 1 user principals!");
                }
                userPrincipal = new UserPrincipal(username);
                wrappedPrincipals.add(userPrincipal);
            } else {
                // group (a.k.a role). It may be a simple principal or a 
                // Group. If it is a group, we simply wrap it. If it isn't
                // we use our simple RolePrincipal instead.
                BasicRole role;
                if (p instanceof Group) {
                    role = new WrappedRolePrincipal((Group)p);
                    wrappedPrincipals.add(role);
                    roles.add(role);
                } else {
                    role = new RolePrincipal(p.getName());
                    wrappedPrincipals.add(role);
                    roles.add(role);
                }
            }
        }
        // Remove old principals and push the newly wrapped ones
        principals.clear();
        principals.addAll(wrappedPrincipals);
        // Finally, inform the user principal about the roles it is a member of.
        // We need this in order to be able to do something (filtering/authorization)
        // with these roles from the web storage servlet.
        if (userPrincipal != null) {
            userPrincipal.setRoles(roles);
        }

        debugLog(Level.FINEST, "Committed changes for '" + username + "'");
        return true;
    }

    @Override
    public boolean abort() throws LoginException {
        if (subject != null) {
            // remove any principals
            Set<Principal> principals = subject.getPrincipals();
            principals.clear();
        }
        debugLog(Level.FINEST, "Login aborted!");
        return true;
    }

    @Override
    public boolean logout() throws LoginException {
        try {
            delegateContext.logout();
            Set<Principal> principals = subject.getPrincipals();
            principals.clear();
            debugLog(Level.FINEST, "Logged out successfully!");
            return true;
        } catch (LoginException e) {
            debugLog(Level.FINEST, "Logout failed!" + e.getMessage());
            return false;
        }
    }
    
    /*
     * Package private method in order to get at the subject
     */
    final Subject getSubject() {
        return this.subject;
    }
    
}

