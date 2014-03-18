/*
 * Copyright 2012-2014 Red Hat, Inc.
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

package com.redhat.thermostat.web.endpoint.internal;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.redhat.thermostat.common.utils.HostPortPair;
import com.redhat.thermostat.common.utils.HostPortsParser;
import com.redhat.thermostat.common.utils.LoggingUtils;
import com.redhat.thermostat.shared.config.CommonPaths;
import com.redhat.thermostat.shared.config.InvalidConfigurationException;
import com.redhat.thermostat.shared.locale.Translate;

class EmbeddedServletContainerConfiguration {

    private static final Logger logger = LoggingUtils.getLogger(EmbeddedServletContainerConfiguration.class);
    private static final Translate<LocaleResources> t = LocaleResources.createLocalizer();
    private final Properties systemConfiguration;
    private final Properties userConfiguration;
    private final CommonPaths paths;
    
    private EmbeddedServletContainerConfiguration(CommonPaths paths, Properties systemConfiguration, Properties userConfiguration) {
        this.systemConfiguration = systemConfiguration;
        this.userConfiguration = userConfiguration;
        this.paths = paths;
    }
    
    EmbeddedServletContainerConfiguration(CommonPaths paths, File sysConfig, File userConfig) {
        this(paths, new Properties(), new Properties());
        // init from system config if existing
        initConfig(sysConfig, this.systemConfiguration);
        // init from user config if existing
        initConfig(userConfig, this.userConfiguration);
    }
    
    // For testing only
    EmbeddedServletContainerConfiguration(File systemConfiguration, File userConfiguration) throws InvalidConfigurationException {
        this(null, systemConfiguration, userConfiguration);
    }
    
    // For testing only
    EmbeddedServletContainerConfiguration(Properties systemConfiguration, Properties userConfiguration) {
        this(null, systemConfiguration, userConfiguration);
    }
    
    /**
     * Main Constructor
     * 
     * @param paths
     * @throws InvalidConfigurationException
     */
    EmbeddedServletContainerConfiguration(CommonPaths paths) throws InvalidConfigurationException {
        this(paths, getSystemWebStorageServiceConfigFile(paths), getUserWebStorageServiceConfigFile(paths));
    }
    
    private static File getUserWebStorageServiceConfigFile(CommonPaths paths) throws InvalidConfigurationException {
        return new File(paths.getUserConfigurationDirectory(), "web-storage-service.properties");
    }

    private static File getSystemWebStorageServiceConfigFile(CommonPaths paths) throws InvalidConfigurationException {
        return new File(paths.getSystemConfigurationDirectory(), "web-storage-service.properties");
    }

    private static void initConfig(File configFile,
            Properties configProps) {
        try (FileInputStream configFis = new FileInputStream(configFile)) {
            configProps.load(configFis);
        } catch (FileNotFoundException e) {
            // ignore, allow files to not exist on initialization
        } catch (IOException e) {
            throw new InvalidConfigurationException(e);
        }
    }

    HostPortPair getHostsPortsConfig() throws InvalidConfigurationException {
        String hostsPortsString = systemConfiguration.getProperty(ConfigKeys.SERVLET_CONTAINER_BIND_ADDRESS.name());
        // user config overrides system config
        String userPortsString = userConfiguration.getProperty(ConfigKeys.SERVLET_CONTAINER_BIND_ADDRESS.name());
        if (userPortsString != null) {
            hostsPortsString = userPortsString;
        }
        // config must not be empty in both, system AND user config
        if (hostsPortsString == null) {
            throw new InvalidConfigurationException(t.localize(LocaleResources.BIND_ADDRESS_NULL));
        }
        HostPortsParser parser = new HostPortsParser(hostsPortsString);
        parser.parse();
        List<HostPortPair> hostsPorts = parser.getHostsPorts();
        if (hostsPorts.size() != 1) {
            throw new InvalidConfigurationException("Cannot parse configuration (lists not allowed!)");
        }
        return hostsPorts.get(0);
    }
    
    /* The path to the context where the web archive gets bound to */
    String getContextPath() {
        return "/thermostat/storage";
    }
    
    File getAbsolutePathToExplodedWebArchive() {
        File thermostatHome = paths.getSystemThermostatHome();
        // The thermostat build produces an exploded war in $THERMOSTAT_HOME/webapp
        File webArchiveDir = new File(thermostatHome, "webapp");
        return webArchiveDir;
    }
    
    boolean isEnableTLS() {
        String sslProp = systemConfiguration.getProperty(ConfigKeys.USE_SSL.name());
        // user config overrides system config
        String userProp = userConfiguration.getProperty(ConfigKeys.USE_SSL.name());
        if (userProp != null) {
            sslProp = userProp;
        }
        // default to false if neither system nor user config present
        boolean propVal = Boolean.parseBoolean(sslProp);
        return propVal;
    }
    
    String getConnectionUrl() throws InvalidConfigurationException {
        String httpPrefix = "http";
        if (isEnableTLS()) {
            httpPrefix = httpPrefix + "s";
        }
        HostPortPair hostPort = getHostsPortsConfig();
        String host = hostPort.getHost();
        if (host.indexOf(':') >= 0) {
            // host is an IPv6 literal, enclose with '[' and ']' in order to
            // be rfc3986 conformant
            host = "[" + host + "]";
        }
        String connectUrl = String.format("%s://%s:%s%s", httpPrefix, host, hostPort.getPort(), getContextPath());
        logger.log(Level.FINE, "Using agent connection URL '" + connectUrl + "'");
        return connectUrl;
    }
    
    String getAbsolutePathToJaasConfig() throws InvalidConfigurationException {
        File etcPath = paths.getSystemConfigurationDirectory();
        File thermostatJaasConf = new File(etcPath, "thermostat_jaas.conf");
        try {
            return thermostatJaasConf.getCanonicalPath();
        } catch (IOException e) {
            throw new InvalidConfigurationException(e);
        }
    }
    
    static enum ConfigKeys {
        /* String: The bind address. host:port format */
        SERVLET_CONTAINER_BIND_ADDRESS,
        /* Boolean: If set to "true" makes the embedded servlet container
         * boot up with SSL support. It uses config in ssl.properties for
         * keystore et al.
         */
        USE_SSL
    }
}
