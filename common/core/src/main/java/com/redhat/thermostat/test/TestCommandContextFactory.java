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

package com.redhat.thermostat.test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PipedOutputStream;
import java.io.PrintStream;

import org.osgi.framework.BundleContext;

import com.redhat.thermostat.common.cli.Arguments;
import com.redhat.thermostat.common.cli.CommandContext;
import com.redhat.thermostat.common.cli.CommandContextFactory;
import com.redhat.thermostat.common.cli.CommandRegistry;
import com.redhat.thermostat.common.cli.Console;

public class TestCommandContextFactory extends CommandContextFactory {

    private ByteArrayOutputStream out;
    private ByteArrayOutputStream err;
    private ExceptionThrowingInputStream in;

    public TestCommandContextFactory() {
        this(null);
    }

    public TestCommandContextFactory(BundleContext bCtx) {
        super(bCtx);
        reset();
    }

    private CommandRegistry commandRegistry = new TestCommandRegistry();

    private TestConsole console;
    private PipedOutputStream inOut;

    private class TestConsole implements Console {

        @Override
        public PrintStream getOutput() {
            return new PrintStream(out);
        }

        @Override
        public PrintStream getError() {
            return new PrintStream(err);
        }

        @Override
        public InputStream getInput() {
            return in;
        }
        
    }

    @Override
    public CommandContext createContext(final Arguments args) {
        return new CommandContext() {

            @Override
            public Console getConsole() {
                return new TestConsole();
            }

            @Override
            public Arguments getArguments() {
                return args;
            }

            @Override
            public CommandRegistry getCommandRegistry() {
                return commandRegistry;
            }

            @Override
            public CommandContextFactory getCommandContextFactory() {
                return TestCommandContextFactory.this;
            }
            
        };
    }

    @Override
    public CommandRegistry getCommandRegistry() {
        return commandRegistry;
    }

    public String getOutput() {
        return new String(out.toByteArray());
    }

    public void setInput(String input) {
        try {
            inOut.write(input.getBytes());
            inOut.flush();
        } catch (IOException e) {
            RuntimeException ex = new RuntimeException(e);
            throw ex;
        }
    }

    public String getError() {
        return new String(err.toByteArray());
    }

    public void reset() {
        out = new ByteArrayOutputStream();
        err = new ByteArrayOutputStream();
        inOut = new PipedOutputStream();
        try {
            in = new ExceptionThrowingInputStream(inOut);
        } catch (IOException e) {
            RuntimeException ex = new RuntimeException(e);
            throw ex;
        }
        console = new TestConsole();
    }

    public Console getConsole() {
        return console;
    }

    public void setInputThrowsException(IOException ex) {
        in.setException(ex);
    }
}
