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

package com.redhat.thermostat.vm.cpu.agent.internal;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.redhat.thermostat.agent.utils.ProcDataSource;
import com.redhat.thermostat.common.utils.LoggingUtils;

/**
 * Extract status information about the process from /proc/. This is what tools
 * like {@code ps} and {@code top} use.
 *
 * @see {@code proc(5)}
 */
public class ProcessStatusInfoBuilder {

    private static final Logger logger = LoggingUtils.getLogger(ProcessStatusInfoBuilder.class);

    private final ProcDataSource dataSource;

    public ProcessStatusInfoBuilder(ProcDataSource dataSource) {
        this.dataSource = dataSource;
    }

    public ProcessStatusInfo build(int pid) {
        try (BufferedReader reader = new BufferedReader(dataSource.getStatReader(pid))) {
            return build(reader);
        } catch (IOException e) {
            logger.log(Level.WARNING, "unable to read stat info for " + pid);
        }

        return null;
    }

    private ProcessStatusInfo build(Reader r) throws IOException {

        int pid = -1;
        long utime = -1;
        long stime = -1;

        Scanner scanner = null;

        /* TODO map these (effectively c) data types to java types more sanely */

        try (BufferedReader reader = new BufferedReader(r)) {
            String statusLine = reader.readLine();

            /* be prepared for process names like '1 ) 2 3 4 foo 5' */

            scanner = new Scanner(statusLine);
            pid = scanner.nextInt();
            scanner.close();

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
        }

        return new ProcessStatusInfo(pid, utime, stime);

    }

}

