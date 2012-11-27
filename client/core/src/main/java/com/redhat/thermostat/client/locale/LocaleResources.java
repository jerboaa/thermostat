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

import com.redhat.thermostat.common.locale.Translate;

public enum LocaleResources {

    MISSING_INFO,
    INFORMATION_NOT_AVAILABLE,

    MAIN_WINDOW_TREE_ROOT_NAME,

    CONNECTION_FAILED_TO_CONNECT_TITLE,
    CONNECTION_FAILED_TO_CONNECT_DESCRIPTION,
    CONNECTION_WIZARD,
    CONNECTION_QUIT,

    BUTTON_CLOSE,
    BUTTON_NEXT,
    BUTTON_CANCEL,
    BUTTON_OK,

    MENU_FILE,
    MENU_FILE_EXIT,
    MENU_EDIT,
    MENU_EDIT_CONFIGURE_CLIENT,
    MENU_EDIT_ENABLE_HISTORY_MODE,
    MENU_VIEW,
    MENU_VIEW_AGENTS,
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

    NUMBER_AND_UNIT,

    SEARCH_HINT,

    CHART_DURATION_SELECTOR_LABEL,

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

    HOME_PANEL_SECTION_SUMMARY,
    HOME_PANEL_TOTAL_MACHINES,
    HOME_PANEL_TOTAL_JVMS,
    HOME_PANEL_SECTION_ISSUES,
    HOME_PANEL_NO_ISSUES,

    HOST_INFO_TAB_IO,

    VM_INFO_TAB_CPU,
    VM_INFO_TAB_GC,

    VM_CPU_TITLE,
    VM_CPU_CHART_LOAD_LABEL,
    VM_CPU_CHART_TIME_LABEL,

    VM_GC_TITLE,

    VM_GC_COLLECTOR_OVER_GENERATION,
    VM_GC_COLLECTOR_CHART_REAL_TIME_LABEL,
    VM_GC_COLLECTOR_CHART_GC_TIME_LABEL,

    AGENT_INFO_WINDOW_TITLE,
    AGENT_INFO_AGENTS_LIST,
    AGENT_INFO_AGENT_SECTION_TITLE,
    AGENT_INFO_AGENT_NAME_LABEL,
    AGENT_INFO_AGENT_ID_LABEL,
    AGENT_INFO_AGENT_COMMAND_ADDRESS_LABEL,
    AGENT_INFO_AGENT_START_TIME_LABEL,
    AGENT_INFO_AGENT_STOP_TIME_LABEL,
    AGENT_INFO_AGENT_RUNNING,
    AGENT_INFO_BACKENDS_SECTION_TITLE,
    AGENT_INFO_BACKEND_NAME_COLUMN,
    AGENT_INFO_BACKEND_STATUS_COLUMN,
    AGENT_INFO_BACKEND_STATUS_ACTIVE,
    AGENT_INFO_BACKEND_STATUS_INACTIVE,
    AGENT_INFO_BACKEND_DESCRIPTION_LABEL,

    CLIENT_PREFS_WINDOW_TITLE,
    CLIENT_PREFS_CONNECTION,
    CLIENT_PREFS_STORAGE_USERNAME,
    CLIENT_PREFS_STORAGE_PASSWORD,
    CLIENT_PREFS_STORAGE_URL,
    CLIENT_PREFS_STORAGE_SAVE_ENTITLEMENTS,
    ;

    static final String RESOURCE_BUNDLE =
            "com.redhat.thermostat.client.locale.strings";

    public static Translate<LocaleResources> createLocalizer() {
        return new Translate<>(RESOURCE_BUNDLE, LocaleResources.class);
    }
}
