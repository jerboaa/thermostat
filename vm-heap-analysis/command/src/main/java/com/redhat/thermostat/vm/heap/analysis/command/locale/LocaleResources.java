/*
 * Copyright 2012-2016 Red Hat, Inc.
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

package com.redhat.thermostat.vm.heap.analysis.command.locale;

import com.redhat.thermostat.shared.locale.Translate;

public enum LocaleResources {
    HOST_SERVICE_UNAVAILABLE,
    VM_SERVICE_UNAVAILABLE,
    HEAP_SERVICE_UNAVAILABLE,
    AGENT_SERVICE_UNAVAILABLE,
    REQUEST_QUEUE_UNAVAILABLE,
    
    HEADER_TIMESTAMP,
    HEADER_AGENT_ID,
    HEADER_VM_ID,
    HEADER_HEAP_ID,
    HEADER_OBJECT_ID,
    HEADER_OBJECT_TYPE,
    
    INVALID_LIMIT,
    HEAP_ID_NOT_FOUND,
    HEAP_ID_REQUIRED,
    SEARCH_TERM_REQUIRED,
    OBJECT_ID_REQUIRED,
    HEAP_DUMP_ERROR,
    HEAP_DUMP_ERROR_AGENT_DEAD,
    
    COMMAND_HEAP_DUMP_DONE,
    COMMAND_HEAP_DUMP_DONE_NOID,

    COMMAND_FIND_ROOT_NO_ROOT_FOUND,

    COMMAND_OBJECT_INFO_OBJECT_ID,
    COMMAND_OBJECT_INFO_TYPE,
    COMMAND_OBJECT_INFO_SIZE,
    COMMAND_OBJECT_INFO_HEAP_ALLOCATED,
    COMMAND_OBJECT_INFO_REFERENCES,
    COMMAND_OBJECT_INFO_REFERRERS,

    OBJECT_NOT_FOUND_MESSAGE,
    ERROR_READING_HISTOGRAM_MESSAGE,

    COMMAND_SAVE_HEAP_DUMP_SAVED_TO_FILE,
    COMMAND_SAVE_HEAP_DUMP_ERROR_SAVING,
    COMMAND_SAVE_HEAP_DUMP_ERROR_CLOSING_STREAM,
    
    TABLE_CLASS_NAME,
    TABLE_NUMBER_INSTANCES,
    TABLE_TOTAL_SIZE,
    ;
    
    static final String RESOURCE_BUNDLE = "com.redhat.thermostat.vm.heap.analysis.command.locale.strings";

    public static Translate<LocaleResources> createLocalizer() {
        return new Translate<>(RESOURCE_BUNDLE, LocaleResources.class);
    }
}

