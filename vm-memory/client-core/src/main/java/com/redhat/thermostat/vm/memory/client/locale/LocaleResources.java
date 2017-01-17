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

package com.redhat.thermostat.vm.memory.client.locale;

import com.redhat.thermostat.shared.locale.Translate;

public enum LocaleResources {

    VM_INFO_TAB_MEMORY,
    ERROR_PERFORMING_GC,
    MEMORY_REGIONS_HEADER,

    TAB_MEMORY,
    TAB_MEMORY_TOOLTIP,
    TAB_TLAB,
    TAB_TLAB_TOOLTIP,

    TLAB_TOTAL_ALLOCATING_THREADS,
    TLAB_TOTAL_ALLOCATIONS,
    TLAB_TOTAL_REFILLS,
    TLAB_MAX_REFILLS,
    TLAB_TOTAL_SLOW_ALLOCATIONS,
    TLAB_MAX_SLOW_ALLOCATIONS,
    TLAB_TOTAL_GC_WASTE,
    TLAB_MAX_GC_WASTE,
    TLAB_TOTAL_SLOW_WASTE,
    TLAB_MAX_SLOW_WASTE,
    TLAB_TOTAL_FAST_WASTE,
    TLAB_MAX_FAST_WASTE,

    RESOURCE_MISSING,
    CHART_DURATION_SELECTOR_LABEL,

    TLAB_CHART_NUMBER_AXIS,
    TLAB_CHART_BYTE_AXIS,

    ;

    public static final String RESOURCE_BUNDLE =
            "com.redhat.thermostat.vm.memory.client.locale.strings";
    
    public static Translate<LocaleResources> createLocalizer() {
        return new Translate<>(RESOURCE_BUNDLE, LocaleResources.class);
    }
}

