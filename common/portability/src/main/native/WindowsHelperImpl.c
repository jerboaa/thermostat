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
# include <Sddl.h>
# include <Lm.h>
# include <Winternl.h>
#endif

#if !defined(STATUS_NOT_IMPLEMENTED)
# define STATUS_NOT_IMPLEMENTED 0xC0000002;
#endif

#define MAX_NAME 256
#ifndef NI_MAXHOST
#define NI_MAXHOST 1025
#endif /* NI_MAXHOST */

// to debug the getEnvironment0() code, uncomment this
//#define DEBUG_GETENV

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
  (JNIEnv *env, jclass winHelperClass, jboolean prependDomain) {

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
  (JNIEnv *env, jclass winHelperClass, jlongArray array) {

    testLength(env, array, 3);

    // Get the element pointer
    jlong* data = (*env)->GetLongArrayElements(env, array, 0);

    // NOTE: after version 8.1, this function is deprecated, and may just return the compile OS
    /***
    OSVERSIONINFOEX vinfo;
    vinfo.dwOSVersionInfoSize = sizeof(vinfo);
    GetVersionEx((LPOSVERSIONINFO)(&vinfo));
    data[0] = vinfo.dwMajorVersion;
    data[1] = vinfo.dwMinorVersion;
    data[2] = vinfo.dwBuildNumber;
    ***/

    LPBYTE bufPtr = 0;
    NetWkstaGetInfo(NULL, 100, &bufPtr);
    data[0] = ((WKSTA_INFO_100*)(bufPtr))->wki100_ver_major;
    data[1] = ((WKSTA_INFO_100*)(bufPtr))->wki100_ver_minor;
    data[2] = 0; /* unavailable */
    NetApiBufferFree(bufPtr);
    (*env)->ReleaseLongArrayElements(env, array, data, 0);
}


/*
 * Class:     com_redhat_thermostat_common_portability_internal_windows_WindowsHelperImpl
 * Method:    getGlobalMemoryStatus0
 * Signature: ([J)V
 */
JNIEXPORT boolean JNICALL Java_com_redhat_thermostat_common_portability_internal_windows_WindowsHelperImpl_getGlobalMemoryStatus0
  (JNIEnv *env, jclass winHelperClass, jlongArray array) {
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
  (JNIEnv *env, jclass winHelperClass) {
    // Get extended ids.
    int CPUInfo[4] = {-1};
    __cpuid(CPUInfo, 0x80000000);
    unsigned int nExIds = CPUInfo[0];

    // Get the information associated with each extended ID.
    char CPUBrandString[0x40] = { 0 };
    for(unsigned int i = 0x80000000; i <= nExIds; ++i) {
        __cpuid(CPUInfo, i);

        // Interpret CPU brand string and cache information.
        if  (i == 0x80000002) {
            memcpy(CPUBrandString, CPUInfo, sizeof(CPUInfo));
        } else if(i == 0x80000003) {
            memcpy(CPUBrandString + 16, CPUInfo, sizeof(CPUInfo));
        } else if(i == 0x80000004) {
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
  (JNIEnv *env, jclass winHelperClass) {

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
  (JNIEnv *env, jclass winHelperClass) {

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
  (JNIEnv *env, jclass winHelperClass, jint pid) {
    HANDLE hProcess;

    hProcess = (pid == 0) ? GetCurrentProcess() : OpenProcess(PROCESS_QUERY_INFORMATION | PROCESS_VM_READ, FALSE, pid);
    if (NULL == hProcess)
        return NULL;

    HANDLE hToken = NULL;

    if(!OpenProcessToken(hProcess, TOKEN_QUERY, &hToken)) {
        CloseHandle( hProcess );
        return NULL;
    }

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
            CloseHandle(hToken);
            CloseHandle(hProcess);
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
        CloseHandle(hToken);
        CloseHandle(hProcess);
        return NULL;
    }

    LPWSTR stringSid;
    ConvertSidToStringSidW(ptu->User.Sid, &stringSid);
    jstring s = (*env)->NewString(env, (const jchar *)stringSid, (jsize)wcslen(stringSid));

    LocalFree(stringSid);

    if (ptu != NULL) {
        HeapFree(GetProcessHeap(), 0, (LPVOID)ptu);
    }
    CloseHandle(hToken);
    CloseHandle(hProcess);
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

    if(!OpenProcessToken( hProcess, TOKEN_QUERY, &hToken)) {
        CloseHandle(hProcess);
        return NULL;
    }

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
            CloseHandle(hToken);
            CloseHandle(hProcess);
            return NULL;
        }

        ptu = (PTOKEN_USER)HeapAlloc(GetProcessHeap(), HEAP_ZERO_MEMORY, dwLength);

        if (ptu == NULL) {
            CloseHandle(hToken);
            CloseHandle(hProcess);
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
        CloseHandle(hToken);
        CloseHandle(hProcess);
        return NULL;
    }

    DWORD dwSize = MAX_NAME;
    SID_NAME_USE SidType;
    wchar_t lpName[MAX_NAME];
    wchar_t lpDomain[MAX_NAME*2 + 1];  // room for '\' + lpName
    jstring s = NULL;

    if( !LookupAccountSidW(NULL , ptu->User.Sid, lpName, &dwSize, lpDomain, &dwSize, &SidType)) {
        DWORD dwResult = GetLastError();
        if(dwResult == ERROR_NONE_MAPPED) {
            wcscpy(lpName, L"NONE_MAPPED");
        } else {
            fprintf(stderr, "LookupAccountSid Error %ld\n", GetLastError());
        }
    } else {
        if (prependDomain) {
            wcscat(lpDomain, L"\\");
            wcscat(lpDomain, lpName);
            s = (*env)->NewString(env, (const jchar *)lpDomain, (jsize)wcslen(lpDomain));
        } else {
            s = (*env)->NewString(env, (const jchar *)lpName, (jsize)wcslen(lpName));
        }
    }

    if (ptu != NULL) {
        HeapFree(GetProcessHeap(), 0, (LPVOID)ptu);
    }
    CloseHandle(hToken);
    CloseHandle(hProcess);
    return s;
}

/**

    The getEnvironment0() code is a redo of some 32-bit code examples found all over the web - updated and turned into JNI

    See

    https://blogs.msdn.microsoft.com/matt_pietrek/2004/08/25/reading-another-processs-environment/
    https://sites.google.com/site/x64lab/home/notes-on-x64-windows-gui-programming/exploring-peb-process-environment-block (blog MPL)
    https://github.com/conix-security/zer0m0n/blob/master/src/driver/include/nt/structures/RTL_USER_PROCESS_PARAMETERS.h (GPL 3)
    https://blog.gapotchenko.com/eazfuscator.net/reading-environment-variables
    https://www.codeproject.com/Articles/25647/Read-Environment-Strings-of-Remote-Process
    https://www.reactos.org/
**/

/**
 * Wrapper to call NtQueryInformationProcess API by Run-Time Dynamic Linking
 * Check MSDN Documentation : http://msdn2.microsoft.com/en-us/library/ms684280(VS.85).aspx
 **/
NTSTATUS QueryInformationProcesss(
		IN HANDLE ProcessHandle,
		IN PROCESSINFOCLASS ProcessInformationClass,
		OUT PVOID ProcessInformation,
		IN ULONG ProcessInformationLength,
		OUT PULONG ReturnLength OPTIONAL
		)
{
	typedef NTSTATUS ( __stdcall *QueryInfoProcess) (
		IN HANDLE ProcessHandle,
		IN PROCESSINFOCLASS ProcessInformationClass,
		OUT PVOID ProcessInformation,
		IN ULONG ProcessInformationLength,
		OUT PULONG ReturnLength OPTIONAL
		);

	HMODULE hModNTDll = LoadLibrary("ntdll.dll");

	if (!hModNTDll) {
		fprintf(stderr, "Error Loading library\n");
	}

	QueryInfoProcess QueryProcInfo = (QueryInfoProcess) GetProcAddress(hModNTDll, "NtQueryInformationProcess");
	if (!QueryProcInfo) {
		fprintf(stderr, "Can't find NtQueryInformationProcess in ntdll.dll");
		return STATUS_NOT_IMPLEMENTED;
	}

	NTSTATUS ntStat =  QueryProcInfo( ProcessHandle,
		  							  ProcessInformationClass,
									  ProcessInformation,
									  ProcessInformationLength,
									  ReturnLength );

	FreeLibrary(hModNTDll);
	return ntStat;
}

static BOOL checkReadAccess(HANDLE hProcess, void* pAddress, int* nSize) {
#if defined(DEBUG_GETENV)
    fprintf(stderr, "enter HasReadAccess(%ld, addr = 0x%lx\n", (long)hProcess, (long)pAddress);
#endif //DEBUG_GETENV
    MEMORY_BASIC_INFORMATION64 memInfo;
    SIZE_T msize = VirtualQueryEx(hProcess, pAddress, (PMEMORY_BASIC_INFORMATION)(&memInfo), sizeof(memInfo));
    if(msize == 0 || PAGE_NOACCESS == memInfo.Protect || PAGE_EXECUTE == memInfo.Protect) {
        *nSize = 0;
        fprintf(stderr, "Failed to query memory access, err=%ld prot=%ld NOACC=%ld  EX=%ld msize=%ld\n", (long)GetLastError(),
             (long)memInfo.Protect, (long)PAGE_NOACCESS, (long)PAGE_EXECUTE, (long)msize);
        return FALSE;
    }
    *nSize = memInfo.RegionSize;
#if defined(DEBUG_GETENV)
    fprintf(stderr, "exit HasReadAccess(addr = 0x%lx size = 0x%lx type=0x%lx\n", (long)pAddress, (long)(memInfo.RegionSize), (long)memInfo.Type);
#endif //DEBUG_GETENV
    return TRUE;
}


typedef struct PEB64 {
    BYTE Reserved1[2];
    BYTE BeingDebugged;
    BYTE Reserved2[21];
    PPEB_LDR_DATA LoaderData;
    PRTL_USER_PROCESS_PARAMETERS ProcessParameters;
    //BYTE Reserved3[520];
    BYTE r3[144];
    DWORD OSMajor;  // Warning - OSMajor/Minor/Build don't seem to be correct.
    DWORD OSMinor;
    WORD  OSBuild;
    WORD  r4;
    DWORD OSPlatformID;
    DWORD r5[3];
    BYTE  r6[252];
    PPS_POST_PROCESS_INIT_ROUTINE PostProcessInitRoutine;
    BYTE Reserved4[136];
    ULONG SessionId;
} PEB64;

typedef struct RTL_USER_PROCESS_PARAMETERS64 {
    BYTE Reserved1[16];
    BYTE Reserved2[0x28];
    UNICODE_STRING CurrentWorkingDirectory; // 0x38
    BYTE r3[0x18];
    UNICODE_STRING ImagePathName; // 0x60
    UNICODE_STRING CommandLine;   // 0x70
    PVOID EnvAddr;                // 0x80
  } RTL_USER_PROCESS_PARAMETERS64 ;

/*
 * Caveat - does not check for invalid structures in future (or past) versions of Windows
 *
 * callmode = 0 returns DirectByteBuffer, 1 = String cwd, 2 = String execuatable, 3 = String command line
 *
 * Class:     com_redhat_thermostat_common_portability_internal_windows_WindowsHelperImpl
 * Method:    getEnvironment0
 * Signature: ()[Ljava/lang/String;
 */
JNIEXPORT jobject JNICALL
Java_com_redhat_thermostat_common_portability_internal_windows_WindowsHelperImpl_getEnvironment0
  (JNIEnv *env, jclass winHelperClass, jlong hProcess, jint callmode) {
    PROCESS_BASIC_INFORMATION procBasicInfo = { 0 };

    ULONG uReturnLength = 0;
    NTSTATUS ntStat = QueryInformationProcesss((HANDLE)hProcess,
        ProcessBasicInformation,
        &procBasicInfo,
        sizeof(procBasicInfo),
        &uReturnLength);

    if (ntStat != 0) {
        fprintf(stderr, "QueryInformationProcess() returns error 0x%0lx (id=%ld retlen=%ld PBI size=%ld)\n", (long)ntStat,
            (long)procBasicInfo.UniqueProcessId, (long)uReturnLength, (long)sizeof(procBasicInfo));
    }

#if defined(DEBUG_GETENV)
    fprintf(stderr, "QueryInformationProcesss() ret=%ld id=%ld retlen=%ld PBI size=%ld\n",
        (long)ntStat, (long)procBasicInfo.UniqueProcessId, (long)uReturnLength, (long)sizeof(procBasicInfo));
#endif //DEBUG_GETENV

    // Read the process environment block
    PEB64 procEnvBlock = { 0 };
    SIZE_T returnByteCount = 0;

#if defined(DEBUG_GETENV)
    fprintf(stderr, "reading PEB64 at 0x%ld pebsize=%ld\n", (long)procBasicInfo.PebBaseAddress, (long)sizeof(procEnvBlock));
#endif //DEBUG_GETENV

    // read the PEB from the other process (which is assumed to be 64-bit)
    if (!ReadProcessMemory((HANDLE)hProcess,(LPCVOID)procBasicInfo.PebBaseAddress, &procEnvBlock, sizeof(procEnvBlock), &returnByteCount)) {
        fprintf(stderr, "Error Reading Process Memory err=%ld", GetLastError());
        return NULL;
    }

#if defined(DEBUG_GETENV)
    fprintf(stderr, "read PEB64 at 0x%ld size 0x%ld ppaddr=0x%ld\n", (long)procBasicInfo.PebBaseAddress,
        (long)returnByteCount, (long)procEnvBlock.ProcessParameters);
    fprintf(stderr, "OS is %d.%d build %d platform 0x%lx\n", procEnvBlock.OSMajor, procEnvBlock.OSMinor, procEnvBlock.OSBuild, (long)procEnvBlock.OSPlatformID);
#endif //DEBUG_GETENV

    // Get the address of RTL_USER_PROCESS_PARAMETERS structure
    UCHAR* puPEB = (UCHAR*)&procEnvBlock;
    // retrieve a 64-bit pointer (to an usnsigned char) from PEB + 0x20
    // this code appears to retrieve a 32 bit pointer
    UCHAR* pRTLUserInfo = (UCHAR*) *((UINT_PTR *)(puPEB + 0x20));
    int readableSize = 0;

#if defined(DEBUG_GETENV)
    fprintf(stderr, "testing RTL_USER_PROCESS_PARAMETERS memory at 0x%ld\n", (long)pRTLUserInfo);
#endif //DEBUG_GETENV

    if (!checkReadAccess((HANDLE)hProcess, pRTLUserInfo, &readableSize)) {
        fprintf(stderr, "Error Reading Process Memory err=%ld", GetLastError());
        return NULL;
    }

#if defined(DEBUG_GETENV)
    fprintf(stderr, "zzz memory at 0x%ld size=0x%ld\n", (long)pRTLUserInfo, (long)readableSize);
    fprintf(stderr, "reading RTLUserInfo at 0x%ld size 0x%ld\n", (long)pRTLUserInfo, (long)sizeof(RTL_USER_PROCESS_PARAMETERS64));
#endif //DEBUG_GETENV

    // Get the first 0x64 bytes of RTL_USER_PROCESS_PARAMETERS strcuture
    RTL_USER_PROCESS_PARAMETERS64 upp = {0};
    if (!ReadProcessMemory((HANDLE)hProcess, (LPCVOID)pRTLUserInfo, &upp, sizeof(RTL_USER_PROCESS_PARAMETERS64), &returnByteCount)) {
        fprintf(stderr, "Error Reading Process Memory err=%ld", GetLastError());
        return NULL;
    }

#if defined(DEBUG_GETENV)
    fprintf(stderr, "have read RTLUserInfo at 0x%ld size 0x%ld\n", (long)pRTLUserInfo, (long)returnByteCount);
#endif //DEBUG_GETENV

#if defined(DEBUG_GETENV)
    unsigned short* ch = (unsigned short*)(&upp);
    for (int i = 0; i < sizeof(RTL_USER_PROCESS_PARAMETERS64) / 2; i++) {
        fprintf(stderr, "  0x%04x = 0x%04x\n", i*2, ch[i]);
    }
#endif //DEBUG_GETENV

    // cwd
    if (callmode == 1) {
#if defined(DEBUG_GETENV)
        fprintf(stderr, "reading CurrentWorkingDirectory string at 0x%lx length %ld\n", (long)upp.CurrentWorkingDirectory.Buffer, (long)upp.CurrentWorkingDirectory.Length);
#endif //DEBUG_GETENV
        WCHAR* sb = malloc(upp.CurrentWorkingDirectory.Length);
        if (!ReadProcessMemory((HANDLE)hProcess, (LPCVOID)upp.CurrentWorkingDirectory.Buffer, sb, upp.CurrentWorkingDirectory.Length, &returnByteCount)) {
            fprintf(stderr, "Error Reading Process Memory err=%ld bytes=%ld\n", GetLastError(), (long)returnByteCount);
            free(sb);
            return NULL;
        }
        jstring s = (*env)->NewString(env, (const jchar *)sb, (jsize)upp.CurrentWorkingDirectory.Length / 2);
        free(sb);
        return s;
    }

    // executable image path
    if (callmode == 2) {
#if defined(DEBUG_GETENV)
        fprintf(stderr, "reading string at 0x%lx length %ld\n", (long)upp.ImagePathName.Buffer, (long)upp.ImagePathName.Length);
#endif //DEBUG_GETENV
        WCHAR* sb = malloc(upp.ImagePathName.Length);
        if (!ReadProcessMemory((HANDLE)hProcess, (LPCVOID)upp.ImagePathName.Buffer, sb, upp.ImagePathName.Length, &returnByteCount)) {
            fprintf(stderr, "Error Reading Process Memory err=%ld bytes=%ld\n", GetLastError(), (long)returnByteCount);
            free(sb);
            return NULL;
        }
        jstring s = (*env)->NewString(env, (const jchar *)sb, (jsize)upp.ImagePathName.Length / 2);
        free(sb);
        return s;
    }

    // command line
    if (callmode == 3) {
#if defined(DEBUG_GETENV)
        fprintf(stderr, "reading string at 0x%lx length %ld\n", (long)upp.CommandLine.Buffer, (long)upp.CommandLine.Length);
#endif //DEBUG_GETENV
        WCHAR* sb = malloc(upp.CommandLine.Length);
        if (!ReadProcessMemory((HANDLE)hProcess, (LPCVOID)upp.CommandLine.Buffer, sb, upp.CommandLine.Length, &returnByteCount)) {
            fprintf(stderr, "Error Reading Process Memory err=%ld bytes=%ld\n", GetLastError(), (long)returnByteCount);
            free(sb);
            return NULL;
        }
        jstring s = (*env)->NewString(env, (const jchar *)sb, (jsize)upp.CommandLine.Length / 2);
        free(sb);
        return s;
    }

    // Get the value at offset 0x48 to get the pointer to environment string block
    if (callmode == 0) {
        WCHAR* strPtr = (WCHAR*)(upp.EnvAddr);

        // find out how much we can read, and then read it
        // this will read more than just the environment, but there's not a lot we can do cleanly
        if (!checkReadAccess((HANDLE)hProcess, strPtr, &readableSize)) {
            fprintf(stderr, "Error Reading Process Memory err=%ld siz=0x%lx", GetLastError(), (long)readableSize);
            return NULL;
        }
        // constrain readableSize to some maximum
        // 0x38000 was observed on a windows 10 system (it's about 229K, so it's huge)
        static const int MAX_ENV_BUFFER_SIZE = 0x38000;
        if (readableSize > MAX_ENV_BUFFER_SIZE) {
            readableSize = MAX_ENV_BUFFER_SIZE;
        }
#if defined(DEBUG_GETENV)
        fprintf(stderr, "reading string at 0x%lx length %ld\n", (long)strPtr, (long)readableSize);
#endif //DEBUG_GETENV
        WCHAR* sb = malloc(readableSize);
        if (!ReadProcessMemory((HANDLE)hProcess, (LPCVOID)strPtr, sb, readableSize, &returnByteCount)) {
            fprintf(stderr, "Error Reading Process Memory err=%ld bytes=%ld\n", GetLastError(), (long)returnByteCount);
            free(sb);
            return NULL;
        }
#if defined(DEBUG_GETENV)
            unsigned char* ch = (unsigned char*)(sb);
            for (int i = 0; i < 100; i++) {
                fprintf(stderr, "  0x%04x = 0x%02x\n", i, ch[i]);
            }
#endif //DEBUG_GETENV
        // NOTE - consider ignoring error 299 (incomplete read) and just return returnByteCount bytes here.
        jobject bytebuffer = (*env)->NewDirectByteBuffer(env, sb, readableSize);
        return bytebuffer;
    }

    return NULL;
}

static unsigned __int64 convertFileTimeToInt64( const FILETIME * pFileTime ) {
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
  (JNIEnv *env, jclass winHelperClass, jint pid, jlongArray array) {
    testLength(env, array, 4);

    HANDLE hProcess;
    PROCESS_MEMORY_COUNTERS pmc;

    hProcess = (pid == 0) ? GetCurrentProcess() : OpenProcess( PROCESS_QUERY_INFORMATION | PROCESS_VM_READ, FALSE, pid );
    if (NULL == hProcess)
        return FALSE;

    // Get the element pointer
    jlong* data = (*env)->GetLongArrayElements(env, array, 0);

    pmc.cb = sizeof(pmc);
    if (GetProcessMemoryInfo(hProcess, &pmc, sizeof(pmc))) {
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

    if (GetProcessTimes( hProcess, &creationTime, &exitTime, &kernelTime, &userTime)) {
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
  (JNIEnv *env, jclass winHelperClass, jint pid, jlongArray array) {
    testLength(env, array, 6);

    HANDLE hProcess;
    IO_COUNTERS iocounters;

    hProcess = (pid == 0) ? GetCurrentProcess() : OpenProcess(PROCESS_QUERY_INFORMATION | PROCESS_VM_READ, FALSE, pid);
    if (NULL == hProcess)
        return FALSE;

    BOOL rc = GetProcessIoCounters(hProcess, &iocounters);
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

    HANDLE hProcess = (pid == 0) ? GetCurrentProcess() : OpenProcess(PROCESS_QUERY_INFORMATION | PROCESS_VM_READ, FALSE, pid);
    return (jlong) hProcess;
}

/*
 * Class:     com_redhat_thermostat_common_portability_internal_windows_WindowsHelperImpl
 * Method:    getCurrentProcessID0
 * Signature: ()I
 */
JNIEXPORT jint JNICALL Java_com_redhat_thermostat_common_portability_internal_windows_WindowsHelperImpl_getCurrentProcessID0
  (JNIEnv *env, jclass winHelperClass) {

    return (jint) GetCurrentProcessId();
}

/*
 * Class:     com_redhat_thermostat_common_portability_internal_windows_WindowsHelperImpl
 * Method:    getLimitedProcessHandle0
 * Signature: (I)J
 */
JNIEXPORT jlong JNICALL Java_com_redhat_thermostat_common_portability_internal_windows_WindowsHelperImpl_getLimitedProcessHandle0
  (JNIEnv *env, jclass winHelperClass, jint pid) {

    HANDLE hProcess = (pid == 0) ? GetCurrentProcess() : OpenProcess(PROCESS_QUERY_LIMITED_INFORMATION, FALSE, pid);
    return (jlong) hProcess;
}

/*
 * Class:     com_redhat_thermostat_common_portability_internal_windows_WindowsHelperImpl
 * Method:    closeHandle0
 * Signature: (J)V
 */
JNIEXPORT void JNICALL Java_com_redhat_thermostat_common_portability_internal_windows_WindowsHelperImpl_closeHandle0
  (JNIEnv *env, jclass winHelperClass, jlong handle) {

    CloseHandle((HANDLE)handle);
}

/*
 * Class:     com_redhat_thermostat_common_portability_internal_windows_WindowsHelperImpl
 * Method:    terminateProcess0
 * Signature: (IIB)V
 */
JNIEXPORT jboolean JNICALL Java_com_redhat_thermostat_common_portability_internal_windows_WindowsHelperImpl_terminateProcess0
  (JNIEnv *env, jclass winHelperClass, jint pid, jint exitCode, jint waitMillis) {

    HANDLE hProcess = (pid == 0) ? GetCurrentProcess() : OpenProcess(PROCESS_QUERY_INFORMATION | PROCESS_VM_READ, FALSE, pid);
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


/*
 * Class:     Java_com_redhat_thermostat_common_portability_internal_windows_WindowsHelperImpl
 * Method:    freeDirectBuffer0
 * Signature: (Ljava/nio/ByteBuffer;)V
 */
JNIEXPORT void JNICALL Java_com_redhat_thermostat_common_portability_internal_windows_WindowsHelperImpl_freeDirectBuffer0
  (JNIEnv *env, jobject obj, jobject bytebuffer) {

    void *buffer = (*env)->GetDirectBufferAddress(env, bytebuffer);
    free(buffer);
}
