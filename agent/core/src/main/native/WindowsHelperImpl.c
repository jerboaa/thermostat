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

#include "com_redhat_thermostat_agent_utils_windows_WindowsHelperImpl.h"
#include <jni.h>
#include <unistd.h>
#include <string.h>

#if !defined(_WIN32)
# include <netdb.h>
#else // windows
# include <winsock2.h>
# include <psapi.h>
#endif

#ifndef NI_MAXHOST
#define NI_MAXHOST 1025
#endif /* NI_MAXHOST */

#if defined(NDEBUG)
static void testLength(JNIEnv* env, jlongArray array, minLength) {}
#else
static void testLength(JNIEnv* env, jlongArray array, minLength) {
    // sanity test
    jsize len = (*env)->GetArrayLength(env, array);
    assert(len >= minLength);
}
#endif

/*
 * Class:     com_redhat_thermostat_agent_utils_windows_WindowsHelperImpl
 * Method:    getHostName0
 * Signature: ()Ljava/lang/String;
 */
JNIEXPORT jstring JNICALL
Java_com_redhat_thermostat_agent_utils_windows_WindowsHelperImpl_getHostName0
  (JNIEnv *env, jclass winHelperClass, jboolean prependDomain)
{
      char hostname[NI_MAXHOST];
      memset(hostname, 0, sizeof(hostname));

      if (gethostname(hostname,  sizeof(hostname)) == 0) {
          return (*env)->NewStringUTF(env, hostname);
      }
      return NULL;
}

/*
 * Class:     com_redhat_thermostat_agent_utils_windows_WindowsHelperImpl
 * Method:    getOSVersion0
 * Signature: ([J)V
 */
JNIEXPORT void JNICALL Java_com_redhat_thermostat_agent_utils_windows_WindowsHelperImpl_getOSVersion0
  (JNIEnv *env, jclass winHelperClass, jlongArray array)
{
    testLength(env, array, 3);

    // Get the element pointer
    jlong* data = (*env)->GetLongArrayElements(env, array, 0);

    OSVERSIONINFOEX vinfo;
    vinfo.dwOSVersionInfoSize = sizeof(vinfo);
    GetVersionEx(&vinfo);
    data[0] = vinfo.dwMajorVersion;
    data[1] = vinfo.dwMinorVersion;
    data[2] = vinfo.dwBuildNumber;

    (*env)->ReleaseLongArrayElements(env, array, data, 0);
}


/*
 * Class:     com_redhat_thermostat_agent_utils_windows_WindowsHelperImpl
 * Method:    getUid0
 * Signature: (I)I
 */
JNIEXPORT jint JNICALL
Java_com_redhat_thermostat_agent_utils_windows_WindowsHelperImpl_getUid0
  (JNIEnv *env, jclass winHelperClass, jint pid)
{
    // TODO - implement this stub
    return 987654321;
}

/*
 * Class:     com_redhat_thermostat_agent_utils_windows_WindowsHelperImpl
 * Method:    getUserName0
 * Signature: (I)Ljava/lang/String;
 */
JNIEXPORT jstring JNICALL
Java_com_redhat_thermostat_agent_utils_windows_WindowsHelperImpl_getUserName0
  (JNIEnv *env, jclass winHelperClass, jint pid, jboolean prependDomain)
{
    // TODO - implement this stub
    return (*env)->NewStringUTF(env, "(unavailable)");
}

/*
 * Class:     com_redhat_thermostat_agent_utils_windows_WindowsHelperImpl
 * Method:    getEnvironment0
 * Signature: ()[Ljava/lang/String;
 */
JNIEXPORT jobjectArray JNICALL
Java_com_redhat_thermostat_agent_utils_windows_WindowsHelperImpl_getEnvironment0
  (JNIEnv *env, jclass winHelperClass, jint pid)
{
    // TODO - implement this stub
    jobjectArray ret = (jobjectArray)(*env)->NewObjectArray(env, 0, (*env)->FindClass(env, "java/lang/String"), (*env)->NewStringUTF(env, ""));
    return ret;
}


/*
 * Class:     com_redhat_thermostat_agent_utils_windows_WindowsHelperImpl
 * Method:    getGlobalMemoryStatus0
 * Signature: ([J)V
 */
JNIEXPORT boolean JNICALL Java_com_redhat_thermostat_agent_utils_windows_WindowsHelperImpl_getGlobalMemoryStatus0
  (JNIEnv *env, jclass winHelperClass, jlongArray array)
{
    testLength(env, array, 8);

    // Get the element pointer
    jlong* data = (*env)->GetLongArrayElements(env, array, 0);

    // get the memory info
    MEMORYSTATUSEX statex;
    statex.dwLength = sizeof(statex);
    GlobalMemoryStatusEx(&statex);
    data[0] = statex.dwMemoryLoad;
    data[1] = statex.ullTotalPhys;
    data[2] = statex.ullAvailPhys;
    data[3] = statex.ullTotalPageFile;
    data[4] = statex.ullAvailPageFile;
    data[5] = statex.ullTotalVirtual;
    data[6] = statex.ullAvailVirtual;
    data[7] = statex.ullAvailExtendedVirtual;

    (*env)->ReleaseLongArrayElements(env, array, data, 0);
    return TRUE;
}

/*
 * Class:     com_redhat_thermostat_agent_utils_windows_WindowsHelperImpl
 * Method:    getPerformanceInfo0
 * Signature: ([J)V
 */
JNIEXPORT boolean JNICALL Java_com_redhat_thermostat_agent_utils_windows_WindowsHelperImpl_getPerformanceInfo0
  (JNIEnv *env, jclass winHelperClass, jlongArray array)
{
    testLength(env, array, 13);

    // Get the element pointer
    jlong* data = (*env)->GetLongArrayElements(env, array, 0);

    /**
     // from PERFORMANCE_INFORMATION (13 values)
        SIZE_T CommitTotal;
        SIZE_T CommitLimit;
        SIZE_T CommitPeak;
        SIZE_T PhysicalTotal;
        SIZE_T PhysicalAvailable;
        SIZE_T SystemCache;
        SIZE_T KernelTotal;
        SIZE_T KernelPaged;
        SIZE_T KernelNonpaged;
        SIZE_T PageSize;
        DWORD  HandleCount;
        DWORD  ProcessCount;
        DWORD  ThreadCount;
     */

    // get the memeory info
    PERFORMANCE_INFORMATION statex;
    statex.cb = sizeof(statex);
    GetPerformanceInfo(&statex, statex.cb);
    data[0] = statex.CommitTotal;
    data[1] = statex.CommitLimit;
    data[2] = statex.CommitPeak;
    data[3] = statex.PhysicalTotal;
    data[4] = statex.PhysicalAvailable;
    data[5] = statex.SystemCache;
    data[6] = statex.KernelTotal;
    data[7] = statex.KernelPaged;
    data[8] = statex.KernelNonpaged;
    data[9] = statex.PageSize;
    data[10] = statex.HandleCount;
    data[11] = statex.ProcessCount;
    data[12] = statex.ThreadCount;

    (*env)->ReleaseLongArrayElements(env, array, data, 0);
    return TRUE;
}

static unsigned __int64 convertFileTimeToInt64( const FILETIME * pFileTime )
{
  ULARGE_INTEGER largeInt;

  largeInt.LowPart = pFileTime->dwLowDateTime;
  largeInt.HighPart = pFileTime->dwHighDateTime;

  return largeInt.QuadPart;
}

/*
 * Class:     com_redhat_thermostat_agent_utils_windows_WindowsHelperImpl
 * Method:    getProcessMemoryInfo0
 * Signature: (I[J)V
 */
JNIEXPORT jboolean JNICALL Java_com_redhat_thermostat_agent_utils_windows_WindowsHelperImpl_getProcessInfo0
  (JNIEnv *env, jclass winHelperClass, jint pid, jlongArray array)
{
    testLength(env, array, 4);

    // Get the element pointer
    jlong* data = (*env)->GetLongArrayElements(env, array, 0);

    HANDLE hProcess;
    PROCESS_MEMORY_COUNTERS pmc;

    hProcess = OpenProcess( PROCESS_QUERY_INFORMATION | PROCESS_VM_READ, FALSE, pid );
    if (NULL == hProcess)
        return FALSE;

    pmc.cb = sizeof(pmc);
    if ( GetProcessMemoryInfo( hProcess, &pmc, sizeof(pmc)) ) {
        data[0] = pmc.WorkingSetSize;
    }
    else {
        (*env)->ReleaseLongArrayElements(env, array, data, 0);
        return FALSE;
    }

    FILETIME creationTime;
    FILETIME exitTime;
    FILETIME kernelTime;
    FILETIME userTime;

    if ( GetProcessTimes( hProcess, &creationTime, &exitTime, &kernelTime, &userTime ) ) {
        data[1] = convertFileTimeToInt64(&userTime);
        data[2] = convertFileTimeToInt64(&kernelTime);
    }

    CloseHandle(hProcess);

    (*env)->ReleaseLongArrayElements(env, array, data, 0);
    return TRUE;
}

/*
 * Class:     com_redhat_thermostat_agent_utils_windows_WindowsHelperImpl
 * Method:    getCPUString0
 * Signature: ()Ljava/lang/String;
 */
JNIEXPORT jstring JNICALL Java_com_redhat_thermostat_agent_utils_windows_WindowsHelperImpl_getCPUString0
  (JNIEnv *env, jclass winHelperClass)
{
    // Get extended ids.
    int CPUInfo[4] = {-1};
    __cpuid(CPUInfo, 0x80000000);
    unsigned int nExIds = CPUInfo[0];

    // Get the information associated with each extended ID.
    char CPUBrandString[0x40] = { 0 };
    for( unsigned int i=0x80000000; i<=nExIds; ++i)
    {
        __cpuid(CPUInfo, i);

        // Interpret CPU brand string and cache information.
        if  (i == 0x80000002)
        {
            memcpy( CPUBrandString,
            CPUInfo,
            sizeof(CPUInfo));
        }
        else if( i == 0x80000003 )
        {
            memcpy( CPUBrandString + 16,
            CPUInfo,
            sizeof(CPUInfo));
        }
        else if( i == 0x80000004 )
        {
            memcpy(CPUBrandString + 32, CPUInfo, sizeof(CPUInfo));
        }
    }
    return (*env)->NewStringUTF(env, CPUBrandString);

}

/*
 * Class:     com_redhat_thermostat_agent_utils_windows_WindowsHelperImpl
 * Method:    getCPUCount0
 * Signature: ()I
 */
JNIEXPORT jint JNICALL Java_com_redhat_thermostat_agent_utils_windows_WindowsHelperImpl_getCPUCount0
  (JNIEnv *env, jclass winHelperClass)
{
    SYSTEM_INFO sysinfo;
    GetSystemInfo(&sysinfo);

    return sysinfo.dwNumberOfProcessors;
}

/*
 * Class:     com_redhat_thermostat_agent_utils_windows_WindowsHelperImpl
 * Method:    getClockTicksPerSecond0
 * Signature: ()I
 */
JNIEXPORT jlong JNICALL Java_com_redhat_thermostat_agent_utils_windows_WindowsHelperImpl_getClockTicksPerSecond0
  (JNIEnv *env, jclass winHelperClass)
{
    LARGE_INTEGER freq;
    QueryPerformanceFrequency(&freq);

    return (jlong)(freq.QuadPart);
}
