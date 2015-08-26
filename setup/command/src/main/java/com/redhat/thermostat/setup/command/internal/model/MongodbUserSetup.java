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

package com.redhat.thermostat.setup.command.internal.model;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.lang.ProcessBuilder.Redirect;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import java.util.Scanner;

import com.redhat.thermostat.common.ActionEvent;
import com.redhat.thermostat.common.ActionListener;
import com.redhat.thermostat.common.cli.AbstractStateNotifyingCommand;
import com.redhat.thermostat.common.cli.Console;
import com.redhat.thermostat.common.tools.ApplicationState;
import com.redhat.thermostat.launcher.Launcher;
import com.redhat.thermostat.setup.command.locale.LocaleResources;
import com.redhat.thermostat.shared.config.CommonPaths;
import com.redhat.thermostat.shared.locale.Translate;

class MongodbUserSetup implements UserSetup {

    static final String[] STORAGE_START_ARGS = {"storage", "--start", "--permitLocalhostException"};
    static final String[] STORAGE_STOP_ARGS = {"storage", "--stop"};
    private static final String WEB_AUTH_FILE = "web.auth";
    private static boolean storageFailed = false;
    private final UserCredsValidator validator;
    private final Launcher launcher;
    private final CredentialFinder finder;
    private final CredentialsFileCreator fileCreator;
    private final List<ActionListener<ApplicationState>> listeners;
    private final CommonPaths paths;
    private final StampFiles stampFiles;
    private final StructureInformation structureInfo;
    private String username;
    private char[] password;
    private String userComment;
    
    MongodbUserSetup(UserCredsValidator validator, Launcher launcher, CredentialFinder finder, CredentialsFileCreator fileCreator, Console console, CommonPaths paths, StampFiles stampFiles, StructureInformation structureInfo) {
        this.validator = validator;
        this.launcher = launcher;
        this.finder = finder;
        this.fileCreator = fileCreator;
        this.stampFiles = stampFiles;
        this.listeners = new ArrayList<>();
        this.listeners.add(new StorageListener(console));
        this.paths = paths;
        this.structureInfo = structureInfo;
    }
    
    @Override
    public void createUser(String username, char[] password, String comment) {
        validator.validateUsername(username);
        validator.validatePassword(password);
        this.username = username;
        this.password = password;
        this.userComment = comment;
    }

    @Override
    public void commit() throws IOException {
        try {
            addMongodbUser();
        } catch (MongodbUserSetupException e) {
            throw new IOException(e);
        }
    }
    
    private void addMongodbUser() throws MongodbUserSetupException {
        try {
            unlockThermostat();

            startStorage();

            int mongoRetVal = runMongo();
            if (mongoRetVal != 0) {
                stampFiles.deleteSetupCompleteStamp();
                stampFiles.deleteMongodbUserStamp();
                throw new MongodbUserSetupException("Mongodb user setup failed");
            }

            if (isWebAppInstalled()) {
                writeStorageCredentialsFile(username, password, userComment);
            }

            stampFiles.createMongodbUserStamp();

            if (!isWebAppInstalled()) {
                String completeDate = ThermostatSetup.DATE_FORMAT.format(new Date());
                String regularContent = "Created by '" + ThermostatSetup.PROGRAM_NAME + "' on " + completeDate;
                stampFiles.createSetupCompleteStamp(regularContent);
            }
        } catch (IOException | InterruptedException e) {
            stampFiles.deleteSetupCompleteStamp();
            stampFiles.deleteMongodbUserStamp();
            throw new MongodbUserSetupException("Error creating Mongodb user", e);
        } finally {
            if (!storageFailed) {
                stopStorage();
            }
            Arrays.fill(password, '\0'); // clear the password
        }
    }
    
    //package-private for testing
    void unlockThermostat() throws IOException {
        Date date = new Date();
        String timestamp = ThermostatSetup.DATE_FORMAT.format(date);
        String setupTmpUnlockContent = "Temporarily unlocked thermostat via '" + ThermostatSetup.PROGRAM_NAME + "' on " + timestamp + "\n";
        stampFiles.createSetupCompleteStamp(setupTmpUnlockContent);
    }
    
    //package-private for testing
    int runMongo() throws IOException, InterruptedException {
        ProcessBuilder mongoProcessBuilder = new ProcessBuilder("mongo", "127.0.0.1:27518/thermostat");
        mongoProcessBuilder.redirectInput(Redirect.PIPE);
        Process process = mongoProcessBuilder.start();
        File libPath = new File(paths.getSystemThermostatHome(), "lib");
        File createUserTemplate = new File(libPath, "create-user.js");
        // Write to the forked processes stdIn replacing username/password
        // on the fly.
        try (OutputStream pOut = process.getOutputStream();
             PrintWriter outWriter = new PrintWriter(pOut);
             FileInputStream fin = new FileInputStream(createUserTemplate);
             Scanner inScanner = new Scanner(fin)) {
            while (inScanner.hasNextLine()) {
                String line = inScanner.nextLine();
                line = line.replaceAll("\\$USERNAME", username);
                line = line.replaceAll("\\$PASSWORD", String.valueOf(password));
                line = line + "\n";
                outWriter.write(line);
            }
        }
        return process.waitFor();
    }
    
    private void startStorage() throws MongodbUserSetupException {
        launcher.run(STORAGE_START_ARGS, listeners, false);

        if (storageFailed) {
            throw new MongodbUserSetupException("Thermostat storage failed to start");
        }
    }

    private void stopStorage() throws MongodbUserSetupException {
        launcher.run(STORAGE_STOP_ARGS, listeners, false);

        if (storageFailed) {
            throw new MongodbUserSetupException("Thermostat storage failed to stop");
        }
    }
    
    private boolean isWebAppInstalled() {
        return structureInfo.isWebAppInstalled();
    }
    
    private void writeStorageCredentialsFile(String username, char[] password, String comment) throws MongodbUserSetupException {
        try {
            Properties credentialProps = new Properties();
            credentialProps.setProperty("username", username);
            credentialProps.setProperty("password", String.valueOf(password));
            File credentialsFile = finder.getConfiguration(WEB_AUTH_FILE);
            fileCreator.create(credentialsFile);
            credentialProps.store(new FileOutputStream(credentialsFile), comment);
        } catch (IOException e) {
            throw new MongodbUserSetupException("Storing credentials to file " + WEB_AUTH_FILE + " failed!", e);
        }
    }
    
    private static class StorageListener implements ActionListener<ApplicationState> {

        private static final Translate<LocaleResources> translator = LocaleResources.createLocalizer();
        private final Console console;

        private StorageListener(Console console) {
            this.console = console;
        }

        @Override
        public void actionPerformed(ActionEvent<ApplicationState> actionEvent) {
            if (actionEvent.getSource() instanceof AbstractStateNotifyingCommand) {
                AbstractStateNotifyingCommand storage = (AbstractStateNotifyingCommand) actionEvent.getSource();
                // Implementation detail: there is a single Storage instance registered
                // as an OSGi service. We remove ourselves as listener so that we don't get
                // notified in the case that the command is invoked by some other means later.
                storage.getNotifier().removeActionListener(this);

                switch (actionEvent.getActionId()) {
                    case START:
                        storageFailed = false;
                        break;
                    case FAIL:
                        console.getOutput().println(translator.localize(LocaleResources.STORAGE_FAILED).getContents());
                        storageFailed = true;
                        break;
                }
            }
        }
    }

}
