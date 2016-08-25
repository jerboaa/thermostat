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

package com.redhat.thermostat.itest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
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
    
    public static Map<String, String> DEFAULT_ENVIRONMENT;
    public static Map<String, String> DEFAULT_ENV_WITH_LANG_C;

    private static String testName;
    private static String thermostatHome;
    private static String userThermostatHome;
    
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

    public static void setupIntegrationTest(Class testClass) {
        setupIntegrationTest(testClass.getName());
    }

    /**
     * Call this with the name of the test e.g. Test.class.getName()
     * to setup the testing environment. It is expected that integration
     * tests call this in an @BeforeClass first before using other
     * methods provided by this class. Otherwise behaviour is
     * undefined
     * @param testName
     */
    public static void setupIntegrationTest(String testName) {
        /**
         * Configure the log level to FINEST, and configure a file handler so as for
         * log messages to go to USER_THERMOSTAT_HOME/integration-tests.log rather
         * than stdout. This is to ensure integration tests pass without dependency
         * on log levels. See:
         *   http://icedtea.classpath.org/bugzilla/show_bug.cgi?id=1594
         */
        IntegrationTest.testName = testName;
        setUserThermostatHome();
        setThermostatHome();

        /**
         * Clean up the directory if it already exists for the new test run
         * Older test runs may not have removed the directory or subdirectories
         */
        File f = new File(userThermostatHome);
        if (f.exists()) {
            try {
                deleteFilesRecursivelyUnder(f);
            } catch (IOException e) {
            }
        }

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
    
    private static File createUserThermostatHomeAndEtc() {
        File userThHome = new File(getUserThermostatHome());
        userThHome.mkdir();
        File etcThHome = new File(userThHome, "etc");
        etcThHome.mkdir();
        return etcThHome;
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

    protected static Map<String, String> getVerboseModeProperties() {
        Map<String, String> testProperties = new HashMap<>();
        // See AgentApplication.VERBOSE_MODE_PROPERTY
        testProperties.put(AGENT_VERBOSE_MODE_PROP, Boolean.TRUE.toString());
        return testProperties;
    }

    static protected void createAgentAuthFile(String userName, String password) throws IOException {
        File etcHome = createUserThermostatHomeAndEtc();

        List<String> lines = new ArrayList<>();
        lines.add("username=" + userName);
        lines.add("password=" + password);
        Files.write(new File(etcHome, "agent.auth").toPath(), lines, StandardCharsets.UTF_8,
                StandardOpenOption.WRITE, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
    }

    private static void setThermostatHome() {
        String propHome = System.getProperty(ITEST_THERMOSTAT_HOME_PROP);
        if (propHome == null) {
            String relPath = "../../distribution/target/image";
            try {
                IntegrationTest.thermostatHome = new File(relPath).getCanonicalPath();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        } else {
            IntegrationTest.thermostatHome = propHome;
        }

        System.setProperty(THERMOSTAT_HOME, IntegrationTest.thermostatHome);
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

    private static void setUserThermostatHome() {
        String userHomeProp = System.getProperty(ITEST_USER_HOME_PROP);
        if (userHomeProp == null) {
            String relPath = "target/user-home-" + testName;
            try {
                IntegrationTest.userThermostatHome = new File(relPath).getCanonicalPath();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        } else {
            IntegrationTest.userThermostatHome = userHomeProp;
        }

        System.setProperty(USER_THERMOSTAT_HOME, IntegrationTest.userThermostatHome);
    }

    public static String getUserThermostatHome() {
        return IntegrationTest.userThermostatHome;
    }

    /** pre-conditions:
     *    storage must *NOT* be running.
     *    No users set up
     */
    public static void addUserToStorage(String username, String password) throws Exception {
        startStorage("--permitLocalhostException");

        TimeUnit.SECONDS.sleep(3);

        try {
            ExpectJ mongo = new ExpectJ(TIMEOUT_IN_SECONDS);
            Spawn mongoSpawn = mongo.spawn("mongo 127.0.0.1:27518/thermostat");
            File createUser = new File(new File(getThermostatHome(), "libs"), "create-user.js");
            String contents = new String(Files.readAllBytes(createUser.toPath()), StandardCharsets.UTF_8);
            contents = contents.replaceAll("\\$USERNAME", username);
            contents = contents.replaceAll("\\$PASSWORD", password);
            mongoSpawn.send(contents);
            mongoSpawn.send("quit();\n");
            mongoSpawn.expectClose();

            TimeUnit.SECONDS.sleep(3);

        } finally {
            stopStorage();
        }
    }

    public static Spawn spawnThermostat(String... args) throws IOException {
        return spawnThermostat(false, args);
    }
    
    public static Spawn startStorage(String... extraArgs) throws Exception {
        List<String> args = new ArrayList<>(Arrays.asList(new String[] { "storage", "--start", }));
        if (extraArgs != null) {
            args.addAll(Arrays.asList(extraArgs));
        }

        Spawn storage = spawnThermostat(args.toArray(new String[0]));
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
        // JDK 9 renamed this class to ProcessImpl
        final String PROCESS_IMPL_CLASS = "java.lang.ProcessImpl";
        if (!(process.getClass().getName().equals(UNIX_PROCESS_CLASS) || process.getClass().getName().equals(PROCESS_IMPL_CLASS))) {
            throw new IllegalArgumentException("can only kill " + UNIX_PROCESS_CLASS + " or " + PROCESS_IMPL_CLASS + "; input is a " + process.getClass());
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

