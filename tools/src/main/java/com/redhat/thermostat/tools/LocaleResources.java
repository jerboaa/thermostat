/*
 * Copyright 2012 Red Hat, Inc.
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

package com.redhat.thermostat.tools;

public enum LocaleResources {

    MISSING_INFO,

    VALUE_AND_UNIT,

    COMMAND_SERVICE_DESCRIPTION,
    COMMAND_SERVICE_ARGUMENT_START_DESCRIPTION,
    COMMAND_SERVICE_ARGUMENT_STOP_DESCRIPTION,

    COMMAND_LIST_VMS_DESCRIPTION,

    COMMAND_SHELL_DESCRIPTION,

    COMMAND_VM_INFO_DESCRIPTION,

    COMMAND_VM_STAT_DESCRIPTION,
    COMMAND_VM_STAT_ARGUMENT_CONTINUOUS_DESCRIPTION,

    COMMAND_STORAGE_DESCRIPTION,
    COMMAND_STORAGE_ARGUMENT_REQUIRED,
    COMMAND_STORAGE_ARGUMENT_CLUSTER_DESCRIPTION,
    COMMAND_STORAGE_ARGUMENT_DRYRUN_DESCRIPTION,
    COMMAND_STORAGE_ARGUMENT_HELP_DESCRIPTION,
    COMMAND_STORAGE_ARGUMENT_START_DESCRIPTION,
    COMMAND_STORAGE_ARGUMENT_STOP_DESCRIPTION,
    COMMAND_STORAGE_ARGUMENT_QUIET_DESCRIPTION,

    STARTING_AGENT,
    ERROR_STARTING_DB,
    STORAGE_ALREADY_RUNNING,
    STORAGE_ALREADY_RUNNING_WITH_PID,
    SERVER_SHUTDOWN_COMPLETE,
    LOG_FILE_AT,
    CANNOT_START_SERVER,
    CANNOT_SHUTDOWN_SERVER,
    STALE_PID_FILE,
    STALE_PID_FILE_NO_MATCHING_PROCESS,
    STARTING_STORAGE_SERVER,
    CANNOT_EXECUTE_PROCESS,
    SERVER_LISTENING_ON,
    PID_IS,

    VM_INFO_PROCESS_ID,
    VM_INFO_START_TIME,
    VM_INFO_STOP_TIME,
    VM_INFO_MAIN_CLASS,
    VM_INFO_COMMAND_LINE,
    VM_INFO_JAVA_VERSION,
    VM_INFO_VIRTUAL_MACHINE,
    VM_INFO_VM_ARGUMENTS,

    COLUMN_HEADER_HOST_ID,
    COLUMN_HEADER_HOST,
    COLUMN_HEADER_VM_ID,
    COLUMN_HEADER_VM_NAME,
    COLUMN_HEADER_VM_STATUS,
    COLUMN_HEADER_TIME,
    COLUMN_HEADER_CPU_PERCENT,
    COLUMN_HEADER_MEMORY_PATTERN,

    VM_STOP_TIME_RUNNING,
    VM_STATUS_ALIVE,
    VM_STATUS_DEAD,
    ;

    static final String RESOURCE_BUNDLE = "com.redhat.thermostat.tools.strings";
}
