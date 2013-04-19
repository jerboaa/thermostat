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

package com.redhat.thermostat.web.server.auth;

/**
 * Roles thermostat knows about.
 *
 */
public interface Roles {

    final String APPEND = "thermostat-add";
    final String REPLACE = "thermostat-replace";
    final String UPDATE = "thermostat-update";
    final String DELETE = "thermostat-remove";
    final String READ = "thermostat-query";
    final String GET_COUNT = "thermostat-get-count";
    final String LOAD_FILE = "thermostat-load-file";
    final String SAVE_FILE = "thermostat-save-file";
    final String PURGE = "thermostat-purge";
    final String REGISTER_CATEGORY = "thermostat-register-category";
    final String CMD_CHANNEL_VERIFY = "thermostat-cmdc-verify";
    final String CMD_CHANNEL_GENERATE = "thermostat-cmdc-generate";
    final String LOGIN = "thermostat-login";
    final String ACCESS_REALM = "thermostat-realm";
    
    final String[] ALL_ROLES = {
            APPEND, REPLACE, UPDATE, DELETE, READ, GET_COUNT, LOAD_FILE,
            SAVE_FILE, PURGE, REGISTER_CATEGORY, CMD_CHANNEL_GENERATE,
            CMD_CHANNEL_VERIFY, LOGIN, ACCESS_REALM
    };
    
    final String[] AGENT_ROLES = {
            APPEND, REPLACE, UPDATE, DELETE, SAVE_FILE, PURGE,
            REGISTER_CATEGORY, CMD_CHANNEL_VERIFY,
            LOGIN, ACCESS_REALM
    };
    
    final String[] CLIENT_ROLES = {
            ACCESS_REALM, LOGIN, CMD_CHANNEL_GENERATE, LOAD_FILE,
            GET_COUNT, READ, REGISTER_CATEGORY
    };
    
}
