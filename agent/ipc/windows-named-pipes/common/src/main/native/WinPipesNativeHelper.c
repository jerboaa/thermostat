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

#include "com_redhat_thermostat_agent_ipc_winpipes_common_internal_WinPipesNativeHelper.h"

#include <jni.h>
#include <unistd.h>
#include <string.h>
#include <assert.h>

#if defined(_WIN32)
# include <winsock2.h>
# include <Windows.h>
#endif

#define PIPE_TIMEOUT 5000
//#define FILL_READ_BUFFER 1

static jint throw_IOException(JNIEnv *env, const char *message) {
    const char *class_name = "java/io/IOException";
    jclass class = (*env)->FindClass(env, class_name);
    if (class == NULL) {
        return -1;
    }
    return (*env)->ThrowNew(env, class, message);
}

/*
 * Class:     com_redhat_thermostat_agent_ipc_winpipes_common_internal_WinPipesNativeHelper
 * Method:    createNamedPipe0
 * Signature: (Ljava/lang/String;II)J
 */
JNIEXPORT jlong JNICALL Java_com_redhat_thermostat_agent_ipc_winpipes_common_internal_WinPipesNativeHelper_createNamedPipe0
  (JNIEnv *env, jobject obj, jstring pipeName, jint instances, jint bufsize)
{
    const char *pname = (*env)->GetStringUTFChars(env, pipeName, NULL);
    if (pname == NULL) {
        return 0;
    }
    HANDLE pipeHandle = CreateNamedPipe(
           pname,                   // pipe name
           PIPE_ACCESS_DUPLEX |     // read/write access
           FILE_FLAG_OVERLAPPED,    // overlapped mode
           PIPE_TYPE_MESSAGE |      // message-type pipe
           PIPE_READMODE_MESSAGE |  // message-read mode
           PIPE_WAIT,               // blocking mode
           instances,               // number of instances
           bufsize,                 // output buffer size
           bufsize,                 // input buffer size
           PIPE_TIMEOUT,            // client time-out
           NULL);                   // default security attributes

    (*env)->ReleaseStringUTFChars(env, pipeName, pname);
    return (jlong)pipeHandle;
}

/*
 * Class:     com_redhat_thermostat_agent_ipc_winpipes_common_internal_WinPipesNativeHelper
 * Method:    openNamedPipe0
 * Signature: (Ljava/lang/String;)J
 */
JNIEXPORT jlong JNICALL Java_com_redhat_thermostat_agent_ipc_winpipes_common_internal_WinPipesNativeHelper_openExistingNamedPipe0
  (JNIEnv *env, jobject obj, jstring pipeName)
{
    const char *pname = (*env)->GetStringUTFChars(env, pipeName, NULL);
    if (pname == NULL) {
        return 0;
    }

    jlong hnd = (jlong)CreateFile(
         pname,   // pipe name
         GENERIC_READ |  // read and write access
         GENERIC_WRITE,
         0,              // no sharing
         NULL,           // default security attributes
         OPEN_EXISTING,  // opens existing pipe
         0,              // default attributes
         NULL);          // no template file

    (*env)->ReleaseStringUTFChars(env, pipeName, pname);
    return (jlong)hnd;
}

/*
 * Class:     com_redhat_thermostat_agent_ipc_winpipes_common_internal_WinPipesNativeHelper
 * Method:    createEvent0
 * Signature: ()J
 */
JNIEXPORT jlong JNICALL Java_com_redhat_thermostat_agent_ipc_winpipes_common_internal_WinPipesNativeHelper_createEvent0
  (JNIEnv *env, jobject obj, jboolean manual, jboolean initial)
{
    return (jlong)CreateEvent(NULL, manual, initial, NULL);
}

/*
 * Class:     com_redhat_thermostat_agent_ipc_winpipes_common_internal_WinPipesNativeHelper
 * Method:    resetEvent0
 * Signature: (J)V
 */
JNIEXPORT void JNICALL Java_com_redhat_thermostat_agent_ipc_winpipes_common_internal_WinPipesNativeHelper_resetEvent0
  (JNIEnv *env, jobject obj, jlong eventHandle)
{
    ResetEvent((HANDLE)eventHandle);
}

/*
 * Class:     com_redhat_thermostat_agent_ipc_winpipes_common_internal_WinPipesNativeHelper
 * Method:    setEvent0
 * Signature: (J)V
 */
JNIEXPORT void JNICALL Java_com_redhat_thermostat_agent_ipc_winpipes_common_internal_WinPipesNativeHelper_setEvent0
  (JNIEnv *env, jobject obj, jlong eventHandle)
{
    SetEvent((HANDLE)eventHandle);
}

JNIEXPORT jint JNICALL Java_com_redhat_thermostat_agent_ipc_winpipes_common_internal_WinPipesNativeHelper_getLastError0
  (JNIEnv *env, jobject obj)
{
    return GetLastError();
}

/*
 * Class:     com_redhat_thermostat_agent_ipc_winpipes_common_internal_WinPipesNativeHelper
 * Method:    connectNamedPipe0
 * Signature: (JJ[D)Ljava/lang/String;
 *
 * return 0 if sucessfull, ERROR_IO_PENDING if still pending, or GetLastError() if failure
 */
JNIEXPORT jint JNICALL Java_com_redhat_thermostat_agent_ipc_winpipes_common_internal_WinPipesNativeHelper_connectNamedPipe0
  (JNIEnv *env, jobject obj, jlong pipeHandle, jobject ooverlapped)
{
    OVERLAPPED* overlapped = (OVERLAPPED*)(*env)->GetDirectBufferAddress(env, ooverlapped);
    BOOL ret = ConnectNamedPipe((HANDLE)pipeHandle, overlapped);
    return ret ? GetLastError() : 0;
}

/*
 * Class:     com_redhat_thermostat_agent_ipc_winpipes_common_internal_WinPipesNativeHelper
 * Method:    disconnectnamedPipe0
 * Signature: (J)Z
 */
JNIEXPORT jboolean JNICALL Java_com_redhat_thermostat_agent_ipc_winpipes_common_internal_WinPipesNativeHelper_disconnectNamedPipe0
  (JNIEnv *env, jobject obj, jlong pipeHandle)
{
    return DisconnectNamedPipe((HANDLE)pipeHandle);
}

/*
 * Class:     com_redhat_thermostat_agent_ipc_winpipes_common_internal_WinPipesNativeHelper
 * Method:    getNamedPipeClientProcessId0
 * Signature: (J)J
 */
JNIEXPORT jlong JNICALL Java_com_redhat_thermostat_agent_ipc_winpipes_common_internal_WinPipesNativeHelper_getNamedPipeClientProcessId0
  (JNIEnv *env, jobject obj, jlong pipeHandle)
{
    throw_IOException(env, "getNamedPipeClientProcessId0() not yet implemented");
    return -1; // TODO - implement if needed
}

/*
 * Class:     com_redhat_thermostat_agent_ipc_winpipes_common_internal_WinPipesNativeHelper
 * Method:    closeHandle0
 * Signature: (J)Z
 */
JNIEXPORT jboolean JNICALL Java_com_redhat_thermostat_agent_ipc_winpipes_common_internal_WinPipesNativeHelper_closeHandle0
  (JNIEnv *env, jobject obj, jlong handle)
{
    return CloseHandle((HANDLE)handle);
}

/*
 * Class:     com_redhat_thermostat_agent_ipc_winpipes_common_internal_WinPipesNativeHelper
 * Method:    waitForMultipleObjects0
 * Signature: (I[JZI)I
 */
JNIEXPORT jint JNICALL Java_com_redhat_thermostat_agent_ipc_winpipes_common_internal_WinPipesNativeHelper_waitForMultipleObjects0
  (JNIEnv *env, jobject obj, jint numObjects, jlongArray handles, jboolean waitForAll, jint millis)
{
    void* eventHandles = (*env)->GetLongArrayElements(env, handles, NULL);
    int ret = WaitForMultipleObjects(numObjects, eventHandles, waitForAll, millis);
    (*env)->ReleaseLongArrayElements(env, handles, eventHandles, 0);
    return (jint) ret;
}

/*
 * Class:     com_redhat_thermostat_agent_ipc_winpipes_common_internal_WinPipesNativeHelper
 * Method:    getOverlappedResult0
 * Signature: (JJ[DZ)J
 */
JNIEXPORT jint JNICALL Java_com_redhat_thermostat_agent_ipc_winpipes_common_internal_WinPipesNativeHelper_getOverlappedResult0
  (JNIEnv *env, jobject obj, jlong pipeHandle, jobject ooverlapped, jboolean wait)
{
    OVERLAPPED* overlapped = (OVERLAPPED*)(*env)->GetDirectBufferAddress(env, ooverlapped);

    DWORD bytesTransferred = 0;
    BOOL success = GetOverlappedResult( (HANDLE)pipeHandle, overlapped, &bytesTransferred, wait );

    return success ? bytesTransferred : (-bytesTransferred) - 1;
}

/*
 * Class:     com_redhat_thermostat_agent_ipc_winpipes_common_internal_WinPipesNativeHelper
 * Method:    readFileOverlapped0
 * Signature: (JJ[D[BJ)J
 */
JNIEXPORT jboolean JNICALL Java_com_redhat_thermostat_agent_ipc_winpipes_common_internal_WinPipesNativeHelper_readFileOverlapped0
  (JNIEnv *env, jobject obj, jlong handle, jobject ooverlapped, jobject buffer, jint offset, jint bufsize)
{
    OVERLAPPED* overlapped = (OVERLAPPED*)(*env)->GetDirectBufferAddress(env, ooverlapped);
    HANDLE hEvent = overlapped->hEvent;
    memset(overlapped, 0, sizeof(OVERLAPPED));
    overlapped->hEvent = hEvent;
    jbyte* buf =  (*env)->GetDirectBufferAddress(env, buffer);
#if defined(FILL_READ_BUFFER)
    memset(buf + offset, 'r', (int)bufsize);
#endif
    BOOL success = ReadFile( (HANDLE)handle, buf + offset, (int)bufsize, NULL, overlapped );
    return success;
}


/*
 * Class:     com_redhat_thermostat_agent_ipc_winpipes_common_internal_WinPipesNativeHelper
 * Method:    readFile0
 * Signature: (J[BJ)J
 */
JNIEXPORT jint JNICALL Java_com_redhat_thermostat_agent_ipc_winpipes_common_internal_WinPipesNativeHelper_readFile0
  (JNIEnv *env, jobject obj, jlong handle, jbyteArray array, jint offset, jint bufsize)
{
    jbyte* buf =  (*env)->GetByteArrayElements(env, array, 0);
    DWORD bytesTransferred = 0;
    BOOL success = ReadFile( (HANDLE)handle, buf + offset, (int)bufsize, &bytesTransferred, NULL );
    (*env)->ReleaseByteArrayElements(env, array, buf, 0);
    return success ? bytesTransferred : (-bytesTransferred) - 1;
}


/*
 * Class:     com_redhat_thermostat_agent_ipc_winpipes_common_internal_WinPipesNativeHelper
 * Method:    writeFileOverlapped0
 * Signature: (JJ[D[BJ)J
 */
JNIEXPORT jboolean JNICALL Java_com_redhat_thermostat_agent_ipc_winpipes_common_internal_WinPipesNativeHelper_writeFileOverlapped0
  (JNIEnv *env, jobject obj, jlong handle, jobject ooverlapped, jobject buffer, jint offset, jint bufsize)
{
    OVERLAPPED* overlapped = (OVERLAPPED*)(*env)->GetDirectBufferAddress(env, ooverlapped);
    jbyte* buf =  (*env)->GetDirectBufferAddress(env, buffer);
    HANDLE hEvent = overlapped->hEvent;
    memset(overlapped, 0, sizeof(OVERLAPPED));
    overlapped->hEvent = hEvent;

    BOOL success = WriteFile( (HANDLE)handle, buf + offset, (int)bufsize, NULL, overlapped );

    return success;
}

/*
 * Class:     com_redhat_thermostat_agent_ipc_winpipes_common_internal_WinPipesNativeHelper
 * Method:    writeFile0
 * Signature: (J[BJ)J
 */
JNIEXPORT jint JNICALL Java_com_redhat_thermostat_agent_ipc_winpipes_common_internal_WinPipesNativeHelper_writeFile0
  (JNIEnv *env, jobject obj, jlong handle, jobject array, jint offset, jint bufsize)
{
    jbyte* data =  (*env)->GetByteArrayElements(env, array, 0);
    DWORD bytesTransferred = 0;
    BOOL success = WriteFile( (HANDLE)handle, data + offset, (int)bufsize, &bytesTransferred, 0 );
    (*env)->ReleaseByteArrayElements(env, array, data, JNI_ABORT);
    return success ? bytesTransferred : (-bytesTransferred) - 1;
}

JNIEXPORT jlong JNICALL Java_com_redhat_thermostat_agent_ipc_winpipes_common_internal_WinPipesNativeHelper_getConstantWaitObject0
  (JNIEnv *env, jobject obj)
{
    return (jlong)WAIT_OBJECT_0;
}

JNIEXPORT jlong JNICALL Java_com_redhat_thermostat_agent_ipc_winpipes_common_internal_WinPipesNativeHelper_getConstantInfinite0
  (JNIEnv *env, jobject obj)
{
    return (jlong)INFINITE;
}

/*
 * Class:     com_redhat_thermostat_agent_ipc_winpipes_common_internal_WinPipesNativeHelper
 * Method:    getConstantErrorIOPending0
 * Signature: ()J
 */
JNIEXPORT jint JNICALL Java_com_redhat_thermostat_agent_ipc_winpipes_common_internal_WinPipesNativeHelper_getConstantErrorIOPending0
  (JNIEnv *env, jobject obj)
{
    return ERROR_IO_PENDING;
}

/*
 * Class:     com_redhat_thermostat_agent_ipc_winpipes_common_internal_WinPipesNativeHelper
 * Method:    getConstantErrorIOIncomplete0
 * Signature: ()J
 */
JNIEXPORT jint JNICALL Java_com_redhat_thermostat_agent_ipc_winpipes_common_internal_WinPipesNativeHelper_getConstantErrorIOIncomplete0
  (JNIEnv *env, jobject obj)
{
    return ERROR_IO_INCOMPLETE;
}
/*
 * Class:     com_redhat_thermostat_agent_ipc_winpipes_common_internal_WinPipesNativeHelper
 * Method:    getConstantErrorHandleEOF0
 * Signature: ()J
 */
JNIEXPORT jint JNICALL Java_com_redhat_thermostat_agent_ipc_winpipes_common_internal_WinPipesNativeHelper_getConstantErrorHandleEOF0
  (JNIEnv *env, jobject obj)
{
    return ERROR_HANDLE_EOF;
}

/*
 * Class:     com_redhat_thermostat_agent_ipc_winpipes_common_internal_WinPipesNativeHelper
 * Method:    getConstantErrorMoreData0
 * Signature: ()J
 */
JNIEXPORT jint JNICALL Java_com_redhat_thermostat_agent_ipc_winpipes_common_internal_WinPipesNativeHelper_getConstantErrorMoreData0
  (JNIEnv *env, jobject obj)
{
    return ERROR_MORE_DATA;
}

/*
 * Class:     com_redhat_thermostat_agent_ipc_winpipes_common_internal_WinPipesNativeHelper
 * Method:    getConstantErrorPipeBusy0
 * Signature: ()J
 */
JNIEXPORT jint JNICALL Java_com_redhat_thermostat_agent_ipc_winpipes_common_internal_WinPipesNativeHelper_getConstantErrorPipeBusy0
  (JNIEnv *env, jobject obj)
{
    return ERROR_PIPE_BUSY;
}

/*
 * Class:     com_redhat_thermostat_agent_ipc_winpipes_common_internal_WinPipesNativeHelper
 * Method:    getConstantErrorPipeConnected0
 * Signature: ()J
 */
JNIEXPORT jint JNICALL Java_com_redhat_thermostat_agent_ipc_winpipes_common_internal_WinPipesNativeHelper_getConstantErrorPipeConnected0
  (JNIEnv *env, jobject obj)
{
    return ERROR_PIPE_CONNECTED;
}

/*
 * Class:     com_redhat_thermostat_agent_ipc_winpipes_common_internal_WinPipesNativeHelper
 * Method:    getConstantInvalidHandle0
 * Signature: ()J
 */
JNIEXPORT jint JNICALL Java_com_redhat_thermostat_agent_ipc_winpipes_common_internal_WinPipesNativeHelper_getConstantInvalidHandle0
  (JNIEnv *env, jobject obj)
{
    return (jint)INVALID_HANDLE_VALUE;
}

/*
 * Class:     com_redhat_thermostat_agent_ipc_winpipes_common_internal_WinPipesNativeHelper
 * Method:    getConstantErrorBrokenPipe0
 * Signature: ()J
 */
JNIEXPORT jint JNICALL Java_com_redhat_thermostat_agent_ipc_winpipes_common_internal_WinPipesNativeHelper_getConstantErrorBrokenPipe0
  (JNIEnv *env, jobject obj)
{
    return (jint)ERROR_BROKEN_PIPE;
}


/*
 * Class:     com_redhat_thermostat_agent_ipc_winpipes_common_internal_WinPipesNativeHelper
 * Method:    createDirectBuffer0
 * Signature: (I)Ljava/nio/ByteBuffer;
 */
JNIEXPORT jobject JNICALL Java_com_redhat_thermostat_agent_ipc_winpipes_common_internal_WinPipesNativeHelper_createDirectBuffer0
  (JNIEnv *env, jobject obj, jint bufsize)
{
    void* buffer = malloc(bufsize);
    jobject bytebuffer = (*env)->NewDirectByteBuffer(env, buffer, bufsize);
    return bytebuffer;
}

/*
 * Class:     com_redhat_thermostat_agent_ipc_winpipes_common_internal_WinPipesNativeHelper
 * Method:    createDirectOverlapStruct0
 * Signature: ()Ljava/nio/ByteBuffer;
 */
JNIEXPORT jobject JNICALL Java_com_redhat_thermostat_agent_ipc_winpipes_common_internal_WinPipesNativeHelper_createDirectOverlapStruct0
  (JNIEnv *env, jobject obj, jlong eHandle)
{
    const int bufsize = sizeof(OVERLAPPED);
    OVERLAPPED* overlapped = (OVERLAPPED*)calloc(bufsize, 1);
    overlapped->hEvent = (HANDLE)eHandle;
    jobject bytebuffer = (*env)->NewDirectByteBuffer(env, overlapped, bufsize);
    return bytebuffer;
}

/*
 * Class:     com_redhat_thermostat_agent_ipc_winpipes_common_internal_WinPipesNativeHelper
 * Method:    freeDirectBuffer0
 * Signature: (Ljava/nio/ByteBuffer;)V
 */
JNIEXPORT void JNICALL Java_com_redhat_thermostat_agent_ipc_winpipes_common_internal_WinPipesNativeHelper_freeDirectBuffer0
  (JNIEnv *env, jobject obj, jobject bytebuffer)
{
    void *buffer = (*env)->GetDirectBufferAddress(env, bytebuffer);
    free(buffer);
}

/*
 * Class:     com_redhat_thermostat_agent_ipc_winpipes_common_internal_WinPipesNativeHelper
 * Method:    cancelIo0
 * Signature: (J)Z
 */
JNIEXPORT jboolean JNICALL Java_com_redhat_thermostat_agent_ipc_winpipes_common_internal_WinPipesNativeHelper_cancelIo0
  (JNIEnv *env, jobject obj, jlong pipeHandle)
{
    return (jboolean)CancelIo(pipeHandle);
}

/*
 * Class:     com_redhat_thermostat_agent_ipc_winpipes_common_internal_WinPipesNativeHelper
 * Method:    cancelIoEx0
 * Signature: (JLjava/nio/ByteBuffer;)Z
 */
JNIEXPORT jboolean JNICALL Java_com_redhat_thermostat_agent_ipc_winpipes_common_internal_WinPipesNativeHelper_cancelIoEx0
  (JNIEnv *env, jobject obj, jlong pipeHandle, jobject ooverlapped)
{
    OVERLAPPED* overlapped = ooverlapped == 0 ? 0 : (OVERLAPPED*)(*env)->GetDirectBufferAddress(env, ooverlapped);
    return (jboolean)CancelIoEx(pipeHandle, ooverlapped);
}