/*
 * Copyright 2012, 2013 Red Hat, Inc.
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

package com.redhat.thermostat.agent.cli.impl.db;

import java.io.File;
import java.io.IOException;

import com.redhat.thermostat.agent.cli.impl.locale.LocaleResources;
import com.redhat.thermostat.common.ExitStatus;
import com.redhat.thermostat.common.cli.AbstractStateNotifyingCommand;
import com.redhat.thermostat.common.cli.Arguments;
import com.redhat.thermostat.common.cli.CommandContext;
import com.redhat.thermostat.common.cli.CommandException;
import com.redhat.thermostat.common.config.Configuration;
import com.redhat.thermostat.common.config.InvalidConfigurationException;
import com.redhat.thermostat.common.locale.Translate;
import com.redhat.thermostat.common.tools.ApplicationException;
import com.redhat.thermostat.common.tools.ApplicationState;

public class StorageCommand extends AbstractStateNotifyingCommand {

    private static final Translate<LocaleResources> t = LocaleResources.createLocalizer();

    private DBStartupConfiguration configuration;
    private DBOptionParser parser;
    private final ExitStatus exitStatus;
    
    private MongoProcessRunner runner;
    
    public StorageCommand(ExitStatus exitStatus) {
        this.exitStatus = exitStatus;
    }
    
    private void parseArguments(Arguments args) throws InvalidConfigurationException {
    
        Configuration thermostatConfiguration = new Configuration();
        File dbPath = thermostatConfiguration.getStorageDirectory();
        File logFile = thermostatConfiguration.getStorageLogFile();
        File pidFile = thermostatConfiguration.getStoragePidFile();
        File propertyFile = thermostatConfiguration.getStorageConfigurationFile();
        if (!propertyFile.exists()) {
            throw new InvalidConfigurationException(t.localize(LocaleResources.MISSING_DB_CONFIG, propertyFile.toString()));
        }
        // read everything that is in the configs
        this.configuration = new DBStartupConfiguration(propertyFile, dbPath, logFile, pidFile);
        parser = new DBOptionParser(configuration, args);
        parser.parse();
    }
    
    @Override
    public void run(CommandContext ctx) throws CommandException {

        try {
            parseArgsAndRun(ctx);
        } catch (InvalidConfigurationException e) {
            throw new CommandException(e);
        }
    }

    private void parseArgsAndRun(CommandContext ctx)
            throws InvalidConfigurationException {
        parseArguments(ctx.getArguments());

        // dry run means we don't do anything at all
        if (parser.isDryRun()) return;
        
        runner = createRunner();
        try {
            switch (parser.getAction()) {
            case START:
                startService();
                break;
            case STOP:
                stopService();
                break;
             default:
                break;
            }
            getNotifier().fireAction(ApplicationState.SUCCESS);
        } catch (InvalidConfigurationException e) {
            // rethrow
            throw e;
        } catch (Exception e) {
            getNotifier().fireAction(ApplicationState.FAIL, e);
        }
    }
    
    private void startService() throws IOException, InterruptedException, InvalidConfigurationException, ApplicationException {
        try {
            runner.startService();
        } catch (ApplicationException | InvalidConfigurationException | IOException e) {
            // something went wrong set status appropriately. This makes sure
            // that the JVM exits with this status.
            exitStatus.setExitStatus(ExitStatus.EXIT_ERROR);
            // rethrow
            throw e;
        }
        getNotifier().fireAction(ApplicationState.START);
    }
    
    
    private void stopService() throws IOException, InterruptedException, InvalidConfigurationException, ApplicationException {
        try {
            check();
            runner.stopService();
        } catch (ApplicationException | InvalidConfigurationException | InterruptedException | IOException e) {
            // something went wrong set status appropriately. This makes sure
            // that the JVM exits with this status.
            exitStatus.setExitStatus(ExitStatus.EXIT_ERROR);
            throw e;
        }
        getNotifier().fireAction(ApplicationState.STOP);
    }
    
    MongoProcessRunner createRunner() {
        return new MongoProcessRunner(configuration, parser.isQuiet());
    }

    private void check() throws InvalidConfigurationException {
        if (!configuration.getDBPath().exists() ||
            !configuration.getLogFile().getParentFile().exists() || 
            !configuration.getPidFile().getParentFile().exists())
        {
            throw new InvalidConfigurationException(t.localize(LocaleResources.MISSING_DB_DIR));
        }
    }

    public DBStartupConfiguration getConfiguration() {
        return configuration;
    }

    @Override
    public boolean isStorageRequired() {
        return false;
    }

    @Override
    public boolean isAvailableInShell() {
        return false;
    }
}

