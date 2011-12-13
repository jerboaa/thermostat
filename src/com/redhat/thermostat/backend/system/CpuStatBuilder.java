package com.redhat.thermostat.backend.system;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.redhat.thermostat.common.CpuStat;
import com.redhat.thermostat.common.utils.LoggingUtils;

/**
 * Implementation note: uses information from /proc/
 */
public class CpuStatBuilder {

    private static final String LOAD_FILE = "/proc/loadavg";

    private static final Logger logger = LoggingUtils.getLogger(CpuStatBuilder.class);

    public CpuStat build() {
        long timestamp = System.currentTimeMillis();
        double load5 = CpuStat.INVALID_LOAD;
        double load10 = CpuStat.INVALID_LOAD;
        double load15 = CpuStat.INVALID_LOAD;
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new FileReader(LOAD_FILE));
            String[] loadAvgParts = reader.readLine().split(" +");
            if (loadAvgParts.length >= 3) {
                load5 = Double.valueOf(loadAvgParts[0]);
                load10 = Double.valueOf(loadAvgParts[1]);
                load15 = Double.valueOf(loadAvgParts[2]);
            }
        } catch (NumberFormatException nfe) {
            logger.log(Level.WARNING, "error extracting load from " + LOAD_FILE);
        } catch (IOException e) {
            logger.log(Level.WARNING, "unable to read " + LOAD_FILE);
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    logger.log(Level.WARNING, "unable to close " + LOAD_FILE);
                }
            }
        }

        return new CpuStat(timestamp, load5, load10, load15);
    }
}
