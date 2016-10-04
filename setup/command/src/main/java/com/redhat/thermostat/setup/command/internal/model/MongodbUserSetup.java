/*
 * Copyright 2012-2016 Red Hat, Inc.
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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.lang.ProcessBuilder.Redirect;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import java.util.Scanner;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import com.redhat.thermostat.common.ActionEvent;
import com.redhat.thermostat.common.ActionListener;
import com.redhat.thermostat.common.cli.AbstractStateNotifyingCommand;
import com.redhat.thermostat.common.tools.ApplicationState;
import com.redhat.thermostat.common.utils.LoggingUtils;
import com.redhat.thermostat.common.utils.StreamUtils;
import com.redhat.thermostat.launcher.Launcher;
import com.redhat.thermostat.service.process.ProcessHandler;
import com.redhat.thermostat.shared.config.CommonPaths;

class MongodbUserSetup implements UserSetup {

    static final String[] STORAGE_START_ARGS = {"storage", "--start", "--permitLocalhostException"};
    static final String[] STORAGE_STOP_ARGS = {"storage", "--stop"};

    private static final boolean IS_UNIX = !System.getProperty("os.name").contains("Windows");
    private static final String WEB_AUTH_FILE = "web.auth";
    private static final String MONGO_PROCESS = "mongod";
    private static final Logger logger = LoggingUtils.getLogger(MongodbUserSetup.class);
    private final UserCredsValidator validator;
    private final ProcessHandler processHandler;
    private final Launcher launcher;
    private final CredentialFinder finder;
    private final CredentialsFileCreator fileCreator;
    private final CommonPaths paths;
    private final StampFiles stampFiles;
    private final StructureInformation structureInfo;
    private final AuthFileWriter authFileWriter;
    private final KeyringWriter keyringWriter;
    private String username;
    private char[] password;
    private String userComment;
    private Integer pid;
    
    MongodbUserSetup(UserCredsValidator validator, Launcher launcher, ProcessHandler processHandler, CredentialFinder finder, CredentialsFileCreator fileCreator, CommonPaths paths, StampFiles stampFiles, StructureInformation structureInfo, AuthFileWriter authWriter, KeyringWriter keyringWriter) {
        this.validator = validator;
        this.processHandler = processHandler;
        this.launcher = launcher;
        this.finder = finder;
        this.fileCreator = fileCreator;
        this.stampFiles = stampFiles;
        this.paths = paths;
        this.structureInfo = structureInfo;
        this.authFileWriter = authWriter;
        this.keyringWriter = keyringWriter;
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
        boolean thermostatUnlocked = false;
        boolean storageStarted = false;
        try {
            thermostatUnlocked = unlockThermostat();
            
            checkStorageNotRunning();

            storageStarted = startStorage();

            // Sometimes the forked mongod process returns early with success
            // although it does not seem to be completely up. once mongo wants
            // to connect to mongod it fails if connected too soon. We used to
            // sleep for 3 seconds in the script. Let's hope 2 seconds is enough.
            // Yes, agreed, this is unfortunate :(
            Thread.sleep(TimeUnit.SECONDS.toMillis(2));
            
            int mongoRetVal = runMongo();
            if (mongoRetVal != 0) {
                throw new MongodbUserSetupException("Mongodb user setup failed");
            }

            if (isWebAppInstalled()) {
                writeStorageCredentialsFile(username, password, userComment);
            }

            stampFiles.createMongodbUserStamp();

            // If we reached here without exception, storage must have
            // started successfully.
            stopStorage();
            if (!isWebAppInstalled()) {
                // Allow for gui/agent to work out of the box by using mongodb
                // URLs.
                setupForDirectMongodbUrls();
                String completeDate = ThermostatSetup.DATE_FORMAT.format(new Date());
                String regularContent = "Created by '" + ThermostatSetup.PROGRAM_NAME + "' on " + completeDate;
                stampFiles.createSetupCompleteStamp(regularContent);
            }
        } catch (IOException | InterruptedException e) {
            throw new MongodbUserSetupException("Error creating Mongodb user", e);
        } catch (MongodbUserSetupException e) {
            // Stop storage (if need be), remove temp stamp files and rethrow
            cleanupAndReThrow(thermostatUnlocked, storageStarted, e);
        } finally {
            Arrays.fill(password, '\0'); // clear the password
        }
    }

    private void checkStorageNotRunning() throws StorageAlreadyRunningException {
        if (isStorageRunning()) {
            throw new StorageAlreadyRunningException();
        }
    }

    boolean isStorageRunning() {
        File pidFile = paths.getUserStoragePidFile();
        if (!checkPid(pidFile)) {
            return false;
        }
        String processName = processHandler.getProcessName(pid);
        // TODO: check if we want mongos or mongod from the configs
        return (processName != null && processName.equalsIgnoreCase(MONGO_PROCESS));
    }

    // package private for testing
    boolean checkPid(File pidFile) {
        Charset charset = Charset.defaultCharset();
        if (pidFile.exists()) {
            try (BufferedReader reader = Files.newBufferedReader(pidFile.toPath(), charset)) {
                pid = doGetPid(reader);
            } catch (IOException | NumberFormatException e) {
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
        } else {
            return Integer.parseInt(line);
        }
    }

    private void setupForDirectMongodbUrls() throws MongodbUserSetupException {
        try {
            authFileWriter.setCredentials(username, Arrays.copyOf(password, password.length));
            authFileWriter.write();
            keyringWriter.setCredentials(username, Arrays.copyOf(password, password.length));
            keyringWriter.setStorageUrl(ThermostatSetup.MONGODB_STORAGE_URL);
            keyringWriter.write();
        } catch (IOException e) {
            throw new MongodbUserSetupException("Error creating agent.auth file or persisting keyring prefs", e);
        }
    }

    private void cleanupAndReThrow(boolean thermostatUnlocked, boolean storageStarted, MongodbUserSetupException e) throws MongodbUserSetupException {
        if (storageStarted) {
            stopStorage();
        }
        if (thermostatUnlocked) {
            stampFiles.deleteSetupCompleteStamp();
            stampFiles.deleteMongodbUserStamp();
        }
        throw e;
    }

    //package-private for testing
    boolean unlockThermostat() throws IOException {
        if (stampFiles.setupCompleteStampExists()) {
            // Stamp file exists so this is a re-run of setup.
            return false;
        }
        Date date = new Date();
        String timestamp = ThermostatSetup.DATE_FORMAT.format(date);
        String setupTmpUnlockContent = "Temporarily unlocked thermostat via '" + ThermostatSetup.PROGRAM_NAME + "' on " + timestamp + "\n";
        stampFiles.createSetupCompleteStamp(setupTmpUnlockContent);
        return true;
    }

    String[] getMongoRunnerCmdline( final String arg ) {
        return IS_UNIX ? new String[] { "mongo", arg} : new String[] { "cmd", "/C", "mongo.exe", arg };
    }

    //package-private for testing
    int runMongo() throws IOException, InterruptedException {
        logger.fine("running 'mongo 127.0.0.1:27518/thermostat' with piped input");
        ProcessBuilder mongoProcessBuilder = new ProcessBuilder(getMongoRunnerCmdline("127.0.0.1:27518/thermostat"));
        mongoProcessBuilder.redirectInput(Redirect.PIPE);
        Process process = mongoProcessBuilder.start();
        ProcOutErrReader reader = new ProcOutErrReader(process);
        File createUserTemplate = new File(paths.getSystemLibRoot(), "create-user.js");
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
        reader.start();
        int exitStatus = process.waitFor();
        reader.join();
        String errOutput = reader.getErrOutput();
        String stdOutput = reader.getOutput();
        logger.fine("mongo stdout >>> " + stdOutput);
        logger.fine("mongo stderr >>> " + errOutput);
        return exitStatus;
    }
    
    private boolean startStorage() throws MongodbUserSetupException {
        StorageListener listener = runStorage(STORAGE_START_ARGS);
        if (listener.isFailure()) {
            throw new MongodbUserSetupException("Thermostat storage failed to start");
        }
        return true;
    }
    
    private void stopStorage() throws MongodbUserSetupException {
        StorageListener listener = runStorage(STORAGE_STOP_ARGS);
        if (listener.isFailure()) {
            throw new MongodbUserSetupException("Thermostat storage failed to stop");
        }
    }
    
    private StorageListener runStorage(String[] storageArgs) throws MongodbUserSetupException {
        final List<ActionListener<ApplicationState>> listeners = new ArrayList<>();
        CountDownLatch latch = new CountDownLatch(1);
        StorageListener listener = new StorageListener(latch);
        listeners.add(listener);
        launcher.run(storageArgs, listeners, false);
        try {
            latch.await();
        } catch (InterruptedException e) {
            throw new MongodbUserSetupException(e);
        }
        return listener;
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

        private final CountDownLatch latch;
        private boolean failed;

        private StorageListener(CountDownLatch latch) {
            this.latch = latch;
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
                        latch.countDown();
                        break;
                    case STOP:
                        latch.countDown();
                        break;
                    case FAIL:
                        failed = true;
                        latch.countDown();
                        break;
                    default:
                        throw new AssertionError("Unexpected action event: " + actionEvent.getActionId());
                }
            }
        }
        
        public boolean isFailure() {
            return failed;
        }
    }
    
    private static class ProcOutErrReader {
        
        private final Thread errReaderThread;
        private final Thread outReaderThread;
        private final ProcStreamReader outReader;
        private final ProcStreamReader errReader;
        
        private ProcOutErrReader(Process process) {
            outReader = new ProcStreamReader(process.getInputStream());
            errReader = new ProcStreamReader(process.getErrorStream());
            Runnable outRunnable = new Runnable() {
                
                @Override
                public void run() {
                    outReader.readAll();
                }
            };
            Runnable errRunnable = new Runnable() {

                @Override
                public void run() {
                    errReader.readAll();
                }
                
            };
            errReaderThread = new Thread(errRunnable);
            outReaderThread = new Thread(outRunnable);
        }
        
        public void start() {
            errReaderThread.start();
            outReaderThread.start();
        }
        
        public void join() {
            try {
                errReaderThread.join();
            } catch (InterruptedException e) {
                // ignore
            }
            try {
                outReaderThread.join();
            } catch (InterruptedException e) {
                // ignore
            }
        }
        
        public String getOutput() {
            return outReader.getOutput();
        }
        
        public String getErrOutput() {
            return errReader.getOutput();
        }
    }
    
    private static class ProcStreamReader {
        
        private final InputStream in;
        private byte[] contents;
        
        private ProcStreamReader(InputStream in) {
            this.in = in;
        }
        
        public void readAll() {
            try {
                contents = StreamUtils.readAll(in);
            } catch (IOException e) {
                // ignore
            }
        }
        
        public String getOutput() {
            return new String(contents);
        }
    }

    public static class StorageAlreadyRunningException extends MongodbUserSetupException {
        public StorageAlreadyRunningException() {
            super("");
        }
    }

}
