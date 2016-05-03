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

package com.redhat.thermostat.common.utils;

import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import com.redhat.thermostat.common.internal.LocaleResources;
import com.redhat.thermostat.shared.config.CommonPaths;
import com.redhat.thermostat.shared.config.InvalidConfigurationException;
import com.redhat.thermostat.shared.locale.Translate;

/**
 * A few helper functions to facilitate using loggers
 * <p>
 * It would be good idea to call {@link LogManager#readConfiguration()} with a
 * properties file that sets an appropriate value for ".level"
 */
public final class LoggingUtils {

    public enum LogLevel {
        /*
         * Custom log level, intended for use with Thermostat's internal performance
         * analysis framework.  Log messages at this level should be formatted using
         * {@link com.redhat.thermostat.shared.perflog.PerformanceLogFormatter}.
         */
        PERFLOG(new Level("PERFLOG", 50) {
            private static final long serialVersionUID = 1L;
        }),
        ALL(Level.ALL),
        CONFIG(Level.CONFIG),
        FINE(Level.FINE),
        FINER(Level.FINER),
        FINEST(Level.FINEST),
        INFO(Level.INFO),
        OFF(Level.OFF),
        SEVERE(Level.SEVERE),
        WARNING(Level.WARNING);

        private Level level;

        LogLevel(Level level) {
            this.level = level;
        }

        public Level getLevel() {
            return level;
        }
    }

    // package private for testing
    static final String ROOTNAME = "com.redhat.thermostat";

    private static final Translate<LocaleResources> t = LocaleResources.createLocalizer();
    private static final Logger root;

    static {
        root = Logger.getLogger(ROOTNAME);
    }

    private LoggingUtils() {
        /* should not be instantiated */
    }

    /**
     * Set the log level for the logger at the root of the "com.redhat.thermostat" namespace
     * 
     * @param level the minimum level at which logging statements should appear in the logs
     */
    public static void setGlobalLogLevel(Level level) {
        root.setLevel(level);
    }

    /**
     * Returns an appropriate logger to be used by class klass.
     */
    public static Logger getLogger(Class<?> klass) {
        Logger logger = Logger.getLogger(klass.getPackage().getName());
        logger.setLevel(null); // Will inherit from root logger
        return logger;
    }

    /**
     * @deprecated this is done via the launcher now
     */
    @Deprecated
    public static void loadGlobalLoggingConfig(CommonPaths paths) throws InvalidConfigurationException {
        // nothing to do
    }

    /**
     * @deprecated this is done via the launcher now
     */
    @Deprecated
    public static void loadUserLoggingConfig(CommonPaths paths) throws InvalidConfigurationException {
        // nothing to do
    }

    public static Level getEffectiveLogLevel(Logger logger) {
        Level level = logger.getLevel();
        while (level == null) {
            logger = logger.getParent();
            level = logger.getLevel();
        }
        return level;
    }

}

