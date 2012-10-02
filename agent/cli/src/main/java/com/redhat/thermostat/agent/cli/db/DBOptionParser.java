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

package com.redhat.thermostat.agent.cli.db;

import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionGroup;
import org.apache.commons.cli.Options;

import com.redhat.thermostat.common.cli.Arguments;
import com.redhat.thermostat.agent.cli.impl.locale.LocaleResources;
import com.redhat.thermostat.agent.cli.impl.locale.Translate;
import com.redhat.thermostat.common.config.InvalidConfigurationException;
import com.redhat.thermostat.common.config.ThermostatOptionParser;
import com.redhat.thermostat.common.tools.ApplicationState;

public class DBOptionParser implements ThermostatOptionParser {
    
    private boolean quiet;
    
    private DBStartupConfiguration configuration;
    
    private Arguments args;

    private DBArgs serviceAction;
    
    private boolean dryRun;
    
    public DBOptionParser(DBStartupConfiguration configuration, Arguments args) {
        this.args = args;
        this.configuration = configuration;
    }
    
    @Override
    public void parse() throws InvalidConfigurationException {

        if (args.hasArgument(DBArgs.START.option)) {
            serviceAction = DBArgs.START;
        } else if (args.hasArgument(DBArgs.STOP.option)) {
            serviceAction = DBArgs.STOP;
        } else {
            throw new InvalidConfigurationException(Translate.localize(LocaleResources.COMMAND_STORAGE_ARGUMENT_REQUIRED));
        }

        if (args.hasArgument(DBArgs.DRY.option)) {
            dryRun = true;
        }
        
        if (args.hasArgument(DBArgs.QUIET.option)) {
            quiet = true;
        }
        
        // leave at the end, since it depends on the previous settings
        String urlPrefix = configuration.getProtocol();
        String address = configuration.getBindIP();
        long port = configuration.getPort();
        configuration.setDBConnectionString(urlPrefix + "://" + address + ":" + port);
    }

    public boolean isDryRun() {
        return dryRun;
    }
    
    public ApplicationState getAction() {
        return serviceAction.state;
    }

    static enum DBArgs {
                
        DRY("dryRun", Translate.localize(LocaleResources.COMMAND_STORAGE_ARGUMENT_DRYRUN_DESCRIPTION), ApplicationState.NONE),
        
        HELP("help", Translate.localize(LocaleResources.COMMAND_STORAGE_ARGUMENT_HELP_DESCRIPTION), ApplicationState.HELP),
        
        START("start", Translate.localize(LocaleResources.COMMAND_STORAGE_ARGUMENT_START_DESCRIPTION), ApplicationState.START),
        STOP("stop", Translate.localize(LocaleResources.COMMAND_STORAGE_ARGUMENT_STOP_DESCRIPTION), ApplicationState.STOP),
        
        QUIET("quiet", Translate.localize(LocaleResources.COMMAND_STORAGE_ARGUMENT_QUIET_DESCRIPTION), ApplicationState.NONE);
        
        private String option;
        private String description;
        private ApplicationState state;
        
        DBArgs(String option, String description, ApplicationState state) {
            this.option = option;
            this.description = description;
            this.state = state;
        }
    }

    public boolean isQuiet() {
        return quiet;
    }

    public static Options getOptions() {
        Options options = new Options();

        // TODO set default values here instead of needing to check if present later.
        Option dryRunOption = new Option("d", DBArgs.DRY.option, false, DBArgs.DRY.description);
        dryRunOption.setRequired(false);
        options.addOption(dryRunOption);

        OptionGroup startStopGroup = new OptionGroup();
        startStopGroup.setRequired(true);

        Option startOption = new Option("s", DBArgs.START.option, false, DBArgs.START.description);
        startOption.setRequired(false);
        startStopGroup.addOption(startOption);

        Option stopOption = new Option("p", DBArgs.STOP.option, false, DBArgs.STOP.description);
        stopOption.setRequired(false);
        startStopGroup.addOption(stopOption);

        options.addOptionGroup(startStopGroup);

        Option quietOption = new Option("q", DBArgs.QUIET.option, false, DBArgs.QUIET.description);
        quietOption.setRequired(false);
        options.addOption(quietOption);

        return options;
    }
}
