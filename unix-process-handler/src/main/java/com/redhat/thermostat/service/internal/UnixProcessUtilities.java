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

package com.redhat.thermostat.service.internal;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.redhat.thermostat.common.tools.ApplicationException;
import com.redhat.thermostat.common.tools.ProcessStartException;
import com.redhat.thermostat.common.utils.LoggingUtils;
import com.redhat.thermostat.service.process.UNIXProcessHandler;
import com.redhat.thermostat.service.process.UNIXSignal;

class UnixProcessUtilities implements UNIXProcessHandler {

    private static final Logger logger = LoggingUtils.getLogger(UnixProcessUtilities.class);

    private static final boolean IS_UNIX = !System.getProperty("os.name").contains("Windows");

    private static final UNIXProcessHandler instance = IS_UNIX ? new UnixProcessUtilities() : new WindowsProcessUtilities();
    public static UNIXProcessHandler getInstance() {
        return instance;
    }
    
    UnixProcessUtilities() {}

    static class WindowsProcessUtilities extends UnixProcessUtilities {
        public WindowsProcessUtilities() {}

        List<String> buildCommandLine(Integer pid) {
            final List<String> commandLine = new ArrayList<>();
            commandLine.add("tasklist");
            commandLine.add("/FO");
            commandLine.add("csv");
            commandLine.add("/FI");
            commandLine.add("\"PID eq " + String.valueOf(pid) + "\"");
            return commandLine;
        }

        String processStdout(final String outStr) {
            final String [] output = outStr.split(",");
            String result = output[0];
            if (result.length() >= 2 && result.charAt(0) == '"')
                result = result.replace("\"","");
            result = result.replace(".exe","");
            return result;
        }
    }

    @Override
    public void sendSignal(Integer pid, UNIXSignal signal) {
        exec("kill -s " + signal.signalName() + " " + pid);
    }
    
    void exec(String command) {
        Runtime rt = Runtime.getRuntime();
        try {
            rt.exec(command);
        } catch (IOException e) {
            logger.log(Level.WARNING, "can't run kill!", e);
        }
    }

    List<String> buildCommandLine(Integer pid) {
        final List<String> commandLine = new ArrayList<>();
        commandLine.add("ps");
        commandLine.add("--no-heading");
        commandLine.add("-p");
        commandLine.add(String.valueOf(pid));
        return commandLine;
    }

    String processStdout(final String outStr) {
        final String [] output = outStr.split(" ");
        return output[output.length - 1];
    }

    @Override
    public String getProcessName(Integer pid) {
        
        String result = null;
        
        final List<String> commandLine = buildCommandLine(pid);
        
        try {
            Process process = createAndRunProcess(commandLine);
            BufferedReader reader = getProcessOutput(process);
            if (!IS_UNIX) reader.readLine(); // skip header line
            result = reader.readLine();
            if (result != null)
                result = processStdout(result);
            
        } catch (IOException | ApplicationException e) {
            logger.log(Level.WARNING, "can't run '" + commandLine.get(0) + "'!", e);
        }
        
        return result;
    }
    
    /** package-private for testing */
    BufferedReader getProcessOutput(Process process) {
        InputStream in = process.getInputStream();
        InputStreamReader isr = new InputStreamReader(in);
        return new BufferedReader(isr);
    }
    
    /** package-private for testing */
    Process createAndRunProcess(List<String> args) throws IOException, ApplicationException {
        ProcessBuilder builder = new ProcessBuilder(args);
        Process process = null;
        try {
            process = builder.start();
        } catch (IOException e) {
            throw new ProcessStartException(args.get(0), e);
        }
        return process;
    }
}

