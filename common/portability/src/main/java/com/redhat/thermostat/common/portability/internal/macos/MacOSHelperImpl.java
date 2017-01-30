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

package com.redhat.thermostat.common.portability.internal.macos;

import com.redhat.thermostat.common.utils.LoggingUtils;
import com.redhat.thermostat.shared.config.NativeLibraryResolver;
import com.redhat.thermostat.shared.config.OS;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Utility class to access Windows native code
 */
public class MacOSHelperImpl {

    private static final Logger logger = LoggingUtils.getLogger(MacOSHelperImpl.class);

    private static int pagesize = 0;

    public static MacOSHelperImpl INSTANCE;

    static {
        if (OS.IS_MACOS) {
            String lib = NativeLibraryResolver.getAbsoluteLibraryPath("MacOSHelperWrapper");
            try {
                System.load(lib);
                INSTANCE = new MacOSHelperImpl();
                pagesize = (int)getLongSysctl0("vm.pagesize");
            } catch (UnsatisfiedLinkError e) {
                logger.severe("Could not load MacOSHelperWrapper DLL:" + lib);
                INSTANCE = null;
                // do not throw here, because you'll get a NoClassDefFound thrown when running other tests that Mock this class
            }
        } else {
            INSTANCE = null;
        }
    }

    private MacOSHelperImpl() {
    }
    // local host-wide information

    public String getHostName() {
        return getHostName0();
    }

    String getOSName() {
        return System.getProperty("os.name") + " " + System.getProperty("os.version");
    }

    String getOSVersion() {
        final String ostype = getStringSysctl0("kern.ostype");
        final String osrelease = getStringSysctl0("kern.osrelease");
        return ostype + " (Build " + osrelease + ")";
    }

    String getCPUModel() {
        return getStringSysctl0("machdep.cpu.brand_string");
    }

    public int getCPUCount() {
        return (int)getLongSysctl0("hw.logicalcpu"); // factors in hyperthreads
        //return (int)getLongSysctl0("hw.physicalcpu"); (excludes hyperthreading)
    }

    public long getTotalMemory() {
        return getLongSysctl0("hw.memsize");
    }

    long[] getMemoryInfo() {
        /*
            public PortableMemoryStat(long timeStamp,
            long total, long free, long buffers,
            long cached, long swapTotal, long swapFree, long commitLimit)
         */
        final long[] mi = new long[8];
        getGlobalMemoryStatus0(mi);
        return mi;
    }

    public long getClockTicksPerSecond() {
        // we "know" this is 10E7 (units from  getProcessStat)
        // but calculate it anyways from current process
        final long[] info = INSTANCE.getProcessCPUInfo(0);
        return info[4];
    }

    // local process-specific information

    public boolean exists(int pid) {
        return getUid(pid) >= 0;
    }

    public String getUserName(int pid) {
        return getUserName0(pid);
    }

    public int getUid(int pid) {
        final long uid = getProcessUid0(pid);
        return (int)uid;
    }

    Map<String, String> getEnvironment(int pid) {
        // the environment is returned as a 1D array of alternating env names and values
        final String[] envArray = getEnvironment0(pid);
        if (envArray == null || envArray.length == 0) {
            return Collections.emptyMap();
        }

        if (envArray.length % 2 != 0) {
            throw new AssertionError("environment array length not even");
        }

        final Map<String, String> env = new HashMap<>(envArray.length/2);
        for (int i = 0; i < envArray.length / 2; i++) {
            env.put(envArray[i * 2], envArray[i * 2 + 1]);
        }
        return env;
    }

    /**
     * fetch process counters
     * @param pid process id
     * @return long[] working set size, user time, kernel time, (todo: elapsed time), ticks per second
     */
    long[] getProcessCPUInfo(int pid) {
        final long[] info = new long[5];
        getProcessInfo0(pid, info);
        return info;
    }

    long[] getProcessMemInfo(int pid) {
        final long[] info = new long[5];
        getProcessInfo0(pid, info);
        return info;
    }

    long[] getProcessIOInfo(int pid) {
        final long info[] = new long[6];
        getProcessIOInfo0(pid,info);
        return info;
    }

    boolean terminateProcess(int pid) {
        return terminateProcess0(pid,0, -1);
    }

    boolean terminateProcess(int pid, boolean wait) {
        return terminateProcess0(pid,0, 0);
    }

    boolean terminateProcess(int pid, int exitcode, int waitMillis) {
        return terminateProcess0(pid, exitcode, waitMillis);
    }

    public static native long getLongSysctl0( String name );
    public static native String getStringSysctl0( String name );

    private static native String getHostName0();
    private static native boolean getGlobalMemoryStatus0(long[] info);
    private static native boolean getPerformanceInfo0(long[] info);
    private static native long queryPerformanceFrequency0();

    private static native String getUserName0(int pid);
    private static native long getProcessUid0(int pid);
    private static native String[] getEnvironment0(int pid);
    private static native boolean getProcessInfo0(int pid, long[] info);
    private static native boolean getProcessIOInfo0(int pid, long[] info);

    private static native long getCurrentProcessPid0();

    private static native boolean terminateProcess0(int pid, int exitCode, int waitMillis);
}
