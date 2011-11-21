package com.redhat.thermostat.agent;

import java.io.Reader;
import java.util.Properties;

/**
 * Configuration used by the agent during startup, initialization. As opposed to
 * {@link RuntimeConfiguration}, this configuration is not exported.
 */
public class StartupConfiguration {

    private final int localPort;
    private final int remotePort;
    private final String[] backends;

    public StartupConfiguration(Reader configReader) {
        Properties properties = new Properties();
        try {
            properties.load(configReader);
        } catch (Exception e) {
            e.printStackTrace();
        }
        localPort = Integer.valueOf(properties.getProperty("mongod_port"));
        remotePort = Integer.valueOf(properties.getProperty("mongos_port"));
        backends = properties.getProperty("backends").trim().split(",");

    }

    public int getPortForLocal() {
        return localPort;
    }

    public int getPortForRemote() {
        return remotePort;
    }

    public String[] getBackendsToStart() {
        return backends;
    }

}
