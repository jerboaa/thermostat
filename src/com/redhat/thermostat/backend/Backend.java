package com.redhat.thermostat.backend;

import java.util.Map;

/**
 * Represents a monitoring back-end. All the {@link Backend}s should be
 * registered with the {@link BackendRegistry}.
 */
public interface Backend {

    /** Returns the name of the {@link Backend} */
    public String getName();

    /** Returns the description of the {@link Backend} */
    public String getDescription();

    /** Returns the vendor of the {@link Backend} */
    public String getVendor();

    /** Returns the version of the {@link Backend} */
    public String getVersion();

    /**
     * Returns an array of {@link BackendFeature}s supported by this backend. To
     * indicate no supported features, return an empty array, not null.
     */
    public BackendFeature[] getSupportedFeatures();

    /**
     * Returns a map containing the settings of this backend
     */
    public Map<String, String> getSettings();

    /**
     * Activate the {@link Backend}. The backend should start gathering general
     * data (not specific to any vm) and start sending it
     *
     * @return true on success, false if there was an error
     */
    public boolean activate();

    /**
     * Deactivate the {@link Backend}. The backend should release resources at
     * this point.
     *
     * @return true on success
     */
    public boolean deactivate();

    /**
     * Returns a boolean indicating if the backend is currently active on this
     * host
     */
    public boolean isActive();

}
