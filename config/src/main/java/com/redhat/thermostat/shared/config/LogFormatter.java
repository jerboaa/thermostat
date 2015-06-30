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

package com.redhat.thermostat.shared.config;

import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.LogRecord;

public class LogFormatter extends Formatter {

    @Override
    public synchronized String format(LogRecord record) {
        StringBuilder sb = new StringBuilder();
        Level level = record.getLevel();
        sb.append(level.toString() + " - ");
        String[] fullLoggingClassName = record.getSourceClassName().split("\\.");
        String loggingClassName = fullLoggingClassName[fullLoggingClassName.length - 1];
        sb.append(loggingClassName);
        sb.append(": ");
        sb.append(record.getMessage());
        sb.append("\n");
        Throwable thrown = record.getThrown();
        String indent = "  ";
        while (thrown != null) {
            sb.append(indent);
            sb.append("Caused by:\n");
            sb.append(indent);
            sb.append(thrown.getClass().getCanonicalName());
            sb.append(": ");
            sb.append(thrown.getMessage());
            sb.append("\n");
            StackTraceElement[] stack = thrown.getStackTrace();
            for (int i = 0; i < stack.length; i++) {
                sb.append(indent);
                sb.append(stack[i].toString());
                sb.append("\n");
            }
            thrown = thrown.getCause();
            indent = indent.concat("  ");
        }
        return sb.toString();
    }
}

