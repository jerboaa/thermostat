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
