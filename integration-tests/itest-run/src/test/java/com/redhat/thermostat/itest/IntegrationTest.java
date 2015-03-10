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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import com.redhat.thermostat.common.utils.StreamUtils;

import expectj.Executor;
import expectj.ExpectJ;
import expectj.Spawn;

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
    
    private static final String AGENT_VERBOSE_MODE_PROP = "thermostat.agent.verbose";
    private static final String THERMOSTAT_HOME = "THERMOSTAT_HOME";
    private static final String USER_THERMOSTAT_HOME = "USER_THERMOSTAT_HOME";
    
    public static final Map<String, String> DEFAULT_ENVIRONMENT;
    public static final Map<String, String> DEFAULT_ENV_WITH_LANG_C;
    
    /**
     * Configure the log level to FINEST, and configure a file handler so as for
     * log messages to go to USER_THERMOSTAT_HOME/integration-tests.log rather
     * than stdout. This is to ensure integration tests pass without dependency
     * on log levels. See:
     *   http://icedtea.classpath.org/bugzilla/show_bug.cgi?id=1594
     */
    static {
        createUserThermostatHomeAndEtc();
        File loggingProperties = new File(getUserThermostatHome() + File.separator + "etc" + File.separator + "logging.properties");
        File logFile = new File(getUserThermostatHome() + File.separator + "integration-tests.log");
        LogConfigurator configurator = new LogConfigurator(Level.FINEST, loggingProperties, logFile);
        configurator.writeConfiguration();
        
        // Set up environment maps.
        DEFAULT_ENVIRONMENT = new HashMap<>();
        DEFAULT_ENVIRONMENT.put(THERMOSTAT_HOME, getThermostatHome());
        DEFAULT_ENVIRONMENT.put(USER_THERMOSTAT_HOME, getUserThermostatHome());
        DEFAULT_ENV_WITH_LANG_C = new HashMap<>(DEFAULT_ENVIRONMENT);
        DEFAULT_ENV_WITH_LANG_C.put("LANG", "C");
    }
    
    public static class SpawnResult {
        final Process process;
        final Spawn spawn;

        public SpawnResult(Process process, Spawn spawn) {
            this.process = process;
            this.spawn = spawn;
        }
    }

    public static final long TIMEOUT_IN_SECONDS = 30;

    public static final String SHELL_DISCONNECT_PROMPT = "Thermostat - >";
    public static final String SHELL_CONNECT_PROMPT = "Thermostat + >";

    private static final String THERMOSTAT_SCRIPT = "thermostat";
    
    private static void createUserThermostatHomeAndEtc() {
        File userThHome = new File(getUserThermostatHome());
        userThHome.mkdir();
        File etcThHome = new File(userThHome, "etc");
        etcThHome.mkdir();
    }
    
    /**
     * Utility method for creating the setup file - and its parent directories
     * which makes basic thermostat commands to be able to run (instead of
     * getting the launcher warning).
     * 
     * Be sure to call this in @Before/@BeforeClass methods of your tests as
     * appropriate. There is no good way for this base class to know when it
     * should get called.
     */
    protected static void createFakeSetupCompleteFile() {
        String userHome = getUserThermostatHome();
        File fUserHome = new File(userHome);
        fUserHome.mkdir();
        File dataDir = new File(fUserHome, "data");
        dataDir.mkdir();
        File setupFile = new File(dataDir, "setup-complete.stamp");
        try {
            // creates file only if not yet existing
            setupFile.createNewFile();
        } catch (IOException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }
    

    /**
     * Utility method for removing stamp files which may get created by certain
     * integration test runs. For example a test which runs the "service"
     * command now depends on a proper mongodb user to be set up. Setting it up
     * may create the mongodb-user-done.stamp file. Similar with running
     * the thermostat-setup script and setup-complete.stamp.
     * 
     * Be sure to call this in @After/@AfterClass as appropriate. There is no
     * simple way for the base class to know when to erase those files.
     * 
     * @throws IOException
     */
    protected static void removeSetupCompleteStampFiles() throws IOException {
        String mongodbUserDoneFile = getUserThermostatHome() + "/data/mongodb-user-done.stamp";
        String setupStampFile = getUserThermostatHome() + "/data/setup-complete.stamp";
        File mongodbFileStamp = new File(mongodbUserDoneFile);
        File setupFileStamp = new File(setupStampFile);
        removeFileIgnoreMissing(mongodbFileStamp);
        removeFileIgnoreMissing(setupFileStamp);
    }
    
    private static void removeFileIgnoreMissing(File file) throws IOException {
        try {
            Files.delete(file.toPath());
        } catch (NoSuchFileException e) {
            // wanted to delete that file, so that should be fine.
        }
    }
    
    protected static Map<String, String> getVerboseModeProperties() {
        Map<String, String> testProperties = new HashMap<>();
        // See AgentApplication.VERBOSE_MODE_PROPERTY
        testProperties.put(AGENT_VERBOSE_MODE_PROP, Boolean.TRUE.toString());
        return testProperties;
    }

    /* This is a mirror of paths from c.r.t.shared.Configuration */

    public static String getThermostatHome() {
        String propHome = System.getProperty(ITEST_THERMOSTAT_HOME_PROP);
        if (propHome == null) {
        	String relPath = "../../distribution/target/image";
        	try {
        	    return new File(relPath).getCanonicalPath();
        	} catch (IOException e) {
        	    throw new RuntimeException(e);
        	}
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
    
    public static String getSystemBinRoot() {
        return getThermostatHome() + "/bin";
    }

    public static String getUserThermostatHome() {
        String userHomeProp = System.getProperty(ITEST_USER_HOME_PROP);
        if (userHomeProp == null) {
        	String relPath = "../../distribution/target/user-home";
        	try {
                return new File(relPath).getCanonicalPath();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
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

    public static Spawn spawnThermostat(String... args) throws IOException {
        return spawnThermostat(false, args);
    }
    
    public static Spawn startStorage() throws Exception {
        clearStorageDataDirectory();

        Spawn storage = spawnThermostat("storage", "--start", "--permitLocalhostException");
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
    
    public static Spawn spawnScript(String script, String... args) throws IOException {
        return runScript(false, script, args);
    }

    public static Spawn spawnThermostat(boolean localeDependent, String... args) throws IOException {
        return runScript(localeDependent, THERMOSTAT_SCRIPT, args);
    }
    
    private static Spawn runScript(boolean localeDependent, String script, String[] args) throws IOException {
        ExpectJ expect = new ExpectJ(TIMEOUT_IN_SECONDS);
        Executor exec = null;
        if (localeDependent) {
            exec = new LocaleExecutor(script, args);
        } else {
            exec = new SimpleExecutor(script, args);
        }
        return expect.spawn(exec);
    }

    public static SpawnResult spawnThermostatAndGetProcess(String... args)
            throws IOException {
        return runComandAndGetProcess(THERMOSTAT_SCRIPT, args);
    }

    public static SpawnResult spawnThermostatWithPropertiesSetAndGetProcess(
            Map<String, String> props, String... args) throws IOException {
        return runCommandAndGetProcess(THERMOSTAT_SCRIPT, args, props);
    }

    private static SpawnResult runComandAndGetProcess(String script,
            String[] args) throws IOException {
        return runCommandAndGetProcess(THERMOSTAT_SCRIPT, args,
                new HashMap<String, String>());
    }

    private static SpawnResult runCommandAndGetProcess(String script, String[] args, Map<String, String> props) throws IOException {
        final Process[] process = new Process[1];

        ExpectJ expect = new ExpectJ(TIMEOUT_IN_SECONDS);

        Spawn spawn = expect.spawn(new PropertiesExecutor(script, args,
                props) {
            @Override
            public Process execute() throws IOException {
                Process p = super.execute();
                process[0] = p;
                return p;
            }
        });

        return new SpawnResult(process[0], spawn);

    }

    protected static boolean isDevelopmentBuild() {
        boolean isDevelBuild = Boolean.getBoolean("devel.build");
        return isDevelBuild;
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

    public static void handleAuthPrompt(Spawn spawn, String url, String user, String password) throws IOException {
        spawn.send(user + "\r");
        spawn.send(password + "\r");
    }
}

