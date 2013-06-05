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

package com.redhat.thermostat.vm.heap.analysis.client.locale;

import com.redhat.thermostat.shared.locale.Translate;

public enum LocaleResources {
    COMMAND_OBJECT_INFO_OBJECT_ID,
    COMMAND_OBJECT_INFO_TYPE,
    COMMAND_OBJECT_INFO_SIZE,
    COMMAND_OBJECT_INFO_HEAP_ALLOCATED,
    
    HEAP_SECTION_TITLE,
    HEAP_OVERVIEW_TITLE,
    HEAP_CHART_TIME_AXIS,
    HEAP_CHART_HEAP_AXIS,
    HEAP_CHART_CAPACITY,
    HEAP_CHART_USED,
    
    HEAP_DUMP_SECTION_HISTOGRAM,
    HEAP_DUMP_SECTION_OBJECT_BROWSER,
    
    HEAP_DUMP_CLASS_USAGE,
    HEAP_DUMP_HISTOGRAM_COLUMN_CLASS,
    HEAP_DUMP_HISTOGRAM_COLUMN_INSTANCES,
    HEAP_DUMP_HISTOGRAM_COLUMN_SIZE,
    
    HEAP_DUMP_OBJECT_BROWSE_SEARCH_HINT,
    HEAP_DUMP_OBJECT_BROWSE_SEARCH_PATTERN_HELP,
    HEAP_DUMP_OBJECT_BROWSE_SEARCH_LABEL,
    HEAP_DUMP_OBJECT_BROWSE_REFERRERS,
    HEAP_DUMP_OBJECT_BROWSE_REFERENCES,
    HEAP_DUMP_OBJECT_FIND_ROOT,
    
    OBJECT_ROOTS_VIEW_TITLE,

    TRIGGER_HEAP_DUMP,
    HEAP_DUMP_IN_PROGRESS,
    PROCESS_EXITED,
    
    DUMPS_LIST,
    LIST_DUMPS_ACTION,
    
    ;

    static final String RESOURCE_BUNDLE = "com.redhat.thermostat.vm.heap.analysis.client.locale.strings";

    public static Translate<LocaleResources> createLocalizer() {
        return new Translate<>(RESOURCE_BUNDLE, LocaleResources.class);
    }
}

