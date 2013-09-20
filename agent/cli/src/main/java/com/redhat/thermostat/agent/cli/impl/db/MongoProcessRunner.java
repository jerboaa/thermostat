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

package com.redhat.thermostat.agent.cli.impl.db;

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
import com.redhat.thermostat.common.tools.ApplicationException;
import com.redhat.thermostat.common.utils.LoggedExternalProcess;
import com.redhat.thermostat.common.utils.LoggingUtils;
import com.redhat.thermostat.service.process.UnixProcessUtilities;
import com.redhat.thermostat.shared.config.InvalidConfigurationException;
import com.redhat.thermostat.shared.locale.LocalizedString;
import com.redhat.thermostat.shared.locale.Translate;

public class MongoProcessRunner {
    
    private static final Translate<LocaleResources> translator = LocaleResources.createLocalizer();
    private static final Logger logger = LoggingUtils.getLogger(MongoProcessRunner.class);

    private static final String MONGO_PROCESS = "mongod";

    private static final String [] MONGO_BASIC_ARGS = {
        "mongod", "--quiet", "--fork", "--auth", "--nohttpinterface", "--bind_ip"
    };

    private static final String [] MONGO_SHUTDOWN_ARGS = {
        "kill", "-s", "TERM"
    };

    private static final String NO_JOURNAL_ARGUMENT = "--nojournal";
    static final String NO_JOURNAL_FIRST_VERSION = "1.9.2";

    private DBStartupConfiguration configuration;
    private boolean isQuiet;
    private Integer pid;
    
    public MongoProcessRunner(DBStartupConfiguration configuration, boolean quiet) {
        this.configuration = configuration;
        this.isQuiet = quiet;
    }

    private boolean checkPid() {
        File pidfile = configuration.getPidFile();
        Charset charset = Charset.defaultCharset();
        if (pidfile.exists()) {
            try (BufferedReader reader = Files.newBufferedReader(pidfile.toPath(), charset)) {
                pid = doGetPid(reader);
            } catch (IOException ex) {
                logger.log(Level.WARNING, "Exception while reading pid file", ex);
                pid = null;
            } catch (NumberFormatException e) {
                logger.log(Level.WARNING, "Mongo PID file does not contain a valid PID", e);
                pid = null;
            }
        } else {
            pid = null;
        }
        return (pid != null);
    }
    
    // package private for testing
    Integer doGetPid(BufferedReader reader) throws IOException {
        String line = reader.readLine();
        // readLine() returns null on EOF
        if (line == null || line.isEmpty()) {
            return null;
        }
        else {
            return Integer.parseInt(line);
        }
    }

    private void deleteStalePidFile() {
        pid = null;
        LocalizedString message = translator.localize(LocaleResources.STALE_PID_FILE_NO_MATCHING_PROCESS, configuration.getPidFile().toString(), MONGO_PROCESS);
        // Mongo didn't remove its PID file? Work around the issue. Log
        // the event, remove the stale pid file and continue.
        logger.log(Level.WARNING, message.getContents());
        try {
            Files.delete(configuration.getPidFile().toPath());
        } catch (IOException benign) {
            // ignore this benign error
        }
    }
    
    public boolean isStorageRunning() {
        if (!checkPid()) {
            return false;
        }
        
        String processName = UnixProcessUtilities.getInstance().getProcessName(pid);
        // TODO: check if we want mongos or mongod from the configs
        boolean processIsRunning = processName != null && processName.equalsIgnoreCase(MONGO_PROCESS);
        if (!processIsRunning) {
            deleteStalePidFile();
        }
        return processIsRunning;
    }
    
    /**
     * Start the mongod process.
     *
     * @throws ApplicationException to signal an error starting the process. Callers should catch this and handle appropriately.
     */
    public void startService() throws IOException, InterruptedException,
            ApplicationException, InvalidConfigurationException {

        if (isStorageRunning()) {
            LocalizedString message = translator.localize(LocaleResources.STORAGE_ALREADY_RUNNING_WITH_PID, String.valueOf(pid));
            throw new StorageAlreadyRunningException(pid, message.getContents());
        }
        
        String dbVersion;
        try {
            dbVersion = getDBVersion();
        } catch (IOException e) {
            LocalizedString message = translator.localize(
                    LocaleResources.CANNOT_EXECUTE_PROCESS, MONGO_PROCESS);
            throw new ApplicationException(message.getContents(), e);

        }
        List<String> commands = null;
        commands = getStartupCommand(dbVersion);
        
        display(translator.localize(LocaleResources.STARTING_STORAGE_SERVER));
        
        LoggedExternalProcess process = new LoggedExternalProcess(commands);
        int status = -1;
        try {
            status = process.runAndReturnResult();
        } catch (ApplicationException ae) {
            LocalizedString message = translator.localize(LocaleResources.CANNOT_EXECUTE_PROCESS, MONGO_PROCESS);
            throw ae;
        }

        Thread.sleep(500);

        if (status == 0) {
            if (!isStorageRunning()) {
                status = -1;
            }
        }

        if (status == 0) {
            display(translator.localize(LocaleResources.SERVER_LISTENING_ON, configuration.getDBConnectionString()));
            display(translator.localize(LocaleResources.LOG_FILE_AT, configuration.getLogFile().toString()));
            display(translator.localize(LocaleResources.PID_IS, String.valueOf(pid)));
            
        } else {
            // don't display anything when throwing an exception; whatever catches the exception will do so.
            LocalizedString message = translator.localize(LocaleResources.CANNOT_START_SERVER,
                             configuration.getDBPath().toString(),
                             String.valueOf(status));
            throw new StorageStartException(configuration.getDBPath(), status, message.getContents());
        }
    }
    
    /**
     * Stop the mongod process.
     *
     * @throws ApplicationException to signal an error stopping the storage. Callers should catch this and handle appropriately.
     */
    public void stopService() throws IOException, InterruptedException, InvalidConfigurationException, ApplicationException {
 
        if (!isStorageRunning()) {
            LocalizedString message = translator.localize(LocaleResources.STORAGE_NOT_RUNNING);
            throw new StorageNotRunningException(message.getContents());
        }
        List<String> commands = new ArrayList<>(Arrays.asList(MONGO_SHUTDOWN_ARGS));
        commands.add(String.valueOf(pid));

        LoggedExternalProcess process = new LoggedExternalProcess(commands);
        int status = process.runAndReturnResult();
        if (status == 0) {
            display(translator.localize(LocaleResources.SERVER_SHUTDOWN_COMPLETE, configuration.getDBPath().toString()));
            display(translator.localize(LocaleResources.LOG_FILE_AT, configuration.getLogFile().toString()));
            // all went well, make sure to remove pid file.
            try {
                Files.delete(configuration.getPidFile().toPath());
            } catch (IOException e) {
                // ignore
            }
        } else {
            // don't display anything when throwing an exception; whatever catches the exception will do so.
            LocalizedString message = translator.localize(LocaleResources.CANNOT_SHUTDOWN_SERVER,
                    configuration.getDBPath().toString(),
                    String.valueOf(status));
            throw new StorageStopException(configuration.getDBPath(), status, message.getContents());
        }
    }
    
    List<String> getStartupCommand(String dbVersion) throws IOException, InvalidConfigurationException {
        List<String> commands = new ArrayList<>(Arrays.asList(MONGO_BASIC_ARGS));
        
        if (dbVersion.compareTo(NO_JOURNAL_FIRST_VERSION) >= 0) {
            commands.add(1, NO_JOURNAL_ARGUMENT);
        }
        commands.add(configuration.getBindIP());

        commands.add("--dbpath");
        commands.add(configuration.getDBPath().getCanonicalPath());

        commands.add("--logpath");
        commands.add(configuration.getLogFile().getCanonicalPath());

        commands.add("--pidfilepath");
        commands.add(configuration.getPidFile().getCanonicalPath());

        commands.add("--port");
        commands.add(Long.toString(configuration.getPort()));
        
        if (configuration.isSslEnabled()) {
            // check for configuration which has a chance of working :)
            if (configuration.getSslPemFile() == null) {
                throw new InvalidConfigurationException(translator.localize(LocaleResources.MISSING_PEM));
            } else if (configuration.getSslKeyPassphrase() == null) {
                throw new InvalidConfigurationException(translator.localize(LocaleResources.MISSING_PASSPHRASE));
            }
            commands.add("--sslOnNormalPorts");
            commands.add("--sslPEMKeyFile");
            commands.add(configuration.getSslPemFile().getCanonicalPath());
            commands.add("--sslPEMKeyPassword");
            commands.add(configuration.getSslKeyPassphrase());
        }
        
        return commands;
    }
 
    private String getDBVersion() throws IOException {
        Process process = new ProcessBuilder(Arrays.asList("mongod", "--version")).start();
        InputStream out = process.getInputStream();
        return doGetDBVersion(out);
    }
    
    // package private for testing
    String doGetDBVersion(InputStream in) throws IOException {
        // Default to no-journal first version if we can't parse the version
        // output for some reason.
        String versionString = NO_JOURNAL_FIRST_VERSION;
        String firstLine = null;
        try(InputStreamReader reader = new InputStreamReader(in)) {
            BufferedReader bufReader = new BufferedReader(reader);
            firstLine = bufReader.readLine();
            int commaIdx = firstLine.indexOf(",", 12);
            if (commaIdx != -1) {
                versionString = firstLine.substring(12, commaIdx);
            } else {
                versionString = firstLine.substring(12);
            }
        } catch (Exception e) {
            // catching Exception here in order to also catch potential NPEs or
            // IndexOutOfBoundExceptions. If those conditions happen we fall
            // back to the no journal first version.
            logger.log(Level.WARNING, "Failed to parse mongodb version from: '" +
                firstLine + "'. Assuming version " + NO_JOURNAL_FIRST_VERSION, e);
        }
        return versionString;
    }

    private void display(LocalizedString message) {
        if (!isQuiet) {
            System.out.println(message.getContents());
        }
    }
}

