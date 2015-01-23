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

package com.redhat.thermostat.common.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;
import java.util.logging.ConsoleHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import com.redhat.thermostat.common.LogFormatter;
import com.redhat.thermostat.common.locale.LocaleResources;
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

    /*
     * Custom log level, intended for use with Thermostat's internal performance
     * analysis framework.  Log messages at this level should be formatted using
     * {@link com.redhat.thermostat.shared.perflog.PerformanceLogFormatter}.
     */
    public static final Level PERFLOG = new Level("PERFLOG", 50) {
        private static final long serialVersionUID = 1L;
    };

    // package private for testing
    static final String ROOTNAME = "com.redhat.thermostat";

    private static final Translate<LocaleResources> t = LocaleResources.createLocalizer();
    private static final Logger root;

    private static final ConsoleHandler handler;

    private static final String HANDLER_PROP = ROOTNAME + ".handlers";
    private static final String LOG_LEVEL_PROP = ROOTNAME + ".level";
    private static final String DEFAULT_LOG_HANDLER = "java.util.logging.ConsoleHandler";
    private static final Level DEFAULT_LOG_LEVEL = Level.INFO;

    static {
        root = Logger.getLogger(ROOTNAME);
        root.setUseParentHandlers(false);
        for (Handler handler : root.getHandlers()) {
            handler.setFormatter(new LogFormatter());
            // This is workaround for http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4462908
            handler.setLevel(Level.ALL);
        }

        handler = new ConsoleHandler();
        handler.setFormatter(new LogFormatter());
        // This is workaround for http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4462908
        handler.setLevel(Level.ALL);

        enableConsoleLogging();
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
     * Ensures log messages are written to the console as well
     */
    public static void enableConsoleLogging() {
        root.removeHandler(handler);
        root.addHandler(handler);
    }

    public static void disableConsoleLogging() {
        root.removeHandler(handler);
    }

    public static void loadGlobalLoggingConfig(CommonPaths paths) throws InvalidConfigurationException {
        File systemConfigurationDir = paths.getSystemConfigurationDirectory();
        File loggingPropertiesFile = new File(systemConfigurationDir, "logging.properties");
        loadConfig(loggingPropertiesFile);
    }
    

    public static void loadUserLoggingConfig(CommonPaths paths) throws InvalidConfigurationException {
        File userConfigurationDir = paths.getUserConfigurationDirectory();
        File loggingPropertiesFile = new File(userConfigurationDir, "logging.properties");
        loadConfig(loggingPropertiesFile);
    }

    public static Level getEffectiveLogLevel(Logger logger) {
        Level level = logger.getLevel();
        while (level == null) {
            logger = logger.getParent();
            level = logger.getLevel();
        }
        return level;
    }

    // for testing
    static void loadConfig(File loggingPropertiesFile) throws InvalidConfigurationException {
        if (loggingPropertiesFile.isFile()) {
            readLoggingProperties(loggingPropertiesFile);
        }
    }

    private static void readLoggingProperties(File loggingPropertiesFile)
            throws InvalidConfigurationException {
        try (FileInputStream fis = new FileInputStream(loggingPropertiesFile)){
            // Set basic logger configs. Note that this does NOT add handlers.
            // It also resets() handlers. I.e. removes any existing handlers
            // for the root logger.
            LogManager.getLogManager().readConfiguration(fis);
        } catch (SecurityException | IOException e) {
            throw new InvalidConfigurationException(t.localize(LocaleResources.LOGGING_PROPERTIES_ISSUE), e);
        }
        try (FileInputStream fis = new FileInputStream(loggingPropertiesFile)) {
            // Finally add handlers as specified in the property file, with
            // ConsoleHandler and level INFO as default
            configureLogging(getDefaultProps(), fis);
        } catch (SecurityException | IOException e) {
            throw new InvalidConfigurationException(t.localize(LocaleResources.LOGGING_PROPERTIES_ISSUE), e);
        }
    }

    private static void configureLogging(Properties defaultProps, FileInputStream fis) throws IOException {
        Properties props = new Properties(defaultProps);
        props.load(fis);
        String handlers = props.getProperty(HANDLER_PROP);
        for (String clazzName: handlers.split(",")) {
            clazzName = clazzName.trim();
            try {
                // JVM provided class. Using system class loader is safe.
                @SuppressWarnings("rawtypes")
                Class clazz = ClassLoader.getSystemClassLoader().loadClass(clazzName);
                Handler handler = (Handler)clazz.newInstance();
                handler.setLevel(getEffectiveLogLevel(root));
                root.addHandler(handler);
            } catch (Exception e) {
                System.err.print("Could not load log-handler '" + clazzName + "'");
                e.printStackTrace();
            }
        }
    }

    private static Properties getDefaultProps() {
        Properties defaultProps = new Properties();
        defaultProps.put(HANDLER_PROP, DEFAULT_LOG_HANDLER);
        defaultProps.put(LOG_LEVEL_PROP, DEFAULT_LOG_LEVEL);
        return defaultProps;
    }

}

