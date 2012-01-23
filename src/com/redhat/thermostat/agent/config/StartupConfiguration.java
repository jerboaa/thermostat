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

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.redhat.thermostat.agent.Agent;
import com.redhat.thermostat.agent.Defaults;
import com.redhat.thermostat.common.Constants;
import com.redhat.thermostat.common.LaunchException;
import com.redhat.thermostat.common.utils.LoggingUtils;

public final class StartupConfiguration {

    private static Logger logger = LoggingUtils.getLogger(StartupConfiguration.class);

    private final long startTimestamp;

    private Properties props;
    private Level logLevel;
    private boolean localMode;
    private int mongodPort;
    private int mongosPort;
    private String databaseURI;
    private String completeDatabaseURI;

    private String hostname;

    private Agent agent;

    public StartupConfiguration(long startTime, String[] args, Properties props) throws LaunchException {
        this.props = props;

        initFromDefaults();
        initFromProperties();
        initFromArguments(args);

        if (localMode) {
            completeDatabaseURI = databaseURI + ":" + mongodPort;
            hostname = Constants.AGENT_LOCAL_HOSTNAME;
        } else {
            completeDatabaseURI = databaseURI + ":" + mongosPort;
            try {
                InetAddress addr = InetAddress.getLocalHost();
                hostname = addr.getCanonicalHostName();
            } catch (UnknownHostException e) {
                logger.log(Level.FINE, "Error determining local hostname.");
            }
        }
        startTimestamp = startTime;
    }

    private void initFromDefaults() {
        logLevel = Defaults.LOGGING_LEVEL;
        localMode = Defaults.LOCAL_MODE;
        mongodPort = Defaults.MONGOD_PORT;
        mongosPort = Defaults.MONGOS_PORT;
        databaseURI = Defaults.DATABASE_URI;
    }

    private void initFromProperties() {
        if (props.getProperty(Constants.AGENT_PROPERTY_MONGOD_PORT) != null) {
            mongodPort = Integer.valueOf(props.getProperty(Constants.AGENT_PROPERTY_MONGOD_PORT));
        }
        if (props.getProperty(Constants.AGENT_PROPERTY_MONGOS_PORT) != null) {
            mongosPort = Integer.valueOf(props.getProperty(Constants.AGENT_PROPERTY_MONGOS_PORT));
        }
    }

    private void initFromArguments(String[] args) throws LaunchException {
        Arguments arguments;

        arguments = new Arguments(args);
        if (arguments.isModeSpecified()) {
            localMode = arguments.getLocalMode();
        }
        if (arguments.isLogLevelSpecified()) {
            logLevel = arguments.getLogLevel();
        }
    }

    public Level getLogLevel() {
        return logLevel;
    }

    public String getDatabaseURIAsString() {
        return completeDatabaseURI;
    }

    public String getHostname() {
        return hostname;
    }

    public void setAgent(Agent agent) {
        this.agent = agent;
    }

    public List<String> getStartupBackendClassNames() {
        String fullPropertyString = props.getProperty(Constants.AGENT_PROPERTY_BACKENDS);
        if ((fullPropertyString == null) // Avoid NPE
                || (fullPropertyString.length() == 0)) { /* If we do the split() on this empty string,
                                                          * it will produce an array of size 1 containing
                                                          * the empty string, which we do not want.
                                                          */
            return new ArrayList<String>();
        } else {
            return Arrays.asList(fullPropertyString.trim().split(","));
        }
    }

    public Map<String, String> getStartupBackendConfigMap(String backendName) {
        String prefix = backendName + ".";
        Map<String, String> configMap = new HashMap<String, String>();
        for (Entry<Object, Object> e : props.entrySet()) {
            String key = (String) e.getKey();
            if (key.startsWith(prefix)) {
                String value = (String) e.getValue();
                String mapKey = key.substring(prefix.length());
                configMap.put(mapKey, value);
            }
        }
        return configMap;
    }

    public long getStartTime() {
        return startTimestamp;
    }

    /**
     * Exposes the command line arguments in a more object-oriented style.
     * <p>
     * Please check that an option was specified (using the various is*Specified() methods) before using its value.
     */
    private static class Arguments {
        private final boolean localMode;
        private final boolean modeSpecified;
        private final Level logLevel;
        private final boolean logLevelSpecified;

        public Arguments(String[] args) throws LaunchException {
            boolean local = false;
            boolean explicitMode = false;
            Level level = null;
            boolean explicitLogLevel = false;
            for (int index = 0; index < args.length; index++) {
                if (args[index].equals(Constants.AGENT_ARGUMENT_LOGLEVEL)) {
                    index++;
                    if (index < args.length) {
                        try {
                            level = Level.parse(args[index].toUpperCase());
                            explicitLogLevel = true;
                        } catch (IllegalArgumentException iae) {
                            throw new LaunchException("Invalid argument for " + Constants.AGENT_ARGUMENT_LOGLEVEL, iae);
                        }
                    } else {
                        throw new LaunchException("Missing argument for " + Constants.AGENT_ARGUMENT_LOGLEVEL);
                    }
                } else if (args[index].equals(Constants.AGENT_ARGUMENT_LOCAL)) {
                    explicitMode = true;
                    local = true;
                }
            }
            logLevel = level;
            logLevelSpecified = explicitLogLevel;
            localMode = local;
            modeSpecified = explicitMode;
        }

        public boolean isModeSpecified() {
            return modeSpecified;
        }

        public boolean getLocalMode() {
            if (!isModeSpecified()) {
                throw new IllegalStateException("local mode is not valid");
            }
            return localMode;
        }

        public boolean isLogLevelSpecified() {
            return logLevelSpecified;
        }

        public Level getLogLevel() {
            if (!isLogLevelSpecified()) {
                throw new IllegalStateException("log level not explicitly specified");
            }
            return logLevel;
        }
    }
}
