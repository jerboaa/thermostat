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
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.times;

import java.io.File;
import java.io.IOException;
import java.nio.channels.spi.AbstractSelector;
import java.nio.channels.spi.SelectorProvider;
import java.nio.file.FileVisitOption;
import java.nio.file.FileVisitor;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.nio.file.attribute.UserPrincipal;
import java.nio.file.attribute.UserPrincipalLookupService;
import java.util.Set;
import java.util.concurrent.ExecutorService;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import com.redhat.thermostat.agent.ipc.common.internal.IPCProperties;
import com.redhat.thermostat.agent.ipc.common.internal.IPCType;
import com.redhat.thermostat.agent.ipc.server.ThermostatIPCCallbacks;
import com.redhat.thermostat.agent.ipc.unixsocket.common.internal.UnixSocketIPCProperties;
import com.redhat.thermostat.agent.ipc.unixsocket.server.internal.UnixSocketServerTransport.ChannelUtils;
import com.redhat.thermostat.agent.ipc.unixsocket.server.internal.UnixSocketServerTransport.FileUtils;
import com.redhat.thermostat.agent.ipc.unixsocket.server.internal.UnixSocketServerTransport.ThreadCreator;

public class UnixSocketServerTransportTest {
    
    private static final String SERVER_NAME = "test";
    private static final String USERNAME = "testUser";
    
    private UnixSocketServerTransport transport;
    private SelectorProvider provider;
    private AbstractSelector selector;
    private ExecutorService execService;
    private FilenameValidator validator;
    private FileUtils fileUtils;
    private Path socketDirPath;
    private Path ownerDirPath;
    private AcceptThread acceptThread;
    private ThreadCreator threadCreator;
    private FileAttribute<Set<PosixFilePermission>> fileAttr;
    private Path socketPath;
    private ThermostatIPCCallbacks callbacks;
    private ChannelUtils channelUtils;
    private ThermostatLocalServerSocketChannelImpl channel;
    private UnixSocketIPCProperties props;
    private UserPrincipalLookupService lookup;
    private UserPrincipal currentUser;

    @SuppressWarnings("unchecked")
    @Before
    public void setup() throws Exception {
        provider = mock(SelectorProvider.class);
        selector = mock(AbstractSelector.class);
        when(provider.openSelector()).thenReturn(selector);
        
        props = mock(UnixSocketIPCProperties.class);
        File sockDirFile = mock(File.class);
        when(props.getSocketDirectory()).thenReturn(sockDirFile);
        socketDirPath = mock(Path.class);
        when(socketDirPath.toAbsolutePath()).thenReturn(socketDirPath);
        when(socketDirPath.normalize()).thenReturn(socketDirPath);
        when(sockDirFile.toPath()).thenReturn(socketDirPath);
        ownerDirPath = mock(Path.class);
        socketPath = mock(Path.class);
        when(socketDirPath.resolve(USERNAME)).thenReturn(ownerDirPath);
        File socketFile = mock(File.class);
        when(props.getSocketFile(SERVER_NAME, USERNAME)).thenReturn(socketFile);
        when(socketFile.toPath()).thenReturn(socketPath);
        
        fileUtils = mock(FileUtils.class);
        when(fileUtils.exists(socketDirPath)).thenReturn(false);
        when(fileUtils.getPosixFilePermissions(socketDirPath)).thenReturn(PosixFilePermissions.fromString("rwxr-xr-x"));
        when(fileUtils.getPosixFilePermissions(ownerDirPath)).thenReturn(PosixFilePermissions.fromString("rwx------"));
        
        fileAttr = mock(FileAttribute.class);
        when(fileUtils.toFileAttribute(any(Set.class))).thenReturn(fileAttr);
        
        lookup = mock(UserPrincipalLookupService.class);
        when(fileUtils.getUserPrincipalLookupService()).thenReturn(lookup);
        when(fileUtils.getUsername()).thenReturn(USERNAME);
        currentUser = mock(UserPrincipal.class);
        when(currentUser.getName()).thenReturn(USERNAME);
        when(lookup.lookupPrincipalByName(USERNAME)).thenReturn(currentUser);
        when(fileUtils.getOwner(socketDirPath)).thenReturn(currentUser);
        when(fileUtils.getOwner(ownerDirPath)).thenReturn(currentUser);
        
        execService = mock(ExecutorService.class);
        validator = mock(FilenameValidator.class);
        when(validator.validate(any(String.class))).thenReturn(true);
        
        acceptThread = mock(AcceptThread.class);
        threadCreator = mock(ThreadCreator.class);
        when(threadCreator.createAcceptThread(selector, execService)).thenReturn(acceptThread);
        
        channelUtils = mock(ChannelUtils.class);
        channel = mock(ThermostatLocalServerSocketChannelImpl.class);
        when(channel.getSocketFile()).thenReturn(socketFile);
        when(fileUtils.getOwner(socketPath)).thenReturn(currentUser);
        
        callbacks = mock(ThermostatIPCCallbacks.class);
        when(channelUtils.createServerSocketChannel(SERVER_NAME, socketPath, callbacks, props, selector)).thenReturn(channel);
        
        transport = new UnixSocketServerTransport(provider, execService, validator, fileUtils, 
                threadCreator, channelUtils);
    }
    
    @Test
    public void testInit() throws Exception {
        transport.start(props);
        verify(provider).openSelector();
        verify(threadCreator).createAcceptThread(selector, execService);
        assertEquals(socketDirPath, transport.getSocketDirPath());
    }
    
    @Test(expected=IOException.class)
    public void testStartBadProperties() throws Exception {
        // Not UnixSocketIPCProperties
        IPCProperties badProps = mock(IPCProperties.class);
        when(badProps.getType()).thenReturn(IPCType.UNKNOWN);
        transport = new UnixSocketServerTransport(provider, execService, validator, fileUtils, 
                threadCreator, channelUtils);
        transport.start(badProps);
    }
    
    @Test(expected=IOException.class)
    public void testInitBadPath() throws Exception {
        when(socketDirPath.normalize()).thenThrow(new InvalidPathException("TEST", "TEST"));
        transport = new UnixSocketServerTransport(provider, execService, validator, fileUtils, 
                threadCreator, channelUtils);
        transport.start(props);
    }
    
    @Test
    public void testPathNormalized() throws Exception {
        transport.start(props);
        verify(socketDirPath).toAbsolutePath();
        verify(socketDirPath).normalize();
    }
    
    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Test
    public void testStartSuccess() throws Exception {
        transport.start(props);
        
        // Verify the socket directory is created with the proper permissions
        verify(fileUtils).exists(socketDirPath);

        ArgumentCaptor<Set> permsCaptor = ArgumentCaptor.forClass(Set.class);
        verify(fileUtils).toFileAttribute(permsCaptor.capture());
        
        Set<PosixFilePermission> perms = (Set<PosixFilePermission>) permsCaptor.getValue();
        Set<PosixFilePermission> expectedPerms = PosixFilePermissions.fromString("rwxr-xr-x");
        assertEquals(perms, expectedPerms);
        
        verify(fileUtils).createDirectory(socketDirPath, fileAttr);
        
        // Verify the thread to accept connections is started
        verify(acceptThread).start();
    }
    
    @Test
    public void testStartSuccessDirExists() throws Exception {
        when(fileUtils.exists(socketDirPath)).thenReturn(true);
        when(fileUtils.isDirectory(socketDirPath)).thenReturn(true);
        
        transport.start(props);
        
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
            transport.start(props);
            fail("Expected IOException");
        } catch (IOException e) {
            verify(fileUtils, never()).createDirectory(any(Path.class));
            verify(acceptThread, never()).start();
        }
    }
    
    @Test
    public void testStartCreateParent() throws Exception {
        Path sockDirParent = mock(Path.class);
        when(socketDirPath.getParent()).thenReturn(sockDirParent);
        
        transport.start(props);
        verify(fileUtils).createDirectories(sockDirParent);
    }
    
    @Test(expected=IOException.class)
    public void testStartBadOwner() throws Exception {
        UserPrincipal badPrincipal = mock(UserPrincipal.class);
        when(fileUtils.getOwner(socketDirPath)).thenReturn(badPrincipal);
        
        transport.start(props);
    }
    
    @Test(expected=IOException.class)
    public void testStartOwnerCheckUnsupported() throws Exception {
        when(fileUtils.getOwner(socketDirPath)).thenThrow(new UnsupportedOperationException());
        transport.start(props);
    }
    
    @Test(expected=IOException.class)
    public void testStartOwnerCheckNullOwner() throws Exception {
        when(fileUtils.getOwner(socketDirPath)).thenReturn(null);
        transport.start(props);
    }
    
    @Test(expected=IOException.class)
    public void testStartOwnerCheckNullLookup() throws Exception {
        when(lookup.lookupPrincipalByName(USERNAME)).thenReturn(null);
        transport.start(props);
    }
    
    @Test
    public void testStartFailsBadPerm() throws Exception {
        when(fileUtils.exists(socketDirPath)).thenReturn(true);
        when(fileUtils.isDirectory(socketDirPath)).thenReturn(true);
        // Socket directory is world readable
        when(fileUtils.getPosixFilePermissions(socketDirPath)).thenReturn(PosixFilePermissions.fromString("rwxrwxr-x"));
        
        try {
            transport.start(props);
            fail("Expected IOException");
        } catch (IOException e) {
            verify(fileUtils, never()).createDirectory(any(Path.class));
            verify(acceptThread, never()).start();
        }
    }
    
    @Test
    public void testShutdownSuccess() throws Exception {
        transport.start(props);
        mockSocketDirOnShutdown();
        transport.shutdown();
        verify(acceptThread).shutdown();
        verify(channelUtils).closeSelector(selector);
        
        // Check socket directory is removed
        verify(fileUtils).delete(socketPath);
        verify(fileUtils).delete(socketDirPath);
    }
    
    @Test
    public void testShutdownFailure() throws Exception {
        transport.start(props);
        mockSocketDirOnShutdown();
        doThrow(new IOException()).when(acceptThread).shutdown();
        
        try {
            transport.shutdown();
            fail("Expected IO Exception");
        } catch (IOException e) {
            // Socket directory should still be deleted
            verify(fileUtils).delete(socketPath);
            verify(fileUtils).delete(socketDirPath);
        }
    }
    
    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Test
    public void testCreateServer() throws Exception {
        transport.start(props);
        transport.createServer(SERVER_NAME, callbacks);
        
        // Verify the user-specific socket directory is created with the proper permissions
        verify(fileUtils).exists(ownerDirPath);

        ArgumentCaptor<Set> permsCaptor = ArgumentCaptor.forClass(Set.class);
        verify(fileUtils, times(2)).toFileAttribute(permsCaptor.capture());
        
        // First invocation is for top-level socket dir, second is for user-specific dir
        Set<PosixFilePermission> perms = (Set<PosixFilePermission>) permsCaptor.getAllValues().get(1);
        Set<PosixFilePermission> expectedPerms = PosixFilePermissions.fromString("rwx------");
        assertEquals(perms, expectedPerms);
        
        verify(fileUtils).createDirectory(ownerDirPath, fileAttr);
        verify(fileUtils).getPosixFilePermissions(ownerDirPath);
        verify(fileUtils).setOwner(ownerDirPath, currentUser);
        
        verify(fileUtils).exists(socketPath);
        verify(fileUtils, never()).delete(socketPath);
        
        checkChannel();
    }
    
    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Test
    public void testCreateServerWithOwner() throws Exception {
        String otherUsername = "otherUser";
        UserPrincipal owner = mock(UserPrincipal.class);
        when(owner.getName()).thenReturn(otherUsername);
        Path otherOwnerDirPath = mock(Path.class);
        when(socketDirPath.resolve(otherUsername)).thenReturn(otherOwnerDirPath);
        when(fileUtils.getPosixFilePermissions(otherOwnerDirPath)).thenReturn(PosixFilePermissions.fromString("rwx------"));
        when(fileUtils.getOwner(otherOwnerDirPath)).thenReturn(owner);
        
        Path otherSocketPath = mock(Path.class);
        when(fileUtils.getOwner(otherSocketPath)).thenReturn(owner);
        File otherSocketFile = mock(File.class);
        when(props.getSocketFile(SERVER_NAME, otherUsername)).thenReturn(otherSocketFile);
        ThermostatLocalServerSocketChannelImpl otherChannel = mock(ThermostatLocalServerSocketChannelImpl.class);
        when(channelUtils.createServerSocketChannel(SERVER_NAME, otherSocketPath, callbacks, props, selector)).thenReturn(otherChannel);
        when(otherChannel.getSocketFile()).thenReturn(otherSocketFile);
        when(otherSocketFile.toPath()).thenReturn(otherSocketPath);
        
        transport.start(props);
        transport.createServer(SERVER_NAME, callbacks, owner);
        
        // Verify the user-specific socket directory is created with the proper permissions
        verify(fileUtils).exists(otherOwnerDirPath);

        ArgumentCaptor<Set> permsCaptor = ArgumentCaptor.forClass(Set.class);
        verify(fileUtils, times(2)).toFileAttribute(permsCaptor.capture());
        
        // First invocation is for top-level socket dir, second is for user-specific dir
        Set<PosixFilePermission> perms = (Set<PosixFilePermission>) permsCaptor.getAllValues().get(1);
        Set<PosixFilePermission> expectedPerms = PosixFilePermissions.fromString("rwx------");
        assertEquals(perms, expectedPerms);
        
        verify(fileUtils).createDirectory(otherOwnerDirPath, fileAttr);
        verify(fileUtils).getPosixFilePermissions(otherOwnerDirPath);
        verify(fileUtils).setOwner(otherOwnerDirPath, owner);
        
        verify(fileUtils).exists(otherSocketPath);
        verify(fileUtils, never()).delete(otherSocketPath);
        
        verify(channelUtils).createServerSocketChannel(SERVER_NAME, otherSocketPath, callbacks, props, selector);
        verify(fileUtils).setOwner(otherSocketPath, owner);
        ThermostatLocalServerSocketChannelImpl result = transport.getSockets().get(SERVER_NAME);
        assertEquals(otherChannel, result);
    }
    
    @Test
    public void testCreateServerOwnerDirExists() throws Exception {
        transport.start(props);
        
        when(fileUtils.exists(ownerDirPath)).thenReturn(true);
        transport.createServer(SERVER_NAME, callbacks);
        verify(fileUtils, never()).createDirectory(eq(ownerDirPath), any(FileAttribute[].class));
        verify(fileUtils, never()).setOwner(eq(ownerDirPath), any(UserPrincipal.class));
        
        // Should still check permissions
        verify(fileUtils).getPosixFilePermissions(ownerDirPath);
        
        verify(fileUtils).exists(socketPath);
        verify(fileUtils, never()).delete(socketPath);
        
        checkChannel();
    }

    private void checkChannel() throws IOException {
        verify(channelUtils).createServerSocketChannel(SERVER_NAME, socketPath, callbacks, props, selector);
        verify(fileUtils).setOwner(socketPath, currentUser);
        ThermostatLocalServerSocketChannelImpl result = transport.getSockets().get(SERVER_NAME);
        assertEquals(channel, result);
    }
    
    @Test
    public void testCreateServerLeftoverFile() throws Exception {
        transport.start(props);
        when(fileUtils.exists(socketPath)).thenReturn(true);
        transport.createServer(SERVER_NAME, callbacks);
        
        verify(fileUtils).exists(socketPath);
        verify(fileUtils).delete(socketPath);
        
        checkChannel();
    }
    
    @Test(expected=IOException.class)
    public void testCreateServerServerExists() throws Exception {
        ThermostatLocalServerSocketChannelImpl channel = mock(ThermostatLocalServerSocketChannelImpl.class);
        transport.getSockets().put(SERVER_NAME, channel);

        transport.createServer(SERVER_NAME, callbacks);
    }
    
    @Test(expected=IOException.class)
    public void testCreateServerInvalidName() throws Exception {
        when(validator.validate(SERVER_NAME)).thenReturn(false);
        transport.createServer(SERVER_NAME, callbacks);
    }
    
    @Test(expected=IOException.class)
    public void testCreateServerSocketDirPermsChanged() throws Exception {
        transport.start(props);
        when(fileUtils.getPosixFilePermissions(socketDirPath)).thenReturn(PosixFilePermissions.fromString("rwxrwxrwx"));
        transport.createServer(SERVER_NAME, callbacks);
    }
    
    @Test(expected=IOException.class)
    public void testCreateServerOwnerDirPermsChanged() throws Exception {
        transport.start(props);
        when(fileUtils.getPosixFilePermissions(ownerDirPath)).thenReturn(PosixFilePermissions.fromString("rwxrwxrwx"));
        transport.createServer(SERVER_NAME, callbacks);
    }
    
    @Test(expected=IOException.class)
    public void testCreateServerSocketDirOwnerChanged() throws Exception {
        transport.start(props);
        UserPrincipal badPrincipal = mock(UserPrincipal.class);
        when(fileUtils.getOwner(socketDirPath)).thenReturn(badPrincipal);
        transport.createServer(SERVER_NAME, callbacks);
    }
    
    @Test(expected=IOException.class)
    public void testCreateServerOwnerDirOwnerChanged() throws Exception {
        transport.start(props);
        UserPrincipal badPrincipal = mock(UserPrincipal.class);
        when(fileUtils.getOwner(ownerDirPath)).thenReturn(badPrincipal);
        transport.createServer(SERVER_NAME, callbacks);
    }
    
    @Test(expected=IOException.class)
    public void testCreateServerBadSocketOwner() throws Exception {
        transport.start(props);
        UserPrincipal badPrincipal = mock(UserPrincipal.class);
        when(fileUtils.getOwner(socketPath)).thenReturn(badPrincipal);
        transport.createServer(SERVER_NAME, callbacks);
    }
    
    @Test
    public void testServerExists() throws Exception {
        assertFalse(transport.serverExists(SERVER_NAME));
        transport.getSockets().put(SERVER_NAME, channel);
        assertTrue(transport.serverExists(SERVER_NAME));
    }
    
    @Test
    public void testDestroyServer() throws Exception {
        transport.getSockets().put(SERVER_NAME, channel);
        transport.destroyServer(SERVER_NAME);
        
        verify(channel).close();
        verify(fileUtils).delete(socketPath);
    }
    
    @Test(expected=IOException.class)
    public void testDestroyServerNotExist() throws Exception {
        transport.destroyServer(SERVER_NAME);
    }
    
    @Test
    public void testDestroyServerCloseFails() throws Exception {
        doThrow(new IOException()).when(channel).close();
        transport.getSockets().put(SERVER_NAME, channel);
        
        try {
            transport.destroyServer(SERVER_NAME);
            fail("Expected IOException");
        } catch (IOException e) {
            // Socket file should still be deleted
            verify(fileUtils).delete(socketPath);
        }
    }
    
    // Mock a socket directory containing a socket file
    @SuppressWarnings("unchecked")
    private void mockSocketDirOnShutdown() throws IOException {
        when(fileUtils.exists(socketDirPath)).thenReturn(true);
        when(fileUtils.walkFileTree(eq(socketDirPath), anySetOf(FileVisitOption.class), eq(2), any(FileVisitor.class))).thenAnswer(new Answer<Path>() {
            @Override
            public Path answer(InvocationOnMock invocation) throws Throwable {
                FileVisitor<? super Path> visitor = (FileVisitor<? super Path>) invocation.getArguments()[3];
                // Invoke each of the methods we override once
                visitor.visitFile(socketPath, null);
                visitor.postVisitDirectory(socketDirPath, null);
                return socketDirPath;
            }
        });
    }

}
