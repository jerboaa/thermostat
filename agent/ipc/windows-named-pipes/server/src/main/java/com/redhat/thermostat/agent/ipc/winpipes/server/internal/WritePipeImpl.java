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
import java.util.ArrayDeque;
import java.util.Queue;
import java.util.logging.Logger;

class WritePipeImpl implements WindowsEventSelector.EventHandler {

    private static final Logger logger = LoggingUtils.getLogger(WritePipeImpl.class);
    private static WinPipesNativeHelper helper = WinPipesNativeHelper.INSTANCE;

    enum WritePipeState { QUIET_STATE, WRITING_STATE, FLUSHING_WRITE, ERROR_STATE, CLOSED_STATE }

    private WritePipeState writeState;

    private final PipeManager manager;
    private final String pipeName;
    private final long pipeHandle;
    private final Queue<ByteBuffer> writeQueue;
    private final long writeEventHandle;
    private final ByteBuffer writeOverlap;
    private final ByteBuffer writeBuffer;

    WritePipeImpl(PipeManager manager, String pipeName, long pipeHandle, int bufsize) throws IOException {
        this.manager = manager;
        this.pipeName = pipeName;
        this.writeState = WritePipeState.QUIET_STATE;
        this.pipeHandle = pipeHandle;
        this.writeEventHandle = helper.createEvent();
        if (this.writeEventHandle == 0) {
            throw new IOException(this.pipeName + ": can't create a Windows event" + " err=" + helper.getLastError());
        }
        this.writeQueue = new ArrayDeque<>();

        this.writeOverlap = helper.createDirectOverlapStruct(writeEventHandle);
        this.writeBuffer = helper.createDirectBuffer(bufsize);
    }

    public String toString() {
        return "WritePipeImpl(h=" + pipeHandle + " '" + pipeName + "' " + writeState + " q=" + writeQueue.size() + ")";
    }

    @Override
    public long getHandle() {
        return writeEventHandle;
    }

    @Override
    public void processEvent() throws IOException {
        if (handlePendingWrite()) {
            enqueueNextOperation();
        }
    }

    public int write(ByteBuffer src) throws IOException {

        // this call adds buffers to a FIFO queue.
        // if no writes are in progress, this call will kickstart a write on thr front of the queue
        logger.finest("write() - entered " + this + " bytes=" + src.remaining());
        synchronized (writeQueue) {
            writeQueue.add(src);
            logger.finest("write() - adding to writeQueue (new size= " + writeQueue.size() + ") bytes=" + src.remaining());
        }
        if (writeState == WritePipeState.QUIET_STATE) {
            helper.setEvent(writeEventHandle);
        }
        logger.finest("write() - exited " + src.remaining() + " " + this);
        return src.remaining();
    }

    public void close() throws IOException {
        writeState = WritePipeState.CLOSED_STATE;
        helper.cancelAllIo(pipeHandle, writeOverlap);
        helper.freeDirectBuffer(writeOverlap);
        helper.freeDirectBuffer(writeBuffer);
        helper.closeHandle(writeEventHandle);
    }

    private void enqueueNextOperation() throws IOException {
        if (writeState == WritePipeState.QUIET_STATE)
            enqueueWrite();
    }

    /**
     * handlePendingIO - if there is pending I/O then retrieve and process the result
     * It's possible the even was raised with no pending IO
     *
     * @return true if queueNextOperation() should be called, false otherwise
     * @throws IOException if there were any errors interacting with the pipe
     */
    private boolean handlePendingWrite() throws IOException {
        logger.finest("handlePendingWrite() - entered " + this);
        if (writeState != WritePipeState.QUIET_STATE) {
            logger.finest("handlePendingWrite() waiting for overlapped result on " + this + " state=" + writeState);
            final int bytesWritten = helper.getOverlappedResult(pipeHandle, writeOverlap, false);
            final int err = helper.getLastError();
            logger.finest("handlePendingWrite() got overlapped result (bytes=" + bytesWritten + " on " + this + " err=" + err);

            switch (writeState) {
                case WRITING_STATE:
                case FLUSHING_WRITE:
                    if (bytesWritten != writeBuffer.remaining()) {
                        writeState = WritePipeState.ERROR_STATE;
                        manager.resetPipe();
                        return false;
                    }
                    writeBuffer.position(0);
                    writeBuffer.limit(0);
                    break;
                default:
                    throw new IOException("Invalid pipe state " + writeState);
            }
        }
        this.writeState = WritePipeState.QUIET_STATE;
        logger.finest("handlePendingWrite() - exited " + this);
        return true;
    }


    /**
     * enqueue a write, if there's any unwritten data in the writebuffer, or in the write queue
     * @return true if an operation was enqueued
     * @throws IOException if an IO error occurred
     */
    private boolean enqueueWrite() throws IOException {
        logger.finest("enqueueWrite() - entered " + this);
        if (writeBuffer.remaining() == 0 && writeQueue.isEmpty()) {
            if (writeState == WritePipeState.FLUSHING_WRITE) {
                // all the data that's ever going to be on that queue has been fully written
                writeState = WritePipeState.QUIET_STATE;
                logger.finest("enqueueWrite() - exited true " + this);
                return true;
            }
            logger.finest("enqueueWrite() nothing to write - sleeping for a bit");
            helper.resetEvent(writeEventHandle);
            logger.finest("enqueueWrite() - exited false " + this);
            return false;
        }
        synchronized (this.writeQueue) {
            if (writeBuffer.remaining() == 0) {
                writeBuffer.limit(writeBuffer.capacity());
                writeBuffer.put(writeQueue.remove());
                logger.finest("enqueueWrite() - grabbing next buffer from queue (new queue size=" + writeQueue.size() + ")");
                writeBuffer.flip();
            }
        }
        logger.finest("enqueueWrite() - start overlapped writing " + this + " bytes=" + writeBuffer.remaining());
        final boolean ret = helper.writeFileOverlapped(pipeHandle, writeOverlap, writeBuffer);
        writeState = WritePipeState.WRITING_STATE;
        final int err = ret ? 0 : helper.getLastError();
        logger.finest("enqueueWrite() - finished overlapped writing " + this + " bytes written=" + ret + " err=" + err);
        if (!ret && err != 0 && err != 997) {
            writeState = WritePipeState.ERROR_STATE;
            manager.resetPipe();
        }

        logger.finest("enqueueWrite() - exited " + this);
        return writeState != WritePipeState.QUIET_STATE;
    }
}
