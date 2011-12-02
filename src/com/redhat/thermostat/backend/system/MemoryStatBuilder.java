package com.redhat.thermostat.backend.system;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.redhat.thermostat.common.Constants;
import com.redhat.thermostat.common.MemoryStat;
import com.redhat.thermostat.common.NotImplementedException;
import com.redhat.thermostat.common.utils.LoggingUtils;

/**
 * Implementation note: uses information from /proc/
 */
public class MemoryStatBuilder {

    private static final long UNAVAILABLE = -1;

    private static final String MEMINFO_FILE = "/proc/meminfo";

    private static final String KEY_MEMORY_TOTAL = "MemTotal";
    private static final String KEY_MEMORY_FREE = "MemFree";
    private static final String KEY_BUFFERS = "Buffers";
    private static final String KEY_CACHED = "Cached";
    private static final String KEY_SWAP_TOTAL = "SwapTotal";
    private static final String KEY_SWAP_FREE = "SwapFree";
    private static final String KEY_COMMIT_LIMIT = "CommitLimit";

    private static final Logger logger = LoggingUtils.getLogger(MemoryStatBuilder.class);

    public MemoryStat build() {
        long timestamp = System.currentTimeMillis();

        long total = UNAVAILABLE;
        long free = UNAVAILABLE;
        long swapTotal = UNAVAILABLE;
        long swapFree = UNAVAILABLE;
        long buffers = UNAVAILABLE;
        long cached = UNAVAILABLE;
        long commitLimit = UNAVAILABLE;
        try {
            BufferedReader reader = new BufferedReader(new FileReader(MEMINFO_FILE));
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
        } catch (IOException e) {
            logger.log(Level.WARNING, "unable to read " + MEMINFO_FILE);
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
                if (units.equals("kB") || units.equals("KB")) {
                    result = result * Constants.KILOBYTES_TO_BYTES;
                } else {
                    throw new NotImplementedException("unit conversion from " + units + " not implemented");
                }
            }
        } catch (NumberFormatException nfe) {
            logger.log(Level.WARNING, "error extracting memory info from " + MEMINFO_FILE);
        }

        return result;
    }
}
