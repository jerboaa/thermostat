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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isA;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.io.BufferedReader;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import com.redhat.thermostat.common.ActionEvent;
import com.redhat.thermostat.common.ActionListener;
import com.redhat.thermostat.common.ActionNotifier;
import com.redhat.thermostat.common.cli.AbstractStateNotifyingCommand;
import com.redhat.thermostat.common.tools.ApplicationState;
import com.redhat.thermostat.launcher.Launcher;
import com.redhat.thermostat.service.process.UNIXProcessHandler;
import com.redhat.thermostat.setup.command.internal.cli.CharArrayMatcher;
import com.redhat.thermostat.shared.config.CommonPaths;
import com.redhat.thermostat.storage.config.FileStorageCredentials;

public class MongodbUserSetupTest {

    private MongodbUserSetup mongoSetup;
    private StampFiles stampFiles;
    private UNIXProcessHandler processHandler;
    private Launcher mockLauncher;
    private CredentialFinder finder;
    private CredentialsFileCreator fileCreator;
    private CommonPaths paths;
    private StructureInformation info;
    private AuthFileWriter authFileWriter;
    private KeyringWriter keyringWriter;
    
    @Before
    public void setup() {
        paths = mock(CommonPaths.class);
        finder = new CredentialFinder(paths);
        fileCreator = mock(CredentialsFileCreator.class);
        stampFiles = mock(StampFiles.class);
        info = mock(StructureInformation.class);
        mockLauncher = mock(Launcher.class);
        authFileWriter = mock(AuthFileWriter.class);
        keyringWriter = mock(KeyringWriter.class);
        mongoSetup = new MongodbUserSetup(new UserCredsValidator(), mockLauncher, processHandler, finder, fileCreator, paths, stampFiles, info, authFileWriter, keyringWriter) {
            @Override
            int runMongo() {
                //instead of running mongo through ProcessBuilder
                //we need to always return 0 for success in tests
                return 0;
            }

            @Override
            boolean isStorageRunning() {
                // Storage is required to not already be running in
                // order for mongodb user to be added.
                return false;
            }
        };
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void emptyUsernameDisallowed() {
        mongoSetup.createUser("", new char[] { 't' }, null);
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void nullUsernameDisallowed() {
        mongoSetup.createUser(null, new char[] { 't' }, null);
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void nullPasswordDisallowed() {
        mongoSetup.createUser("somebody", null, null);
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void emptyPasswordDisallowed() {
        mongoSetup.createUser("somebody", new char[] {}, null);
    }
    
    @Test
    public void testUnlockThermostat() throws IOException {
        when(stampFiles.setupCompleteStampExists()).thenReturn(false);
        mongoSetup.unlockThermostat();
        ArgumentCaptor<String> argCaptor = ArgumentCaptor.forClass(String.class);
        verify(stampFiles).createSetupCompleteStamp(argCaptor.capture());
        String contentValue = argCaptor.getValue();
        assertTrue(contentValue.startsWith("Temporarily unlocked thermostat"));
        assertTrue(contentValue.contains(ThermostatSetup.PROGRAM_NAME));
    }
    
    @SuppressWarnings("unchecked")
    @Test
    public void testStorageStartFail() {
        final ActionEvent<ApplicationState> mockActionEvent = mock(ActionEvent.class);
        AbstractStateNotifyingCommand mockStorage = mock(AbstractStateNotifyingCommand.class);
        when(mockActionEvent.getSource()).thenReturn(mockStorage);
        when(mockStorage.getNotifier()).thenReturn(mock(ActionNotifier.class));
        final Collection<ActionListener<ApplicationState>> listeners[] = new Collection[1];
        doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                Object[] args = invocation.getArguments();
                listeners[0] = (Collection<ActionListener<ApplicationState>>) args[1];

                when(mockActionEvent.getActionId()).thenReturn(ApplicationState.FAIL);

                for (ActionListener<ApplicationState> listener : listeners[0]) {
                    listener.actionPerformed(mockActionEvent);
                }
                return null;
            }
        }).when(mockLauncher).run(eq(MongodbUserSetup.STORAGE_START_ARGS), isA(Collection.class), anyBoolean());

        try {
            mongoSetup.createUser("foo-user", new char[] { 't' }, "bar comment");
            mongoSetup.commit();
            fail("mongosetup should have failed");
        } catch (IOException e) {
            assertTrue(e.getMessage().contains("Thermostat storage failed to start"));
        }
    }
    
    @SuppressWarnings("unchecked")
    @Test
    public void testStorageStopFail() {
        final ActionEvent<ApplicationState> mockActionEvent = mock(ActionEvent.class);
        AbstractStateNotifyingCommand mockStorage = mock(AbstractStateNotifyingCommand.class);
        when(mockActionEvent.getSource()).thenReturn(mockStorage);
        when(mockStorage.getNotifier()).thenReturn(mock(ActionNotifier.class));
        final Collection<ActionListener<ApplicationState>> listeners[] = new Collection[1];
        doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                Object[] args = invocation.getArguments();
                listeners[0] = (Collection<ActionListener<ApplicationState>>) args[1];

                when(mockActionEvent.getActionId()).thenReturn(ApplicationState.START);

                for (ActionListener<ApplicationState> listener : listeners[0]) {
                    listener.actionPerformed(mockActionEvent);
                }
                return null;
            }
        }).when(mockLauncher).run(eq(MongodbUserSetup.STORAGE_START_ARGS), isA(Collection.class), anyBoolean());

        doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                Object[] args = invocation.getArguments();
                listeners[0] = (Collection<ActionListener<ApplicationState>>) args[1];

                when(mockActionEvent.getActionId()).thenReturn(ApplicationState.FAIL);

                for (ActionListener<ApplicationState> listener : listeners[0]) {
                    listener.actionPerformed(mockActionEvent);
                }
                return null;
            }
        }).when(mockLauncher).run(eq(MongodbUserSetup.STORAGE_STOP_ARGS), isA(Collection.class), anyBoolean());

        try {
            mongoSetup.createUser("foo-user", new char[] { 't' }, "bar comment");
            mongoSetup.commit();
            fail("mongosetup should have failed");
        } catch (IOException e) {
            assertTrue(e.getMessage().contains("Thermostat storage failed to stop"));
        }
    }
    
    @SuppressWarnings("unchecked")
    @Test
    public void testCreateMongodbUserFail() throws IOException {
        final ActionEvent<ApplicationState> mockActionEvent = mock(ActionEvent.class);
        AbstractStateNotifyingCommand mockStorage = mock(AbstractStateNotifyingCommand.class);
        when(mockActionEvent.getSource()).thenReturn(mockStorage);
        when(mockStorage.getNotifier()).thenReturn(mock(ActionNotifier.class));
        final Collection<ActionListener<ApplicationState>> listeners[] = new Collection[1];
        Path testRoot = TestRootHelper.createTestRootDirectory(getClass().getName());
        Path libDir = Paths.get(testRoot.toString(), "libs");
        Files.createDirectory(libDir);
        File createUserJsFile = new File(libDir.toFile(), "create-user.js");
        createUserJsFile.createNewFile();
        when(paths.getSystemThermostatHome()).thenReturn(testRoot.toFile());
        try {
            mongoSetup = new MongodbUserSetup(new UserCredsValidator(), mockLauncher, processHandler, finder, fileCreator, paths, stampFiles, info, authFileWriter, keyringWriter) {
                @Override
                int runMongo() {
                    //return non-zero val to test failure
                    return 1;
                }

                @Override
                boolean isStorageRunning() {
                    // Storage is required to not already be running in
                    // order for mongodb user to be added.
                    return false;
                }
            };
    
            doAnswer(new Answer<Void>() {
                @Override
                public Void answer(InvocationOnMock invocation) throws Throwable {
                    Object[] args = invocation.getArguments();
                    listeners[0] = (Collection<ActionListener<ApplicationState>>) args[1];
    
                    when(mockActionEvent.getActionId()).thenReturn(ApplicationState.START);
    
                    for (ActionListener<ApplicationState> listener : listeners[0]) {
                        listener.actionPerformed(mockActionEvent);
                    }
                    return null;
                }
            }).when(mockLauncher).run(eq(MongodbUserSetup.STORAGE_START_ARGS), isA(Collection.class), anyBoolean());
            
            // We simulate storage starting to work, thus on shut-down it tries
            // to stop storage again.
            doAnswer(new Answer<Void>() {
                @Override
                public Void answer(InvocationOnMock invocation) throws Throwable {
                    Object[] args = invocation.getArguments();
                    listeners[0] = (Collection<ActionListener<ApplicationState>>) args[1];
    
                    when(mockActionEvent.getActionId()).thenReturn(ApplicationState.STOP);
    
                    for (ActionListener<ApplicationState> listener : listeners[0]) {
                        listener.actionPerformed(mockActionEvent);
                    }
                    return null;
                }
            }).when(mockLauncher).run(eq(MongodbUserSetup.STORAGE_STOP_ARGS), isA(Collection.class), anyBoolean());
    
            try {
                mongoSetup.createUser("foo-user", new char[] { 't' }, "bar comment");
                mongoSetup.commit();
                fail("mongosetup should have failed");
            } catch (IOException e) {
                assertTrue(e.getMessage().contains("Mongodb user setup failed"));
            }
            verify(stampFiles).deleteMongodbUserStamp();
            verify(stampFiles).deleteSetupCompleteStamp();
        } finally {
            TestRootHelper.recursivelyRemoveTestRootDirectory(testRoot);
        }
    }
    
    @SuppressWarnings("unchecked")
    @Test
    public void testSetupMongodbUserWebappInstalled() throws IOException {
        Path testRoot = TestRootHelper.createTestRootDirectory(getClass().getName());
        Path libDir = Paths.get(testRoot.toString(), "libs");
        Files.createDirectory(libDir);
        File createUserJsFile = new File(libDir.toFile(), "create-user.js");
        createUserJsFile.createNewFile();
        when(paths.getSystemThermostatHome()).thenReturn(testRoot.toFile());
        try {
            File userDoneFile = File.createTempFile("thermostat", getClass().getName());
            userDoneFile.deleteOnExit();
            File setupCompleteFile = File.createTempFile("thermostat", getClass().getName());
            setupCompleteFile.deleteOnExit();
            when(stampFiles.getMongodbStampFile()).thenReturn(userDoneFile);
            when(stampFiles.getSetupCompleteStampFile()).thenReturn(setupCompleteFile);
    
            // Fake webapp is installed.
            when(info.isWebAppInstalled()).thenReturn(true);
            
            File mockWebAuthFile = File.createTempFile("thermostat", getClass().getName());
            mockWebAuthFile.deleteOnExit();
            finder = mock(CredentialFinder.class);
            when(finder.getConfiguration("web.auth")).thenReturn(mockWebAuthFile);
    
            final ActionEvent<ApplicationState> mockActionEvent = mock(ActionEvent.class);
            AbstractStateNotifyingCommand mockStorage = mock(AbstractStateNotifyingCommand.class);
            when(mockActionEvent.getSource()).thenReturn(mockStorage);
            when(mockStorage.getNotifier()).thenReturn(mock(ActionNotifier.class));
            final Collection<ActionListener<ApplicationState>> listeners[] = new Collection[1];
            doAnswer(new Answer<Void>() {
                @Override
                public Void answer(InvocationOnMock invocation) throws Throwable {
                    Object[] args = invocation.getArguments();
                    listeners[0] = (Collection<ActionListener<ApplicationState>>) args[1];
    
                    when(mockActionEvent.getActionId()).thenReturn(ApplicationState.START);
    
                    for (ActionListener<ApplicationState> listener : listeners[0]) {
                        listener.actionPerformed(mockActionEvent);
                    }
                    return null;
                }
            }).when(mockLauncher).run(eq(MongodbUserSetup.STORAGE_START_ARGS), isA(Collection.class), anyBoolean());

            // We started storage successfully, thus after we are done we
            // stop it again. Mock the storage --stop.
            doAnswer(new Answer<Void>() {
                @Override
                public Void answer(InvocationOnMock invocation) throws Throwable {
                    Object[] args = invocation.getArguments();
                    listeners[0] = (Collection<ActionListener<ApplicationState>>) args[1];
    
                    when(mockActionEvent.getActionId()).thenReturn(ApplicationState.STOP);
    
                    for (ActionListener<ApplicationState> listener : listeners[0]) {
                        listener.actionPerformed(mockActionEvent);
                    }
                    return null;
                }
            }).when(mockLauncher).run(eq(MongodbUserSetup.STORAGE_STOP_ARGS), isA(Collection.class), anyBoolean());
            
            mongoSetup = new MongodbUserSetup(new UserCredsValidator(), mockLauncher, processHandler, finder, fileCreator, paths, stampFiles, info, authFileWriter, keyringWriter) {
                @Override
                int runMongo() {
                    //instead of running mongo through ProcessBuilder
                    //we need to always return 0 for success in tests
                    return 0;
                }

                @Override
                boolean isStorageRunning() {
                    // Storage is required to not already be running in
                    // order for mongodb user to be added.
                    return false;
                }
            };
            String username = "foo-user";
            char[] password = new char[] { 't', 'e', 's', 't' };
            try {
                mongoSetup.createUser(username, password, "bar comment");
                mongoSetup.commit();
                // pass
            } catch (IOException e) {
                e.printStackTrace();
                fail("did not expect exception");
            }
    
            verify(mockLauncher, times(1)).run(eq(MongodbUserSetup.STORAGE_START_ARGS), isA(Collection.class), anyBoolean());
            verify(mockLauncher, times(1)).run(eq(MongodbUserSetup.STORAGE_STOP_ARGS), isA(Collection.class), anyBoolean());
            verify(mockActionEvent, times(2)).getActionId();
            verify(fileCreator).create(mockWebAuthFile);
            verify(stampFiles).createMongodbUserStamp();
            // temp unlocking calls this
            verify(stampFiles, times(1)).createSetupCompleteStamp(any(String.class));
            // we don't expect any interactions for the non-webapp case
            verifyNoMoreInteractions(authFileWriter);
            verifyNoMoreInteractions(keyringWriter);
    
            assertTrue(mockWebAuthFile.exists());
            // make sure credentials file can be read by FileStorageCredentials
            FileStorageCredentials creds = new FileStorageCredentials(mockWebAuthFile);
            assertEquals(username, creds.getUsername());
            // Passed in password array is expected to be cleared.
            assertArrayEquals(new char[] { '\0', '\0', '\0', '\0'}, password);
            assertArrayEquals(new char[] { 't', 'e', 's', 't' }, creds.getPassword());
        } finally {
            TestRootHelper.recursivelyRemoveTestRootDirectory(testRoot);
        }
    }
    
    @SuppressWarnings("unchecked")
    @Test
    public void testSetupMongodbUserNoWebappInstalled() throws IOException {
        Path testRoot = TestRootHelper.createTestRootDirectory(getClass().getName());
        Path libDir = Paths.get(testRoot.toString(), "libs");
        Files.createDirectory(libDir);
        File createUserJsFile = new File(libDir.toFile(), "create-user.js");
        createUserJsFile.createNewFile();
        when(paths.getSystemThermostatHome()).thenReturn(testRoot.toFile());
        // Fake webapp is *not* installed.
        when(info.isWebAppInstalled()).thenReturn(false);
        try {
            File userDoneFile = File.createTempFile("thermostat", getClass().getName());
            userDoneFile.deleteOnExit();
            File setupCompleteFile = File.createTempFile("thermostat", getClass().getName());
            setupCompleteFile.deleteOnExit();
            when(stampFiles.getMongodbStampFile()).thenReturn(userDoneFile);
            when(stampFiles.getSetupCompleteStampFile()).thenReturn(setupCompleteFile);
    
            final ActionEvent<ApplicationState> mockActionEvent = mock(ActionEvent.class);
            AbstractStateNotifyingCommand mockStorage = mock(AbstractStateNotifyingCommand.class);
            when(mockActionEvent.getSource()).thenReturn(mockStorage);
            when(mockStorage.getNotifier()).thenReturn(mock(ActionNotifier.class));
            final Collection<ActionListener<ApplicationState>> listeners[] = new Collection[1];
            doAnswer(new Answer<Void>() {
                @Override
                public Void answer(InvocationOnMock invocation) throws Throwable {
                    Object[] args = invocation.getArguments();
                    listeners[0] = (Collection<ActionListener<ApplicationState>>) args[1];
    
                    when(mockActionEvent.getActionId()).thenReturn(ApplicationState.START);
    
                    for (ActionListener<ApplicationState> listener : listeners[0]) {
                        listener.actionPerformed(mockActionEvent);
                    }
                    return null;
                }
            }).when(mockLauncher).run(eq(MongodbUserSetup.STORAGE_START_ARGS), isA(Collection.class), anyBoolean());

            // We started storage successfully, thus after we are done we
            // stop it again. Mock the storage --stop.
            doAnswer(new Answer<Void>() {
                @Override
                public Void answer(InvocationOnMock invocation) throws Throwable {
                    Object[] args = invocation.getArguments();
                    listeners[0] = (Collection<ActionListener<ApplicationState>>) args[1];
    
                    when(mockActionEvent.getActionId()).thenReturn(ApplicationState.STOP);
    
                    for (ActionListener<ApplicationState> listener : listeners[0]) {
                        listener.actionPerformed(mockActionEvent);
                    }
                    return null;
                }
            }).when(mockLauncher).run(eq(MongodbUserSetup.STORAGE_STOP_ARGS), isA(Collection.class), anyBoolean());
            
            mongoSetup = new MongodbUserSetup(new UserCredsValidator(), mockLauncher, processHandler, finder, fileCreator, paths, stampFiles, info, authFileWriter, keyringWriter) {
                @Override
                int runMongo() {
                    //instead of running mongo through ProcessBuilder
                    //we need to always return 0 for success in tests
                    return 0;
                }

                @Override
                boolean isStorageRunning() {
                    // Storage is required to not already be running in
                    // order for mongodb user to be added.
                    return false;
                }
            };
            String username = "foo-user";
            char[] password = new char[] { 't', 'e', 's', 't' };
            try {
                mongoSetup.createUser(username, password, "bar comment");
                mongoSetup.commit();
                // pass
            } catch (IOException e) {
                e.printStackTrace();
                fail("did not expect exception");
            }
    
            verify(mockLauncher, times(1)).run(eq(MongodbUserSetup.STORAGE_START_ARGS), isA(Collection.class), anyBoolean());
            verify(mockLauncher, times(1)).run(eq(MongodbUserSetup.STORAGE_STOP_ARGS), isA(Collection.class), anyBoolean());
            verify(mockActionEvent, times(2)).getActionId();
            verifyNoMoreInteractions(fileCreator); // We don't want web.auth created
            verify(stampFiles).createMongodbUserStamp();
            
            // twice = 1 x temp unlock, 2 x final creation since webapp is not installed
            verify(stampFiles, times(2)).createSetupCompleteStamp(any(String.class));
            verify(authFileWriter).setCredentials(eq(username), argThat(matchesPassword(new char[] { 't', 'e', 's', 't' })));
            verify(authFileWriter).write();
            verify(keyringWriter).setCredentials(eq(username), argThat(matchesPassword(new char[] { 't', 'e', 's', 't' })));
            verify(keyringWriter).setStorageUrl(ThermostatSetup.MONGODB_STORAGE_URL);
            verify(keyringWriter).write();
    
            // Passed in password array is expected to be cleared.
            assertArrayEquals(new char[] { '\0', '\0', '\0', '\0'}, password);
        } finally {
            TestRootHelper.recursivelyRemoveTestRootDirectory(testRoot);
        }
    }

    @Test
    public void testCheckPidIfFileDoesNotExist() {
        File pidFile = mock(File.class);
        when(pidFile.exists()).thenReturn(false);
        assertFalse(mongoSetup.checkPid(pidFile));
    }
    
    @Test
    public void testDoGetPidNull() throws IOException {
        BufferedReader reader = mock(BufferedReader.class);
        when(reader.readLine()).thenReturn(null);
        Integer pid = mongoSetup.doGetPid(reader);
        assertNull(pid);
    }
    
    private CharArrayMatcher matchesPassword(char[] expected) {
        return new CharArrayMatcher(expected);
    }
}
