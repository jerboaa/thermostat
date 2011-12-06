package com.redhat.thermostat.common.utils;

import java.util.logging.ConsoleHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import com.redhat.thermostat.common.Constants;
import com.redhat.thermostat.common.LogFormatter;

/**
 * A few helper functions to facilitate using loggers
 * <p>
 * It would be good idea to call {@link LogManager#readConfiguration()} with a
 * properties file that sets an appropriate value for ".level"
 */
public final class LoggingUtils {

    private static Logger root;

    private LoggingUtils() {
        /* should not be instantiated */
    }

    /**
     * Should be called once, very-early during program initialization. This
     * works around http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4462908
     */
    public static Logger resetAndGetRootLogger() {
        root = Logger.getLogger("");
        Handler[] handlers = root.getHandlers();
        for (Handler handler : handlers) {
            root.removeHandler(handler);
        }

        ConsoleHandler handler = new ConsoleHandler();
        handler.setFormatter(new LogFormatter());
        handler.setLevel(Level.ALL);
        root.addHandler(handler);

        return root;
    }

    public static void setGlobalLogLevel(Level level) {
        try {
            LogManager.getLogManager().readConfiguration(StringUtils.toInputStream(Constants.LOG_LEVEL_CONFIG + level.toString()));
        } catch (Exception e) {
            if (root != null) {
                root.log(Level.WARNING, "Error setting new log level.", e);
            } else {
                e.printStackTrace();
            }
        }
    }

    /**
     * Returns an appropriate logger to be used by class klass.
     */
    public static Logger getLogger(Class<?> klass) {
        return Logger.getLogger(klass.getPackage().getName());
    }

}
