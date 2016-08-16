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

package com.redhat.thermostat.backend.internal;

import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;

/*
 * Test log handler used for VmMonitor log testing.
 */
public class TestLogHandler extends Handler {
    
    private static final String PROCESS_NOT_FOUND = "Process not found";
    private static final String EXPECTED_UNABLE_TO_ATTACH_PROC_NOT_FOUND_FMT =
            "Tried to attach to a process which no longer exists. Pid was %d";
    private static final String EXPECTED_UNABLE_TO_ATTACH_MSG_FMT =
            "unable to attach to vm %d";
    private boolean unableToAttachLoggedWarning;
    private boolean unableToAttachLoggedFinest;
    private final String unableToAttachMsg;
    private final String procNotFoundMsg;
    
    TestLogHandler(int pid) {
        unableToAttachMsg = String.format(EXPECTED_UNABLE_TO_ATTACH_MSG_FMT, pid);
        procNotFoundMsg = String.format(EXPECTED_UNABLE_TO_ATTACH_PROC_NOT_FOUND_FMT, pid);
    }
    
    @Override
    public void publish(LogRecord record) {
        String logMessage = record.getMessage();
        if (record.getLevel().intValue() >= Level.WARNING.intValue() && 
                logMessage.equals(unableToAttachMsg)) {
            unableToAttachLoggedWarning = diagnoseRecord(record);
        }
        if (record.getLevel().intValue() == Level.FINEST.intValue() &&
                logMessage.equals(procNotFoundMsg)) {
            unableToAttachLoggedFinest = diagnoseRecord(record);
        }
    }

    private boolean diagnoseRecord(LogRecord record) {
        Throwable thrown = record.getThrown();
        if (thrown != null && thrown.getCause() instanceof IllegalArgumentException) {
            return true;
        }
        return false;
    }

    @Override
    public void flush() {
        // nothing
    }

    @Override
    public void close() throws SecurityException {
        // nothing
    }
    
    boolean isUnableToAttachLoggedAsWarning() {
        return unableToAttachLoggedWarning;
    }
    
    boolean isUnableToAttachLoggedAsWarningUnrelated() {
        return !unableToAttachLoggedWarning && !unableToAttachLoggedFinest;
    }
    
    boolean isUnableToAttachLoggedAsFinest() {
        return unableToAttachLoggedFinest;
    }
    
}

