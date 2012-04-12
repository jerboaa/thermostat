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

package com.redhat.thermostat.agent.config;

import java.io.IOException;
import java.util.List;
import java.util.logging.Level;

import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;

import com.redhat.thermostat.common.config.InvalidConfigurationException;
import com.redhat.thermostat.common.config.ThermostatOptionParser;

public class AgentOptionParser implements ThermostatOptionParser {

    private AgentStartupConfiguration configuration;
    private OptionParser parser;
    private List<String> args;
    
    private boolean isHelp;
    
    public AgentOptionParser(AgentStartupConfiguration configuration, List<String> args) {
        this.configuration = configuration;
        this.args = args;
        parser = new OptionParser();
        isHelp = false;
    }
    
    @Override
    public void parse() throws InvalidConfigurationException {

        parser.accepts(Args.DEBUG.option, Args.DEBUG.description);
        parser.accepts(Args.HELP.option, Args.HELP.description);
        parser.accepts(Args.SAVE_ON_EXIT.option, Args.SAVE_ON_EXIT.description);
        
        OptionSpec<String> logLevel =
                parser.accepts(Args.LEVEL.option, Args.LEVEL.description).
                      withRequiredArg();
        OptionSpec<String> dbUrl =
                parser.accepts(Args.DB.option, Args.DB.description).
                      withRequiredArg();
        
        OptionSet options = parser.parse(args.toArray(new String[0]));
        if (options.has(Args.HELP.option)) {
            displayHelp();
            isHelp = true;
            return;
        }
        
        if (options.has(Args.SAVE_ON_EXIT.option)) {
            configuration.setPurge(false);
        }
        
        if (options.has(Args.LEVEL.option)) {
            String levelString = logLevel.value(options);
            Level level = AgentConfigsUtils.getLogLevel(levelString);
            configuration.setLogLevel(level);
        }

        configuration.setDebugConsole(options.has(Args.DEBUG.option));
        
        if (options.has(Args.DB.option)) {
            String url = dbUrl.value(options);
            configuration.setDatabaseURL(url);
        } else {
            if (configuration.getDBConnectionString() == null) {
                System.err.println("database url not specified... must be " +
                                   "either set in config or passed on " +
                                   "the command line");
                displayHelp();
                isHelp = true;
            }
        }
    }
    
    public boolean isHelp() {
        return isHelp;
    }
    
    @Override
    public void displayHelp() {
        try {
            parser.printHelpOn(System.out);
        } catch (IOException ignore) {}
    }
    
    private static enum Args {
        
        // TODO: localize
        LEVEL("logLevel", "log level"),
        SAVE_ON_EXIT("saveOnExit", "save the data on exit"),
        DB("dbUrl", "connect to the given url"),
        DEBUG("debug", "launch with debug console enabled"),
        HELP("help", "print this help and exit");
        
        private String option;
        private String description;
        
        Args(String option, String description) {
            this.option = option;
            this.description = description;
        }
    }
}
