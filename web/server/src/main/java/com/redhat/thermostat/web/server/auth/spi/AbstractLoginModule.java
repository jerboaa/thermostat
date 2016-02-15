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

import java.io.IOException;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.auth.login.LoginException;
import javax.security.auth.spi.LoginModule;

import com.redhat.thermostat.common.utils.LoggingUtils;

/**
 * Base class for Thermostat JAAS login modules.
 *
 */
public abstract class AbstractLoginModule implements LoginModule {
    
    private static final Logger logger = LoggingUtils.getLogger(AbstractLoginModule.class);
    protected CallbackHandler callBackHandler;
    protected Subject subject;
    protected boolean debug = false;
    
    @Override
    public void initialize(Subject subject, CallbackHandler callbackHandler,
            Map<String, ?> sharedState, Map<String, ?> options) {
        this.subject = subject;
        this.callBackHandler = callbackHandler;
        this.debug = "true".equalsIgnoreCase((String)options.get("debug"));
    }

    /**
     * Get username and password from the callback.
     * 
     * @return An array of length two where the first element is the username as
     *         String and the second element is the password as char[].
     * @throws LoginException
     *             if the retrieval fails (e.g. no callback is available).
     */
    protected Object[] getUsernamePasswordFromCallBack() throws LoginException {
        if (callBackHandler == null) {
            throw new LoginException("No callback handler");
        }
        Object[] creds = new Object[2];
        NameCallback nc = new NameCallback("Username: ");
        PasswordCallback pc = new PasswordCallback("Password: ", false);
        Callback[] callbacks = new Callback[] { nc, pc };
        try {
            callBackHandler.handle(callbacks);
            creds[0] = nc.getName();
            creds[1] = pc.getPassword();
            pc.clearPassword();
            return creds;
        } catch (IOException | UnsupportedCallbackException e) {
            logger.log(Level.FINEST, "Can't get username", e);
            throw new LoginException(e.getMessage());
        } 
    }
    
    /**
     * Get the user's name from the callback.
     * 
     * @return The user's name.
     * @throws LoginException
     *             if the retrieval fails (e.g. no callback is available).
     */
    protected String getUsernameFromCallBack() throws LoginException {
        if (callBackHandler == null) {
            throw new LoginException("No callback handler");
        }
        NameCallback nc = new NameCallback("Username: ");
        PasswordCallback pc = new PasswordCallback("Password: ", false);
        Callback[] callbacks = new Callback[] { nc, pc };
        try {
            callBackHandler.handle(callbacks);
            return nc.getName();
        } catch (IOException | UnsupportedCallbackException e) {
            logger.log(Level.FINEST, "Can't get username", e);
            throw new LoginException(e.getMessage());
        } 
    }

    protected final void debugLog(Level level, String message) {
        if (debug) {
            logger.log(level, message);
        }
    }

    protected final void debugLog(Level level, String message, Exception ex) {
        if (debug) {
            logger.log(level, message, ex);
        }
    }
}

