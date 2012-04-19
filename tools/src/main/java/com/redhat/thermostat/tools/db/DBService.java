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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.Properties;

import com.redhat.thermostat.cli.ArgumentSpec;
import com.redhat.thermostat.cli.Arguments;
import com.redhat.thermostat.cli.CommandContext;
import com.redhat.thermostat.cli.CommandException;
import com.redhat.thermostat.common.config.ConfigUtils;
import com.redhat.thermostat.common.config.InvalidConfigurationException;
import com.redhat.thermostat.tools.ApplicationException;
import com.redhat.thermostat.tools.ApplicationState;
import com.redhat.thermostat.tools.BasicCommand;

public class DBService extends BasicCommand {

    private static final String NAME = "storage";

    // TODO: Use LocaleResources for i18n-ized strings.
    private static final String DESCRIPTION = "starts and stops the thermostat storage";

    private static final String USAGE = DESCRIPTION;

    private DBStartupConfiguration configuration;
    private DBOptionParser parser;
    
    private MongoProcessRunner runner;
    
    private void parseArguments(Arguments args) throws InvalidConfigurationException {
    
        this.configuration = new DBStartupConfiguration();
        // configs, read everything that is in the configs
        File propertyFile = ConfigUtils.getStorageConfigurationFile();
        if (!propertyFile.exists()) {
            throw new InvalidConfigurationException("can't access database configuration file " +
                                                    propertyFile);
        }
        readAndSetProperties(propertyFile);
        
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
            
        } catch (Exception e) {
            getNotifier().fireAction(ApplicationState.FAIL);
        }
    }
    
    private void readAndSetProperties(File propertyFile) throws InvalidConfigurationException {
    
        Properties properties = new Properties();
        try {
            properties.load(new FileInputStream(propertyFile));
            
        } catch (IOException e) {
            throw new InvalidConfigurationException(e);
        }
        
        if (properties.containsKey(DBConfig.LOCAL.name())) {
            String port = (String) properties.get(DBConfig.LOCAL.name());
            int localPort = Integer.parseInt(port);
            configuration.setLocalPort(localPort);
        } else {
            throw new InvalidConfigurationException(DBConfig.LOCAL + " property missing");
        }
        
        if (properties.containsKey(DBConfig.CLUSTER.name())) {
            String port = (String) properties.get(DBConfig.CLUSTER.name());
            int localPort = Integer.parseInt(port);
            configuration.setClusterPort(localPort);
        } else {
            throw new InvalidConfigurationException(DBConfig.CLUSTER + " property missing");
        }
        
        if (properties.containsKey(DBConfig.URL.name())) {
            String url = (String) properties.get(DBConfig.URL.name());
            configuration.setUrl(url);
        } else {
            throw new InvalidConfigurationException(DBConfig.URL + " property missing");
        }
        
        if (properties.containsKey(DBConfig.BIND.name())) {
            String ip = (String) properties.get(DBConfig.BIND.name());
            configuration.setBindIP(ip);
        } else {
            throw new InvalidConfigurationException(DBConfig.BIND + " property missing");
        }
    }
    
    private void startService() throws IOException, InterruptedException, InvalidConfigurationException, ApplicationException {
        runner.startService();
        getNotifier().fireAction(ApplicationState.START);
    }
    
    
    private void stopService() throws IOException, InterruptedException, InvalidConfigurationException, ApplicationException {
        check();
        runner.stopService();
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
            throw new InvalidConfigurationException("database directories do not exist...");
        }
    }
    
    @Override
    public DBStartupConfiguration getConfiguration() {
        return configuration;
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public String getDescription() {
        return DESCRIPTION;
    }

    @Override
    public String getUsage() {
        return USAGE;
    }

    @Override
    public Collection<ArgumentSpec> getAcceptedArguments() {
        return DBOptionParser.getAcceptedArguments();
    }

}
