package com.redhat.thermostat.agent;

import java.net.UnknownHostException;
import java.util.UUID;

import com.redhat.thermostat.agent.config.Configuration;

public interface Storage {
    public void connect(String uri) throws UnknownHostException;

    public void setAgentId(UUID id);

    public void addAgentInformation(Configuration config);

    public void removeAgentInformation();

    /**
     * @return {@code null} if the value is invalid or missing
     */
    public String getBackendConfig(String backendName, String configurationKey);

}
