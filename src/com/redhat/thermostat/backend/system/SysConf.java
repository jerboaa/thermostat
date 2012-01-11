package com.redhat.thermostat.backend.system;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * A wrapper over POSIX's sysconf.
 * <p>
 * Implementation notes: uses {@code getconf(1)}
 */
public class SysConf {

    private SysConf() {
        /* do not initialize */
    }

    public static long getClockTicksPerSecond() {
        String ticks = sysConf("CLK_TCK");
        try {
            return Long.valueOf(ticks);
        } catch (NumberFormatException nfe) {
            return 0;
        }
    }

    private static String sysConf(String arg) {
        BufferedReader reader = null;
        try {
            Process process = Runtime.getRuntime().exec(new String[] { "getconf", arg });
            int result = process.waitFor();
            if (result != 0) {
                return null;
            }
            reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            return reader.readLine();
        } catch (IOException e) {
            return null;
        } catch (InterruptedException e) {
            return null;
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    // TODO
                }
            }
        }
    }
}
