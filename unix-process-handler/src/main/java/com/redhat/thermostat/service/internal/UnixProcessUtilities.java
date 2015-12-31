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
    
    private static final UNIXProcessHandler instance = new UnixProcessUtilities();
    public static UNIXProcessHandler getInstance() {
        return instance;
    }
    
    UnixProcessUtilities() {}
    
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
    
    @Override
    public String getProcessName(Integer pid) {
        
        String result = null;
        
        List<String> commandLine = new ArrayList<>();
        commandLine.add("ps");
        commandLine.add("--no-heading");
        commandLine.add("-p");
        commandLine.add(String.valueOf(pid));
        
        try {
            Process process = createAndRunProcess(commandLine);
            BufferedReader reader = getProcessOutput(process);
            result = reader.readLine();
            if (result != null) {
                String [] output = result.split(" ");
                result = output[output.length - 1];
            }
            
        } catch (IOException | ApplicationException e) {
            logger.log(Level.WARNING, "can't run ps!", e);
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

