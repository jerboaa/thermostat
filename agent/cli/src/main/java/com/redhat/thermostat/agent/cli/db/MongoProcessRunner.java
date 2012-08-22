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

package com.redhat.thermostat.agent.cli.db;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.redhat.thermostat.agent.cli.impl.locale.LocaleResources;
import com.redhat.thermostat.agent.cli.impl.locale.Translate;
import com.redhat.thermostat.common.config.InvalidConfigurationException;
import com.redhat.thermostat.common.tools.ApplicationException;
import com.redhat.thermostat.common.utils.LoggedExternalProcess;
import com.redhat.thermostat.common.utils.LoggingUtils;
import com.redhat.thermostat.service.process.UnixProcessUtilities;

public class MongoProcessRunner {
    
    private static final Logger logger = LoggingUtils.getLogger(MongoProcessRunner.class);

    private static final String MONGO_PROCESS = "mongod";

    private static final String [] MONGO_BASIC_ARGS = {
        "mongod", "--quiet", "--fork", "--auth", "--bind_ip"
    };

    private static final String [] MONGO_SHUTDOWN_ARGS = {
        "kill", "-s", "TERM"
    };

    private static final String NO_JOURNAL_ARGUMENT = "--nojournal";
    private static final String NO_JOURNAL_FIRST_VERSION = "1.9.2";

    private DBStartupConfiguration configuration;
    private boolean isQuiet;
    
    public MongoProcessRunner(DBStartupConfiguration configuration, boolean quiet) {
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
    
    public void stopService() throws IOException, InterruptedException, InvalidConfigurationException, ApplicationException {
 
        List<String> commands = new ArrayList<>(Arrays.asList(MONGO_SHUTDOWN_ARGS));
        commands.add(getPid());

        LoggedExternalProcess process = new LoggedExternalProcess(commands);
        int status = process.runAndReturnResult();
        if (status == 0) {
            display(Translate.localize(LocaleResources.SERVER_SHUTDOWN_COMPLETE, configuration.getDBPath().toString()));
            display(Translate.localize(LocaleResources.LOG_FILE_AT, configuration.getLogFile().toString()));
            
        } else {
            
            String message = Translate.localize(LocaleResources.CANNOT_SHUTDOWN_SERVER,
                    configuration.getDBPath().toString(),
                    String.valueOf(status));
            display(message);
            throw new StorageStopException(configuration.getDBPath(), status, message);
        }
    }
    
    private boolean checkExistingProcess() {
        String pid = getPid();
        if (pid == null)
            return false;
        
        String processName = UnixProcessUtilities.getInstance().getProcessName(getPid());
        // TODO: check if we want mongos or mongod from the configs
        return processName != null && processName.equalsIgnoreCase(MONGO_PROCESS);
    }
    
    public void startService() throws IOException, InterruptedException, ApplicationException {

        String pid = getPid();
        if (pid != null) {
            String message = null;
            ApplicationException ex = null;
            if (!checkExistingProcess()) {
                message = Translate.localize(LocaleResources.STALE_PID_FILE_NO_MATCHING_PROCESS, configuration.getPidFile().toString(), MONGO_PROCESS);
                ex = new StalePidFileException(configuration.getPidFile());
            } else {
                message = Translate.localize(LocaleResources.STORAGE_ALREADY_RUNNING_WITH_PID, String.valueOf(pid));
                ex = new StorageAlreadyRunningException(Integer.valueOf(pid), message);
            }
            
            display(message);
            throw ex;
        }
        
        List<String> commands = new ArrayList<>(Arrays.asList(MONGO_BASIC_ARGS));
        String dbVersion = getDBVersion();
        if (dbVersion.compareTo(NO_JOURNAL_FIRST_VERSION) >= 0) {
            commands.add(1, NO_JOURNAL_ARGUMENT);
        }

        // check that the db directory exist
        display(Translate.localize(LocaleResources.STARTING_STORAGE_SERVER));

        commands.add(configuration.getBindIP());

        commands.add("--dbpath");
        commands.add(configuration.getDBPath().getCanonicalPath());

        commands.add("--logpath");
        commands.add(configuration.getLogFile().getCanonicalPath());

        commands.add("--pidfilepath");
        commands.add(configuration.getPidFile().getCanonicalPath());

        commands.add("--port");
        commands.add(Long.toString(configuration.getPort()));
        
        LoggedExternalProcess process = new LoggedExternalProcess(commands);
        int status = -1;
        try {
            status = process.runAndReturnResult();
        } catch (ApplicationException ae) {
            String message = Translate.localize(LocaleResources.CANNOT_EXECUTE_PROCESS, MONGO_PROCESS);
            display(message);
            throw ae;
        }

        Thread.sleep(500);

        if (status == 0) {
            pid = getPid();
            if (pid == null) status = -1;
        }
        
        if (status == 0) {
            display(Translate.localize(LocaleResources.SERVER_LISTENING_ON, configuration.getDBConnectionString()));
            display(Translate.localize(LocaleResources.LOG_FILE_AT, configuration.getLogFile().toString()));
            display(Translate.localize(LocaleResources.PID_IS,  String.valueOf(pid)));
            
        } else {
            
            String message = Translate.localize(LocaleResources.CANNOT_START_SERVER,
                             configuration.getDBPath().toString(),
                             String.valueOf(status));
            display(message);
            throw new StorageStartException(configuration.getDBPath(), status, message);
        }
    }
 
    private String getDBVersion() throws IOException {
        Process process;
        try {
            process = new ProcessBuilder(Arrays.asList("mongod", "--version"))
                    .start();
        } catch (IOException e) {
            String message = Translate.localize(
                    LocaleResources.CANNOT_EXECUTE_PROCESS, MONGO_PROCESS);
            display(message);
            throw e;
        }
        InputStream out = process.getInputStream();
        InputStreamReader reader = new InputStreamReader(out);
        BufferedReader bufReader = new BufferedReader(reader);
        String firstLine = bufReader.readLine();
        int commaIdx = firstLine.indexOf(",", 12);
        String versionString = firstLine.substring(12, commaIdx);
        return versionString;
    }

    private void display(String message) {
        if (!isQuiet) {
            System.out.println(message);
        }
    }
}
