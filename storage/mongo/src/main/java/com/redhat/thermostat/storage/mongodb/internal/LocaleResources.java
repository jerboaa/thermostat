/*
 * Copyright 2012-2017 Red Hat, Inc.
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

package com.redhat.thermostat.storage.mongodb.internal;

import com.redhat.thermostat.shared.locale.Translate;


public enum LocaleResources {
    
    COMMON_PATHS_SERVICE_UNAVAILABLE,
    MONGODB_SETUP_FILE_EXISTS,
    ALREADY_CONNECTED_TO_STORAGE_WARNING,
    UNKNOWN_STORAGE_URL,
    STORAGE_SERVICE_UNAVAILABLE,
    MONGOSTORAGE_RETRIEVAL_FAILED,
    STAMP_FILE_CREATION_FAILED,
    MONGODB_USER_SETUP_COMPLETE,
    LAUNCHER_SERVICE_UNAVAILABLE,
    INTERRUPTED_WAITING_FOR_STORAGE_STOP,
    STORAGE_STOP_FAILED,
    USER_SETUP_COMPLETE,
    USER_SETUP_FAILED,
    UNRECOGNIZED_PAYLOAD_FROM_STORAGE_CMD,
    ADDING_USER_FAILED,
    INTERRUPTED_WAITING_FOR_ADD_USER,
    DISPATCHER_WRONG_OPTION,
    USERNAME_PROMPT,
    PASSWORD_PROMPT
    ;

    static final String RESOURCE_BUNDLE = "com.redhat.thermostat.storage.mongodb.strings";

    public static Translate<LocaleResources> createLocalizer() {
        return new Translate<>(RESOURCE_BUNDLE, LocaleResources.class);
    }

}

