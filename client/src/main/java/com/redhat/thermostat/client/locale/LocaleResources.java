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

package com.redhat.thermostat.client.locale;

public enum LocaleResources {

    MISSING_INFO,

    MAIN_WINDOW_TREE_ROOT_NAME,

    CONNECTION_FAILED_TO_CONNECT_TITLE,
    CONNECTION_FAILED_TO_CONNECT_DESCRIPTION,

    BUTTON_CLOSE,
    BUTTON_NEXT,
    BUTTON_CANCEL,
    BUTTON_OK,

    MENU_FILE,
    MENU_FILE_CONNECT,
    MENU_FILE_IMPORT,
    MENU_FILE_EXPORT,
    MENU_FILE_EXIT,
    MENU_EDIT,
    MENU_EDIT_CONFIGURE_AGENT,
    MENU_EDIT_CONFIGURE_CLIENT,
    MENU_EDIT_ENABLE_HISTORY_MODE,
    MENU_HELP,
    MENU_HELP_ABOUT,

    GARBAGE_COLLECTION,
    YOUNG_GEN,
    EDEN_GEN,
    S0_GEN,
    S1_GEN,
    OLD_GEN,
    PERM_GEN,
    UNKNOWN_GEN,
    SOME_GENERATION,

    SECONDS,
    MINUTES,
    HOURS,
    DAYS,

    STARTUP_MODE_SELECTION_DIALOG_TITLE,
    STARTUP_MODE_SELECTION_INTRO,
    STARTUP_MODE_SELECTION_TYPE_LOCAL,
    STARTUP_MODE_SELECTION_TYPE_REMOTE,
    STARTUP_MODE_SELECTION_TYPE_CLUSTER,
    STARTUP_MODE_SELECTION_URL_LABEL,

    TREE_HOST_TOOLTIP_HOST_NAME,
    TREE_HOST_TOOLTIP_AGENT_ID,
    TREE_HOST_TOOLTIP_VM_NAME,
    TREE_HOST_TOOLTIP_VM_ID,

    ABOUT_DIALOG_LICENSE,
    ABOUT_DIALOG_EMAIL,
    ABOUT_DIALOG_WEBSITE,

    APPLICATION_INFO_VERSION,
    APPLICATION_INFO_DESCRIPTION,
    APPLICATION_INFO_LICENSE,

    HOME_PANEL_SECTION_SUMMARY,
    HOME_PANEL_TOTAL_MACHINES,
    HOME_PANEL_TOTAL_JVMS,
    HOME_PANEL_SECTION_ISSUES,
    HOME_PANEL_NO_ISSUES,

    HOST_INFO_TAB_OVERVIEW,
    HOST_INFO_TAB_MEMORY,
    HOST_INFO_TAB_CPU,
    HOST_INFO_TAB_IO,

    HOST_OVERVIEW_SECTION_BASICS,
    HOST_OVERVIEW_SECTION_HARDWARE,
    HOST_OVERVIEW_SECTION_SOFTWARE,

    HOST_INFO_HOSTNAME,
    HOST_INFO_CPU_COUNT,
    HOST_INFO_CPU_MODEL,
    HOST_INFO_OS_NAME,
    HOST_INFO_OS_KERNEL,

    HOST_INFO_MEMORY_TOTAL,
    HOST_INFO_NETWORK,

    NETWORK_INTERFACE_COLUMN,
    NETWORK_IPV4_COLUMN,
    NETWORK_IPV6_COLUMN,

    HOST_CPU_SECTION_OVERVIEW,
    HOST_CPU_USAGE_CHART_TIME_LABEL,
    HOST_CPU_USAGE_CHART_VALUE_LABEL,
    HOST_MEMORY_SECTION_OVERVIEW,
    HOST_MEMORY_CHART_TITLE,
    HOST_MEMORY_CHART_TIME_LABEL,
    HOST_MEMORY_CHART_SIZE_LABEL,
    HOST_MEMORY_TOTAL,
    HOST_MEMORY_FREE,
    HOST_MEMORY_USED,
    HOST_SWAP_TOTAL,
    HOST_SWAP_FREE,
    HOST_BUFFERS,

    VM_INFO_TAB_OVERVIEW,
    VM_INFO_TAB_CPU,
    VM_INFO_TAB_MEMORY,
    VM_INFO_TAB_GC,
    VM_INFO_TAB_CLASSES,

    VM_INFO_SECTION_PROCESS,
    VM_INFO_SECTION_JAVA,
    VM_INFO_PROCESS_ID,
    VM_INFO_START_TIME,
    VM_INFO_STOP_TIME,
    VM_INFO_RUNNING,
    VM_INFO_MAIN_CLASS,
    VM_INFO_COMMAND_LINE,
    VM_INFO_JAVA_VERSION,
    VM_INFO_VM,
    VM_INFO_VM_ARGUMENTS,
    VM_INFO_VM_NAME_AND_VERSION,
    VM_INFO_PROPERTIES,
    VM_INFO_ENVIRONMENT,
    VM_INFO_LIBRARIES,

    VM_CPU_CHART_LOAD_LABEL,
    VM_CPU_CHART_TIME_LABEL,

    VM_MEMORY_SPACE_TITLE,
    VM_MEMORY_SPACE_USED,
    VM_MEMORY_SPACE_FREE,
    VM_MEMORY_SPACE_ADDITIONAL,

    VM_GC_COLLECTOR_OVER_GENERATION,
    VM_GC_COLLECTOR_CHART_REAL_TIME_LABEL,
    VM_GC_COLLECTOR_CHART_GC_TIME_LABEL,

    VM_LOADED_CLASSES,
    VM_CLASSES_CHART_REAL_TIME_LABEL,
    VM_CLASSES_CHART_LOADED_CLASSES_LABEL,

    CONFIGURE_AGENT_WINDOW_TITLE,
    CONFIGURE_AGENT_AGENTS_LIST,
    CONFIGURE_ENABLE_BACKENDS,

    CLIENT_PREFS_WINDOW_TITLE,
    CLIENT_PREFS_GENERAL,
    CLIENT_PREFS_STORAGE_URL,
    ;

    static final String RESOURCE_BUNDLE =
            "com.redhat.thermostat.client.locale.strings";
}
