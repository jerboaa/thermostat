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

package com.redhat.thermostat.vm.overview.client.locale;

import com.redhat.thermostat.shared.locale.Translate;

public enum LocaleResources {
    VM_INFO_TAB_OVERVIEW,
    
    VM_INFO_TITLE,
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
    VM_INFO_USER,
    VM_INFO_USER_UNKNOWN,
    ;

    static final String RESOURCE_BUNDLE =
            "com.redhat.thermostat.vm.overview.client.locale.strings";

    public static Translate<LocaleResources> createLocalizer() {
        return new Translate<>(RESOURCE_BUNDLE, LocaleResources.class);
    }
}

