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

package com.redhat.thermostat.itest;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import expectj.Executor;

/**
 * Runs any script in $THERMOSTAT_HOME/bin with the given name, args
 * and enviroment.
 *
 */
class EnvironmentExecutor implements Executor {

    private final Map<String, String> env;
    private final String[] args;
    private final String script;
    private final String binRoot;

    /**
     * 
     * @param script The script name (e.g. "thermostat")
     * @param args The arguments and options to the script
     * @param env The environment variables in key value format
     */
    public EnvironmentExecutor(String script, String[] args, Map<String, String> env) {
        this(IntegrationTest.getSystemBinRoot(), script, args, env);
    }
    
    // Enable testing without invoking static initializer of IntegrationTest
    EnvironmentExecutor(String binRoot, String script, String[] args, Map<String, String> env) {
        this.args = args;
        this.env = env;
        this.script = script;
        this.binRoot = binRoot;
    }

    @Override
    public Process execute() throws IOException {
        List<String> commands = buildCommmands();
        ProcessBuilder builder = new ProcessBuilder(commands);
        updateEnvironment(getBuilderEnvironment(builder));
        return startProcess(builder);
    }

    List<String> buildCommmands() {
        List<String> commands = new ArrayList<>(1 + args.length);
        String command = buildScriptPath();
        commands.add(command);
        commands.addAll(Arrays.asList(args));
        return commands;
    }

    private String buildScriptPath() {
        return binRoot + "/" + script;
    }

    // for testing
    protected void updateEnvironment(Map<String, String> toUpdate) {
        for (Entry<String, String> entry : env.entrySet()) {
            toUpdate.put(entry.getKey(), entry.getValue());
        }
    }
    
    // for testing
    protected Map<String, String> getBuilderEnvironment(ProcessBuilder builder) {
        return builder.environment();
    }
    
    // for testing
    protected Process startProcess(ProcessBuilder builder) throws IOException {
        return builder.start();
    }
    
    
    // for testing
    Map<String, String> getEnv() {
        return env;
    }
    
    @Override
    public String toString() {
        return script + convertArgsToString(args);
    }
    
    private static String convertArgsToString(String[] args) {
        StringBuilder result = new StringBuilder();
        if (args != null) {
            for (String arg : args) {
                result.append(" ").append(arg);
            }
        }
        return result.toString();
    }
}