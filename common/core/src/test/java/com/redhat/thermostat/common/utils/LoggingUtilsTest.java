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

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertEquals;

import java.io.File;
import java.util.Enumeration;
import java.util.logging.ConsoleHandler;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class LoggingUtilsTest {
    
    private static final File GLOBAL_CONFIG = new File(LoggingUtilsTest.class.getResource("/globalLogging.properties").getFile());
    private static final File USER_CONFIG = new File(LoggingUtilsTest.class.getResource("/userLogging.properties").getFile());

    private LogManager logManager;
    private Logger mongodbLogger;
    
    @Before
    public void setUp() {
        // this is intentionally using Logger (not LoggingUtils)
        mongodbLogger = Logger.getLogger("com.mongodb");
        logManager = LogManager.getLogManager();
    }
    
    @After
    public void tearDown() {
        mongodbLogger = null;
    }
    
    @Test
    public void testGlobalLoggingHandlersAreCreated() throws Exception {
        LoggingUtils.loadConfig(GLOBAL_CONFIG);
        verifyContainsRootLogger(logManager.getLoggerNames());
        Logger rootLogger = logManager.getLogger(LoggingUtils.ROOTNAME);
        FileHandler handler = (FileHandler)getLogHandler(rootLogger, FileHandler.class);
        assertNotNull(handler);
        assertEquals(SimpleFormatter.class.getName(), handler.getFormatter().getClass().getName());
        assertEquals(Level.WARNING.getName(), rootLogger.getLevel().getName());
        assertEquals(Level.OFF.getName(), mongodbLogger.getLevel().getName());
    }
    
    @Test
    public void testHandlersHaveSameLogLevelAsRoot() throws Exception {
        LoggingUtils.loadConfig(GLOBAL_CONFIG);
        Logger rootLogger = logManager.getLogger(LoggingUtils.ROOTNAME);
        FileHandler handler = (FileHandler)getLogHandler(rootLogger, FileHandler.class);
        assertEquals(Level.WARNING, rootLogger.getLevel());
        assertEquals(rootLogger.getLevel(), handler.getLevel());
        LoggingUtils.loadConfig(USER_CONFIG);
        rootLogger = logManager.getLogger(LoggingUtils.ROOTNAME);
        ConsoleHandler handler2 = (ConsoleHandler)getLogHandler(rootLogger, ConsoleHandler.class);
        assertEquals(Level.FINEST, rootLogger.getLevel());
        assertEquals(rootLogger.getLevel(), handler2.getLevel());
    }
    
    @Test
    public void testUserLoggingConfigOverridesGlobal() throws Exception {
        LoggingUtils.loadConfig(GLOBAL_CONFIG);
        LoggingUtils.loadConfig(USER_CONFIG);
        verifyContainsRootLogger(logManager.getLoggerNames());
        Logger rootLogger = logManager.getLogger(LoggingUtils.ROOTNAME);
        // global config has FileHandler, user config ConsoleHandler
        ConsoleHandler handler = (ConsoleHandler)getLogHandler(rootLogger, ConsoleHandler.class);
        assertNotNull(handler);
        assertEquals(Level.FINEST.getName(), rootLogger.getLevel().getName());
        assertEquals(Level.INFO.getName(), mongodbLogger.getLevel().getName());
    }

    @SuppressWarnings("rawtypes") 
    private Handler getLogHandler(Logger logger, Class clazz) {
        Handler[] handlers = logger.getHandlers();
        assertEquals(1, handlers.length);
        for (Handler handler: logger.getHandlers()) {
            if (handler.getClass().getName().equals(clazz.getName())) {
                return handler;
            }
        }
        return null;
    }

    private void verifyContainsRootLogger(Enumeration<String> loggerNames) {
        boolean found = false;
        while (loggerNames.hasMoreElements()) {
            String logger = loggerNames.nextElement();
            if (logger.equals(LoggingUtils.ROOTNAME)) {
                found = true;
                break;
            }
        }
        assertTrue(found);
    }
}

