/*
 * Copyright 2012-2017 Red Hat, Inc.
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

package com.redhat.thermostat.service.internal.windows;

import com.redhat.thermostat.service.process.ProcessHandler;
import com.redhat.thermostat.service.process.UNIXSignal;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

public class WindowsProcessUtilitiesTest {

    private BufferedReader reader;
    private BufferedReader emptyReader;

    private List<String> processArguments = new ArrayList<>();
    private ProcessHandler process;

    @Before
    public void setUp() {

        String data = "headerline\r\n\"fluff.exe\",\"1868\",\"Console\",\"1\",\"25,952 K\"";
        reader = new BufferedReader(new StringReader(data));
        emptyReader = new BufferedReader(new StringReader(""));

        processArguments.clear();

        process = new WindowsProcessUtilities() {
            @Override
            protected Process createAndRunProcess(List<String> args)
                    throws IOException {
                processArguments.addAll(args);
                return null;
            }

            @Override
            protected void exec(String command) {
                processArguments.add(command);
            }

            @Override
            public BufferedReader getProcessOutput(Process process) {
                return reader;
            }
        };
    }

    @Test
    public void getProcessName() {

        String result = process.getProcessName(12345);
        Assert.assertEquals("fluff", result);
        Assert.assertTrue(processArguments.contains("\"PID eq 12345\""));
        Assert.assertTrue(processArguments.contains("tasklist"));
    }

    @Test
    public void getProcessNameNoOutput() {

        // redefine, since we need an empty reader
        final ProcessHandler process = new WindowsProcessUtilities() {
                @Override
                protected Process createAndRunProcess(List<String> args)
                        throws IOException {
                    processArguments.addAll(args);
                    return null;
                }

                @Override
                public BufferedReader getProcessOutput(Process process) {
                    return emptyReader;
                }
            };

        
        String result = process.getProcessName(12345);
        Assert.assertNull(result);
    }    
}

