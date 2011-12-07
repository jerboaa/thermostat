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

    private LoggingUtils() {
        /* should not be instantiated */
    }

    /**
     * Pretty much every one of the utility methods in this static class should call this method before doing anything else.
     */
    private static void ensureRootLogger() {
        if (root == null) {
            root = Logger.getLogger("com.redhat.thermostat");
            root.setUseParentHandlers(false);
            for (Handler handler : root.getHandlers()) {
                handler.setFormatter(new LogFormatter());
            }
        }
    }

    /**
     * This is workaround for http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4462908
     * Must be called when changing LogManager configuration (such as via readConfiguration())
     */
    public static void setGlobalLogLevel(Level level) {
        ensureRootLogger();
        root.setLevel(level);
        for (Handler handler : root.getHandlers()) {
            handler.setLevel(level);
        }
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
     * 
     */
    public static void useDevelConsole() {
        ensureRootLogger();
        ConsoleHandler handler = new ConsoleHandler();
        handler.setFormatter(new LogFormatter());;
        root.addHandler(handler);
    }

}
