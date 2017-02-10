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

package com.redhat.thermostat.agent.ipc.winpipes.common.internal;

import com.redhat.thermostat.shared.config.NativeLibraryResolver;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * Wrapper for Windows native methods pertaining to named pipes
 */
public class WinPipesNativeHelper {

    public static WinPipesNativeHelper INSTANCE = new WinPipesNativeHelper();

    static {
        String lib = NativeLibraryResolver.getAbsoluteLibraryPath("WinPipesNativeWrapper");
        System.load(lib);
    }

    // windows constants
    public static final long WAIT_OBJECT_0 = getConstantWaitObject0();
    public static final long INVALID_HANDLE = 0;
    public static final long INFINITE = getConstantInfinite0();

    // windows 'error' codes
    public static final int ERROR_SUCCESS = 0;
    public static final int ERROR_IO_PENDING = getConstantErrorIOPending0();
    public static final int ERROR_IO_INCOMPLETE = getConstantErrorIOIncomplete0();
    public static final int ERROR_HANDLE_EOF = getConstantErrorHandleEOF0();
    public static final int ERROR_MORE_DATA = getConstantErrorMoreData0();
    public static final int ERROR_PIPE_BUSY = getConstantErrorPipeBusy0();
    public static final int ERROR_BROKEN_PIPE = getConstantErrorBrokenPipe0();
    public static final int INVALID_HANDLE_VALUE = getConstantInvalidHandle0();
    public static final int ERROR_PIPE_CONNECTED = getConstantErrorPipeConnected0();

    private WinPipesNativeHelper() {
    }

    public ByteBuffer createDirectBuffer(int buffersize) {
        final ByteBuffer buf = createDirectBuffer0(buffersize);
        buf.limit(0);
        return buf;
    }

    public ByteBuffer createDirectOverlapStruct(long eHandle) {
        return createDirectOverlapStruct0(eHandle);
    }

    public void freeDirectBuffer(final ByteBuffer byteBuffer) throws IOException {
        ensureDirectBuffer(byteBuffer);
        freeDirectBuffer0(byteBuffer);
    }

    public long createNamedPipe(final String pipeName, int instances, int buffersize) {
        return createNamedPipe0(pipeName, instances, buffersize);
    }

    /**
     * Open existing named pipe (as client, for synchronous access)
     * @param pipeName name of Windows named pipe e.g. '\\.\pipe\thermostat-pipe-command-channel'
     * @return windows handle or 0 if failure
     */
    long openNamedPipe(final String pipeName) {
        return openExistingNamedPipe0(pipeName);
    }

    public long createEvent() {
        return createEvent0(true, false);
    }

    public void resetEvent(long eventHandle) {
        resetEvent0(eventHandle);
    }

    public void setEvent(long eventHandle) {
        setEvent0(eventHandle);
    }

    public int getLastError() {
        return getLastError0();
    }
    /**
     * Wait for a client connection on this pipe
     *
     * The eventHandle will be inserted into the OVERLAP struct by the native code
     * (Java can't do this as the OOVERLAP struct is opaque to Java)
     *
     * @param pipeHandle pipe handle
     * @param ooverlapped contiguous array used to store a Windows OVERLAPPED structure
     * @return 0 if sucessful, ERROR_IO_PENDING if pending, or other error code on failure
     */
    public int connectNamedPipe(long pipeHandle, ByteBuffer ooverlapped) throws IOException {
        ensureDirectBuffer(ooverlapped);
        return connectNamedPipe0(pipeHandle, ooverlapped);
    }

    public boolean disconnectNamedPipe(long pipeHandle) {
        return disconnectNamedPipe0(pipeHandle);
    }

    public long getNamedPipeClientProcessId(long pipeHandle) {
        return getNamedPipeClientProcessId0(pipeHandle);
    }

    public boolean closeHandle(long handle) {
        return closeHandle0(handle);
    }

    public int waitForMultipleObjects(int n, long[] ehandles, boolean waitAll, int millis) {
        return waitForMultipleObjects0(n, ehandles, waitAll, millis);
    }

    /**
     * get result of overlapped operation
     *
     * @param pipeHandle handle
     * @param ooverlapped buffer to store Windows OVERLAPPED structure
     * @param wait true to wait for result, false to return immediately
     * @return bytes transferred (>= 0) if success, (- bytes transferred -1) if failure
     */
    public int getOverlappedResult(long pipeHandle, ByteBuffer ooverlapped, boolean wait) throws IOException {
        ensureDirectBuffer(ooverlapped);
        return getOverlappedResult0(pipeHandle, ooverlapped, wait);
    }

    /**
     * Read from a file in overlapped mode
     *
     * The eventHandle will be inserted into the OVERLAP struct by the native code
     * (Java can't do this as the OOVERLAP struct is opaque to Java)
     *
     * @param pipeHandle pipe handle
     * @param ooverlapped contiguous array used to store a Windows OVERLAPPED structure
     * @param buffer byte buffer to be read into
     * @return true if successful
     */
    public boolean readFileOverlapped(long pipeHandle, ByteBuffer ooverlapped, ByteBuffer buffer) throws IOException {
        ensureDirectBuffer(ooverlapped);
        ensureDirectBuffer(buffer);
        return readFileOverlapped0(pipeHandle, ooverlapped, buffer, buffer.position(), buffer.remaining());
    }

    /**
     * Read from a file in syncronous mode
     *
     * @param pipeHandle pipe handle
     * @param buffer byte buffer to be read into
     * @return >= 0 if 0 or more bytes read, -1 if error
     */
    int readFile(long pipeHandle, ByteBuffer buffer) {
        int count = readFile0(pipeHandle, buffer.array(), buffer.position(), buffer.remaining());
        if (count >= 0) {
            buffer.position(buffer.position() + count);
        }
        return count;
    }

    /**
     * Write to a file in overlapped mode
     *
     * The eventHandle will be inserted into the OVERLAP struct by the native code
     * (Java can't do this as the OOVERLAP struct is opaque to Java)
     *
     * @param pipeHandle pipe handle
     * @param ooverlapped contiguous array used to store a Windows OVERLAPPED structure
     * @param buffer byte buffer to be written
     * @return true if IO pending, false if error
     */
    public boolean writeFileOverlapped(long pipeHandle, ByteBuffer ooverlapped, ByteBuffer buffer) throws IOException {
        ensureDirectBuffer(ooverlapped);
        ensureDirectBuffer(buffer);
        return writeFileOverlapped0(pipeHandle, ooverlapped, buffer, buffer.position(), buffer.remaining());
        // don't modify buffer's position() here, becuase need to wait until the buffer has been fully written
    }

    /**
     * Write to a file in synchronous mode
     *
     * @param pipeHandle pipe handle
     * @param buffer byte buffer to be written
     * @return >= 0 if 0 or more bytes written, -1 if error
     */
    public int writeFile(long pipeHandle,  ByteBuffer buffer) {
        final int count = writeFile0(pipeHandle, buffer.array(), buffer.position(), buffer.remaining());
        if (count > 0)
            buffer.position(buffer.position() + count);
        return count;
    }

    /**
     * Write to a file in synchronous mode
     *
     * @param pipeHandle pipe handle
     * @param buffer byte array to be written
     * @return >= 0 if 0 or more bytes written, -1 if error
     */
    public int writeFile(long pipeHandle,  byte[] buffer) {
        return writeFile0(pipeHandle, buffer, 0, buffer.length);
    }

    private void ensureDirectBuffer( ByteBuffer b) throws IOException {
        if (!b.isDirect()) {
            throw new IOException("ByteBuffer is not a DirectByteBuffer");
        }
    }

    // native functions

    private native long createNamedPipe0(final String pipeName, int instances, int buffersize);

    private native long createEvent0(boolean manual, boolean initiallySignalled);
    private native void resetEvent0(long handle);
    private native void setEvent0(long handle);

    private native int getLastError0();

    /**
     * Connect to a namedpipe client (native code)
     *
     * The eventHandle will be inserted into the OVERLAP struct by the native code
     * (Java can't do this as the OOVERLAP struct is opaque to Java)
     *
     * @param pipeHandle pipe handle
     * @param ooverlapped contiguous array used to store a Windows OVERLAPPED structure
     * @return return 0 if sucessfull, ERROR_IO_PENDING if still pending, or GetLastError() if failure
     */
    private native int connectNamedPipe0(long pipeHandle, ByteBuffer ooverlapped);

    private native long openExistingNamedPipe0(final String pipeName);

    private native boolean disconnectNamedPipe0(long pipeHandle);
    private native long getNamedPipeClientProcessId0(long pipeHandle);
    private native boolean closeHandle0(long handle);
    private native int waitForMultipleObjects0(int n, long[] ehandles, boolean waitAll, int millis);
    private native int getOverlappedResult0(long pipeHandle, ByteBuffer ooverlapped, boolean wait);

    /**
     * Read from a file in overlapped mode (native code)
     *
     * The eventHandle will be inserted into the OVERLAP struct by the native code
     * (Java can't do this as the OOVERLAP struct is opaque to Java)
     *
     * @param pipeHandle pipe handle
     * @param ooverlapped contiguous array used to store a Windows OVERLAPPED structure
     * @param buffer byte buffer to be read into
     * @param bufsize max number of bytes to read
     * @return >= 0 if 0 ore more bytes read, -1 if pending IO, -2 if other error
     */
    private native boolean readFileOverlapped0(long pipeHandle, ByteBuffer ooverlapped, ByteBuffer buffer, int position, int bufsize);

    /**
     * Write to a file in overlapped mode (native code)
     *
     * The eventHandle will be inserted into the OVERLAP struct by the native code
     * (Java can't do this as the OOVERLAP struct is opaque to Java)
     *
     * @param pipeHandle pipe handle
     * @param ooverlapped contiguous array used to store a Windows OVERLAPPED structure
     * @param buffer byte buffer to be written
     * @param bufsize number of bytes to write
     * @return true if successful
     */
    private native boolean writeFileOverlapped0(long pipeHandle, ByteBuffer ooverlapped, ByteBuffer buffer, int position, int bufsize);

    /**
     * Cancel pending asynchronous IO
     * Will not cancel pending IO for this handle started on another thread
     *
     * @param pipeHandle handle with pending IO to cancel
     * @return false if fails
     */
    public boolean cancelIO(long pipeHandle) {
        return cancelIo0(pipeHandle);
    }

    /**
     * Cancel pending asynchronous IO
     * Cancells all pending IO on all threads
     *
     * @param pipeHandle handle with pending IO to cancel
     * @return false if fails
     */
    public boolean cancelAllIo(long pipeHandle, ByteBuffer ooverlapped) {
        return cancelIoEx0(pipeHandle, ooverlapped);
    }

    /**
     * Cancel pending asynchronous IO
     * Will not cancel pending IO for this handle started on another thread
     *
     * @param pipeHandle handle with pending IO to cancel
     * @return false if fails
     */
    private native boolean cancelIo0(long pipeHandle);

    /**
     * Cancel pending asynchronous IO
     * Cancells all pending IO on all threads
     *
     * @param pipeHandle handle with pending IO to cancel
     * @return false if fails
     */
    private native boolean cancelIoEx0(long pipeHandle, ByteBuffer ooverlapped);

    private native int readFile0(long pipeHandle, byte[] buffer, int position, int bufsize);
    private native int writeFile0(long pipeHandle, byte[] buffer, int position, int bufsize);

    private static native long getConstantWaitObject0();
    private static native long getConstantInfinite0();

    private static native int getConstantErrorIOPending0();
    private static native int getConstantErrorIOIncomplete0();
    private static native int getConstantErrorHandleEOF0();
    private static native int getConstantErrorMoreData0();
    private static native int getConstantErrorPipeBusy0();
    private static native int getConstantInvalidHandle0();
    private static native int getConstantErrorPipeConnected0();
    private static native int getConstantErrorBrokenPipe0();

    // bytebuffer maniuplation
    private native ByteBuffer createDirectBuffer0(int buffersize);
    private native ByteBuffer createDirectOverlapStruct0(long eHandle);
    private native void freeDirectBuffer0(final ByteBuffer byteBuffer);
}
