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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import com.redhat.thermostat.common.config.ConfigUtils;
import com.redhat.thermostat.common.config.InvalidConfigurationException;
import com.redhat.thermostat.common.utils.LoggedExternalProcess;
import com.redhat.thermostat.tools.BasicApplication;

public class DBService extends BasicApplication {

    private DBStartupConfiguration configuration;
    private DBOptionParser parser;
    
    private static final String [] MONGO_BASIC_ARGS = {
        "mongod", "--quiet", "--fork", "--nojournal", "--noauth", "--bind_ip"
    };
    
    private static final String [] MONGO_SHUTDOWN_ARGS = {
        "mongod", "--shutdown", "--dbpath"
    };
    
    @Override
    public void parseArguments(List<String> args) throws InvalidConfigurationException {
    
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
    
    private void startService() throws IOException, InterruptedException {
        
        String pid = checkPid();
        if (pid != null) {
            String message = "cannot start server " + configuration.getDBPath() +
                    ", found pid file rom previous run, please, cleanup";
            display(message);
            notifyFail();
            return;
        }
        
        List<String> commands = new ArrayList<>(Arrays.asList(MONGO_BASIC_ARGS));
       
        // check that the db directory exist
        display("starting storage server...");

        commands.add(configuration.getBindIP());

        commands.add("--dbpath");
        commands.add(configuration.getDBPath().getCanonicalPath());

        commands.add("--logpath");
        commands.add(configuration.getLogFile().getCanonicalPath());

        commands.add("--pidfilepath");
        commands.add(configuration.getPidFile().getCanonicalPath());

        commands.add("--port");
        if (configuration.isLocal()) {
            commands.add(Long.toString(configuration.getLocalPort()));
        } else {
            commands.add(Long.toString(configuration.getClusterPort()));
        }
        
        LoggedExternalProcess process = new LoggedExternalProcess(commands);
        int status = process.runAndReturnResult();
        if (status == 0) {
            pid = checkPid();
            if (pid == null) status = -1;
        }
        
        if (status == 0) {
            display("server listening on ip: " + configuration.getDBConnectionString());
            display("log file is here: " + configuration.getLogFile());
            display("pid: " + pid);
            notifySuccess();
            
        } else {
            
            String message = "cannot start server " + configuration.getDBPath() +
                             ", exit status: " + status +
                             ". Please check that your configuration is valid";
            display(message);
            notifyFail();
        }
    }
    
    private String checkPid() {
        String pid = null;
        // check the pid to be sure
        File pidfile = configuration.getPidFile();
        Charset charset = Charset.defaultCharset();
        if (pidfile.exists()) {
            try (BufferedReader reader = Files.newBufferedReader(pidfile.toPath(), charset)) {
                pid = reader.readLine();
                if (pid == null || pid.isEmpty()) {
                    pid = null;
                }
            } catch (IOException ignore) {
                ignore.printStackTrace();
                pid = null;
            }
        }
        
        return pid;
    }
    
    private void stopService() throws IOException, InterruptedException, InvalidConfigurationException {
        check();
        
        List<String> commands = new ArrayList<>(Arrays.asList(MONGO_SHUTDOWN_ARGS));
        commands.add(configuration.getDBPath().getCanonicalPath());

        LoggedExternalProcess process = new LoggedExternalProcess(commands);
        int status = process.runAndReturnResult();
        if (status == 0) {
            display("server shutdown complete: " + configuration.getDBPath());
            display("log file is here: " + configuration.getLogFile());
            notifySuccess();
            
        } else {
            // TODO: check the pid and see if it's running or not
            // perhaps was already down
            String message = "cannot shutdown server " + configuration.getDBPath() +
                    ", exit status: " + status +
                    ". Please check that your configuration is valid";
            display(message);
            notifyFail();
        }
    }
    
    @Override
    public void run() {
        
        if (parser.isDryRun()) return;
        
        try {
            switch (parser.getAction()) {
            case START:
                startService();
                break;
            case STOP:
                stopService();
                break;
            }
        } catch (Exception e) {
            // TODO: exception should be at least logged
            notifyFail();
        }
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
    public void printHelp() {
        parser.displayHelp();
    }
    
    @Override
    public DBStartupConfiguration getConfiguration() {
        return configuration;
    }
    
    private void display(String message) {
        if (!parser.isQuiet()) {
            System.out.println(message);
        }
    }
    
    public static void main(String[] args) throws InvalidConfigurationException {
        DBService service = new DBService();
        service.parseArguments(Arrays.asList(args));
        service.run();
    }
}
