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

package com.redhat.thermostat.eclipse;

import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import com.redhat.thermostat.eclipse.internal.Activator;

public class LoggerFacility {

    private ILog log;
    private static LoggerFacility instance;
    private static boolean isLoggingEnabled; // want default of false

    private LoggerFacility() {
        log = Activator.getDefault().getLog();
        isLoggingEnabled = Activator.inDebugMode();
    }

    /**
     * Get a LoggerFacility singleton.
     * 
     * @return The singleton instance.
     */
    public static LoggerFacility getInstance() {
        if (instance == null) {
            instance = new LoggerFacility();
        }
        return instance;
    }

    /**
     * Logs messages with the given severity.
     * 
     * @param message
     *            The human readable localized message.
     * @param throwable
     *            The exception which occurred.
     */
    public void log(int severity, String message, Throwable throwable) {
        if (isLoggingEnabled) {
            log.log(new Status(severity, Activator.PLUGIN_ID, message,
                    throwable));
        }
    }

    /**
     * Logs messages with the given severity.
     * 
     * @param message
     *            A human readable localized message.
     * @param severity
     *            A suitable severity. See {@link IStatus}.
     */
    public void log(int severity, String message) {
        if (isLoggingEnabled) {
            log.log(new Status(severity, Activator.PLUGIN_ID, message));
        }
    }
}

