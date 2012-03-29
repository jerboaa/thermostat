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

package com.redhat.thermostat.tools.db;

import java.io.IOException;
import java.util.List;

import joptsimple.OptionParser;
import joptsimple.OptionSet;

import com.redhat.thermostat.common.config.InvalidConfigurationException;
import com.redhat.thermostat.common.config.ThermostatOptionParser;
import com.redhat.thermostat.tools.ApplicationState;

class DBOptionParser implements ThermostatOptionParser {
    
    private boolean quiet;
    
    private DBStartupConfiguration configuration;
    
    private List<String> args;
    private OptionParser parser;

    private DBArgs serviceAction;
    
    private boolean dryRun;
    
    DBOptionParser(DBStartupConfiguration configuration, List<String> args) {
        this.args = args;
        this.configuration = configuration;
        parser = new OptionParser();
    }
    
    @Override
    public void parse() throws InvalidConfigurationException {
        
        parser.accepts(DBArgs.START.option, DBArgs.START.description);
        parser.accepts(DBArgs.STOP.option, DBArgs.STOP.description);
        
        parser.accepts(DBArgs.DRY.option, DBArgs.DRY.description);
        
        parser.accepts(DBArgs.HELP.option, DBArgs.HELP.description);
        parser.accepts(DBArgs.QUIET.option, DBArgs.QUIET.description);
             
        OptionSet options = parser.parse(args.toArray(new String[0]));
        if (!options.hasOptions() || options.has(DBArgs.HELP.option)) {
            displayHelp();
            return;
        }

        if (options.has(DBArgs.START.option)) {
            serviceAction = DBArgs.START;
        } else if (options.has(DBArgs.STOP.option)) {
            serviceAction = DBArgs.STOP;
            
        } else {
            throw new InvalidConfigurationException("either --start or --stop must be given");
        }

        if (options.has(DBArgs.DRY.option)) {
            dryRun = true;
        }
        
        if (options.has(DBArgs.QUIET.option)) {
            quiet = true;
        }
        
        // leave at the end, since it depends on the previous settings
        String url = configuration.getUrl();
        long port = configuration.getLocalPort();
        configuration.setLocal(true);
        if (options.has(DBArgs.CLUSTER.option)) {
            port = configuration.getClusterPort();
            configuration.setLocal(false);
        }
        configuration.setDBConnectionString(url + ":" + port);
    }

    boolean isDryRun() {
        return dryRun;
    }
    
    @Override
    public void displayHelp() {
        
        if (quiet) return;
        
        try {
            System.out.println("Module [DBService]");
            parser.printHelpOn(System.out);
        } catch (IOException ignore) {}
    }
    
    ApplicationState getAction() {
        return serviceAction.state;
    }
    
    static enum DBArgs {
                        
        CLUSTER("cluster", "launch the db in cluster mode, if not specified, " +
                "local mode is the default", ApplicationState.NONE),
                
        DRY("dry-run", "run the service in dry run mode", ApplicationState.NONE),
        
        HELP("help", "print this usage help", ApplicationState.HELP),
        
        START("start", "start the database", ApplicationState.START),
        STOP("stop", "stop the database", ApplicationState.STOP),
        
        QUIET("quiet", "don't produce any output", ApplicationState.NONE);
        
        private String option;
        private String description;
        private ApplicationState state;
        
        DBArgs(String option, String description, ApplicationState state) {
            this.option = option;
            this.description = description;
            this.state = state;
        }
    }

    boolean isQuiet() {
        return quiet;
    }
}
