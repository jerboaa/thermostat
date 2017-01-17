/*
 * Copyright 2012-2017 Red Hat, Inc.
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

package com.redhat.thermostat.storage.cli.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.util.List;

import com.redhat.thermostat.service.process.ProcessHandler;
import com.redhat.thermostat.shared.config.OS;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.redhat.thermostat.shared.config.InvalidConfigurationException;

public class MongoProcessRunnerTest {

    private MongoProcessRunner runner;
    private DBStartupConfiguration config;
    private ProcessHandler processHandler;

    private static final String NO_JOURNAL_MONGODB_VERSION = "2.0.0";
    private static final String JOURNAL_MONGODB_VERSION = "1.8.0";
    private static final String NO_LOCALHOST_EXCPTN_VERSION = "2.2.0";

    private static final String OLDEST_MONGODB_VERSION = "1.8.0";
    private static final String LATEST_MONGODB_VERSION = "3.2.0";

    private static final String BIND_IP = "127.0.0.1";
    private static final long PORT = 12456;
    
    @Before
    public void setUp() {
        File dbPath = new File("/path/to/db");
        File logPath = new File("/path/to/log");
        File pidFile = new File("/path/to/pid");
        config = mock(DBStartupConfiguration.class);
        when(config.getBindIP()).thenReturn(BIND_IP);
        when(config.getPort()).thenReturn(PORT);
        when(config.getDBPath()).thenReturn(dbPath);
        when(config.getLogFile()).thenReturn(logPath);
        when(config.getPidFile()).thenReturn(pidFile);

        processHandler = mock(ProcessHandler.class);

        runner = new MongoProcessRunner(processHandler, config, false, false);
    }
    
    @After
    public void tearDown() {
        runner = null;
        config = null;
    }

    @Test
    public void testCommandArgumentsWithOldestMongodbVersion() throws Exception {
        String[] linuxExpected = { "mongod", "--quiet", "--fork",
                "--auth", "--nohttpinterface", "--bind_ip", config.getBindIP(),
                "--dbpath", config.getDBPath().getCanonicalPath(), "--logpath",
                config.getLogFile().getCanonicalPath(), "--pidfilepath",
                config.getPidFile().getCanonicalPath(), "--port",
                Long.toString(config.getPort()) };
        String[] winExpected = { "cmd", "/c", "start", "/b", "mongod", "--quiet",
                "--auth", "--nohttpinterface", "--bind_ip", config.getBindIP(),
                "--dbpath", config.getDBPath().getCanonicalPath(), "--logpath",
                config.getLogFile().getCanonicalPath(), "--pidfilepath",
                config.getPidFile().getCanonicalPath(), "--port",
                Long.toString(config.getPort()) };
        String expected[] = OS.IS_UNIX ? linuxExpected : winExpected;
        List<String> cmds = runner.getStartupCommand(OLDEST_MONGODB_VERSION);
        String[] actual = cmds.toArray(new String[0]);
        verifyEquals(expected, actual);
    }

    @Test
    public void testCommandArgumentsWithLatestMongodbVersion() throws Exception {
        String[] linuxExpected = { "mongod", "--quiet", "--fork",
                "--auth", "--nohttpinterface", "--bind_ip", config.getBindIP(),
                "--nojournal", "--dbpath", config.getDBPath().getCanonicalPath(), "--logpath",
                config.getLogFile().getCanonicalPath(), "--pidfilepath",
                config.getPidFile().getCanonicalPath(), "--port",
                Long.toString(config.getPort()), "--setParameter", "enableLocalhostAuthBypass=0"};
        String[] winExpected = { "cmd", "/c", "start", "/b", "mongod", "--quiet",
                "--auth", "--nohttpinterface", "--bind_ip", config.getBindIP(),
                "--nojournal", "--dbpath", config.getDBPath().getCanonicalPath(), "--logpath",
                config.getLogFile().getCanonicalPath(), "--pidfilepath",
                config.getPidFile().getCanonicalPath(), "--port",
                Long.toString(config.getPort()), "--setParameter", "enableLocalhostAuthBypass=0"};
        String expected[] = OS.IS_UNIX ? linuxExpected : winExpected;
        List<String> cmds = runner.getStartupCommand(LATEST_MONGODB_VERSION);
        String[] actual = cmds.toArray(new String[0]);
        verifyEquals(expected, actual);
    }
    
    @Test
    public void testCommandArgumentsWithJournalVersion() throws Exception {
        List<String> cmds = runner.getStartupCommand(NO_JOURNAL_MONGODB_VERSION);
        assertTrue(cmds.contains("--nojournal"));
    }

    @Test
    public void testCommandArgumentsWithNoJournalVersion() throws Exception {
        List<String> cmds = runner.getStartupCommand(JOURNAL_MONGODB_VERSION);
        assertFalse(cmds.contains("--nojournal"));
    }
    
    @Test
    public void testLocalhostExcptn() throws Exception {
        String[] expected = new String[] { "--setParameter", "enableLocalhostAuthBypass=0"};
        List<String> cmds = runner.getStartupCommand(MongoProcessRunner.LOCALHOST_EXPTN_FIRST_VERSION);
        String[] actual = cmds.subList(cmds.size() - 2, cmds.size()).toArray(new String[0]);
        verifyEquals(expected, actual);
    }
    
    /*
     * Tests whether the localhost exception parameter is *not* set even
     * though the mongod version would technically support it. Equivalent
     * of passing --permitLocalhostException to "thermostat storage --start".
     * 
     * Having this flag is useful for testing.
     */
    @Test
    public void testLocalhostExcptnDisabled() throws Exception {
        runner = new MongoProcessRunner(processHandler, config, false, true);
        List<String> cmds = runner.getStartupCommand(MongoProcessRunner.LOCALHOST_EXPTN_FIRST_VERSION);
        assertFalse(cmds.contains("enableLocalhostAuthBypass=0"));
    }
    
    @Test
    public void testLocalhostExcptnNotSupported() throws Exception {
        List<String> cmds = runner.getStartupCommand(NO_LOCALHOST_EXCPTN_VERSION);
        assertFalse(cmds.contains("enableLocalhostAuthBypass=0"));
    }
    
    @Test
    public void testCommandArgumentsWithSSLEnabled() throws Exception {
        when(config.isSslEnabled()).thenReturn(true);
        File pemFile = new File("/path/to/cert_and_key.pem");
        when(config.getSslPemFile()).thenReturn(pemFile);
        when(config.getSslKeyPassphrase()).thenReturn("non-null");
        String[] linuxExpected = { "mongod", "--quiet", "--fork", "--auth",
                "--nohttpinterface", "--bind_ip", config.getBindIP(),
                "--dbpath", config.getDBPath().getCanonicalPath(), "--logpath",
                config.getLogFile().getCanonicalPath(), "--pidfilepath",
                config.getPidFile().getCanonicalPath(), "--port",
                Long.toString(config.getPort()), "--sslOnNormalPorts",
                "--sslPEMKeyFile", config.getSslPemFile().getCanonicalPath(),
                "--sslPEMKeyPassword", config.getSslKeyPassphrase()
        };
        String[] winExpected = { "cmd", "/c", "start", "/b", "mongod", "--quiet", "--auth",
                "--nohttpinterface", "--bind_ip", config.getBindIP(),
                "--dbpath", config.getDBPath().getCanonicalPath(), "--logpath",
                config.getLogFile().getCanonicalPath(), "--pidfilepath",
                config.getPidFile().getCanonicalPath(), "--port",
                Long.toString(config.getPort()), "--sslOnNormalPorts",
                "--sslPEMKeyFile", config.getSslPemFile().getCanonicalPath(),
                "--sslPEMKeyPassword", config.getSslKeyPassphrase()
        };
        String expected[] = OS.IS_UNIX ? linuxExpected : winExpected;
        List<String> cmds = runner.getStartupCommand(JOURNAL_MONGODB_VERSION);
        String[] actual = cmds.toArray(new String[0]);
        verifyEquals(expected, actual);
    }
    
    @Test
    public void testCommandArgumentsWithSSLEnabledThrowsInvalidConfigException() throws IOException {
        when(config.isSslEnabled()).thenReturn(true);
        // PEM file can't be null when SSL == true
        when(config.getSslPemFile()).thenReturn(null);
        try {
            runner.getStartupCommand(JOURNAL_MONGODB_VERSION);
            fail("Should have thrown exception!");
        } catch (InvalidConfigurationException e) {
            assertEquals("No SSL PEM file specified!", e.getMessage());
        }
        // Key password can't be null when SSL == true and keyfile present
        File pemFile = new File("/path/to/ssl.pem");
        when(config.getSslPemFile()).thenReturn(pemFile);
        when(config.getSslKeyPassphrase()).thenReturn(null);
        try {
            runner.getStartupCommand(JOURNAL_MONGODB_VERSION);
            fail("Should have thrown exception!");
        } catch (InvalidConfigurationException e) {
            assertEquals("No SSL key passphrase set!", e.getMessage());
        }
    }
    
    @Test
    public void testCheckPidNull() throws IOException {
        BufferedReader reader = mock(BufferedReader.class);
        when(reader.readLine()).thenReturn(null);
        Integer pid = runner.doGetPid(reader);
        assertNull(pid);
    }
    
    @Test
    public void canGetVersionFromVersionCmdOutputV22() throws IOException {
        String versionOutput = "db version v2.2.4, pdfile version 4.5\n" +
                               "Mon Aug 26 17:13:45 git version: nogitversion";
        ByteArrayInputStream in = new ByteArrayInputStream(versionOutput.getBytes());
        String version = runner.doGetDBVersion(in);
        assertEquals("2.2.4", version);
    }
    
    @Test
    public void canGetVersionFromVersionCmdOutputV24() throws IOException {
        String versionOutput = "db version v2.4.5\n" +
                               "Mon Aug 26 18:01:28.404 git version: nogitversion";
        ByteArrayInputStream in = new ByteArrayInputStream(versionOutput.getBytes());
        String version = runner.doGetDBVersion(in);
        assertEquals("2.4.5", version);
    }
    
    @Test
    public void canProceedIfGetDbVersionThrowsException() throws IOException {
        String versionOutput = "foo\n" +
                               "Mon Aug 26 18:01:28.404 git version: nogitversion";
        ByteArrayInputStream in = new ByteArrayInputStream(versionOutput.getBytes());
        String version = runner.doGetDBVersion(in);
        assertEquals(MongoProcessRunner.NO_JOURNAL_FIRST_VERSION, version);
    }

    private void verifyEquals(String[] expected, String[] actual) {
        assertEquals(expected.length, actual.length);
        for (int i=0; i < expected.length; i++) {
            assertEquals(expected[i], actual[i]);
        }
    }
}
