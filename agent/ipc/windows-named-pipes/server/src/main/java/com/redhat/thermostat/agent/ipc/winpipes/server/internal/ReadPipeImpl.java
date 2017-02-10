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

import com.redhat.thermostat.agent.ipc.winpipes.common.internal.WinPipesNativeHelper;
import com.redhat.thermostat.common.utils.LoggingUtils;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.logging.Logger;

/**
 * Handle all read operations on this pipe
 */
class ReadPipeImpl implements WindowsEventSelector.EventHandler {

    private static final Logger logger = LoggingUtils.getLogger(ReadPipeImpl.class);
    private static WinPipesNativeHelper helper = WinPipesNativeHelper.INSTANCE;

    enum ReadPipeState { UNKNOWN_STATE, CONNECTING_STATE, READING_STATE, ERROR_STATE, CLOSED_STATE }

    private ReadPipeState readState;

    private final String pipeName;
    private final PipeManager manager;
    private final long pipeHandle;
    private final long readEventHandle;
    private final ByteBuffer readOverlap;
    private final ByteBuffer readBuffer;
    private ClientHandler clientHandler;

    ReadPipeImpl(PipeManager manager, String pipeName, long pipeHandle, int bufsize) throws IOException {
        this.manager = manager;
        this.pipeName = pipeName;
        this.readState = ReadPipeState.UNKNOWN_STATE;
        this.pipeHandle = pipeHandle;
        this.readEventHandle = helper.createEvent();
        if (this.readEventHandle == WinPipesNativeHelper.INVALID_HANDLE) {
            throw new IOException("can't create a Windows event" + " err=" + helper.getLastError());
        }
        this.readOverlap = helper.createDirectOverlapStruct(readEventHandle);
        this.readBuffer = helper.createDirectBuffer(bufsize);
    }

    public String toString() {
        return "ReadPipeImpl(h=" + pipeHandle + " '" + pipeName + "' " + readState + ")";
    }

    ReadPipeState getReadState() {
        return readState;
    }

    @Override
    public long getHandle() {
        return readEventHandle;
    }

    @Override
    public void processEvent() throws IOException {
        if (handlePendingRead()) {
            enqueueRead();
        }
    }

    public void close() throws IOException {
        readState = ReadPipeState.CLOSED_STATE;
        helper.cancelAllIo(pipeHandle, readOverlap);
        helper.freeDirectBuffer(readOverlap);
        helper.freeDirectBuffer(readBuffer);
        helper.closeHandle(readEventHandle);
    }

    /**
     * Wait for a client to connect to this pipe
     * Since we created the pipe in blocking mode, this call will block.
     */
    boolean connectToNewClient() throws IOException {

        logger.info("connectToNewClient - entered " + this);
        final int ret = helper.connectNamedPipe(pipeHandle, readOverlap);
        logger.info("connectToNewClient on " + this + " returns " + ret);
        if (ret == WinPipesNativeHelper.ERROR_IO_PENDING) {
            readState = ReadPipeState.CONNECTING_STATE;
        } else if (ret == WinPipesNativeHelper.ERROR_SUCCESS || ret == WinPipesNativeHelper.ERROR_PIPE_CONNECTED) {
            // if it's not pending, and no exception was thrown, then we must be connected
            logger.info("connectToNewClient switching to READING_STATE");
            helper.resetEvent(readEventHandle);
            clientHandler = manager.handleNewClientConnection();
            readState = ReadPipeState.READING_STATE;
        } else {
            throw new IOException("connectNamedPipe(" + pipeName + ") returns err=" + ret);
        }
        logger.info("connectToNewClient - exitting " + this);
        return readState == ReadPipeState.CONNECTING_STATE;
    }

    /**
     * process the incoming read data
     * - read all data until there's nothing left
     * - if there's more data expected for the current message, then enqueue a read.
     * @throws IOException if there's an i/o or protocol error
     */
    private void enqueueRead() throws IOException {
        logger.finest("enqueueRead() - entered " + this);
        readBuffer.position(0);
        readBuffer.limit(readBuffer.capacity());
        logger.finest("enqueueRead() calling readFileOverlapped(" + this + ")");
        final boolean ret = helper.readFileOverlapped(pipeHandle, readOverlap, readBuffer);
        final int err = ret ? 0 : helper.getLastError();
        logger.finest("enqueueRead() readFileOverlapped() returns " + ret + " err=" + err);
        if (ret || (err == WinPipesNativeHelper.ERROR_SUCCESS) || err == WinPipesNativeHelper.ERROR_IO_PENDING) {
            readState = ReadPipeState.READING_STATE;
        } else if (err == WinPipesNativeHelper.ERROR_BROKEN_PIPE) {
            // the other end closed the pipe
            readState = ReadPipeState.CLOSED_STATE;
            manager.resetPipe();
        } else {
            readState = ReadPipeState.ERROR_STATE;
            manager.resetPipe();
        }
        logger.finest("enqueueRead() - exiting " + this);
    }

    /**
     * handlePendingIO - if there is pending I/O then retrieve and process the result
     *
     * @return true if queueNextOperation() should be called, false otherwise
     * @throws IOException if there were any errors interacting with the pipe
     */
    private boolean handlePendingRead() throws IOException {
        logger.finest("handlePendingRead() - entered " + this);
        if (readState == ReadPipeState.READING_STATE) {
            logger.finest("handlePendingRead() waiting for overlapped result on " + this + " state=" + readState);
            final int bytesRead = helper.getOverlappedResult(pipeHandle, readOverlap, false);
            final int err = helper.getLastError();
            logger.finest("handlePendingRead() got overlapped result (bytes=" + bytesRead + " on " + this + " err=" + err);

            switch (readState) {
                case CONNECTING_STATE:
                    if (bytesRead < 0) {
                        final String msg = "Error reading pipe " + pipeName + " err=" + err;
                        logger.warning(msg);
                        throw new IOException(msg);
                    }
                    clientHandler = manager.handleNewClientConnection();
                    readState = ReadPipeState.READING_STATE;
                    break;
                case READING_STATE:
                    if (bytesRead < 0) {
                        readState = ReadPipeState.ERROR_STATE;
                        manager.resetPipe();
                        return false;
                    }
                    readBuffer.limit(bytesRead);
                        /*final boolean readFully =*/ clientHandler.handleRead(readBuffer);
                    // there may or may not be messages on the write queue at this point
                    // if readFully is true, then there will be at some point *there may be some already, and we need to wait for them.
                    readState = ReadPipeState.READING_STATE;
                    break;
                default:
                    throw new IOException("Invalid pipe state " + readState);
            }
        }
        logger.finest("handlePendingRead() - exited " + this);
        return true;
    }
}
