package com.redhat.thermostat.backend.system;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.redhat.thermostat.common.utils.LoggingUtils;

/**
 * Implementation note: this relies on the {@code lsb_release} program to work.
 */
public class DistributionIdentity {

    private static final String DISTRIBUTION_NAME = "distributor id";
    private static final String DISTRIBUTION_VERSION = "release";

    private static final Logger logger = LoggingUtils.getLogger(DistributionIdentity.class);

    private final String name;
    private final String version;

    public DistributionIdentity() {
        String tempName = "Unknown Distribution";
        String tempVersion = "Unknown";
        try {
            Process lsbProc = Runtime.getRuntime().exec(new String[] { "lsb_release", "-a" });
            InputStream progOutput = lsbProc.getInputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(progOutput));
            String line;
            while ((line = reader.readLine()) != null) {
                int sepLocation = line.indexOf(":");
                if (sepLocation != -1) {
                    String key = line.substring(0, sepLocation).toLowerCase();
                    if (key.equals(DISTRIBUTION_NAME)) {
                        tempName = line.substring(sepLocation + 1).trim();
                    } else if (key.equals(DISTRIBUTION_VERSION)) {
                        tempVersion = line.substring(sepLocation + 1).trim();
                    }
                }
            }
        } catch (IOException e) {
            logger.log(Level.WARNING, "unable to identify distribution");
        }
        name = tempName;
        version = tempVersion;

        logger.log(Level.FINE, "distro-name: " + name);
        logger.log(Level.FINE, "distro-version: " + version);
    }

    /**
     * @return the name of the distrbution, or {@code null} if it can not be
     * identified
     */
    public String getName() {
        return name;
    }

    /**
     * @return the release of the distribution or {@code null} if it can not be
     * identified
     */
    public String getVersion() {
        return version;
    }

}