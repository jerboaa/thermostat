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

package com.redhat.thermostat.itest;

import java.io.IOException;

import expectj.Executor;

/**
 * Runs any script in $THERMOSTAT_HOME/bin with the given name, args
 * and enviroment.
 *
 */
class EnvironmentExecutor implements Executor {

    private final String[] env;
    private final String args;
    private final String script;

    /**
     * 
     * @param script The script name (e.g. "thermostat")
     * @param args The space separated list of arguments
     * @param env List of environment variables in key=value pair format.
     */
    public EnvironmentExecutor(String script, String args, String[] env) {
        this.args = args;
        this.env = env;
        this.script = script;
    }

    @Override
    public Process execute() throws IOException {
        String command = buildCommand();
        Process p = Runtime.getRuntime().exec(command, env);
        return p;
    }

    @Override
    public String toString() {
        return script + " " + args;
    }
    
    private String buildCommand() {
        return IntegrationTest.getSystemBinRoot() + "/" + script + " " + args;
    }
    
    // for testing
    String[] getEnv() {
        return env;
    }
}