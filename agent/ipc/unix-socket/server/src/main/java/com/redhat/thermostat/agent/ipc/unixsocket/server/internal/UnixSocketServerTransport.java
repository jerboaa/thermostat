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

package com.redhat.thermostat.agent.ipc.unixsocket.server.internal;

import java.io.File;
import java.io.IOException;
import java.nio.channels.Selector;
import java.nio.channels.spi.SelectorProvider;
import java.nio.file.FileVisitOption;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.nio.file.attribute.UserPrincipal;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

import com.redhat.thermostat.agent.ipc.common.internal.IPCProperties;
import com.redhat.thermostat.agent.ipc.common.internal.IPCType;
import com.redhat.thermostat.agent.ipc.server.ServerTransport;
import com.redhat.thermostat.agent.ipc.server.ThermostatIPCCallbacks;
import com.redhat.thermostat.agent.ipc.unixsocket.common.internal.UnixSocketIPCProperties;
import com.redhat.thermostat.agent.ipc.unixsocket.common.internal.UserPrincipalUtils;
import com.redhat.thermostat.common.utils.LoggingUtils;

class UnixSocketServerTransport implements ServerTransport {
    
    private static final Logger logger = LoggingUtils.getLogger(UnixSocketServerTransport.class);
    // Permissions for the top-level sockets directory
    private static final Set<PosixFilePermission> SOCKET_DIR_PERM;
    static {
        Set<PosixFilePermission> perms = new HashSet<>();
        perms.add(PosixFilePermission.OWNER_READ);
        perms.add(PosixFilePermission.OWNER_WRITE);
        perms.add(PosixFilePermission.OWNER_EXECUTE);
        perms.add(PosixFilePermission.GROUP_READ);
        perms.add(PosixFilePermission.GROUP_EXECUTE);
        perms.add(PosixFilePermission.OTHERS_READ);
        perms.add(PosixFilePermission.OTHERS_EXECUTE);
        SOCKET_DIR_PERM = Collections.unmodifiableSet(perms);
    }
    // Permissions for the user-specific socket directories, under the top-level directory
    private static final Set<PosixFilePermission> OWNER_DIR_PERM;
    static {
        Set<PosixFilePermission> perms = new HashSet<>();
        perms.add(PosixFilePermission.OWNER_READ);
        perms.add(PosixFilePermission.OWNER_WRITE);
        perms.add(PosixFilePermission.OWNER_EXECUTE);
        OWNER_DIR_PERM = Collections.unmodifiableSet(perms);
    }
    
    private final SelectorProvider selectorProvider;
    // Access/modification of this field should by synchronized
    private final Map<String, ThermostatLocalServerSocketChannelImpl> sockets;
    private final FilenameValidator validator;
    private final ExecutorService execService;
    private final FileUtils fileUtils;
    private final ChannelUtils channelUtils;
    private final ThreadCreator threadCreator;
    private final UserPrincipalUtils userUtils;
    
    private UnixSocketIPCProperties props;
    private AcceptThread acceptThread;
    private Selector selector;
    private Path socketDir;
    private UserPrincipal currentUser;
    
    UnixSocketServerTransport(SelectorProvider selectorProvider) {
        this(selectorProvider, Executors.newFixedThreadPool(determineDefaultThreadPoolSize(), new CountingThreadFactory()), 
                new FilenameValidator(), new FileUtils(), new ThreadCreator(), new ChannelUtils(), new UserPrincipalUtils());
    }
    
    UnixSocketServerTransport(SelectorProvider selectorProvider, ExecutorService execService, FilenameValidator validator, 
            FileUtils fileUtils, ThreadCreator threadCreator, ChannelUtils channelCreator, UserPrincipalUtils userUtils) {
        this.selectorProvider = selectorProvider;
        this.sockets = new HashMap<>();
        this.validator = validator;
        this.execService = execService;
        this.fileUtils = fileUtils;
        this.channelUtils = channelCreator;
        this.threadCreator = threadCreator;
        this.userUtils = userUtils;
    }
    
    @Override
    public void start(IPCProperties props) throws IOException {
        if (!(props instanceof UnixSocketIPCProperties)) {
            IPCType type = props.getType();
            throw new IOException("Unsupported IPC type: " + type.getConfigValue());
        }
        this.props = (UnixSocketIPCProperties) props;
        
        // Get UserPrincipal for currently logged-in user
        this.currentUser = userUtils.getCurrentUser();
        
        // Prepare socket directory with strict permissions, which will contain the socket file when bound
        File sockDirFile = ((UnixSocketIPCProperties) props).getSocketDirectory();
        this.socketDir = createSocketDirPath(sockDirFile);
        checkSocketDir();
        
        // Open the Selector and start accepting connections
        this.selector = selectorProvider.openSelector();
        this.acceptThread = threadCreator.createAcceptThread(selector, execService);
        acceptThread.start();
        logger.info("Agent IPC service started");
    }
    
    @Override
    public IPCType getType() {
        return IPCType.UNIX_SOCKET;
    }
    
    private Path createSocketDirPath(File sockDirFile) throws IOException {
        try {
            // Make absolute and remove redundant elements
            return sockDirFile.toPath().toAbsolutePath().normalize();
        } catch (InvalidPathException e) {
            throw new IOException("Invalid socket directory path", e);
        }
    }

    private Path getPathToServer(String name, UserPrincipal owner) throws IOException {
        checkName(name);
        String ownerName = owner.getName();
        return props.getSocketFile(name, ownerName).toPath();
    }

    private void checkName(String name) throws IOException {
        Objects.requireNonNull(name, "Server name cannot be null");
        if (name.isEmpty()) {
            throw new IOException("Server name cannot be empty");
        }
        // Require limited character set for name
        boolean okay = validator.validate(name);
        if (!okay) {
            throw new IOException("Illegal server name");
        }
    }

    @Override
    public synchronized void createServer(String name, ThermostatIPCCallbacks callbacks) throws IOException {
        createServer(name, callbacks, currentUser);
    }
    
    public synchronized void createServer(String name, ThermostatIPCCallbacks callbacks, UserPrincipal owner) throws IOException {
        // Check if the socket has already been created and we know about it
        if (sockets.containsKey(name)) {
            throw new IOException("IPC server with name \"" + name + "\" already exists");
        }

        // Check for existing socket
        Path socketPath = getPathToServer(name, owner);
        if (fileUtils.exists(socketPath)) {
            // Must have been left behind, so delete before attempting to bind
            fileUtils.delete(socketPath);
        }
        
        // Check that socket directory permissions haven't changed
        if (!permissionsMatch(socketDir, SOCKET_DIR_PERM)) {
            throw new IOException("Socket directory permissions are insecure");
        }
        checkOwner(socketDir, "Socket directory");
        
        // Check if owner subdirectory exists, create it if not
        String ownerName = owner.getName(); // TODO uid would be better for directory name to keep paths short
        Path ownerDir = socketDir.resolve(ownerName);
        if (!fileUtils.exists(ownerDir)) {
            fileUtils.createDirectory(ownerDir, fileUtils.toFileAttribute(OWNER_DIR_PERM));
            fileUtils.setOwner(ownerDir, owner);
        }
        // Check the permissions are what we expect
        if (!permissionsMatch(ownerDir, OWNER_DIR_PERM)) {
            throw new IOException("Socket directory permissions are insecure for user: " + ownerName);
        }
        checkOwner(ownerDir, owner, "User-specific socket directory");
        
        // Create socket
        ThermostatLocalServerSocketChannelImpl socket = 
                channelUtils.createServerSocketChannel(name, socketPath, callbacks, props, selector);
        
        // Verify owner of new socket file
        File socketFile = socket.getSocketFile();
        fileUtils.setOwner(socketFile.toPath(), owner);
        checkOwner(socketFile.toPath(), owner, "Socket file " + socketFile.getName());
        sockets.put(name, socket);
    }
    
    private void checkSocketDir() throws IOException {
        if (!fileUtils.exists(socketDir)) {
            // Socket directory doesn't exist, so create it
            prepareSocketDir(socketDir);
        } else if (!fileUtils.isDirectory(socketDir)) {
            throw new IOException("Socket directory exists, but is not a directory");
        } else if (!permissionsMatch(socketDir, SOCKET_DIR_PERM)) {
            throw new IOException("Socket directory has incorrect permissions");
        } // else -> socket directory exists and is valid
        
        // Verify correct ownership
        checkOwner(socketDir, "Socket directory");
        logger.fine("Using Unix socket directory: " + socketDir.toString());
    }
    
    private boolean permissionsMatch(Path path, Set<PosixFilePermission> permissions) throws IOException {
        Set<PosixFilePermission> actualPerms = fileUtils.getPosixFilePermissions(path);
        return permissions.equals(actualPerms);
    }
    
    private void checkOwner(Path path, String errorMessagePrefix) throws IOException {
        checkOwner(path, currentUser, errorMessagePrefix);
    }
    
    private void checkOwner(Path path, UserPrincipal expectedOwner, String errorMessagePrefix) throws IOException {
        try {
            UserPrincipal owner = fileUtils.getOwner(path);
            if (owner == null) {
                throw new IOException("Unable to determine owner for path: " + path.toString());
            } else if (!owner.equals(expectedOwner)) {
                throw new IOException(errorMessagePrefix + " insecure with owner: " + owner.getName());
            }
        } catch (UnsupportedOperationException e) {
            throw new IOException("Cannot determine owner from file system", e);
        }
    }
    
    private void prepareSocketDir(Path path) throws IOException {
        Path parent = path.getParent();
        if (parent != null) {
            // Create parent directories
            fileUtils.createDirectories(parent);
        }
        // Create socket directory with 700 permissions
        fileUtils.createDirectory(path, fileUtils.toFileAttribute(SOCKET_DIR_PERM));
    }
    
    private void deleteSocketDir(Path path) throws IOException {
        if (fileUtils.exists(path)) {
            // Empty sockets directory
            int maxDepth = 2; // top-level socket directory + user-specific subdirectories
            fileUtils.walkFileTree(path, EnumSet.noneOf(FileVisitOption.class), maxDepth, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file,
                        BasicFileAttributes attrs) throws IOException {
                    fileUtils.delete(file);
                    return FileVisitResult.CONTINUE;
                }
                @Override
                public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                    if (exc == null) {
                        fileUtils.delete(dir);
                        return FileVisitResult.CONTINUE;
                    } else {
                        // Exception occurred, abort cleanup
                        throw exc;
                    }
                }
            });
        }
    }
    
    @Override
    public synchronized boolean serverExists(String name) throws IOException {
        return sockets.containsKey(name);
    }     
    
    @Override
    public synchronized void destroyServer(String name) throws IOException {
        if (!sockets.containsKey(name)) {
            throw new IOException("IPC server with name \"" + name + "\" does not exist");
        }
        // Remove socket from known sockets
        ThermostatLocalServerSocketChannelImpl socket = sockets.remove(name);
        
        try {
            // Close socket
            socket.close();
        } finally {
            // Delete socket file
            Path socketPath = socket.getSocketFile().toPath();
            fileUtils.delete(socketPath);
        }
    }

    @Override
    public void shutdown() throws IOException {
        try {
            // Stop accepting connections and close selector afterward
            acceptThread.shutdown();
            channelUtils.closeSelector(selector);
            logger.info("Agent IPC service stopped");
        } finally {
            deleteSocketDir(socketDir);
        }
    }
    
    private static int determineDefaultThreadPoolSize() {
        // Make the number of default thread pool size a function of available
        // processors.
        return Runtime.getRuntime().availableProcessors() * 2;
    }
    
    private static class CountingThreadFactory implements ThreadFactory {
        
        private final AtomicInteger threadCount;
        
        private CountingThreadFactory() {
            this.threadCount = new AtomicInteger();
        }
        
        @Override
        public Thread newThread(Runnable r) {
            // Create threads with a recognizable name
            return new Thread(r, "AcceptThread-" + threadCount.incrementAndGet());
        }
        
    }
    

    /* For testing purposes */
    Path getSocketDirPath() {
        return socketDir;
    }
    
    /* For testing purposes */
    Map<String, ThermostatLocalServerSocketChannelImpl> getSockets() {
        return sockets;
    }
    
    /* For testing purposes */
    static class FileUtils {
        
        boolean exists(Path path) {
            return Files.exists(path);
        }
        
        void delete(Path path) throws IOException {
            Files.delete(path);
        }
        
        boolean isDirectory(Path path) {
            return Files.isDirectory(path);
        }
        
        Path createDirectory(Path dir, FileAttribute<?>... attrs) throws IOException {
            return Files.createDirectory(dir, attrs);
        }
        
        Path createDirectories(Path dir, FileAttribute<?>... attrs) throws IOException {
            return Files.createDirectories(dir, attrs);
        }
        
        Set<PosixFilePermission> getPosixFilePermissions(Path path) throws IOException {
            return Files.getPosixFilePermissions(path);
        }
        
        FileAttribute<Set<PosixFilePermission>> toFileAttribute(Set<PosixFilePermission> perms) {
            return PosixFilePermissions.asFileAttribute(perms);
        }
        
        Path walkFileTree(Path start, Set<FileVisitOption> options, int maxDepth, 
                FileVisitor<? super Path> visitor) throws IOException {
            return Files.walkFileTree(start, options, maxDepth, visitor);
        }
        
        UserPrincipal getOwner(Path path) throws IOException {
            return Files.getOwner(path);
        }
        
        Path setOwner(Path path, UserPrincipal owner) throws IOException {
            return Files.setOwner(path, owner);
        }
    }
    
    /* For testing purposes */
    static class ThreadCreator {
        AcceptThread createAcceptThread(Selector selector, ExecutorService execService) {
            return new AcceptThread(selector, execService);
        }
    }
    
    /* For testing purposes */
    static class ChannelUtils {
        ThermostatLocalServerSocketChannelImpl createServerSocketChannel(String name, Path socketPath, 
                ThermostatIPCCallbacks callbacks, IPCProperties props, Selector selector) throws IOException {
            return ThermostatLocalServerSocketChannelImpl.open(name, socketPath.toFile(), callbacks, props, selector);
        }
        void closeSelector(Selector selector) throws IOException {
            selector.close();
        }
    }
    
    /* For testing purposes */
    static class ProcessCreator {
        Process startProcess(ProcessBuilder builder) throws IOException {
            return builder.start();
        }
    }
}
