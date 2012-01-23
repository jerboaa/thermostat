/*
 * Copyright 2012 Red Hat, Inc.
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

package com.redhat.thermostat.common.utils;

import java.util.logging.ConsoleHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import com.redhat.thermostat.common.LogFormatter;

/**
 * A few helper functions to facilitate using loggers
 * <p>
 * It would be good idea to call {@link LogManager#readConfiguration()} with a
 * properties file that sets an appropriate value for ".level"
 */
public final class LoggingUtils {

    private static Logger root = null;
    private static final String ROOTNAME = "com.redhat.thermostat";

    private LoggingUtils() {
        /* should not be instantiated */
    }

    /**
     * Pretty much every one of the utility methods in this static class should call this method before doing anything else.
     */
    private static void ensureRootLogger() {
        if (root == null) {
            root = Logger.getLogger(ROOTNAME);
            root.setUseParentHandlers(false);
            for (Handler handler : root.getHandlers()) {
                handler.setFormatter(new LogFormatter());
                // This is workaround for http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4462908
                handler.setLevel(Level.ALL);
            }
        }
    }

    /**
     * Set the log level for the logger at the root of the "com.redhat.thermostat" namespace
     * 
     * @param level the minimum level at which logging statements should appear in the logs
     */
    public static void setGlobalLogLevel(Level level) {
        ensureRootLogger();
        root.setLevel(level);
    }

    /**
     * Returns an appropriate logger to be used by class klass.
     */
    public static Logger getLogger(Class<?> klass) {
        ensureRootLogger();
        Logger logger = Logger.getLogger(klass.getPackage().getName());
        logger.setLevel(null); // Will inherit from root logger
        return logger;
    }

    /**
     * Ensures log messages are written to the console as well
     */
    public static void useDevelConsole() {
        ensureRootLogger();
        ConsoleHandler handler = new ConsoleHandler();
        handler.setFormatter(new LogFormatter());
        // This is workaround for http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4462908
        handler.setLevel(Level.ALL);
        root.addHandler(handler);
    }

}
