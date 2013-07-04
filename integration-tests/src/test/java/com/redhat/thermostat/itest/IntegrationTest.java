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
import java.util.ArrayList;
import java.util.List;

import com.redhat.thermostat.common.utils.StreamUtils;

import expectj.Executor;
import expectj.ExpectJ;
import expectj.Spawn;
import expectj.TimeoutException;

/**
 * Helper methods to support writing an integration test.
 */
public class IntegrationTest {

    public static final long TIMEOUT_IN_SECONDS = 30;

    public static final String SHELL_PROMPT = "Thermostat >";

    public static String getThermostatExecutable() {
        return "../distribution/target/bin/thermostat";
    }
    
    public static String getThermostatHome() {
        return "../distribution/target";
    }

    public static String getStorageDataDirectory() {
        return "../distribution/target/storage/db";
    }

    public static Spawn spawnThermostat(String... args) throws IOException {
        return spawnThermostat(false, args);
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
        //System.out.println("executing: '" + toExecute + "'");
        if (localeDependent) {
            Executor exec = new LocaleExecutor(toExecute);
            return expect.spawn(exec);
        } else {
            return expect.spawn(toExecute);
        }
    }

    public static Spawn spawn(List<String> args) throws IOException {
        ExpectJ expect = new ExpectJ(TIMEOUT_IN_SECONDS);
        StringBuilder result = new StringBuilder();
        for (String arg : args) {
            result.append(arg).append(" ");
        }
        return expect.spawn(result.substring(0, result.length() - 1));
    }

    public static Spawn spawn(Executor executor) throws IOException {
        ExpectJ expect = new ExpectJ(TIMEOUT_IN_SECONDS);
        return expect.spawn(executor);
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

    public static void deleteFilesUnder(String path) throws IOException {
        String[] filesToDelete = new File(path).list();
        for (String toDelete : filesToDelete) {
            File theFile = new File(path, toDelete);
            if (!theFile.delete()) {
                if (theFile.exists()) {
                    throw new IOException("cant delete: '" + theFile.toString() + "'.");
                }
            }
        }
    }

    public static void assertCommandIsFound(String stdOutContents, String stdErrContents) {
        assertFalse(stdOutContents.contains("unknown command"));
        assertFalse(stdErrContents.contains("unknown command"));
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

    private static class LocaleExecutor implements Executor {

        public static final String[] LANG_C = { "LANG=C " };
        private String process;

        public LocaleExecutor(String process) {
            this.process = process;
        }

        @Override
        public Process execute() throws IOException {
            return Runtime.getRuntime().exec(process, LANG_C);
        }

        @Override
        public String toString() {
            return process;
        }
    }
}

