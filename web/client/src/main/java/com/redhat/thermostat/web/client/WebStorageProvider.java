package com.redhat.thermostat.web.client;

import com.redhat.thermostat.common.cli.AuthenticationConfiguration;
import com.redhat.thermostat.storage.config.StartupConfiguration;
import com.redhat.thermostat.storage.core.Storage;
import com.redhat.thermostat.storage.core.StorageProvider;

public class WebStorageProvider implements StorageProvider {

    private StartupConfiguration config;
    
    @Override
    public Storage createStorage() {
        WebStorage storage = new WebStorage();
        storage.setEndpoint(config.getDBConnectionString());
        if (config instanceof AuthenticationConfiguration) {
            AuthenticationConfiguration authConf = (AuthenticationConfiguration) config;
            storage.setAuthConfig(authConf.getUsername(), authConf.getPassword());
        }
        return storage;
    }

    @Override
    public void setConfig(StartupConfiguration config) {
        this.config = config;
    }

    @Override
    public boolean canHandleProtocol() {
        // use http since this might be https at some point
        return config.getDBConnectionString().startsWith("http");
    }

}
