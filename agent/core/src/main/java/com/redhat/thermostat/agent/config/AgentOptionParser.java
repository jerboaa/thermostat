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

import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;

import com.redhat.thermostat.common.cli.Arguments;
import com.redhat.thermostat.common.config.InvalidConfigurationException;
import com.redhat.thermostat.common.config.ThermostatOptionParser;

public class AgentOptionParser implements ThermostatOptionParser {

    private AgentStartupConfiguration configuration;
    private Arguments args;
    
    private boolean isHelp;
    
    public AgentOptionParser(AgentStartupConfiguration configuration, Arguments args) {
        this.configuration = configuration;
        this.args = args;
        isHelp = false;
    }
    
    @Override
    public void parse() throws InvalidConfigurationException {

        if (args.hasArgument(Args.SAVE_ON_EXIT.option)) {
            configuration.setPurge(false);
        }

        configuration.setDebugConsole(args.hasArgument(Args.DEBUG.option));
        
        if (args.hasArgument(Args.DB.option)) {
            String url = args.getArgument(Args.DB.option);
            configuration.setDatabaseURL(url);
        } else {
            if (configuration.getDBConnectionString() == null) {
                System.err.println("database url not specified... must be " +
                                   "either set in config or passed on " +
                                   "the command line");
                isHelp = true;
            }
        }
        configuration.setUsername(args.getArgument(Args.USERNAME.option));
        configuration.setPassword(args.getArgument(Args.PASSWORD.option));
    }
    
    public boolean isHelp() {
        return isHelp;
    }
    
    private static enum Args {
        
        // TODO: localize
        SAVE_ON_EXIT("saveOnExit", "save the data on exit"),
        DB("dbUrl", "connect to the given url"),
        USERNAME("username", "the username to use for authentication"),
        PASSWORD("password", "the password to use for authentication"),
        DEBUG("debug", "launch with debug console enabled"),
        HELP("help", "print this help and exit");
        
        private String option;
        private String description;
        
        Args(String option, String description) {
            this.option = option;
            this.description = description;
        }
    }

    public static Options getOptions() {
        Options options = new Options();

        Option saveOnExitOption = new Option("s", Args.SAVE_ON_EXIT.option, false, Args.SAVE_ON_EXIT.description);
        saveOnExitOption.setRequired(false);
        options.addOption(saveOnExitOption);

        Option dbOption = new Option("d", Args.DB.option, true, Args.DB.description);
        dbOption.setRequired(true);
        options.addOption(dbOption);

        Option usernameOption = new Option("u", Args.USERNAME.option, true, Args.USERNAME.description);
        usernameOption.setRequired(false);
        options.addOption(usernameOption);

        Option passwordOption = new Option("p", Args.PASSWORD.option, true, Args.PASSWORD.description);
        passwordOption.setRequired(false);
        options.addOption(passwordOption);

        Option debugOption = new Option("v", Args.DEBUG.option, false, Args.DEBUG.description);
        debugOption.setRequired(false);
        options.addOption(debugOption);

        return options;
    }
}
