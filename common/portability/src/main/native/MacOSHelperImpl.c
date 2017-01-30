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

#include "com_redhat_thermostat_common_portability_internal_macos_MacOSHelperImpl.h"
#include <jni.h>
#include <unistd.h>
#include <string.h>
#include <stdlib.h>
#include <stdio.h>
#include <stdint.h>
#include <netdb.h>
#include <sys/sysctl.h>
#include <sys/types.h>
#include <pwd.h>
#include <mach/vm_statistics.h>

#define MAX_NAME 256
#ifndef NI_MAXHOST
#define NI_MAXHOST 1025
#endif /* NI_MAXHOST */

#if defined(DEBUG)
static void testLength(JNIEnv* env, jlongArray array, int minLength) {
    // sanity test
    jsize len = (*env)->GetArrayLength(env, array);
    assert(len >= minLength);
}
#else
static void testLength(JNIEnv* env, jlongArray array, int minLength) {}
#endif

#if !defined(TRUE)
# define TRUE 1
# define FALSE 0
#endif

static jint throw_IOException(JNIEnv *env, const char *message) {
    const char *class_name = "java/io/IOException";
    jclass class = (*env)->FindClass(env, class_name);
    if (class == NULL) {
        return -1;
    }
    return (*env)->ThrowNew(env, class, message);
}

/*
 * Class:     com_redhat_thermostat_common_portability_internal_macos_MacOSHelperImpl
 * Method:    getHostName0
 * Signature: ()Ljava/lang/String;
 */
JNIEXPORT jstring JNICALL
Java_com_redhat_thermostat_common_portability_internal_macos_MacOSHelperImpl_getHostName0
  (JNIEnv *env, jclass winHelperClass)
{
      char hostname[NI_MAXHOST];
      memset(hostname, 0, sizeof(hostname));

      if (gethostname(hostname,  sizeof(hostname)) == 0) {
          return (*env)->NewStringUTF(env, hostname);
      }
      return NULL;
}

/*
 * Class:     com_redhat_thermostat_common_portability_internal_macos_MacOSHelperImpl
 * Method:    getGlobalMemoryStatus0
 * Signature: ([J)V
 */
JNIEXPORT jboolean JNICALL Java_com_redhat_thermostat_common_portability_internal_macos_MacOSHelperImpl_getGlobalMemoryStatus0
  (JNIEnv *env, jclass winHelperClass, jlongArray array)
{
    testLength(env, array, 8);

    // Get the element pointer
    jlong* data = (*env)->GetLongArrayElements(env, array, 0);


    (*env)->ReleaseLongArrayElements(env, array, data, 0);
    return TRUE;
}

/*
 * Class:     com_redhat_thermostat_common_portability_internal_macos_MacOSHelperImpl
 * Method:    getPerformanceInfo0
 * Signature: ([J)V
 */
JNIEXPORT jboolean JNICALL Java_com_redhat_thermostat_common_portability_internal_macos_MacOSHelperImpl_getPerformanceInfo0
  (JNIEnv *env, jclass winHelperClass, jlongArray array)
{
    testLength(env, array, 13);

    // Get the element pointer
    jlong* data = (*env)->GetLongArrayElements(env, array, 0);

    (*env)->ReleaseLongArrayElements(env, array, data, 0);
    return TRUE;
}

/*
 * Class:     com_redhat_thermostat_common_portability_internal_macos_MacOSHelperImpl
 * Method:    queryPerformanceFrequency0
 * Signature: ()I
 */
JNIEXPORT jlong JNICALL Java_com_redhat_thermostat_common_portability_internal_macos_MacOSHelperImpl_queryPerformanceFrequency0
  (JNIEnv *env, jclass winHelperClass)
{
    return (jlong)(9999);
}

uid_t uidFromPid(pid_t pid)
{
    uid_t uid = -1;

    struct kinfo_proc process;
    size_t procBufferSize = sizeof(process);

    // Compose search path for sysctl. Here you can specify PID directly.
    const u_int pathLenth = 4;
    int path[pathLenth] = {CTL_KERN, KERN_PROC, KERN_PROC_PID, pid};

    int sysctlResult = sysctl(path, pathLenth, &process, &procBufferSize, NULL, 0);

    // If sysctl did not fail and process with PID available - take UID.
    if ((sysctlResult == 0) && (procBufferSize != 0))
    {
        uid = process.kp_eproc.e_ucred.cr_uid;
    }

    return uid;
}

/*
 * Class:     com_redhat_thermostat_common_portability_internal_macos_MacOSHelperImpl
 * Method:    getUserName0
 * Signature: (I)Ljava/lang/String;
 */
JNIEXPORT jstring JNICALL
Java_com_redhat_thermostat_common_portability_internal_macos_MacOSHelperImpl_getUserName0
  (JNIEnv *env, jclass winHelperClass, jint pid)
{
    uid_t uid = uidFromPid(pid);

    struct passwd pwdbuf;
    struct passwd* pwdPtr;
    char buf[1024];

    int rc = getpwuid_r(uid, &pwdbuf, buf, sizeof(buf), &pwdPtr);

    if (rc) {
        throw_IOException(env, "getpwuid_r() error");
    }
    else if (pwdPtr == NULL) {
        // no such entry
        return NULL;
    }
    return (*env)->NewStringUTF(env, pwdbuf.pw_name);
}

/*
 * Class:     com_redhat_thermostat_common_portability_internal_macos_MacOSHelperImpl
 * Method:    getEnvironment0
 * Signature: ()[Ljava/lang/String;
 */
JNIEXPORT jobjectArray JNICALL
Java_com_redhat_thermostat_common_portability_internal_macos_MacOSHelperImpl_getEnvironment0
  (JNIEnv *env, jclass winHelperClass, jint pid)
{
    // TODO - implement this stub - not eay (have to open the process memory and poke around)
    // for now, return an empty array
    jobjectArray ret = (jobjectArray)(*env)->NewObjectArray(env, 0, (*env)->FindClass(env, "java/lang/String"), (*env)->NewStringUTF(env, ""));
    return ret;
}

/*
 * Class:     com_redhat_thermostat_common_portability_internal_macos_MacOSHelperImpl
 * Method:    getProcessMemoryInfo0
 * Signature: (I[J)V
 */
JNIEXPORT jboolean JNICALL Java_com_redhat_thermostat_common_portability_internal_macos_MacOSHelperImpl_getProcessInfo0
  (JNIEnv *env, jclass winHelperClass, jint pid, jlongArray array)
{
    testLength(env, array, 4);

    // Get the element pointer
    jlong* data = (*env)->GetLongArrayElements(env, array, 0);
    (*env)->ReleaseLongArrayElements(env, array, data, 0);
    return TRUE;
}

/*
 * Class:     com_redhat_thermostat_common_portability_internal_macos_MacOSHelperImpl
 * Method:    getProcessIOInfo0
 * Signature: (I[J)V
 */
JNIEXPORT jboolean JNICALL Java_com_redhat_thermostat_common_portability_internal_macos_MacOSHelperImpl_getProcessIOInfo0
  (JNIEnv *env, jclass winHelperClass, jint pid, jlongArray array)
{
    testLength(env, array, 6);

    // Get the element pointer
    jlong* data = (*env)->GetLongArrayElements(env, array, 0);
    (*env)->ReleaseLongArrayElements(env, array, data, 0);
    return TRUE;
}

/*
 * Class:     com_redhat_thermostat_common_portability_internal_macos_MacOSHelperImpl
 * Method:    getProcessHandle0
 * Signature: (I)J
 */
JNIEXPORT jlong JNICALL Java_com_redhat_thermostat_common_portability_internal_macos_MacOSHelperImpl_getCurrentProcessPid0
  (JNIEnv *env, jclass winHelperClass) {

    return (jlong) getpid();
}

/*
 * Class:     com_redhat_thermostat_common_portability_internal_macos_MacOSHelperImpl
 * Method:    getProcessHandle0
 * Signature: (I)J
 */
JNIEXPORT jlong JNICALL Java_com_redhat_thermostat_common_portability_internal_macos_MacOSHelperImpl_getProcessUid0
  (JNIEnv *env, jclass winHelperClass, jint pid) {
    return (jlong) uidFromPid(pid);
}

/*
 * Class:     com_redhat_thermostat_common_portability_internal_macos_MacOSHelperImpl
 * Method:    terminateProcess0
 * Signature: (IIB)V
 */
JNIEXPORT jboolean JNICALL Java_com_redhat_thermostat_common_portability_internal_macos_MacOSHelperImpl_terminateProcess0
  (JNIEnv *env, jclass winHelperClass, jint pid, jint exitCode, jint waitMillis) {

    return TRUE;
}

/*
 * Class:     com_redhat_thermostat_common_portability_internal_macos_MacOSHelperImpl
 * Method:    getLongSysctl0
 * Signature: (Ljava/lang/String;)J
 */
JNIEXPORT jlong JNICALL Java_com_redhat_thermostat_common_portability_internal_macos_MacOSHelperImpl_getLongSysctl0
  (JNIEnv *env, jclass winHelperClass, jstring info) {

    const char* info_cstr = (*env)->GetStringUTFChars(env, info, NULL);

    jlong ret = 0;
    size_t size = sizeof(ret);

    if (sysctlbyname(info_cstr, &ret, &size, NULL, 0) < 0) {
        throw_IOException(env, "bad sysctl() string");
    }

    return ret;
}

/*
 * Class:     com_redhat_thermostat_common_portability_internal_macos_MacOSHelperImpl
 * Method:    getStringSysctl0
 * Signature: (Ljava/lang/String;)Ljava/lang/String;
 */
JNIEXPORT jstring JNICALL Java_com_redhat_thermostat_common_portability_internal_macos_MacOSHelperImpl_getStringSysctl0
  (JNIEnv *env, jclass winHelperClass, jstring info) {

    const char* info_cstr = (*env)->GetStringUTFChars(env, info, NULL);

    size_t size = 0;

    if (sysctlbyname(info_cstr, NULL, &size, NULL, 0) < 0) {
        throw_IOException(env, "bad sysctl() string");
    }

    char* buffer = malloc(size);

    if (sysctlbyname(info_cstr, buffer, &size, NULL, 0) < 0) {
        throw_IOException(env, "bad sysctl() string");
    }

    jstring ret = (*env)->NewStringUTF(env, buffer);

    free(buffer);

    return ret;
}


