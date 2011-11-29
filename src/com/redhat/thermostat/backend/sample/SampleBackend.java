package com.redhat.thermostat.backend.sample;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.redhat.thermostat.backend.Backend;
import com.redhat.thermostat.common.utils.LoggingUtils;

/** Just an example backend implementation.  This is really just to test the loading and configuration mechanisms
 *
 */
public class SampleBackend extends Backend {
    private final String NAME = "sample-backend";
    private final String DESCRIPTION = "A backend which does nothing at all.";
    private final String VENDOR = "Nobody";
    private final String VERSION = "0.1";
    private boolean currentlyActive = false;

    private Logger logger = LoggingUtils.getLogger(SampleBackend.class);

    public SampleBackend() {
        super();
    }

    @Override
    protected void setConfigurationValue(String name, String value) {
        logger.log(Level.FINE, "Setting configuration value for backend: " + this.NAME);
        logger.log(Level.FINE, "key: " + name + "    value: " + value);
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
    public String getVendor() {
        return VENDOR;
    }

    @Override
    public String getVersion() {
        return VERSION;
    }

    @Override
    public Map<String, String> getConfigurationMap() {
        return new HashMap<String, String>();
    }

    @Override
    public boolean activate() {
        currentlyActive = true;
        return true;
    }

    @Override
    public boolean deactivate() {
        currentlyActive = false;
        return true;
    }

    @Override
    public boolean isActive() {
        return currentlyActive;
    }

}
