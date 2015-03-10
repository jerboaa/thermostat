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

package com.redhat.thermostat.itest;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.SimpleFormatter;

class LogConfigurator {

    private static final String TH_HANDLERS_PROP = "com.redhat.thermostat.handlers";
    private static final String TH_LOG_LEVEL_PROP = "com.redhat.thermostat.level";
    private static final String FILE_HANDLER_PATTERN_PROP = "java.util.logging.FileHandler.pattern";
    private static final String FILE_HANDLER_FORMATTER_PROP = "java.util.logging.FileHandler.formatter"; 
    
    private final Level desiredLevel;
    private final File loggingPropertiesFile;
    private final File destinationLogFile;
    
    LogConfigurator(Level desiredLevel, File loggingPropertiesFile, File destinationLogFile) {
        this.desiredLevel = desiredLevel;
        this.loggingPropertiesFile = loggingPropertiesFile;
        this.destinationLogFile = destinationLogFile;
    }
    
    /**
     * Configure integration tests to use a FileHandler and only a FileHandler.
     * @return valid thermostat logging.properties.
     */
    Properties getLoggingProperties() {
        Properties props = new Properties();
        // configure java.util.logging.FileHandler
        props.put(FILE_HANDLER_PATTERN_PROP, destinationLogFile.getAbsolutePath());
        props.put(FILE_HANDLER_FORMATTER_PROP, SimpleFormatter.class.getName());
        // configure thermostat to use a FileHandler only
        props.put(TH_HANDLERS_PROP, FileHandler.class.getName());
        props.put(TH_LOG_LEVEL_PROP, desiredLevel.toString());
        return props;
    }
    
    /**
     * Writes the log configuration to disk.
     */
    void writeConfiguration() {
        // Clean up potentially existing log files from previous runs. We cannot
        // delete on exit since jenkins (might) archive this file for debug purposes.
        if (this.destinationLogFile.exists()) {
            this.destinationLogFile.delete();
        }
        Properties props = getLoggingProperties();
        try (FileOutputStream fout = new FileOutputStream(loggingPropertiesFile)) {
            props.store(fout, "Thermostat integration test logging properties");
        } catch (IOException e) {
            throw new RuntimeException("Failed to write itest logging configuration");
        }
        System.out.println("Configured logging.properties for integration tests:");
        System.out.println("  Log level:            " + desiredLevel.toString());
        System.out.println("  Destination log file: " + destinationLogFile.getAbsolutePath());
    }
}
