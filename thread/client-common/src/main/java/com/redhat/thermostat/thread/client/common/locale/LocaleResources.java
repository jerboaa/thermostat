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

package com.redhat.thermostat.thread.client.common.locale;

import com.redhat.thermostat.shared.locale.Translate;

public enum LocaleResources {

    NAME,
    ID,
    FIRST_SEEN,
    LAST_SEEN,
    WAIT_COUNT,
    BLOCK_COUNT,
    RUNNING,
    WAITING,
    SLEEPING,
    MONITOR,
    
    START_RECORDING,
    STOP_RECORDING,
    THREAD_MONITOR_SWITCH,
    
    VM_CAPABILITIES,
    VM_DEADLOCK,
    TABLE,
    DETAILS,
    TIMELINE,
    THREAD_COUNT,
    
    LIVE_THREADS,
    DAEMON_THREADS,
    
    THREAD_CONTROL_PANEL,
    THREAD_DUMP,
    
    MISSING_INFO,
    THREAD_DETAILS_EMTPY,

    CHECK_FOR_DEADLOCKS,
    DEADLOCK_WAITING_ON,
    DEADLOCK_THREAD_NAME,
    DEADLOCK_THREAD_TOOLTIP,
    DEADLOCK_EDGE_TOOLTIP,

    RESTORE_ZOOM,
    ZOOM_IN,
    ZOOM_OUT,

    LOCKS,
    LOCK_COLUMN_NAME,
    LOCK_COLUMN_VALUE,
    LOCK_DESCRIPTION_CONTENDED_LOCK_ATTEMPS,
    LOCK_DESCRIPTION_DEFLATIONS,
    LOCK_DESCRIPTION_EMPTY_NOTIFICATIONS,
    LOCK_DESCRIPTION_FAILED_SPINS,
    LOCK_DESCRIPTION_FUTILE_WAKEUPS,
    LOCK_DESCRIPTION_INFLATIONS,
    LOCK_DESCRIPTION_MON_EXTANT,
    LOCK_DESCRIPTION_MON_IN_CIRCULATION,
    LOCK_DESCRIPTION_MON_SCAVENGED,
    LOCK_DESCRIPTION_NOTIFICATIONS,
    LOCK_DESCRIPTION_PARKS,
    LOCK_DESCRIPTION_PRIVATE_A,
    LOCK_DESCRIPTION_PRIVATE_B,
    LOCK_DESCRIPTION_SLOW_ENTER,
    LOCK_DESCRIPTION_SLOW_EXIT,
    LOCK_DESCRIPTION_SLOW_NOTIFY,
    LOCK_DESCRIPTION_SLOW_NOTIFY_ALL,
    LOCK_DESCRIPTION_SUCCESSFUL_SPINS,

    ;

    public static final String RESOURCE_BUNDLE =
            "com.redhat.thermostat.thread.client.common.locale.strings";
    
    public static Translate<LocaleResources> createLocalizer() {
        return new Translate<>(RESOURCE_BUNDLE, LocaleResources.class);
    }
}

