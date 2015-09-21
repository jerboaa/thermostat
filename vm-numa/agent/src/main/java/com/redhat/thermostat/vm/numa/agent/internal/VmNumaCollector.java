/*
 * Copyright 2012-2015 Red Hat, Inc.
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

package com.redhat.thermostat.vm.numa.agent.internal;

import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.redhat.thermostat.common.utils.LoggingUtils;
import com.redhat.thermostat.common.utils.StreamUtils;
import com.redhat.thermostat.vm.numa.common.VmNumaStat;

public class VmNumaCollector {

    private static final String PROCESS = "numastat";
    private static final Logger logger = LoggingUtils.getLogger(VmNumaCollector.class);

    private final ProcessBuilder processBuilder;
    private final VmNumaStatParser parser;

    public VmNumaCollector(int pid) {
        this.processBuilder = new ProcessBuilder(PROCESS, String.valueOf(pid));
        this.parser = new VmNumaStatParser();
    }

    public VmNumaStat collect() throws ParseException {
        String output = getOutput();
        return parser.parse(output);
    }

    private String getOutput() {
        try {
            Process p = startProcess();

            InputStream is = p.getInputStream();
            return new String(StreamUtils.readAll(is));
        } catch (InterruptedException | IOException e) {
            logger.log(Level.WARNING, "Unable to run process numastat");
        }
        return "";
    }

    //For testing
    Process startProcess() throws InterruptedException, IOException {
        Process p = processBuilder.start();
        p.waitFor();
        return p;
    }
}
