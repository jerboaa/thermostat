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

package com.redhat.thermostat.tools.db;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.redhat.thermostat.common.config.InvalidConfigurationException;
import com.redhat.thermostat.common.tools.ApplicationException;
import com.redhat.thermostat.common.utils.LoggedExternalProcess;
import com.redhat.thermostat.common.utils.LoggingUtils;

import com.redhat.thermostat.service.process.UnixProcessUtilities;

class MongoProcessRunner {
    
    private static final Logger logger = LoggingUtils.getLogger(MongoProcessRunner.class);
    
    private static final String [] MONGO_BASIC_ARGS = {
        "mongod", "--quiet", "--fork", "--nojournal", "--noauth", "--bind_ip"
    };
    
    private static final String [] MONGO_SHUTDOWN_ARGS = {
        "mongod", "--shutdown", "--dbpath"
    };
    
    private DBStartupConfiguration configuration;
    private boolean isQuiet;
    
    MongoProcessRunner(DBStartupConfiguration configuration, boolean quiet) {
        this.configuration = configuration;
        this.isQuiet = quiet;
    }
   
    private String getPid() {
        
        String pid = null;
        
        File pidfile = configuration.getPidFile();
        Charset charset = Charset.defaultCharset();
        if (pidfile.exists()) {
            try (BufferedReader reader = Files.newBufferedReader(pidfile.toPath(), charset)) {
                pid = reader.readLine();
                if (pid == null || pid.isEmpty()) {
                    pid = null;
                }
            } catch (IOException ex) {
                logger.log(Level.WARNING, "Exception while reading pid file", ex);
                pid = null;
            }
        }
        
        return pid;
    }
    
    void stopService() throws IOException, InterruptedException, InvalidConfigurationException, ApplicationException {
 
        List<String> commands = new ArrayList<>(Arrays.asList(MONGO_SHUTDOWN_ARGS));
        commands.add(configuration.getDBPath().getCanonicalPath());

        LoggedExternalProcess process = new LoggedExternalProcess(commands);
        int status = process.runAndReturnResult();
        if (status == 0) {
            display("server shutdown complete: " + configuration.getDBPath());
            display("log file is here: " + configuration.getLogFile());
            
        } else {
            
            String message = "cannot shutdown server " + configuration.getDBPath() +
                    ", exit status: " + status +
                    ". Please check that your configuration is valid";
            display(message);
            throw new ApplicationException(message);
        }
    }
    
    private boolean checkExistingProcess() {
        String pid = getPid();
        if (pid == null)
            return false;
        
        String processName = UnixProcessUtilities.getInstance().getProcessName(getPid());
        // TODO: check if we want mongos or mongod from the configs
        return processName != null && processName.equalsIgnoreCase("mongod");
    }
    
    void startService() throws IOException, InterruptedException, ApplicationException {
        
        String pid = getPid();
        if (pid != null) {
            String message = null;
            if (!checkExistingProcess()) {
                message = "A stale pid file (" + configuration.getPidFile() + ") is present " +
                    "but there is no matching mongod process. Please remove the file if it has been shut down";
            } else {
                message = "An instance of the storage is already running with pid " + pid;
            }
            
            display(message);
            throw new ApplicationException(message);
        }
        
        List<String> commands = new ArrayList<>(Arrays.asList(MONGO_BASIC_ARGS));
       
        // check that the db directory exist
        display("starting storage server...");

        commands.add(configuration.getBindIP());

        commands.add("--dbpath");
        commands.add(configuration.getDBPath().getCanonicalPath());

        commands.add("--logpath");
        commands.add(configuration.getLogFile().getCanonicalPath());

        commands.add("--pidfilepath");
        commands.add(configuration.getPidFile().getCanonicalPath());

        commands.add("--port");
        if (configuration.isLocal()) {
            commands.add(Long.toString(configuration.getLocalPort()));
        } else {
            commands.add(Long.toString(configuration.getClusterPort()));
        }
        
        LoggedExternalProcess process = new LoggedExternalProcess(commands);
        int status = -1;
        try {
            status = process.runAndReturnResult();
        } catch (ApplicationException ae) {
            String message = "can not execute mongo process. is it installed?";
            display(message);
            throw ae;
        }

        if (status == 0) {
            pid = getPid();
            if (pid == null) status = -1;
        }
        
        if (status == 0) {
            display("server listening on ip: " + configuration.getDBConnectionString());
            display("log file is here: " + configuration.getLogFile());
            display("pid: " + pid);
            
        } else {
            
            String message = "cannot start server " + configuration.getDBPath() +
                             ", exit status: " + status +
                             ". Please check that your configuration is valid";
            display(message);
            throw new ApplicationException(message);
        }
    }
 
    private void display(String message) {
        if (!isQuiet) {
            System.out.println(message);
        }
    }
}
