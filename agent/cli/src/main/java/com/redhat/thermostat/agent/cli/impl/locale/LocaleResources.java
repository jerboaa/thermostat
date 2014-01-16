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

package com.redhat.thermostat.agent.cli.impl.locale;

import com.redhat.thermostat.shared.locale.Translate;

public enum LocaleResources {

    STARTING_AGENT,

    COMMAND_STORAGE_ARGUMENT_REQUIRED,

    STORAGE_ALREADY_RUNNING,
    STORAGE_ALREADY_RUNNING_WITH_PID,
    SERVICE_FAILED_TO_START_DB,
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
    LAUNCHER_UNAVAILABLE,
    MISSING_PROPERTY,
    MISSING_PEM,
    MISSING_PASSPHRASE,
    MISSING_DB_CONFIG,
    MISSING_DB_DIR,
    STORAGE_RUNNING,
    STORAGE_NOT_RUNNING
    ;

    static final String RESOURCE_BUNDLE = "com.redhat.thermostat.agent.cli.impl.strings";

    public static Translate<LocaleResources> createLocalizer() {
        return new Translate<>(RESOURCE_BUNDLE, LocaleResources.class);
    }

}

