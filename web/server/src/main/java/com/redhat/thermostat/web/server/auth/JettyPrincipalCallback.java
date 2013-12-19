/*
 * Copyright 2012-2014 Red Hat, Inc.
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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.security.Principal;
import java.util.Objects;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.security.auth.Subject;

import com.redhat.thermostat.common.utils.LoggingUtils;
import com.redhat.thermostat.web.server.containers.ContainerVersion;
import com.redhat.thermostat.web.server.containers.ServletContainerInfo;

/**
 * Principal callback for the jetty container.
 *
 */
class JettyPrincipalCallback implements PrincipalCallback {
    
    private static final String JETTY8_JAAS_USER_PRINCIPAL_CLASS_NAME = "org.eclipse.jetty.plus.jaas.JAASUserPrincipal";
    private static final String JETTY9_JAAS_USER_PRINCIPAL_CLASS_NAME = "org.eclipse.jetty.jaas.JAASUserPrincipal";
    
    private static final Logger logger = LoggingUtils.getLogger(JettyPrincipalCallback.class);
    private final ServletContainerInfo info;
    
    JettyPrincipalCallback(ServletContainerInfo info) {
        this.info = Objects.requireNonNull(info);
    }
    
    @Override
    public UserPrincipal getUserPrincipal(Principal principal) {
        Subject subject = getJettySubject(principal);
        Set<UserPrincipal> userPrincipals = subject.getPrincipals(UserPrincipal.class);
        if (userPrincipals.size() != 1) {
            throw new IllegalStateException("Number of thermostat user principals must be exactly 1!");
        }
        return userPrincipals.iterator().next();
    }
    
    private Subject getJettySubject(Principal principal) {
        ContainerVersion version = info.getContainerVersion();
        // Jetty has our principal on the accessible subject.
        // The package of the JAASUserPrincipal class changed between
        // Jetty 8 and Jetty 9.
        if (version.getMajor() <= 8) {
            return getSubjectFromClass(principal, JETTY8_JAAS_USER_PRINCIPAL_CLASS_NAME);
        } else {
            // >= 9 and go with jetty 9's name
            return getSubjectFromClass(principal, JETTY9_JAAS_USER_PRINCIPAL_CLASS_NAME);
        }
    }
    
    private Subject getSubjectFromClass(Principal principal, String clazzName) {
        Subject subject = null;
        try {
            // Do this via reflection in order to avoid a hard dependency
            // on jetty-plus.
            Class<?> jassUserPrincipal = Class.forName(clazzName);
            Method method = jassUserPrincipal.getDeclaredMethod("getSubject");
            subject = (Subject)method.invoke(principal);
        } catch (ClassNotFoundException | NoSuchMethodException
                | SecurityException | IllegalAccessException
                | IllegalArgumentException | InvocationTargetException e) {
            // log and continue
            logger.log(Level.WARNING, e.getMessage(), e);
        }
        if (subject == null) {
            throw new IllegalStateException(
                    "Could not retrieve subject from principal of type "
                            + principal.getClass().getName());
        }
        return subject;
    }

}
