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
import com.redhat.thermostat.common.config.Configuration;
import com.redhat.thermostat.common.config.InvalidConfigurationException;

/**
 * A few helper functions to facilitate using loggers
 * <p>
 * It would be good idea to call {@link LogManager#readConfiguration()} with a
 * properties file that sets an appropriate value for ".level"
 */
public final class LoggingUtils {

    // package private for testing
    static final String ROOTNAME = "com.redhat.thermostat";

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

    public static void loadGlobalLoggingConfig() throws InvalidConfigurationException {
        File thermostatConfigurationDir = new File(new Configuration().getConfigurationDir());
        File loggingPropertiesFile = new File(thermostatConfigurationDir, "logging.properties");
        loadConfig(loggingPropertiesFile);
    }
    

    public static void loadUserLoggingConfig() throws InvalidConfigurationException {
        File thermostatUserDir = new File(new Configuration().getThermostatUserHome());
        File loggingPropertiesFile = new File(thermostatUserDir, "logging.properties");
        loadConfig(loggingPropertiesFile);
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
            throw new InvalidConfigurationException("Could not read logging.properties", e);
        }
        try (FileInputStream fis = new FileInputStream(loggingPropertiesFile)) {
            // Finally add handlers as specified in the property file, with
            // ConsoleHandler and level INFO as default
            configureLogging(getDefaultProps(), fis);
        } catch (SecurityException | IOException e) {
            throw new InvalidConfigurationException("Could not read logging.properties", e);
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
                handler.setLevel(root.getLevel());
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

