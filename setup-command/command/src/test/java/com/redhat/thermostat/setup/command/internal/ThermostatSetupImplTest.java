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

package com.redhat.thermostat.setup.command.internal;

import com.redhat.thermostat.common.ActionEvent;
import com.redhat.thermostat.common.ActionListener;
import com.redhat.thermostat.common.ActionNotifier;
import com.redhat.thermostat.common.cli.AbstractStateNotifyingCommand;
import com.redhat.thermostat.common.tools.ApplicationState;
import com.redhat.thermostat.launcher.Launcher;
import com.redhat.thermostat.shared.config.CommonPaths;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Arrays;
import java.util.Collection;
import java.util.Properties;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.times;

public class ThermostatSetupImplTest {

    private Path testRoot;
    private Path thermostatSysHome;
    private Path thermostatUserHome;
    private Path sysConfigDir;
    private Path userConfigDir;
    private Path userDataDir;
    private Path userAgentAuth;
    private Path credentialsFile;
    private Path userPropertiesFile;
    private Path rolesPropertiesFile;
    private static final String username = "mongodevuser";
    private static final String password = "mongodevpassword";
    private static final String[] STORAGE_START_ARGS = {"storage", "--start", "--permitLocalhostException"};
    private static final String[] STORAGE_STOP_ARGS = {"storage", "--stop"};
    private static final String WEB_AUTH_FILE = "web.auth";
    private static final String USERS_PROPERTIES = "thermostat-users.properties";
    private static final String ROLES_PROPERTIES = "thermostat-roles.properties";
    private static final String THERMOSTAT_AGENT = "thermostat-agent";

    private ThermostatSetupImpl tSetup;
    private CommonPaths paths;
    private Launcher mockLauncher;
    private OutputStream out;
    private static ActionEvent<ApplicationState> mockActionEvent;
    private static Collection<ActionListener<ApplicationState>> listeners;
    private CredentialFinder mockCredentialFinder;

    private void makeTempFilesAndDirectories() throws IOException {
        testRoot = Files.createTempDirectory("thermostat");

        thermostatSysHome = testRoot.resolve("system");
        Files.createDirectory(thermostatSysHome);
        thermostatUserHome = testRoot.resolve("user");
        Files.createDirectory(thermostatUserHome);

        sysConfigDir = thermostatSysHome.resolve("etc");
        Files.createDirectories(sysConfigDir);
        Path sysLibDir = thermostatSysHome.resolve("lib");
        Files.createDirectories(sysLibDir);

        userConfigDir = thermostatUserHome.resolve("etc");
        Files.createDirectories(userConfigDir);
        userDataDir = thermostatUserHome.resolve("data");
        Files.createDirectories(userDataDir);

        userAgentAuth = userConfigDir.resolve("agent.auth");
        credentialsFile = sysConfigDir.resolve(WEB_AUTH_FILE);
        userPropertiesFile = sysConfigDir.resolve(USERS_PROPERTIES);
        rolesPropertiesFile = sysConfigDir.resolve(ROLES_PROPERTIES);

        //create a dummy create-user.js
        Path createUserScript = sysLibDir.resolve("create-user.js");
        Files.write(createUserScript, new byte[]{});
    }

    @Before
    public void setup() throws IOException, InterruptedException {
        makeTempFilesAndDirectories();

        out = new ByteArrayOutputStream();

        paths = mock(CommonPaths.class);
        when(paths.getSystemThermostatHome()).thenReturn(thermostatSysHome.toFile());
        when(paths.getUserThermostatHome()).thenReturn(thermostatUserHome.toFile());
        when(paths.getUserAgentAuthConfigFile()).thenReturn(userAgentAuth.toFile());
        when(paths.getSystemConfigurationDirectory()).thenReturn(sysConfigDir.toFile());
        when(paths.getUserConfigurationDirectory()).thenReturn(userConfigDir.toFile());

        mockLauncher = mock(Launcher.class);
        mockActionEvent = mock(ActionEvent.class);
        AbstractStateNotifyingCommand mockStorageCommand = mock(AbstractStateNotifyingCommand.class);
        ActionNotifier<ApplicationState> mockNotifier = mock(ActionNotifier.class);
        when(mockStorageCommand.getNotifier()).thenReturn(mockNotifier);
        mockActionEvent = mock(ActionEvent.class);
        when(mockActionEvent.getSource()).thenReturn(mockStorageCommand);
        when(mockActionEvent.getPayload()).thenReturn(new String("Test String"));
        mockCredentialFinder = mock(CredentialFinder.class);
        when(mockCredentialFinder.getConfiguration(WEB_AUTH_FILE)).thenReturn(credentialsFile.toFile());
        when(mockCredentialFinder.getConfiguration(USERS_PROPERTIES)).thenReturn(userPropertiesFile.toFile());
        when(mockCredentialFinder.getConfiguration(ROLES_PROPERTIES)).thenReturn(rolesPropertiesFile.toFile());

        tSetup = new ThermostatSetupImpl(mockLauncher, paths, new PrintStream(out), mockCredentialFinder) {
            @Override
            int runMongo() {
                //instead of running mongo through ProcessBuilder
                //we need to always return 0 for success in tests
                return 0;
            }
        };
    }

    @After
    public void teardown() throws IOException {
        paths = null;
        mockLauncher = null;

        Files.walkFileTree(testRoot, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Files.delete(file);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                if (exc == null) {
                    Files.delete(dir);
                    return FileVisitResult.CONTINUE;
                } else {
                    throw exc;
                }
            }
        });
    }

    @Test
    public void testUnlockThermostatUnlockFileCreated() throws IOException {
        String fileData;
        File setupCompleteFile = new File(thermostatUserHome + "/data/setup-complete.stamp");
        Path setupCompleteFilePath = Paths.get(setupCompleteFile.toString());

        Files.createDirectories(setupCompleteFilePath.getParent());
        tSetup.unlockThermostat();
        fileData = new String(Files.readAllBytes(setupCompleteFilePath));

        assertTrue(setupCompleteFile.exists());
        assertTrue(fileData.contains("Temporarily unlocked"));
    }

    @Test
    public void testSetupMongodbUser() throws IOException {
        File userDoneFile = new File(userDataDir.toString() + "/mongodb-user-done.stamp");
        File setupCompleteFile = new File(userDataDir.toString() + "/setup-complete.stamp");

        //create path to webapp so web.auth creation is invoked
        //when ThermostatSetup.createMongodbUser() is called
        Path webAppPath = thermostatSysHome.resolve("webapp");
        Files.createDirectories(webAppPath);

        doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                Object[] args = invocation.getArguments();
                listeners = (Collection<ActionListener<ApplicationState>>) args[1];

                when(mockActionEvent.getActionId()).thenReturn(ApplicationState.START);

                for (ActionListener<ApplicationState> listener : listeners) {
                    listener.actionPerformed(mockActionEvent);
                }
                return null;
            }
        }).when(mockLauncher).run(eq(STORAGE_START_ARGS), isA(Collection.class), anyBoolean());

        boolean exTriggered = false;
        try {
            tSetup.createMongodbUser(username, password.toCharArray());
        } catch (MongodbUserSetupException e) {
            exTriggered = true;
        }

        assertFalse(exTriggered);

        verify(mockLauncher, times(1)).run(eq(STORAGE_START_ARGS), isA(Collection.class), anyBoolean());
        verify(mockLauncher, times(1)).run(eq(STORAGE_STOP_ARGS), isA(Collection.class), anyBoolean());
        verify(mockActionEvent, times(1)).getActionId();

        assertTrue(userDoneFile.exists());
        assertTrue(setupCompleteFile.exists());

        assertTrue(credentialsFile.toFile().exists());
        String credentialsData = new String(Files.readAllBytes(credentialsFile));
        assertTrue(credentialsData.contains("storage.username=" + username));
        assertTrue(credentialsData.contains("storage.password=" + password));

        String setupCompleteData = new String(Files.readAllBytes(setupCompleteFile.toPath()));
        assertTrue(setupCompleteData.contains("Created by Thermostat Setup"));
    }

    @Test
    public void testStorageStartFail() {
        doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                Object[] args = invocation.getArguments();
                listeners = (Collection<ActionListener<ApplicationState>>) args[1];

                when(mockActionEvent.getActionId()).thenReturn(ApplicationState.FAIL);

                for (ActionListener<ApplicationState> listener : listeners) {
                    listener.actionPerformed(mockActionEvent);
                }
                return null;
            }
        }).when(mockLauncher).run(eq(STORAGE_START_ARGS), isA(Collection.class), anyBoolean());

        try {
            tSetup.createMongodbUser(username, password.toCharArray());
            //shouldn't get here
            fail();
        } catch (MongodbUserSetupException e) {
            assertTrue(e.getMessage().contains("Thermostat storage failed to start"));
        }
    }

    @Test
    public void testStorageStopFail() {
        doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                Object[] args = invocation.getArguments();
                listeners = (Collection<ActionListener<ApplicationState>>) args[1];

                when(mockActionEvent.getActionId()).thenReturn(ApplicationState.START);

                for (ActionListener<ApplicationState> listener : listeners) {
                    listener.actionPerformed(mockActionEvent);
                }
                return null;
            }
        }).when(mockLauncher).run(eq(STORAGE_START_ARGS), isA(Collection.class), anyBoolean());

        doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                Object[] args = invocation.getArguments();
                listeners = (Collection<ActionListener<ApplicationState>>) args[1];

                when(mockActionEvent.getActionId()).thenReturn(ApplicationState.FAIL);

                for (ActionListener<ApplicationState> listener : listeners) {
                    listener.actionPerformed(mockActionEvent);
                }
                return null;
            }
        }).when(mockLauncher).run(eq(STORAGE_STOP_ARGS), isA(Collection.class), anyBoolean());

        try {
            tSetup.createMongodbUser(username, password.toCharArray());
            //shouldn't get here
            fail();
        } catch (MongodbUserSetupException e) {
            assertTrue(e.getMessage().contains("Thermostat storage failed to stop"));
        }
    }

    @Test
    public void testCreateMongodbUserFail() {
        tSetup = new ThermostatSetupImpl(mockLauncher, paths, new PrintStream(out), mockCredentialFinder) {
            @Override
            int runMongo() {
                //return non-zero val to test failure
                return 1;
            }
        };

        doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                Object[] args = invocation.getArguments();
                listeners = (Collection<ActionListener<ApplicationState>>) args[1];

                when(mockActionEvent.getActionId()).thenReturn(ApplicationState.START);

                for (ActionListener<ApplicationState> listener : listeners) {
                    listener.actionPerformed(mockActionEvent);
                }
                return null;
            }
        }).when(mockLauncher).run(eq(STORAGE_START_ARGS), isA(Collection.class), anyBoolean());

        try {
            tSetup.createMongodbUser(username, password.toCharArray());
            //shouldn't get here
            fail();
        } catch (MongodbUserSetupException e) {
            assertTrue(e.getMessage().contains("Mongodb user setup failed"));
        }
    }

    @Test
    public void testSetupThermostatUser() throws IOException {
        String clientUser = "client-tester";
        String agentUser = "agent-tester";
        String userPassword = "tester";

        String[] agentRoles = new String[] {
                    UserRoles.CMD_CHANNEL_VERIFY,
                    UserRoles.LOGIN,
                    UserRoles.PREPARE_STATEMENT,
                    UserRoles.PURGE,
                    UserRoles.REGISTER_CATEGORY,
                    UserRoles.ACCESS_REALM,
                    UserRoles.SAVE_FILE,
                    UserRoles.WRITE,
                    UserRoles.GRANT_FILES_WRITE_ALL,
        };
        String[] clientRoles = new String[] {
                    UserRoles.GRANT_AGENTS_READ_ALL,
                    UserRoles.CMD_CHANNEL_GENERATE,
                    UserRoles.GRANT_HOSTS_READ_ALL,
                    UserRoles.LOAD_FILE,
                    UserRoles.LOGIN,
                    UserRoles.PREPARE_STATEMENT,
                    UserRoles.READ,
                    UserRoles.ACCESS_REALM,
                    UserRoles.REGISTER_CATEGORY,
                    UserRoles.GRANT_VMS_READ_BY_USERNAME_ALL,
                    UserRoles.GRANT_VMS_READ_BY_VM_ID_ALL,
                    UserRoles.GRANT_FILES_READ_ALL,
                    UserRoles.WRITE,
        };

        tSetup.createThermostatUser(agentUser, userPassword.toCharArray(), agentRoles);
        tSetup.createThermostatUser(clientUser, userPassword.toCharArray(), clientRoles);

        //check agent credentials file
        assertTrue(userAgentAuth.toFile().exists());
        String userAgentAuthData = new String(Files.readAllBytes(userAgentAuth));
        assertTrue(userAgentAuthData.contains("username=agent-tester"));
        assertTrue(userAgentAuthData.contains("password=tester"));

        //check AgentUser file
        assertTrue(userPropertiesFile.toFile().exists());

        //check clientAdmin file
        assertTrue(rolesPropertiesFile.toFile().exists());
    }

    @Test
    public void testWebAppInstalledSuccess() throws IOException {
        Path webAppPath = thermostatSysHome.resolve("webapp");
        Files.createDirectories(webAppPath);
        assertTrue(tSetup.isWebAppInstalled());
    }

    @Test
    public void testWebAppInstalledFail() throws IOException {
        //Call isWebAppInstalled() without creating
        //a THERMOSTAT_SYS_HOME/webapp directory
        assertFalse(tSetup.isWebAppInstalled());
    }

    @Test
    public void testPropertiesWriter() throws IOException {
        String key = THERMOSTAT_AGENT;
        String[] roles  = new String[] {
                UserRoles.LOGIN,
                UserRoles.PREPARE_STATEMENT,
                UserRoles.PURGE,
                UserRoles.REGISTER_CATEGORY,
        };
        StringBuilder rolesBuilder = new StringBuilder();
        for (int i = 0; i < roles.length - 1; i++) {
            rolesBuilder.append(roles[i] + ", " + System.getProperty("line.separator"));
        }
        rolesBuilder.append(roles[roles.length - 1]);
        String value = rolesBuilder.toString();

        Properties propsToStore = new Properties();
        propsToStore.setProperty(key, value);
        FileOutputStream roleStream = new FileOutputStream(rolesPropertiesFile.toFile());
        propsToStore.store(new ThermostatSetupImpl.PropertiesWriter(roleStream), null);

        Properties propsToLoad = new Properties();
        propsToLoad.load(new FileInputStream(rolesPropertiesFile.toFile()));
        String[] loadedRoles = propsToLoad.getProperty(key).split(",\\s+");

        assertTrue(Arrays.asList(roles).containsAll(Arrays.asList(loadedRoles)));
    }
}
