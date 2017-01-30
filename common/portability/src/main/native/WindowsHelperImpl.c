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

#include "com_redhat_thermostat_common_portability_internal_windows_WindowsHelperImpl.h"
#include <jni.h>
#include <unistd.h>
#include <string.h>

#if !defined(_WIN32)
# include <netdb.h>
#else // windows
# include <winsock2.h>
# include <psapi.h>
# include <intrin.h>
#endif

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

/*
 * Class:     com_redhat_thermostat_common_portability_internal_windows_WindowsHelperImpl
 * Method:    getHostName0
 * Signature: ()Ljava/lang/String;
 */
JNIEXPORT jstring JNICALL
Java_com_redhat_thermostat_common_portability_internal_windows_WindowsHelperImpl_getHostName0
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
 * Class:     com_redhat_thermostat_common_portability_internal_windows_WindowsHelperImpl
 * Method:    getOSVersion0
 * Signature: ([J)V
 */
JNIEXPORT void JNICALL Java_com_redhat_thermostat_common_portability_internal_windows_WindowsHelperImpl_getOSVersion0
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
 * Class:     com_redhat_thermostat_common_portability_internal_windows_WindowsHelperImpl
 * Method:    getGlobalMemoryStatus0
 * Signature: ([J)V
 */
JNIEXPORT boolean JNICALL Java_com_redhat_thermostat_common_portability_internal_windows_WindowsHelperImpl_getGlobalMemoryStatus0
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
 * Class:     com_redhat_thermostat_common_portability_internal_windows_WindowsHelperImpl
 * Method:    getCPUString0
 * Signature: ()Ljava/lang/String;
 */
JNIEXPORT jstring JNICALL Java_com_redhat_thermostat_common_portability_internal_windows_WindowsHelperImpl_getCPUString0
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
 * Class:     com_redhat_thermostat_common_portability_internal_windows_WindowsHelperImpl
 * Method:    getCPUCount0
 * Signature: ()I
 */
JNIEXPORT jint JNICALL Java_com_redhat_thermostat_common_portability_internal_windows_WindowsHelperImpl_getCPUCount0
  (JNIEnv *env, jclass winHelperClass)
{
    SYSTEM_INFO sysinfo;
    GetSystemInfo(&sysinfo);

    return sysinfo.dwNumberOfProcessors;
}

/*
 * Class:     com_redhat_thermostat_common_portability_internal_windows_WindowsHelperImpl
 * Method:    queryPerformanceFrequency0
 * Signature: ()I
 */
JNIEXPORT jlong JNICALL Java_com_redhat_thermostat_common_portability_internal_windows_WindowsHelperImpl_queryPerformanceFrequency0
  (JNIEnv *env, jclass winHelperClass)
{
    LARGE_INTEGER freq;
    QueryPerformanceFrequency(&freq);

    return (jlong)(freq.QuadPart);
}

/*
 * Class:     com_redhat_thermostat_common_portability_internal_windows_WindowsHelperImpl
 * Method:    getProcessSID0
 * Signature: (I)I
 */
JNIEXPORT jstring JNICALL
Java_com_redhat_thermostat_common_portability_internal_windows_WindowsHelperImpl_getProcessSID0
  (JNIEnv *env, jclass winHelperClass, jint pid)
{
    HANDLE hProcess;

    hProcess = (pid == 0) ? GetCurrentProcess() : OpenProcess( PROCESS_QUERY_INFORMATION | PROCESS_VM_READ, FALSE, pid );
    if (NULL == hProcess)
        return NULL;

    HANDLE hToken = NULL;

    if( !OpenProcessToken( hProcess, TOKEN_QUERY, &hToken ) ) {
        CloseHandle( hProcess );
        return NULL;
    }

    DWORD dwSize = MAX_NAME;
    DWORD dwLength = 0;
    PTOKEN_USER ptu = NULL;

    if (!GetTokenInformation(
        hToken,         // handle to the access token
        TokenUser,      // get information about the token's groups
        (LPVOID) ptu,   // pointer to PTOKEN_USER buffer
        0,              // size of buffer
        &dwLength       // receives required buffer size
    )) {
        if (GetLastError() != ERROR_INSUFFICIENT_BUFFER) {
            CloseHandle( hToken );
            CloseHandle( hProcess );
            return NULL;
        }

        ptu = (PTOKEN_USER)HeapAlloc(GetProcessHeap(), HEAP_ZERO_MEMORY, dwLength);

        if (ptu == NULL) {
            CloseHandle( hToken );
            CloseHandle( hProcess );
            return NULL;
        }
    }

    if (!GetTokenInformation(
         hToken,         // handle to the access token
         TokenUser,      // get information about the token's groups
         (LPVOID) ptu,   // pointer to PTOKEN_USER buffer
         dwLength,       // size of buffer
         &dwLength       // receives required buffer size
         )) {
        if (ptu != NULL) {
            HeapFree(GetProcessHeap(), 0, (LPVOID)ptu);
        }
        CloseHandle( hToken );
        CloseHandle( hProcess );
        return NULL;
    }

    LPWSTR stringSid;
    ConvertSidToStringSidW( ptu->User.Sid, &stringSid );
    jstring s = (*env)->NewString(env, (const jchar *)stringSid, (jsize)wcslen(stringSid));

    LocalFree(stringSid);

    if (ptu != NULL) {
        HeapFree(GetProcessHeap(), 0, (LPVOID)ptu);
    }
    CloseHandle( hToken );
    CloseHandle( hProcess );
    return s;
}

/*
 * Class:     com_redhat_thermostat_common_portability_internal_windows_WindowsHelperImpl
 * Method:    getUserName0
 * Signature: (I)Ljava/lang/String;
 */
JNIEXPORT jstring JNICALL
Java_com_redhat_thermostat_common_portability_internal_windows_WindowsHelperImpl_getUserName0
  (JNIEnv *env, jclass winHelperClass, jint pid, jboolean prependDomain)
{
    HANDLE hProcess;

    hProcess = (pid == 0) ? GetCurrentProcess() : OpenProcess( PROCESS_QUERY_INFORMATION | PROCESS_VM_READ, FALSE, pid );
    if (NULL == hProcess)
        return NULL;

    HANDLE hToken = NULL;

    if( !OpenProcessToken( hProcess, TOKEN_QUERY, &hToken ) ) {
        CloseHandle( hProcess );
        return NULL;
    }

    DWORD dwSize = MAX_NAME;
    DWORD dwLength = 0;
    PTOKEN_USER ptu = NULL;

    if (!GetTokenInformation(
        hToken,         // handle to the access token
        TokenUser,      // get information about the token's groups
        (LPVOID) ptu,   // pointer to PTOKEN_USER buffer
        0,              // size of buffer
        &dwLength       // receives required buffer size
    )) {
        if (GetLastError() != ERROR_INSUFFICIENT_BUFFER) {
            CloseHandle( hToken );
            CloseHandle( hProcess );
            return NULL;
        }

        ptu = (PTOKEN_USER)HeapAlloc(GetProcessHeap(), HEAP_ZERO_MEMORY, dwLength);

        if (ptu == NULL) {
            CloseHandle( hToken );
            CloseHandle( hProcess );
            return NULL;
        }
    }

    if (!GetTokenInformation(
         hToken,         // handle to the access token
         TokenUser,      // get information about the token's groups
         (LPVOID) ptu,   // pointer to PTOKEN_USER buffer
         dwLength,       // size of buffer
         &dwLength       // receives required buffer size
         )) {
        if (ptu != NULL) {
            HeapFree(GetProcessHeap(), 0, (LPVOID)ptu);
        }
        CloseHandle( hToken );
        CloseHandle( hProcess );
        return NULL;
    }

    SID_NAME_USE SidType;
    wchar_t lpName[MAX_NAME];
    wchar_t lpDomain[MAX_NAME*2 + 1];  // room for '\' + lpName
    jstring s = NULL;

    if( !LookupAccountSidW( NULL , ptu->User.Sid, lpName, &dwSize, lpDomain, &dwSize, &SidType ) )
    {
        DWORD dwResult = GetLastError();
        if( dwResult == ERROR_NONE_MAPPED )
           strcpy (lpName, "NONE_MAPPED" );
        else
        {
            printf("LookupAccountSid Error %u\n", GetLastError());
        }
    }
    else
    {
        if (prependDomain) {
            wcscat(lpDomain, L"\\");
            wcscat(lpDomain, lpName);
            s = (*env)->NewString(env, (const jchar *)lpDomain, (jsize)wcslen(lpDomain));
        }
        else {
            s = (*env)->NewString(env, (const jchar *)lpName, (jsize)wcslen(lpName));
        }
    }

    if (ptu != NULL) {
        HeapFree(GetProcessHeap(), 0, (LPVOID)ptu);
    }
    CloseHandle( hToken );
    CloseHandle( hProcess );
    return s;
}

/*
 * Class:     com_redhat_thermostat_common_portability_internal_windows_WindowsHelperImpl
 * Method:    getEnvironment0
 * Signature: ()[Ljava/lang/String;
 */
JNIEXPORT jobjectArray JNICALL
Java_com_redhat_thermostat_common_portability_internal_windows_WindowsHelperImpl_getEnvironment0
  (JNIEnv *env, jclass winHelperClass, jint pid)
{
    // TODO - implement this stub - not eay (have to open the process memory and poke around)
    // for now, return an empty array
    jobjectArray ret = (jobjectArray)(*env)->NewObjectArray(env, 0, (*env)->FindClass(env, "java/lang/String"), (*env)->NewStringUTF(env, ""));
    return ret;
}

static unsigned __int64 convertFileTimeToInt64( const FILETIME * pFileTime )
{
  ULARGE_INTEGER largeInt;

  largeInt.LowPart = pFileTime->dwLowDateTime;
  largeInt.HighPart = pFileTime->dwHighDateTime;

  return largeInt.QuadPart;
}

/*
 * Class:     com_redhat_thermostat_common_portability_internal_windows_WindowsHelperImpl
 * Method:    getProcessMemoryInfo0
 * Signature: (I[J)V
 */
JNIEXPORT jboolean JNICALL Java_com_redhat_thermostat_common_portability_internal_windows_WindowsHelperImpl_getProcessInfo0
  (JNIEnv *env, jclass winHelperClass, jint pid, jlongArray array)
{
    testLength(env, array, 4);

    HANDLE hProcess;
    PROCESS_MEMORY_COUNTERS pmc;

    hProcess = (pid == 0) ? GetCurrentProcess() : OpenProcess( PROCESS_QUERY_INFORMATION | PROCESS_VM_READ, FALSE, pid );
    if (NULL == hProcess)
        return FALSE;

    // Get the element pointer
    jlong* data = (*env)->GetLongArrayElements(env, array, 0);

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
        // times returned from GetProcessTimes() are in 100-nanosecond units.
        data[1] = convertFileTimeToInt64(&userTime);
        data[2] = convertFileTimeToInt64(&kernelTime);
        data[3] = convertFileTimeToInt64(&creationTime);
        data[4] = 10000000; // 100 nanonseconds is this many ticks per second
    }

    CloseHandle(hProcess);

    (*env)->ReleaseLongArrayElements(env, array, data, 0);
    return TRUE;
}

/*
 * Class:     com_redhat_thermostat_common_portability_internal_windows_WindowsHelperImpl
 * Method:    getProcessIOInfo0
 * Signature: (I[J)V
 */
JNIEXPORT jboolean JNICALL Java_com_redhat_thermostat_common_portability_internal_windows_WindowsHelperImpl_getProcessIOInfo0
  (JNIEnv *env, jclass winHelperClass, jint pid, jlongArray array)
{
    testLength(env, array, 6);

    HANDLE hProcess;
    IO_COUNTERS iocounters;

    hProcess = (pid == 0) ? GetCurrentProcess() : OpenProcess( PROCESS_QUERY_INFORMATION | PROCESS_VM_READ, FALSE, pid );
    if (NULL == hProcess)
        return FALSE;

    BOOL rc = GetProcessIoCounters( hProcess, &iocounters );
    if (!rc) {
        CloseHandle(hProcess);
        return FALSE;
    }

    // Get the element pointer
    jlong* data = (*env)->GetLongArrayElements(env, array, 0);

    data[0] = iocounters.ReadOperationCount;
    data[1] = iocounters.WriteOperationCount;
    data[2] = iocounters.ReadTransferCount;
    data[3] = iocounters.WriteTransferCount;
    data[4] = iocounters.OtherOperationCount;
    data[5] = iocounters.OtherTransferCount;

    (*env)->ReleaseLongArrayElements(env, array, data, 0);
    CloseHandle(hProcess);
    return TRUE;
}


/*
 * Class:     com_redhat_thermostat_common_portability_internal_windows_WindowsHelperImpl
 * Method:    getProcessHandle0
 * Signature: (I)J
 */
JNIEXPORT jlong JNICALL Java_com_redhat_thermostat_common_portability_internal_windows_WindowsHelperImpl_getCurrentProcessHandle0
  (JNIEnv *env, jclass winHelperClass) {

    HANDLE hProcess;
    hProcess = GetCurrentProcess();
    return (jlong) hProcess;
}

/*
 * Class:     com_redhat_thermostat_common_portability_internal_windows_WindowsHelperImpl
 * Method:    getProcessHandle0
 * Signature: (I)J
 */
JNIEXPORT jlong JNICALL Java_com_redhat_thermostat_common_portability_internal_windows_WindowsHelperImpl_getProcessHandle0
  (JNIEnv *env, jclass winHelperClass, jint pid) {

    HANDLE hProcess = (pid == 0) ? GetCurrentProcess() : OpenProcess( PROCESS_QUERY_INFORMATION | PROCESS_VM_READ, FALSE, pid );
    return (jlong) hProcess;
}

/*
 * Class:     com_redhat_thermostat_common_portability_internal_windows_WindowsHelperImpl
 * Method:    getLimitedProcessHandle0
 * Signature: (I)J
 */
JNIEXPORT jlong JNICALL Java_com_redhat_thermostat_common_portability_internal_windows_WindowsHelperImpl_getLimitedProcessHandle0
  (JNIEnv *env, jclass winHelperClass, jint pid) {

    HANDLE hProcess = (pid == 0) ? GetCurrentProcess() : OpenProcess( PROCESS_QUERY_LIMITED_INFORMATION, FALSE, pid );
    return (jlong) hProcess;
}

/*
 * Class:     com_redhat_thermostat_common_portability_internal_windows_WindowsHelperImpl
 * Method:    closeHandle0
 * Signature: (J)V
 */
JNIEXPORT void JNICALL Java_com_redhat_thermostat_common_portability_internal_windows_WindowsHelperImpl_closeHandle0
  (JNIEnv *env, jclass winHelperClass, jlong handle){

    CloseHandle((HANDLE)handle);
}

/*
 * Class:     com_redhat_thermostat_common_portability_internal_windows_WindowsHelperImpl
 * Method:    terminateProcess0
 * Signature: (IIB)V
 */
JNIEXPORT jboolean JNICALL Java_com_redhat_thermostat_common_portability_internal_windows_WindowsHelperImpl_terminateProcess0
  (JNIEnv *env, jclass winHelperClass, jint pid, jint exitCode, jint waitMillis) {

    HANDLE hProcess = (pid == 0) ? GetCurrentProcess() : OpenProcess( PROCESS_QUERY_INFORMATION | PROCESS_VM_READ, FALSE, pid );
    if (!hProcess) {
        return FALSE;
    }
    TerminateProcess(hProcess, (unsigned int)exitCode);
    if (waitMillis >= 0) {
        WaitForSingleObject(hProcess, waitMillis);
    }
    CloseHandle(hProcess);
    return TRUE;
}


