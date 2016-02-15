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

package com.redhat.thermostat.storage.cli.internal;

import com.redhat.thermostat.common.cli.Arguments;
import com.redhat.thermostat.common.config.ThermostatOptionParser;
import com.redhat.thermostat.shared.config.InvalidConfigurationException;
import com.redhat.thermostat.shared.locale.Translate;
import com.redhat.thermostat.storage.cli.internal.locale.LocaleResources;

public class DBOptionParser implements ThermostatOptionParser {
    
    private static final Translate<LocaleResources> translator = LocaleResources.createLocalizer();

    private boolean quiet;
    
    private boolean localhostExceptionAllowed;
    
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
        } else if (args.hasArgument(DBArgs.STATUS.option)) {
            serviceAction = DBArgs.STATUS;
        } else {
            throw new InvalidConfigurationException(translator.localize(LocaleResources.COMMAND_STORAGE_ARGUMENT_REQUIRED));
        }
        
        if (args.hasArgument(DBArgs.USE_LOCALHOST_EXPN.option)) {
            localhostExceptionAllowed = true;
        }

        if (args.hasArgument(DBArgs.DRY.option)) {
            dryRun = true;
        }
        
        if (args.hasArgument(DBArgs.QUIET.option)) {
            quiet = true;
        }
        
        // leave at the end, since it depends on the previous settings
        String address = configuration.getBindIP();
        long port = configuration.getPort();
        configuration.setDBConnectionString("mongodb://" + address + ":" + port);
    }

    public boolean isDryRun() {
        return dryRun;
    }
    
    public DBArgs getAction() {
        return serviceAction;
    }

    static enum DBArgs {
                
        DRY("dryRun"),
        
        HELP("help"),
        
        START("start"),
        STOP("stop"),
        
        QUIET("quiet"),
        
        STATUS("status"),
        
        USE_LOCALHOST_EXPN("permitLocalhostException");
        
        private String option;
        
        DBArgs(String option) {
            this.option = option;
        }
    }

    public boolean isQuiet() {
        return quiet;
    }
    
    public boolean isLocalHostExceptionAllowed() {
        return localhostExceptionAllowed;
    }
}

