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

public interface UserRoles {

    /**
     * Allows for a user to read records tied to any host.
     */
    final String GRANT_HOSTS_READ_ALL = "thermostat-hosts-grant-read-hostname-ALL";
    /**
     * Allows for a user to read records tied to any JVM id.
     */
    final String GRANT_VMS_READ_BY_VM_ID_ALL = "thermostat-vms-grant-read-vmId-ALL";
    /**
     * Allows for a user to read records tied to any username the JVM is running as.
     */
    final String GRANT_VMS_READ_BY_USERNAME_ALL = "thermostat-vms-grant-read-username-ALL";
    /**
     * Allows for a user to read any file from storage.
     */
    final String GRANT_FILES_READ_ALL = "thermostat-files-grant-read-filename-ALL";
    /**
     * Allows for a user to write any file to storage.
     */
    final String GRANT_FILES_WRITE_ALL = "thermostat-files-grant-write-filename-ALL";
    /**
     * Allows for a user to see records tied to any agent.
     */
    final String GRANT_AGENTS_READ_ALL = "thermostat-agents-grant-read-agentId-ALL";

    final String GRANT_CMD_CHANNEL_GARBAGE_COLLECT = "thermostat-cmdc-grant-garbage-collect";
    final String GRANT_CMD_CHANNEL_DUMP_HEAP = "thermostat-cmdc-grant-dump-heap";
    final String GRANT_CMD_CHANNEL_GRANT_THREAD_HARVESTER = "thermostat-cmdc-grant-thread-harvester";
    final String GRANT_CMD_CHANNEL_KILLVM = "thermostat-cmdc-grant-killvm";
    final String GRANT_CMD_CHANNEL_PING = "thermostat-cmdc-grant-ping";
    final String GRANT_CMD_CHANNEL_JMX_TOGGLE_NOTIFICATION = "thermostat-cmdc-grant-jmx-toggle-notifications";
    final String GRANT_CMD_PROFILE_VM = "thermostat-cmdc-grant-profile-vm";

    final String PREPARE_STATEMENT = "thermostat-prepare-statement";
    final String READ = "thermostat-query";
    final String WRITE = "thermostat-write";
    final String LOAD_FILE = "thermostat-load-file";
    final String SAVE_FILE = "thermostat-save-file";
    final String PURGE = "thermostat-purge";
    final String REGISTER_CATEGORY = "thermostat-register-category";
    final String CMD_CHANNEL_VERIFY = "thermostat-cmdc-verify";
    final String CMD_CHANNEL_GENERATE = "thermostat-cmdc-generate";
    final String LOGIN = "thermostat-login";
    final String ACCESS_REALM = "thermostat-realm";
    
    /**
     * Basic role memberships for agent users
     */
    final String[] AGENT_ROLES = {
            UserRoles.PREPARE_STATEMENT,
            UserRoles.WRITE,
            UserRoles.CMD_CHANNEL_VERIFY,
            UserRoles.LOGIN,
            UserRoles.PURGE,
            UserRoles.REGISTER_CATEGORY,
            UserRoles.ACCESS_REALM,
            UserRoles.SAVE_FILE,
    };

    /**
     * Basic role memberships for client users
     */
    final String[] CLIENT_ROLES = {
            UserRoles.ACCESS_REALM,
            UserRoles.LOGIN,
            UserRoles.WRITE, // clients need to write notes
            UserRoles.READ,
            UserRoles.PREPARE_STATEMENT,
            UserRoles.CMD_CHANNEL_GENERATE,
            UserRoles.LOAD_FILE,
            UserRoles.REGISTER_CATEGORY
    };
    
    final String[] ADMIN_READALL = new String[] {
            UserRoles.GRANT_FILES_READ_ALL,
            UserRoles.GRANT_HOSTS_READ_ALL,
            UserRoles.GRANT_VMS_READ_BY_USERNAME_ALL,
            UserRoles.GRANT_VMS_READ_BY_VM_ID_ALL,
            UserRoles.GRANT_AGENTS_READ_ALL
    };

    final String[] CMD_CHANNEL_GRANT_ALL_ACTIONS = new String[] {
            UserRoles.GRANT_CMD_CHANNEL_DUMP_HEAP,
            UserRoles.GRANT_CMD_CHANNEL_GARBAGE_COLLECT,
            UserRoles.GRANT_CMD_CHANNEL_GRANT_THREAD_HARVESTER,
            UserRoles.GRANT_CMD_CHANNEL_JMX_TOGGLE_NOTIFICATION,
            UserRoles.GRANT_CMD_CHANNEL_KILLVM,
            UserRoles.GRANT_CMD_CHANNEL_PING,
            UserRoles.GRANT_CMD_PROFILE_VM
    };
}
