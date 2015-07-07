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

package com.redhat.thermostat.launcher.internal;

import com.redhat.thermostat.shared.locale.Translate;

public enum LocaleResources {

    MISSING_LAUNCHER,

    CANNOT_GET_COMMAND_INFO,
    UNKNOWN_COMMAND,
    COMMAND_COULD_NOT_LOAD_BUNDLES,
    COMMAND_DESCRIBED_BUT_NOT_AVAILALBE,
    COMMAND_AVAILABLE_INSIDE_SHELL,
    COMMAND_AVAILABLE_INSIDE_SHELL_ONLY,
    COMMAND_AVAILABLE_OUTSIDE_SHELL,
    COMMAND_AVAILABLE_OUTSIDE_SHELL_ONLY,

    COMMAND_HELP_COMMAND_LIST_HEADER,
    COMMAND_HELP_COMMAND_OPTION_HEADER,

    COMMAND_SHELL_USER_GUIDE,
    COMMAND_SHELL_IO_EXCEPTION,

    OPTION_DB_URL_DESC,
    OPTION_LOG_LEVEL_DESC,
    OPTION_HELP_DESC,

    MISSING_OPTION,
    MISSING_OPTIONS,
    PARSE_EXCEPTION_MESSAGE,

    LAUNCHER_USER_AUTH_PROMPT_ERROR,
    LAUNCHER_MALFORMED_URL,
    LAUNCHER_CONNECTION_ERROR,
    LAUNCHER_FIRST_LAUNCH_MSG,

    INVALID_DB_URL,
    PARSE_ISSUES_CALLED_BEFORE_PARSE,
    PARSER_ERROR,
    PARSER_WARNING,
    ;

    static final String RESOURCE_BUNDLE = "com.redhat.thermostat.launcher.internal.strings";

    public static Translate<LocaleResources> createLocalizer() {
        return new Translate<>(RESOURCE_BUNDLE, LocaleResources.class);
    }
}

