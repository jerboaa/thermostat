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
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.redhat.thermostat.common.utils.LoggingUtils;
import com.redhat.thermostat.shared.config.InvalidConfigurationException;
import com.redhat.thermostat.shared.config.internal.CommonPathsImpl;
import com.redhat.thermostat.web.server.auth.BasicRole;
import com.redhat.thermostat.web.server.auth.RolePrincipal;

/**
 * Class responsible for parsing roles from a properties file.
 *
 */
class RolesAmender {
    
    private static final Logger logger = LoggingUtils.getLogger(RolesAmender.class);
    private static final String DEFAULT_ROLES_FILE = "thermostat-roles.properties";
    private static final String ROLE_SEPARATOR = ",";
    private static final Set<BasicRole> EMPTY_SET = new HashSet<>(0);
    // A username => roles mapping
    private Map<String, Set<BasicRole>> rolesMap;
    // The set of all users we know about
    private final Set<Object> users; 

    RolesAmender(Set<Object> users) {
        // this is the default configuration supplied with thermostat
        // it should not be overriden by editing this configuraton file
        // see javadocs of PropertiesUsernameRolesLoginModule
        this((new CommonPathsImpl().getSystemConfigurationDirectory() + File.separator + DEFAULT_ROLES_FILE), users);
    }
    
    RolesAmender(String rolesFile, Set<Object> users) {
        this.users = Objects.requireNonNull(users);
        loadRoles(new File(rolesFile));
    }
    
    /**
     * Gets the set of roles for a specific username.
     * 
     * @param username
     * @return The role-set for the given user or an empty set if this user is
     *         not a member of any role. 
     * @throws IllegalStateException If the roles database was null.
     */
    Set<BasicRole> getRoles(String username) throws IllegalStateException {
        if (rolesMap == null) {
            throw new IllegalStateException("Roles database missing");
        }
        Set<BasicRole> roles = rolesMap.get(username);
        if (roles == null) {
            // username not in list of roles, default to empty set for her
            return EMPTY_SET;
        }
        return roles;
    }

    private void loadRoles(File file) {
        if (rolesMap == null) {
            Properties rawRoles = new Properties();
            try (FileInputStream stream = new FileInputStream(file)) {
                rawRoles.load(stream);
            } catch (IOException e) {
                String msg = "Failed to load roles from properties";
                logger.log(Level.SEVERE, msg, e);
                throw new InvalidConfigurationException(msg); 
            }
            try {
                prepareRolesMap(rawRoles);
            } catch (Throwable e) {
                String msg = "Failed to parse roles";
                logger.log(Level.SEVERE, msg, e);
                throw new IllegalStateException(msg);
            }
        }
    }

    private void prepareRolesMap(Properties rawRoles) {
        rolesMap = new HashMap<>();
        Map<String, RolesInfo> rolesSoFar = new HashMap<>();
        Iterator<Object> it = users.iterator();
        while (it.hasNext()) {
            // users came from props, this should be a safe cast to string
            String username = (String)it.next();
            String rolesRaw = null;
            rolesRaw = rawRoles.getProperty(username);
            if (rolesRaw == null) {
                // Since the list of usernames is not necessarily a subset of
                // lines listed in roles for the case where there are just users
                // defined, but don't appear in the roles file (i.e. have no
                // role memberships). In this case, we simply skip this entry.
                continue;
            }
            rolesRaw = rolesRaw.trim();
            String[] roles = rolesRaw.split(ROLE_SEPARATOR);
            Set<BasicRole> uRoles = new HashSet<>();
            for (String tmp: roles) {
                String role = tmp.trim();
                if (role.equals("")) {
                    // skip empty role names
                    continue;
                }
                RolesInfo info = rolesSoFar.get(role);
                if (info == null) {
                    // new role, define it and create role-info
                    BasicRole r = new RolePrincipal(role);
                    info = new RolesInfo(r);
                    info.getMemberUsers().add(username);
                    rolesSoFar.put(role, info);
                }
                info.getMemberUsers().add(username);
                uRoles.add(info.getRole());
            }
            // finished one username, add it to roles map, to the intermediate
            // set cache, and remove entry from raw roles list.
            rolesMap.put(username, uRoles);
            rawRoles.remove(username);
        }
        // what's left now are recursive role definitions.
        Set<Object> recursiveRoles = rawRoles.keySet();
        for (Object r: recursiveRoles) {
            RolesInfo recRole = rolesSoFar.get((String)r);
            if (recRole == null) {
                // This is bad news, new role defined but no username we know
                // of is member of that role
                throw new IllegalStateException("Recursive role '" + r + "' defined, but no user is a member");
            }
            String memberRoles = rawRoles.getProperty((String)r).trim();
            for (String tmp: memberRoles.split(ROLE_SEPARATOR)) {
                String member = tmp.trim();
                if (member.equals("")) {
                    // skip empty role name
                    continue;
                }
                if (users.contains(member)) {
                    throw new IllegalStateException("User '" + member + "' part of recursive role definition!");
                }
                RolesInfo role = rolesSoFar.get(member);
                if (role == null) {
                    // new role, define it and add it as member to recursive
                    // role
                    BasicRole ro = new RolePrincipal(member);
                    role = new RolesInfo(ro);
                    rolesSoFar.put(member, role);
                }
                recRole.getRole().addMember(role.getRole());
            }
            expandRoles(recRole);
        }
    }
    
    /*
     * Add roles to users which are member of a recursive role
     */
    private void expandRoles(RolesInfo recRole) {
        Iterator<String> memberUsersIterator = recRole.getMemberUsers().iterator();
        while (memberUsersIterator.hasNext()) {
            @SuppressWarnings("unchecked") // we've added them so safe
            Enumeration<BasicRole> members = (Enumeration<BasicRole>)recRole.getRole().members();
            String username = memberUsersIterator.next();
            Set<BasicRole> userRoles = rolesMap.get(username);
            while (members.hasMoreElements()) {
                BasicRole r = members.nextElement();
                userRoles.add(r);
            }
        }
    }

    /*
     * Container data structure which is used for (role => member users) lookup.
     */
    static class RolesInfo {
        
        private final Set<String> memberUsers;
        private final BasicRole role;
        
        RolesInfo(BasicRole role) {
            this.role = role;
            memberUsers = new HashSet<>();
        }
        
        Set<String> getMemberUsers() {
            return memberUsers;
        }
        
        BasicRole getRole() {
            return role;
        }
    }
    
}

