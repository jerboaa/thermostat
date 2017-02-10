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

package com.redhat.thermostat.agent.ipc.winpipes.server.internal;

import com.redhat.thermostat.agent.ipc.server.ThermostatIPCCallbacks;
import com.redhat.thermostat.agent.ipc.winpipes.common.internal.WinPipesNativeHelper;
import com.redhat.thermostat.common.utils.LoggingUtils;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.WritableByteChannel;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.logging.Logger;

/**
 * one ClientPipeInstance per expected client
 * Created when pipe is created - the pipe can't handle more clients than there are ClientPipeInstances
 *
 * On a connect, will create a ClientHandler (destroyed on disconnect)
 * On a read or write, will delegate that to the ClientHandler, which will decide if the next action is a read or write
 * Disconnect destroys the active ClientHandler
 *
 * see https://msdn.microsoft.com/en-us/library/windows/desktop/aa365603%28v=vs.85%29.aspx
 * http://www.winsocketdotnetworkprogramming.com/winsock2programming/winsock2advancednamedpipe15.html
 * see http://www.winsocketdotnetworkprogramming.com/winsock2programming/winsock2advancednamedpipe15c.html
 */
class ClientPipeInstance implements WritableByteChannel, PipeManager {

    enum PipeState { UNKNOWN_STATE, CONNECTING_STATE, CONNECTED_STATE, CLOSED_STATE }

    private PipeState state;
    private static final Logger logger = LoggingUtils.getLogger(ClientPipeInstance.class);

    private final ClientPipeInstanceHelper clientHandlerCreator;
    private final ExecutorService execService;
    private final ThermostatIPCCallbacks ipcCallbacks;
    private final String pipeName;
    private final long pipeHandle;

    private ReadPipeImpl readHandler;
    private WritePipeImpl writeHandler;

    ClientPipeInstance(final String name, int instances, int bufsize, ExecutorService execService, ThermostatIPCCallbacks cb) throws IOException {
        this(name, instances, bufsize, execService, cb, new ClientPipeInstanceHelper());
    }

    ClientPipeInstance(final String name, int instances, int bufsize, ExecutorService execService, ThermostatIPCCallbacks cb, ClientPipeInstanceHelper cpiHelper) throws IOException {

        this.state = PipeState.UNKNOWN_STATE;
        this.pipeName = name;
        this.execService = execService;
        this.ipcCallbacks = cb;
        this.clientHandlerCreator = cpiHelper;
        this.pipeHandle = clientHandlerCreator.createNamedPipe(name, instances, bufsize);
        if (this.pipeHandle == 0) {
            throw new IOException("can't create Windows named pipe " + name + " err=" + clientHandlerCreator.getLastError());
        }

        this.readHandler = new ReadPipeImpl(this, name, pipeHandle, bufsize);
        this.writeHandler = new WritePipeImpl(this, name, pipeHandle, bufsize);
    }

    public String toString() {
        return "ClientPipeInstance(t=" + Thread.currentThread().getId() + "hnd=" + pipeHandle + ", read=" + readHandler + " write=" + writeHandler +")";
    }

    public String getName() {
        return pipeName;
    }

    WindowsEventSelector.EventHandler getReadHandler() {
        return readHandler;
    }

    WindowsEventSelector.EventHandler getWriteHandler() {
        return writeHandler;
    }

    // for WritableByteChannel
    @Override
    public boolean isOpen() {
        return state == PipeState.CONNECTED_STATE || state == PipeState.CONNECTING_STATE;
    }

    @Override
    public void close() throws IOException {
        if (state != PipeState.CLOSED_STATE) {
            logger.finest("closing " + this);
            readHandler.close();
            writeHandler.close();
            clientHandlerCreator.closeHandle(pipeHandle);
            state = PipeState.CLOSED_STATE;
        }
    }

    @Override
    public int write(ByteBuffer src) throws IOException {
        return writeHandler.write(src);
    }

    /**
     * Wait for a client to connect to this pipe
     * Since we created the pipe in blocking mode, this call will block.
     */
    boolean connectToNewClient() throws IOException {
        state = PipeState.CONNECTING_STATE;
        final boolean ret = readHandler.connectToNewClient();
        state = readHandler.getReadState() == ReadPipeImpl.ReadPipeState.READING_STATE ? PipeState.CONNECTED_STATE : PipeState.CONNECTING_STATE;
        return ret;
    }

    private void disconnect() throws IOException {
        logger.finest("WinPipe disconnect() " + this);
        if (!clientHandlerCreator.disconnectNamedPipe(pipeHandle)) {
            throw new IOException("could not disconnect named pipe");
        }
    }

    @Override
    public void resetPipe() throws IOException {
        logger.finest("WinPipe resetPipe() " + this);
        disconnect();
        state = PipeState.UNKNOWN_STATE;
        connectToNewClient();
    }

    @Override
    public ClientHandler handleNewClientConnection() {
        return this.clientHandlerCreator.createClientHandler(this, execService, ipcCallbacks);
    }

    // for testing
    static class ClientPipeInstanceHelper {

        private static WinPipesNativeHelper helper = WinPipesNativeHelper.INSTANCE;

        ClientHandler createClientHandler(ClientPipeInstance pi, ExecutorService execService, ThermostatIPCCallbacks callbacks) {
            return new ClientHandler(pi, execService, callbacks);
        }

        long createNamedPipe(final String pipeName, int instances, int buffersize) {
            return helper.createNamedPipe(pipeName, instances, buffersize);
        }

        boolean disconnectNamedPipe(long pipeHandle) {
            return helper.disconnectNamedPipe(pipeHandle);
        }

        boolean closeHandle(long handle) {
            return helper.closeHandle(handle);
        }

        int getLastError() {
            return helper.getLastError();
        }
    }
}
