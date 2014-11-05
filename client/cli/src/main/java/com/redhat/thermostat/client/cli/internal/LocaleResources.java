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

package com.redhat.thermostat.client.cli.internal;

import com.redhat.thermostat.shared.locale.Translate;

public enum LocaleResources {

    MISSING_INFO,
    MISSING_LAUNCHER,

    HOST_SERVICE_UNAVAILABLE,
    VM_SERVICE_UNAVAILABLE,
    VM_CPU_SERVICE_NOT_AVAILABLE,
    VM_MEMORY_SERVICE_NOT_AVAILABLE,
    AGENT_SERVICE_UNAVAILABLE,
    BACKEND_SERVICE_UNAVAILABLE,

    COMMAND_CONNECT_ALREADY_CONNECTED,
    COMMAND_CONNECT_FAILED_TO_CONNECT,
    COMMAND_CONNECT_INVALID_STORAGE,
    COMMAND_CONNECT_NO_KEYRING,
    COMMAND_CONNECT_ERROR,
    COMMAND_CONNECT_USER_PROMPT_ERROR,

    COMMAND_DISCONNECT_NOT_CONNECTED,
    COMMAND_DISCONNECT_ERROR,

    COMMAND_SHELL_IO_EXCEPTION,

    COMMAND_INTERRUPTED,

    VM_INFO_VM_ID,
    VM_INFO_PROCESS_ID,
    VM_INFO_START_TIME,
    VM_INFO_STOP_TIME,
    VM_INFO_MAIN_CLASS,
    VM_INFO_COMMAND_LINE,
    VM_INFO_JAVA_VERSION,
    VM_INFO_VIRTUAL_MACHINE,
    VM_INFO_VM_ARGUMENTS,
    VM_INFO_USER,
    VM_INFO_USER_UNKNOWN,

    AGENT_ID,
    CONFIG_LISTEN_ADDRESS,
    START_TIME,
    STOP_TIME,
    AGENT_ACTIVE,
    BACKEND,
    STATUS,
    DESCRIPTION,

    AGENT_INFO_BACKEND_STATUS_ACTIVE,
    AGENT_INFO_BACKEND_STATUS_INACTIVE,

    COLUMN_HEADER_HOST_ID,
    COLUMN_HEADER_HOST,
    COLUMN_HEADER_VM_ID,
    COLUMN_HEADER_VM_PID,
    COLUMN_HEADER_VM_NAME,
    COLUMN_HEADER_VM_STATUS,
    COLUMN_HEADER_TIME,

    VM_STOP_TIME_RUNNING,
    VM_STATUS_ALIVE,
    VM_STATUS_DEAD,

    HOSTID_REQUIRED_MESSAGE,
    VMID_REQUIRED_MESSAGE,
    AGENT_ID_REQUIRED_MESSAGE,
    
    PURGING_AGENT_DATA,
    STORAGE_UNAVAILABLE,
    AGENT_UNAVAILABLE,
    CANNOT_PURGE_AGENT_RUNNING,
    AGENT_NOT_FOUND,
    PURGE_EXPENSIVE_OPERATION_WARNING,
    PURGE_EXPENSIVE_OPERATION_PROMPT,
    PURGE_CANCELLED_MESSAGE,
    AFFIRMATIVE_RESPONSES,

    VM_STAT_INVALID_SINCE_ARGUMENT,
    ;

    static final String RESOURCE_BUNDLE = "com.redhat.thermostat.client.cli.strings";

    public static Translate<LocaleResources> createLocalizer() {
        return new Translate<>(RESOURCE_BUNDLE, LocaleResources.class);
    }
}

