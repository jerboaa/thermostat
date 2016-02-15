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

package com.redhat.thermostat.vm.io.agent.internal;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.redhat.thermostat.agent.utils.ProcDataSource;
import com.redhat.thermostat.common.utils.LoggingUtils;

/**
 * Extract information from {@code /proc/<pid>/io}.
 */
public class ProcIoDataReader {

    private static final Logger logger = LoggingUtils.getLogger(ProcIoDataReader.class);

    private final ProcDataSource dataSource;

    public ProcIoDataReader(ProcDataSource dataSource) {
        this.dataSource = dataSource;
    }

    public ProcIoData read(int pid) {
        try (BufferedReader reader = new BufferedReader(dataSource.getIoReader(pid))) {
            return read(reader);
        } catch (IOException e) {
            logger.log(Level.WARNING, "unable to read io info for " + pid, e);
        }

        return null;
    }

    private ProcIoData read(BufferedReader r) throws IOException {
        // The file format is described at:
        // http://git.kernel.org/cgit/linux/kernel/git/torvalds/linux.git/tree/Documentation/filesystems/proc.txt

        final int UNKNOWN_VALUE = -1;
        long rchar = UNKNOWN_VALUE;
        long wchar = UNKNOWN_VALUE;
        long syscr = UNKNOWN_VALUE;
        long syscw = UNKNOWN_VALUE;
        long read_bytes = UNKNOWN_VALUE;
        long write_bytes = UNKNOWN_VALUE;
        long cancelled_write_bytes = UNKNOWN_VALUE;

        String line;
        while ((line = r.readLine()) != null) {
            String[] parts = line.split(":");
            String key = parts[0].trim();
            String value = parts[1].trim();
            switch (key) {
                case "rchar":
                    rchar = Long.valueOf(value);
                    break;
                case "wchar":
                    wchar = Long.valueOf(value);
                    break;
                case "syscr":
                    syscr = Long.valueOf(value);
                    break;
                case "syscw":
                    syscw = Long.valueOf(value);
                    break;
                case "read_bytes":
                    read_bytes = Long.valueOf(value);
                    break;
                case "write_bytes":
                    write_bytes = Long.valueOf(value);
                    break;
                case "cancelled_write_bytes":
                    cancelled_write_bytes = Long.valueOf(value);
                    break;
            }
        }

        return new ProcIoData(rchar, wchar, syscr, syscw, read_bytes, write_bytes, cancelled_write_bytes);
    }

}

