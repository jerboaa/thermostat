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

package com.redhat.thermostat.itest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.redhat.thermostat.common.utils.StreamUtils;

import expectj.Executor;
import expectj.ExpectJ;
import expectj.Spawn;
import expectj.TimeoutException;

/**
 * Helper methods to support writing an integration test.
 * <p>
 * This class should be used by all integration tests to start
 * thermostat and to obtain paths to various locations. Starting
 * thermostat manually will cause issues with wrong paths being
 * used.
 */
public class IntegrationTest {
    
    public static final String ITEST_USER_HOME_PROP = "com.redhat.thermostat.itest.thermostatUserHome";
    public static final String ITEST_THERMOSTAT_HOME_PROP = "com.redhat.thermostat.itest.thermostatHome";

    public static class SpawnResult {
        final Process process;
        final Spawn spawn;

        public SpawnResult(Process process, Spawn spawn) {
            this.process = process;
            this.spawn = spawn;
        }
    }

    // FIXME Make sure all methods are using a sane environment that's set up correctly

    public static final long TIMEOUT_IN_SECONDS = 30;

    public static final String SHELL_PROMPT = "Thermostat >";

    private static final String THERMOSTAT_HOME = "THERMOSTAT_HOME";
    private static final String USER_THERMOSTAT_HOME = "USER_THERMOSTAT_HOME";

    /* This is a mirror of paths from c.r.t.shared.Configuration */

    private static String getThermostatExecutable() {
        return getThermostatHome() + "/bin/thermostat";
    }
    
    public static String getThermostatHome() {
        String propHome = System.getProperty(ITEST_THERMOSTAT_HOME_PROP);
        if (propHome == null) {
            return "../../distribution/target/image";
        } else {
            return propHome;
        }
    }

    public static String getSystemPluginHome() {
        return getThermostatHome() + "/plugins";
    }

    public static String getConfigurationDir() {
        return getThermostatHome() + "/etc";
    }

    public static String getUserThermostatHome() {
        String userHomeProp = System.getProperty(ITEST_USER_HOME_PROP);
        if (userHomeProp == null) {
            return "../../distribution/target/user-home";
        } else {
            return userHomeProp;
        }
    }

    public static String getStorageDataDirectory() {
        return getUserThermostatHome() + "/data/db";
    }

    public static void clearStorageDataDirectory() throws IOException {
        File storageDir = new File(getStorageDataDirectory());
        if (storageDir.exists()) {
            if (storageDir.isDirectory()) {
                deleteFilesRecursivelyUnder(storageDir);
            } else {
                throw new IllegalStateException(storageDir + " exists but is not a directory");
            }
        }
    }

    public static Process runThermostat(String... args) throws IOException {
        List<String> completeArgs = new ArrayList<String>(args.length+1);
        completeArgs.add(getThermostatExecutable());
        completeArgs.addAll(Arrays.asList(args));
        ProcessBuilder builder = buildThermostatProcess(completeArgs);

        return builder.start();
    }

    public static Spawn spawnThermostat(String... args) throws IOException {
        return spawnThermostat(false, args);
    }
    
    public static Spawn startStorage() throws Exception {
        clearStorageDataDirectory();

        Spawn storage = spawnThermostat("storage", "--start");
        try {
            storage.expect("pid:");
        } catch (IOException e) {
            // this may happen if storage is already running.
            e.printStackTrace();
            String stdOutContents = storage.getCurrentStandardOutContents();
            
            System.err.flush();
            System.out.flush();
            System.err.println("stdout was: -->" + stdOutContents +"<--");
            System.err.println("stderr was: -->" + storage.getCurrentStandardErrContents() + "<--");
            System.err.flush();
            assertFalse(stdOutContents.contains("Storage is already running with pid"));
            throw new Exception("Something funny is going on when trying to start storage!", e);
        }
        storage.expectClose();

        assertNoExceptions(storage.getCurrentStandardOutContents(), storage.getCurrentStandardErrContents());
        return storage;
    }
    
    public static Spawn stopStorage() throws Exception {
        Spawn storage = spawnThermostat("storage", "--stop");
        storage.expect("server shutdown complete");
        storage.expectClose();
        assertNoExceptions(storage.getCurrentStandardOutContents(), storage.getCurrentStandardErrContents());
        return storage;
    }

    public static Spawn spawnThermostat(boolean localeDependent, String... args) throws IOException {
        ExpectJ expect = new ExpectJ(TIMEOUT_IN_SECONDS);
        StringBuilder result = new StringBuilder(getThermostatExecutable());
        if (args != null) {
            for (String arg : args) {
                result.append(" ").append(arg);
            }
        }
        String toExecute = result.toString();
        Executor exec = null;
        if (localeDependent) {
            exec = new LocaleExecutor(toExecute);
        } else {
            exec = new SimpleExecutor(toExecute);
        }
        return expect.spawn(exec);
    }

    public static SpawnResult spawnThermostatAndGetProcess(String... args) throws IOException {
        final List<String> completeArgs = new ArrayList<String>(args.length+1);
        completeArgs.add(getThermostatExecutable());
        completeArgs.addAll(Arrays.asList(args));

        final Process[] process = new Process[1];

        ExpectJ expect = new ExpectJ(TIMEOUT_IN_SECONDS);

        Spawn spawn = expect.spawn(new Executor() {
            @Override
            public Process execute() throws IOException {
                ProcessBuilder builder = buildThermostatProcess(completeArgs);
                Process service = builder.start();
                process[0] = service;
                return service;
            }
        });

        return new SpawnResult(process[0], spawn);
    }

    private static ProcessBuilder buildThermostatProcess(List<String> args) {
        ProcessBuilder builder = new ProcessBuilder(args);
        builder.environment().put(THERMOSTAT_HOME, getThermostatHome());
        builder.environment().put(USER_THERMOSTAT_HOME, getUserThermostatHome());

        return builder;
    }

    /**
     * Generic method to run a program.
     * <p>
     * DO NOT USE THIS TO RUN THERMOSTAT ITSELF. It does not set up the
     * environment correctly, using incorrect data and possibly overwriting
     * important data.
     */
    public static Spawn spawn(List<String> args) throws IOException {
        ExpectJ expect = new ExpectJ(TIMEOUT_IN_SECONDS);
        StringBuilder result = new StringBuilder();
        for (String arg : args) {
            result.append(arg).append(" ");
        }
        return expect.spawn(result.substring(0, result.length() - 1));
    }

    /**
     * Kill the process and all its children, recursively. Sends SIGTERM.
     */
    public static void killRecursively(Process process) throws Exception {
        killRecursively(getPid(process));
    }

    private static void killRecursively(int pid) throws Exception {
        List<Integer> childPids = findChildPids(pid);
        for (Integer childPid : childPids) {
            killRecursively(childPid);
        }
        killProcess(pid);
    }

    private static void killProcess(int processId) throws Exception {
        System.err.println("Killing process with pid: " + processId);
        Runtime.getRuntime().exec("kill " + processId).waitFor();
    }

    private static List<Integer> findChildPids(int processId) throws IOException {
        String children = new String(StreamUtils.readAll(Runtime.getRuntime().exec("ps --ppid " + processId + " -o pid=").getInputStream()));
        String[] childPids = children.split("\n");
        List<Integer> result = new ArrayList<>();
        for (String childPid : childPids) {
            String pidString = childPid.trim();
            if (pidString.length() == 0) {
                continue;
            }
            try {
                result.add(Integer.parseInt(pidString));
            } catch (NumberFormatException nfe) {
                System.err.println(nfe);
            }
        }
        return result;
    }

    private static int getPid(Process process) throws Exception {
        final String UNIX_PROCESS_CLASS = "java.lang.UNIXProcess";
        if (!process.getClass().getName().equals(UNIX_PROCESS_CLASS)) {
            throw new IllegalArgumentException("can only kill " + UNIX_PROCESS_CLASS + "; input is a " + process.getClass());
        }

        Class<?> processClass = process.getClass();
        Field pidField = processClass.getDeclaredField("pid");
        pidField.setAccessible(true);
        return (int) pidField.get(process);
    }

    private static void deleteFilesRecursivelyUnder(File path) throws IOException {
        if (!path.isDirectory()) {
            throw new IOException("Cannot delete files under a non-directory: " + path);
        }
        File[] filesToDelete = path.listFiles();
        if (filesToDelete == null) {
            throw new IOException("Error getting directory listing: " + path);
        }
        for (File theFile : filesToDelete) {
            if (theFile.isDirectory()) {
                deleteFilesRecursivelyUnder(theFile);
            }
            Files.deleteIfExists(theFile.toPath());
        }
    }

    /** Confirm that there are no 'command not found'-like messages in the spawn's stdout/stderr */
    public static void assertCommandIsFound(Spawn spawn) {
        assertCommandIsFound(spawn.getCurrentStandardOutContents(), spawn.getCurrentStandardErrContents());
    }

    public static void assertCommandIsFound(String stdOutContents, String stdErrContents) {
        assertFalse(stdOutContents.contains("unknown command"));
        assertFalse(stdErrContents.contains("unknown command"));
    }

    /** Confirm that there are no exception stack traces in the spawn's stdout/stderr */
    public static void assertNoExceptions(Spawn spawn) {
        assertNoExceptions(spawn.getCurrentStandardOutContents(), spawn.getCurrentStandardErrContents());
    }

    public static void assertNoExceptions(String stdOutContents, String stdErrContents) {
        assertFalse(stdOutContents.contains("Exception"));
        assertFalse(stdErrContents.contains("Exception"));
    }

    public static void assertOutputEndsWith(String stdOutContents, String expectedOutput) {
        String endOfOut = stdOutContents.substring(stdOutContents.length() - expectedOutput.length());
        assertEquals(expectedOutput, endOfOut);
    }

    public static void handleAuthPrompt(Spawn spawn, String url, String user, String password) throws IOException, TimeoutException {
        spawn.expect("Please enter username for storage at " + url + ":");
        spawn.send(user + "\r");
        spawn.expect("Please enter password for storage at " + url + ":");
        spawn.send(password + "\r");
    }

    private static class LocaleExecutor extends EnvironmentExecutor {

        public static final String[] ENV_WITH_LANG_C = {
                THERMOSTAT_HOME + "=" + getThermostatHome(),
                USER_THERMOSTAT_HOME + "=" + getUserThermostatHome(),
                "LANG=C"
        };

        public LocaleExecutor(String process) {
            super(process, ENV_WITH_LANG_C);
        }

    }

    private static class SimpleExecutor extends EnvironmentExecutor {

        public static final String[] ENV_WITH = {
                THERMOSTAT_HOME + "=" + getThermostatHome(),
                USER_THERMOSTAT_HOME + "=" + getUserThermostatHome(),
        };

        public SimpleExecutor(String process) {
            super(process, ENV_WITH);
        }
    }

    private static class EnvironmentExecutor implements Executor {

        private final String[] env;
        private final String process;

        public EnvironmentExecutor(String process, String[] env) {
            this.process = process;
            this.env = env;
        }

        @Override
        public Process execute() throws IOException {
            return Runtime.getRuntime().exec(process, env);
        }

        @Override
        public String toString() {
            return process;
        }
    }
}

