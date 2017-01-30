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

package com.redhat.thermostat.common.portability.linux;

import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;

/**
 * Wrapper for files under {@code /proc/}. See proc(5) for details about this.
 *
 * This class is inherently unportable, but a _lot_ of Linux code needs refactoring
 * before it can be make package private
 *
 * Note that different Unix-like OSs may or may not have a /proc (and the format may be different)
 * for example Darwin/OSX doesn't have /proc
 */
public class ProcDataSource {

    private static final String LOAD_FILE = "/proc/loadavg";
    private static final String STAT_FILE = "/proc/stat";
    private static final String MEMINFO_FILE = "/proc/meminfo";
    private static final String CPUINFO_FILE = "/proc/cpuinfo";

    private static final String PID_ENVIRON_FILE = "/proc/${pid}/environ";
    private static final String PID_IO_FILE = "/proc/${pid}/io";
    private static final String PID_STAT_FILE = "/proc/${pid}/stat";
    private static final String PID_STATUS_FILE = "/proc/${pid}/status";

    /**
     * Returns a reader for /proc/cpuinfo
     */
    public Reader getCpuInfoReader() throws IOException {
        return new FileReader(CPUINFO_FILE);
    }

    /**
     * Returns a reader for /proc/loadavg
     */
    public Reader getCpuLoadReader() throws IOException {
        return new FileReader(LOAD_FILE);
    }

    /**
     * Returns a reader for /proc/stat. Kernel/System statistics.
     */
    public Reader getStatReader() throws IOException {
        return new FileReader(STAT_FILE);
    }

    /**
     * Returns a reader for /proc/meminfo
     */
    public Reader getMemInfoReader() throws IOException {
        return new FileReader(MEMINFO_FILE);
    }

    /**
     * Returns a reader for /proc/$PID/environ
     */
    public Reader getEnvironReader(int pid) throws IOException {
        return new FileReader(getPidFile(PID_ENVIRON_FILE, pid));
    }

    /**
     * Returns a reader for /proc/$PID/io
     */
    public Reader getIoReader(int pid) throws IOException {
        return new FileReader(getPidFile(PID_IO_FILE, pid));
    }

    /**
     * Returns a reader for /proc/$PID/stat
     */
    public Reader getStatReader(int pid) throws IOException {
        return new FileReader(getPidFile(PID_STAT_FILE, pid));
    }
    
    /**
     * Returns a reader for /proc/$PID/status
     */
    public Reader getStatusReader(int pid) throws IOException {
        return new FileReader(getPidFile(PID_STATUS_FILE, pid));
    }

    private String getPidFile(String fileName, int pid) {
        return fileName.replace("${pid}", Integer.toString(pid));
    }

}

