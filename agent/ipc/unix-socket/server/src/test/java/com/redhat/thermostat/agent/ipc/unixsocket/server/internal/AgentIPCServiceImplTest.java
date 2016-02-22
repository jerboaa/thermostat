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

package com.redhat.thermostat.agent.ipc.unixsocket.server.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anySetOf;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.nio.channels.Selector;
import java.nio.file.DirectoryStream;
import java.nio.file.Path;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ExecutorService;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import com.redhat.thermostat.agent.ipc.common.internal.IPCProperties;
import com.redhat.thermostat.agent.ipc.common.internal.IPCType;
import com.redhat.thermostat.agent.ipc.server.ThermostatIPCCallbacks;
import com.redhat.thermostat.agent.ipc.unixsocket.common.internal.UnixSocketIPCProperties;
import com.redhat.thermostat.agent.ipc.unixsocket.server.internal.AgentIPCServiceImpl.ChannelCreator;
import com.redhat.thermostat.agent.ipc.unixsocket.server.internal.AgentIPCServiceImpl.FileUtils;
import com.redhat.thermostat.agent.ipc.unixsocket.server.internal.AgentIPCServiceImpl.ThreadCreator;
import com.redhat.thermostat.shared.config.CommonPaths;

public class AgentIPCServiceImplTest {
    
    private static final String SERVER_NAME = "test";
    
    private AgentIPCServiceImpl ipcService;
    private Selector selector;
    private CommonPaths paths;
    private ExecutorService execService;
    private FilenameValidator validator;
    private FileUtils fileUtils;
    private Path socketDirPath;
    private AcceptThread acceptThread;
    private ThreadCreator threadCreator;
    private FileAttribute<Set<PosixFilePermission>> fileAttr;
    private Path socketPath;
    private ThermostatIPCCallbacks callbacks;
    private ChannelCreator channelCreator;
    private ThermostatLocalServerSocketChannelImpl channel;

    @SuppressWarnings("unchecked")
    @Before
    public void setup() throws Exception {
        selector = mock(Selector.class);
        
        paths = mock(CommonPaths.class);
        File userData = mock(File.class);
        when(paths.getUserRuntimeDataDirectory()).thenReturn(userData);
        Path userDataPath = mock(Path.class);
        when(userData.toPath()).thenReturn(userDataPath);
        socketDirPath = mock(Path.class);
        UnixSocketIPCProperties props = mock(UnixSocketIPCProperties.class);
        File socketDirFile = mock(File.class);
        when(socketDirFile.toPath()).thenReturn(socketDirPath);
        when(props.getSocketDirectory()).thenReturn(socketDirFile);
        socketPath = mock(Path.class);
        when(socketDirPath.resolve(AgentIPCServiceImpl.SOCKET_PREFIX + SERVER_NAME)).thenReturn(socketPath);
        
        fileUtils = mock(FileUtils.class);
        when(fileUtils.exists(socketDirPath)).thenReturn(false);
        
        fileAttr = mock(FileAttribute.class);
        when(fileUtils.toFileAttribute(any(Set.class))).thenReturn(fileAttr);
        
        execService = mock(ExecutorService.class);
        validator = mock(FilenameValidator.class);
        when(validator.validate(any(String.class))).thenReturn(true);
        
        acceptThread = mock(AcceptThread.class);
        threadCreator = mock(ThreadCreator.class);
        when(threadCreator.createAcceptThread(selector, execService)).thenReturn(acceptThread);
        
        channelCreator = mock(ChannelCreator.class);
        channel = mock(ThermostatLocalServerSocketChannelImpl.class);
        File socketFile = mock(File.class);
        when(socketFile.toPath()).thenReturn(socketPath);
        when(channel.getSocketFile()).thenReturn(socketFile);
        
        callbacks = mock(ThermostatIPCCallbacks.class);
        when(channelCreator.createServerSocketChannel(SERVER_NAME, socketPath, callbacks, selector)).thenReturn(channel);
        
        ipcService = new AgentIPCServiceImpl(selector, props, execService, validator, fileUtils, 
                threadCreator, channelCreator);
    }
    
    @Test
    public void testInit() throws Exception {
        verify(threadCreator).createAcceptThread(selector, execService);
        assertEquals(socketDirPath, ipcService.getSocketDirPath());
    }
    
    @Test(expected=IOException.class)
    public void testInitBadProperties() throws Exception {
        // Not UnixSocketIPCProperties
        IPCProperties props = mock(IPCProperties.class);
        when(props.getType()).thenReturn(IPCType.UNKNOWN);
        ipcService = new AgentIPCServiceImpl(selector, props, execService, validator, fileUtils, 
                threadCreator, channelCreator);
    }
    
    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Test
    public void testStartSuccess() throws Exception {
        ipcService.start();
        
        // Verify the socket directory is created with the proper permissions
        verify(fileUtils).exists(socketDirPath);

        ArgumentCaptor<Set> permsCaptor = ArgumentCaptor.forClass(Set.class);
        verify(fileUtils).toFileAttribute(permsCaptor.capture());
        
        Set<PosixFilePermission> perms = (Set<PosixFilePermission>) permsCaptor.getValue();
        Set<PosixFilePermission> expectedPerms = PosixFilePermissions.fromString("rwxrwx---");
        assertEquals(perms, expectedPerms);
        
        verify(fileUtils).createDirectory(socketDirPath, fileAttr);
        
        // Verify the thread to accept connections is started
        verify(acceptThread).start();
    }
    
    @Test
    public void testStartSuccessDirExists() throws Exception {
        when(fileUtils.exists(socketDirPath)).thenReturn(true);
        when(fileUtils.isDirectory(socketDirPath)).thenReturn(true);
        when(fileUtils.getPosixFilePermissions(socketDirPath)).thenReturn(PosixFilePermissions.fromString("rwxrwx---"));
        
        ipcService.start();
        
        // Verify the existing directory is checked and used
        verify(fileUtils).exists(socketDirPath);
        verify(fileUtils).isDirectory(socketDirPath);
        verify(fileUtils).getPosixFilePermissions(socketDirPath);
        
        verify(fileUtils, never()).toFileAttribute(anySetOf(PosixFilePermission.class));
        verify(fileUtils, never()).createDirectory(any(Path.class));
        
        // Verify the thread to accept connections is started
        verify(acceptThread).start();
    }
    
    @Test
    public void testStartFailsNotDir() throws Exception {
        when(fileUtils.exists(socketDirPath)).thenReturn(true);
        // Socket directory is not a directory
        when(fileUtils.isDirectory(socketDirPath)).thenReturn(false);
        
        try {
            ipcService.start();
            fail("Expected IOException");
        } catch (IOException e) {
            verify(fileUtils, never()).createDirectory(any(Path.class));
            verify(acceptThread, never()).start();
        }
    }
    
    @Test
    public void testStartFailsBadPerm() throws Exception {
        when(fileUtils.exists(socketDirPath)).thenReturn(true);
        when(fileUtils.isDirectory(socketDirPath)).thenReturn(true);
        // Socket directory is world readable
        when(fileUtils.getPosixFilePermissions(socketDirPath)).thenReturn(PosixFilePermissions.fromString("rwxrwxr-x"));
        
        try {
            ipcService.start();
            fail("Expected IOException");
        } catch (IOException e) {
            verify(fileUtils, never()).createDirectory(any(Path.class));
            verify(acceptThread, never()).start();
        }
    }
    
    @Test
    public void testShutdownSuccess() throws Exception {
        mockSocketDirOnShutdown();
        ipcService.shutdown();
        verify(acceptThread).shutdown();
        
        // Check socket directory is removed
        verify(fileUtils).delete(socketPath);
        verify(fileUtils).delete(socketDirPath);
    }
    
    @Test
    public void testShutdownFailure() throws Exception {
        mockSocketDirOnShutdown();
        doThrow(new IOException()).when(acceptThread).shutdown();
        
        try {
            ipcService.shutdown();
            fail("Expected IO Exception");
        } catch (IOException e) {
            // Socket directory should still be deleted
            verify(fileUtils).delete(socketPath);
            verify(fileUtils).delete(socketDirPath);
        }
    }
    
    @Test
    public void testCreateServer() throws Exception {
        ipcService.createServer(SERVER_NAME, callbacks);
        
        verify(fileUtils).exists(socketPath);
        verify(fileUtils, never()).delete(socketPath);
        
        checkChannel();
    }

    private void checkChannel() throws IOException {
        verify(channelCreator).createServerSocketChannel(SERVER_NAME, socketPath, callbacks, selector);
        ThermostatLocalServerSocketChannelImpl result = ipcService.getSockets().get(SERVER_NAME);
        assertEquals(channel, result);
    }
    
    @Test
    public void testCreateServerLeftoverFile() throws Exception {
        when(fileUtils.exists(socketPath)).thenReturn(true);
        ipcService.createServer(SERVER_NAME, callbacks);
        
        verify(fileUtils).exists(socketPath);
        verify(fileUtils).delete(socketPath);
        
        checkChannel();
    }
    
    @Test(expected=IOException.class)
    public void testCreateServerServerExists() throws Exception {
        ThermostatLocalServerSocketChannelImpl channel = mock(ThermostatLocalServerSocketChannelImpl.class);
        ipcService.getSockets().put(SERVER_NAME, channel);

        ipcService.createServer(SERVER_NAME, callbacks);
    }
    
    @Test(expected=IOException.class)
    public void testCreateServerInvalidName() throws Exception {
        when(validator.validate(SERVER_NAME)).thenReturn(false);
        ipcService.createServer(SERVER_NAME, callbacks);
    }
    
    @Test
    public void testServerExists() throws Exception {
        assertFalse(ipcService.serverExists(SERVER_NAME));
        ipcService.getSockets().put(SERVER_NAME, channel);
        assertTrue(ipcService.serverExists(SERVER_NAME));
    }
    
    @Test
    public void testDestroyServer() throws Exception {
        ipcService.getSockets().put(SERVER_NAME, channel);
        ipcService.destroyServer(SERVER_NAME);
        
        verify(channel).close();
        verify(fileUtils).delete(socketPath);
    }
    
    @Test(expected=IOException.class)
    public void testDestroyServerNotExist() throws Exception {
        ipcService.destroyServer(SERVER_NAME);
    }
    
    @Test
    public void testDestroyServerCloseFails() throws Exception {
        doThrow(new IOException()).when(channel).close();
        ipcService.getSockets().put(SERVER_NAME, channel);
        
        try {
            ipcService.destroyServer(SERVER_NAME);
            fail("Expected IOException");
        } catch (IOException e) {
            // Socket file should still be deleted
            verify(fileUtils).delete(socketPath);
        }
    }
    
    // Mock a socket directory containing a socket file
    private void mockSocketDirOnShutdown() throws IOException {
        when(fileUtils.exists(socketDirPath)).thenReturn(true);
        DirectoryStream<Path> dirStream = mockDirectoryStream(socketPath);
        when(fileUtils.newDirectoryStream(socketDirPath)).thenReturn(dirStream);
    }

    @SuppressWarnings("unchecked")
    private DirectoryStream<Path> mockDirectoryStream(Path socketFile) {
        DirectoryStream<Path> dirStream = (DirectoryStream<Path>) mock(DirectoryStream.class);
        Iterator<Path> iterator = mock(Iterator.class);
        when(iterator.hasNext()).thenReturn(true).thenReturn(false);
        when(iterator.next()).thenReturn(socketFile);
        when(dirStream.iterator()).thenReturn(iterator);
        return dirStream;
    }
    
}
