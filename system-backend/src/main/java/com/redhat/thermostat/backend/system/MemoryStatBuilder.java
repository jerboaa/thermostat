/*
 * Copyright 2012 Red Hat, Inc.
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

package com.redhat.thermostat.backend.system;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.redhat.thermostat.common.NotImplementedException;
import com.redhat.thermostat.common.Size;
import com.redhat.thermostat.common.utils.LoggingUtils;
import com.redhat.thermostat.storage.model.MemoryStat;
import com.redhat.thermostat.utils.ProcDataSource;

/**
 * Implementation note: uses information from /proc/
 */
public class MemoryStatBuilder {

    private static final long UNAVAILABLE = -1;

    private static final String KEY_MEMORY_TOTAL = "MemTotal";
    private static final String KEY_MEMORY_FREE = "MemFree";
    private static final String KEY_BUFFERS = "Buffers";
    private static final String KEY_CACHED = "Cached";
    private static final String KEY_SWAP_TOTAL = "SwapTotal";
    private static final String KEY_SWAP_FREE = "SwapFree";
    private static final String KEY_COMMIT_LIMIT = "CommitLimit";

    private static final Logger logger = LoggingUtils.getLogger(MemoryStatBuilder.class);

    private final ProcDataSource dataSource;

    public MemoryStatBuilder(ProcDataSource dataSource) {
        this.dataSource = dataSource;
    }

    protected MemoryStat build() {
        long timestamp = System.currentTimeMillis();

        long total = UNAVAILABLE;
        long free = UNAVAILABLE;
        long swapTotal = UNAVAILABLE;
        long swapFree = UNAVAILABLE;
        long buffers = UNAVAILABLE;
        long cached = UNAVAILABLE;
        long commitLimit = UNAVAILABLE;

        try (BufferedReader reader = new BufferedReader(dataSource.getMemInfoReader())) {
            String line = null;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(":");
                if (parts.length == 2) {
                    String key = parts[0].trim();
                    long value = getValue(parts[1].trim());
                    if (key.equals(KEY_MEMORY_TOTAL)) {
                        total = value;
                    } else if (key.equals(KEY_MEMORY_FREE)) {
                        free = value;
                    } else if (key.equals(KEY_SWAP_TOTAL)) {
                        swapTotal = value;
                    } else if (key.equals(KEY_SWAP_FREE)) {
                        swapFree = value;
                    } else if (key.equals(KEY_BUFFERS)) {
                        buffers = value;
                    } else if (key.equals(KEY_CACHED)) {
                        cached = value;
                    } else if (key.equals(KEY_COMMIT_LIMIT)) {
                        commitLimit = value;
                    }
                }
            }
        } catch (IOException ioe) {
            logger.log(Level.WARNING, "unable to read memory info");
        }

        return new MemoryStat(timestamp, total, free, buffers, cached, swapTotal, swapFree, commitLimit);
    }

    private long getValue(String rawValue) {
        String[] parts = rawValue.split(" +");
        String value = rawValue;
        String units = null;
        if (parts.length > 1) {
            value = parts[0];
            units = parts[1];
        }

        long result = UNAVAILABLE;
        try {
            result = Long.parseLong(value);
            if (units != null) {
                // /proc/meminfo uses kB instead of KiB, incorrectly
                if (units.equals("kB") || units.equals("KB")) {
                    result = (long) new Size(result, Size.Unit.KiB).convertTo(Size.Unit.B).getValue();
                } else {
                    throw new NotImplementedException("unit conversion from " + units + " not implemented");
                }
            }
        } catch (NumberFormatException nfe) {
            logger.log(Level.WARNING, "error extracting memory info");
        }

        return result;
    }
}
