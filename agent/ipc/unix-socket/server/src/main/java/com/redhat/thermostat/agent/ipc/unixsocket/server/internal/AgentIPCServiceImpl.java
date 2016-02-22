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

import java.io.IOException;
import java.nio.channels.Selector;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Collections;
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
import com.redhat.thermostat.agent.ipc.server.AgentIPCService;
import com.redhat.thermostat.agent.ipc.server.ThermostatIPCCallbacks;
import com.redhat.thermostat.agent.ipc.unixsocket.common.internal.UnixSocketIPCProperties;
import com.redhat.thermostat.common.utils.LoggingUtils;

class AgentIPCServiceImpl implements AgentIPCService {
    
    private static final Logger logger = LoggingUtils.getLogger(AgentIPCServiceImpl.class);
    // Filename prefix for socket file
    static final String SOCKET_PREFIX = "sock-";
    // Client environment variable containing path to socket directory
    static final String ENVVAR_SOCKET_DIR = "__THERMOSTAT_IPC_SOCKET_DIR";
    // Permissions to allow only the owner and group access to the directory
    private static final Set<PosixFilePermission> SOCKET_DIR_PERM;
    static {
        Set<PosixFilePermission> perms = new HashSet<>();
        perms.add(PosixFilePermission.OWNER_READ);
        perms.add(PosixFilePermission.OWNER_WRITE);
        perms.add(PosixFilePermission.OWNER_EXECUTE);
        perms.add(PosixFilePermission.GROUP_READ);
        perms.add(PosixFilePermission.GROUP_WRITE);
        perms.add(PosixFilePermission.GROUP_EXECUTE);
        SOCKET_DIR_PERM = Collections.unmodifiableSet(perms);
    }
    
    private final Selector selector;
    private final Path socketDir;
    private final AcceptThread acceptThread;
    // Access/modification of this field should by synchronized
    private final Map<String, ThermostatLocalServerSocketChannelImpl> sockets;
    private final FilenameValidator validator;
    private final FileUtils fileUtils;
    private final ChannelCreator channelCreator;
    
    AgentIPCServiceImpl(Selector selector, IPCProperties props) throws IOException {
        this(selector, props, Executors.newFixedThreadPool(determineDefaultThreadPoolSize(), new CountingThreadFactory()), 
                new FilenameValidator(), new FileUtils(), new ThreadCreator(), new ChannelCreator());
    }
    
    AgentIPCServiceImpl(Selector selector, IPCProperties props, ExecutorService execService, 
            FilenameValidator validator, FileUtils fileUtils, ThreadCreator threadCreator,
            ChannelCreator channelCreator) throws IOException {
        this.selector = selector;
        this.sockets = new HashMap<>();
        this.validator = validator;
        this.fileUtils = fileUtils;
        this.channelCreator = channelCreator;
        
        if (!(props instanceof UnixSocketIPCProperties)) {
            IPCType type = props.getType();
            throw new IOException("Unsupported IPC type: " + type.getConfigValue());
        }
        this.socketDir = ((UnixSocketIPCProperties) props).getSocketDirectory().toPath();
        
        this.acceptThread = threadCreator.createAcceptThread(selector, execService);
    }
    
    void start() throws IOException {
        // Prepare socket directory with strict permissions, which will contain the socket file when bound
        checkSocketDir();
        acceptThread.start();
        logger.info("Agent IPC service started");
    }

    private Path getPathToServer(String name) throws IOException {
        checkName(name);
        return socketDir.resolve(SOCKET_PREFIX + name);
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
        // Check if the socket has already been created and we know about it
        if (sockets.containsKey(name)) {
            throw new IOException("IPC server with name \"" + name + "\" already exists");
        }

        // Create socket
        Path socketPath = getPathToServer(name);
        if (fileUtils.exists(socketPath)) {
            // Must have been left behind, so delete before attempting to bind
            fileUtils.delete(socketPath);
        }
        ThermostatLocalServerSocketChannelImpl socket = 
                channelCreator.createServerSocketChannel(name, socketPath, callbacks, selector);
        sockets.put(name, socket);
    }
    
    private void checkSocketDir() throws IOException {
        if (!fileUtils.exists(socketDir)) {
            // Socket directory doesn't exist, so create it
            prepareSocketDir(socketDir);
        } else if (!fileUtils.isDirectory(socketDir)) {
            throw new IOException("Socket directory exists, but is not a directory");
        } else if (!permissionsMatch(socketDir)) {
            throw new IOException("Socket directory has incorrect permissions");
        } // else -> socket directory exists and is valid
    }
    
    private boolean permissionsMatch(Path path) throws IOException {
        Set<PosixFilePermission> acutalPerms = fileUtils.getPosixFilePermissions(path);
        return SOCKET_DIR_PERM.equals(acutalPerms);
    }

    private void prepareSocketDir(Path path) throws IOException {
        // Create directory with 770 permissions
        fileUtils.createDirectory(path, fileUtils.toFileAttribute(SOCKET_DIR_PERM));
    }
    
    private void deleteSocketDir(Path path) throws IOException {
        if (fileUtils.exists(path)) {
            DirectoryStream<Path> entries = fileUtils.newDirectoryStream(path);
            // Empty directory
            for (Path entry : entries) {
                fileUtils.delete(entry);
            }
            // Delete directory
            fileUtils.delete(path);
        }
    }
    
    @Override
    public synchronized boolean serverExists(String name) {
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

    void shutdown() throws IOException {
        try {
            acceptThread.shutdown();
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
        
        Set<PosixFilePermission> getPosixFilePermissions(Path path) throws IOException {
            return Files.getPosixFilePermissions(path);
        }
        
        FileAttribute<Set<PosixFilePermission>> toFileAttribute(Set<PosixFilePermission> perms) {
            return PosixFilePermissions.asFileAttribute(perms);
        }
        
        DirectoryStream<Path> newDirectoryStream(Path dir) throws IOException {
            return Files.newDirectoryStream(dir);
        }
        
    }
    
    /* For testing purposes */
    static class ThreadCreator {
        AcceptThread createAcceptThread(Selector selector, ExecutorService execService) {
            return new AcceptThread(selector, execService);
        }
    }
    
    /* For testing purposes */
    static class ChannelCreator {
        ThermostatLocalServerSocketChannelImpl createServerSocketChannel(String name, Path socketPath, 
                ThermostatIPCCallbacks callbacks, Selector selector) throws IOException {
            return ThermostatLocalServerSocketChannelImpl.open(name, socketPath.toFile(), callbacks, selector);
        }
    }
    
    /* For testing purposes */
    static class ProcessCreator {
        Process startProcess(ProcessBuilder builder) throws IOException {
            return builder.start();
        }
    }
}
