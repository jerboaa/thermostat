package com.redhat.thermostat.backend.system;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.redhat.thermostat.common.utils.LoggingUtils;

/**
 * Extract status information about the process from /proc/. This is what tools
 * like {@code ps} and {@code top} use.
 *
 * @see {@code proc(5)}
 */
public class ProcessStatusInfo {

    private static final Logger logger = LoggingUtils.getLogger(ProcessStatusInfo.class);

    /* All times are measured in clock ticks */

    /* TODO map these (effectively c) data types to java types more sanely */

    private int pid;
    private long utime;
    private long stime;

    public static ProcessStatusInfo getFor(int pid) {
        return new ProcessStatusInfo(pid);
    }

    private ProcessStatusInfo(int pid) {
        Scanner scanner = null;
        String fileName = "/proc/" + pid + "/stat";
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new FileReader(fileName));
            String statusLine = reader.readLine();

            /* be prepared for process names like '1 ) 2 3 4 foo 5' */

            scanner = new Scanner(statusLine);
            this.pid = scanner.nextInt();
            scanner.close();

            int execStartNamePos = statusLine.indexOf('(');
            int execEndNamePos = statusLine.lastIndexOf(')');

            String cleanStatusLine = statusLine.substring(execEndNamePos + 1);

            scanner = new Scanner(cleanStatusLine);
            /* state = */scanner.next();
            /* ppid = */scanner.nextInt();
            /* pgrp = */scanner.nextInt();
            /* session = */scanner.nextInt();
            /* tty_nr = */scanner.nextInt();
            /* tpgid = */scanner.nextInt();
            /* flags = */scanner.nextInt();
            /* minflt = */scanner.nextLong();
            /* cminflt = */scanner.nextLong();
            /* majflt = */scanner.nextLong();
            /* cmajflt = */scanner.nextLong();
            utime = scanner.nextLong();
            stime = scanner.nextLong();
            scanner.close();

        } catch (IOException e) {
            logger.log(Level.WARNING, "unable to read " + fileName);
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    logger.log(Level.WARNING, "unable to close " + fileName);
                }
            }
        }
    }

    public int getPid() {
        return pid;
    }

    /**
     * @return the time this process has spent in user-mode as a number of
     * kernel ticks
     */
    public long getUserTime() {
        return utime;
    }

    /**
     * @return the time this process spent in kernel-mode as a number of kernel
     * ticks
     */
    public long getKernelTime() {
        return stime;
    }

}
