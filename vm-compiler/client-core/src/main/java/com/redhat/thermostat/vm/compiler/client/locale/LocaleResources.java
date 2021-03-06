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

package com.redhat.thermostat.vm.compiler.client.locale;

import com.redhat.thermostat.shared.locale.Translate;

public enum LocaleResources {

    VM_INFO_TAB_COMPILER,
    VM_COMPILER_HEADER,

    STATS_TABLE_COLUMN_NAME,
    STATS_TABLE_COLUMN_VALUE,

    STAT_CHART_NUMBER_AXIS,
    STAT_CHART_TIME_AXIS,

    STATS_TOTAL_COMPILES,
    STATS_TOTAL_BAILOUTS,
    STATS_TOTAL_INVALIDATES,
    STATS_COMPILATION_TIME,
    STATS_LAST_SIZE,
    STATS_LAST_TYPE,
    STATS_LAST_METHOD,
    STATS_LAST_FAILED_TYPE,
    STATS_LAST_FAILED_METHOD,

    COMPILE_TYPE_NO_COMPILE,
    COMPILE_TYPE_NO_FAILED_COMPILE,
    COMPILE_TYPE_NORMAL_COMPILE,
    COMPILE_TYPE_OSR_COMPILE,
    COMPILE_TYPE_NATIVE_COMPILE,

    ;

    public static final String RESOURCE_BUNDLE =
            "com.redhat.thermostat.vm.compiler.client.locale.strings";

    public static Translate<LocaleResources> createLocalizer() {
        return new Translate<>(RESOURCE_BUNDLE, LocaleResources.class);
    }
}

