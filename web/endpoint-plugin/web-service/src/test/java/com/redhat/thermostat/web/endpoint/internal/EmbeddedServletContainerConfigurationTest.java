/*
 * Copyright 2012-2017 Red Hat, Inc.
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.util.Properties;

import org.junit.Test;

import com.redhat.thermostat.common.utils.HostPortPair;
import com.redhat.thermostat.shared.config.CommonPaths;
import com.redhat.thermostat.shared.config.InvalidConfigurationException;
import com.redhat.thermostat.web.endpoint.internal.EmbeddedServletContainerConfiguration;
import com.redhat.thermostat.web.endpoint.internal.EmbeddedServletContainerConfiguration.ConfigKeys;

public class EmbeddedServletContainerConfigurationTest {
    
    /*
     * empty configs defaults to false
     */
    @Test
    public void canGetSSLConfigNothingSpecified() {
        Properties systemConfig = new Properties();
        Properties userConfig = new Properties();
        EmbeddedServletContainerConfiguration config = new EmbeddedServletContainerConfiguration(systemConfig, userConfig);
        assertFalse(config.isEnableTLS());
    }
    
    /*
     * Verifies that user config overrides system config
     */
    @Test
    public void canGetUserSSLConfig() {
        Properties systemConfig = new Properties();
        systemConfig.put(ConfigKeys.USE_SSL.name(), "false");
        Properties userConfig = new Properties();
        userConfig.put(ConfigKeys.USE_SSL.name(), "true");
        EmbeddedServletContainerConfiguration config = new EmbeddedServletContainerConfiguration(systemConfig, userConfig);
        assertTrue(config.isEnableTLS());
    }
    
    /*
     * Verifies that system config works
     */
    @Test
    public void canGetSystemSSLConfig() {
        Properties systemConfig = new Properties();
        systemConfig.put(ConfigKeys.USE_SSL.name(), "true");
        Properties userConfig = new Properties();
        EmbeddedServletContainerConfiguration config = new EmbeddedServletContainerConfiguration(systemConfig, userConfig);
        assertTrue(config.isEnableTLS());
    }
    
    @Test
    public void garbageSSLConfigValsDoNotFail() {
        Properties systemConfig = new Properties();
        systemConfig.put(ConfigKeys.USE_SSL.name(), "foo");
        Properties userConfig = new Properties();
        userConfig.put(ConfigKeys.USE_SSL.name(), "bar");
        EmbeddedServletContainerConfiguration config = new EmbeddedServletContainerConfiguration(systemConfig, userConfig);
        assertFalse(config.isEnableTLS());
    }

    /*
     * No user config specified (empty properties) but
     * system config has ips/ports pair.
     */
    @Test
    public void canGetSystemConfigListenAddress() throws InvalidConfigurationException {
        Properties systemConfig = new Properties();
        Properties userConfig = new Properties();
        String host = "127.0.0.1";
        int port = 8888;
        String systemConfigPairs = host + ":" + port;
        systemConfig.put(ConfigKeys.SERVLET_CONTAINER_BIND_ADDRESS.name(), systemConfigPairs);
        EmbeddedServletContainerConfiguration config = new EmbeddedServletContainerConfiguration(systemConfig, userConfig);
        HostPortPair hostPort = config.getHostsPortsConfig();
        assertEquals(host, hostPort.getHost());
        assertEquals(port, hostPort.getPort());
    }
    
    /*
     * If both system *and* user config are specified, user config wins.
     */
    @Test
    public void userConfigOverridesSystemConfigListenAddress() throws InvalidConfigurationException {
        Properties systemConfig = new Properties();
        Properties userConfig = new Properties();
        String host = "127.0.0.1";
        int port = 8888;
        String systemConfigPairs = host + ":" + port;
        String userHost = "host.example.com";
        int userPort = 3334;
        String userConfigPairs = userHost + ":" + userPort;
        systemConfig.put(ConfigKeys.SERVLET_CONTAINER_BIND_ADDRESS.name(), systemConfigPairs);
        userConfig.put(ConfigKeys.SERVLET_CONTAINER_BIND_ADDRESS.name(), userConfigPairs);
        EmbeddedServletContainerConfiguration config = new EmbeddedServletContainerConfiguration(systemConfig, userConfig);
        HostPortPair hostPort = config.getHostsPortsConfig();
        assertEquals(userHost, hostPort.getHost());
        assertEquals(userPort, hostPort.getPort());
    }
    
    /*
     * If neither system nor user config has the CONFIG_LISTEN_ADDRESS key
     * config is invalid.
     */
    @Test(expected = InvalidConfigurationException.class)
    public void testInvalidConfigNullListenAddress() {
        Properties systemConfig = new Properties();
        Properties userConfig = new Properties();
        EmbeddedServletContainerConfiguration config = new EmbeddedServletContainerConfiguration(systemConfig, userConfig);
        // this should throw InvalidConfigException
        config.getHostsPortsConfig();
    }
    
    /*
     * Implementation detail. The host/port parser supports parsing lists.
     * However, in this context only one listen address can be specified. This
     * test ensures that an InvalidConfigurationException is thrown if config
     * contains a list.
     */
    @Test
    public void testListenAddressInvalidConfigList() {
        Properties systemConfig = new Properties();
        Properties userConfig = new Properties();
        String hostPortList = "127.0.0.1:8889,127.0.1.1:9999";
        systemConfig.put(ConfigKeys.SERVLET_CONTAINER_BIND_ADDRESS.name(), hostPortList);
        EmbeddedServletContainerConfiguration config = new EmbeddedServletContainerConfiguration(systemConfig, userConfig);
        try {
            config.getHostsPortsConfig();
            fail("expected InvalidConfigurationException due to host/port list size > 1 in system config");
        } catch (InvalidConfigurationException e) {
            // pass
        }
        userConfig.put(ConfigKeys.SERVLET_CONTAINER_BIND_ADDRESS.name(), hostPortList);
        systemConfig.clear();
        config = new EmbeddedServletContainerConfiguration(systemConfig, userConfig);
        try {
            config.getHostsPortsConfig();
            fail("expected InvalidConfigurationException due to host/port list size > 1 in user config");
        } catch (InvalidConfigurationException e) {
            // pass
        }
    }
    
    /*
     * Config should not throw and exception on instantiation if user config
     * file does not exist.
     */
    @Test
    public void canLoadConfigWithUserFileNotExisting() throws Exception {
        File systemFile = File.createTempFile("thermostat-tests", EmbeddedServletContainerConfigurationTest.class.getName());
        systemFile.deleteOnExit();
        assertTrue(systemFile.exists());
        File userFile = new File("/thermostat/i-do-not-exists");
        assertFalse(userFile.exists());
        try {
            new EmbeddedServletContainerConfiguration(systemFile, userFile);
            // pass
        } catch (InvalidConfigurationException e) {
            fail("Instantiation should not throw ICE due to missing files: " + e.getMessage());
        }
    }
    
    /*
     * Config should not throw and exception on instantiation if system config
     * file does not exist.
     */
    @Test
    public void canLoadConfigWithSystemFileNotExisting() throws Exception {
        File userFile = File.createTempFile("thermostat-tests", EmbeddedServletContainerConfigurationTest.class.getName());
        userFile.deleteOnExit();
        assertTrue(userFile.exists());
        File sysFile = new File("/thermostat/i-do-not-exists");
        assertFalse(sysFile.exists());
        try {
            new EmbeddedServletContainerConfiguration(sysFile, userFile);
            // pass
        } catch (InvalidConfigurationException e) {
            fail("Instantiation should not throw ICE due to missing files: " + e.getMessage());
        }
    }
    
    /*
     * Config should not throw and exception on instantiation if user config
     * and system config file does not exist.
     */
    @Test
    public void canLoadConfigWithSystemAndUserFileNotExisting() {
        File sysFile = new File("/thermostat/i-do-not-exists-system");
        File userFile = new File("/thermostat/i-do-not-exists-user");
        assertFalse(sysFile.exists());
        assertFalse(userFile.exists());
        try {
            new EmbeddedServletContainerConfiguration(sysFile, userFile);
            // pass
        } catch (InvalidConfigurationException e) {
            fail("Instantiation should not throw ICE due to missing files: " + e.getMessage());
        }
    }
    
    @Test
    public void testGetWebArchivePath() {
        CommonPaths paths = mock(CommonPaths.class);
        File fooThermostatHome = new File("/foo/path");
        when(paths.getSystemThermostatHome()).thenReturn(fooThermostatHome);
        
        // Any non-null file will do for user/system config files
        File irrelevantForTest = new File("irrelevant");
        
        EmbeddedServletContainerConfiguration config = new EmbeddedServletContainerConfiguration(paths, irrelevantForTest, irrelevantForTest);
        File expected = new File(fooThermostatHome, "webapp");
        File actual = config.getAbsolutePathToExplodedWebArchive();
        assertEquals(expected.getAbsolutePath(), actual.getAbsolutePath());
    }
    
    /*
     * If a request log configuration is set, be sure config paths are available
     */
    @Test
    public void verifyRequestLogConfiguration() {
        CommonPaths paths = mock(CommonPaths.class);
        File testUserHomeLogs = new File("/test/userhome/logs");
        when(paths.getUserLogDirectory()).thenReturn(testUserHomeLogs);

        // system config only
        Properties userConfig = new Properties();
        Properties systemConfig = new Properties();
        systemConfig.setProperty(ConfigKeys.REQUEST_LOG_FILENAME.name(), "logfile.log");
        EmbeddedServletContainerConfiguration config = new EmbeddedServletContainerConfiguration(paths, systemConfig, userConfig);
        assertTrue("Should have request log config", config.hasRequestLogConfig());
        assertEquals("/test/userhome/logs/logfile.log", config.getAbsolutePathToRequestLog());
        
        // user config only
        userConfig = new Properties();
        systemConfig = new Properties();
        userConfig.setProperty(ConfigKeys.REQUEST_LOG_FILENAME.name(), "userlogFile.log");
        config = new EmbeddedServletContainerConfiguration(paths, systemConfig, userConfig);
        assertTrue("Should have request log config", config.hasRequestLogConfig());
        assertEquals("/test/userhome/logs/userlogFile.log", config.getAbsolutePathToRequestLog());
        
        // user and system config
        userConfig = new Properties();
        systemConfig = new Properties();
        userConfig.setProperty(ConfigKeys.REQUEST_LOG_FILENAME.name(), "userlogFile.log");
        systemConfig.setProperty(ConfigKeys.REQUEST_LOG_FILENAME.name(), "systemlogFile.log");
        config = new EmbeddedServletContainerConfiguration(paths, systemConfig, userConfig);
        assertTrue("Should have request log config", config.hasRequestLogConfig());
        assertEquals("User config overrides system config",
                     "/test/userhome/logs/userlogFile.log",
                     config.getAbsolutePathToRequestLog());
        
        // no config
        userConfig = new Properties();
        systemConfig = new Properties();
        config = new EmbeddedServletContainerConfiguration(paths, systemConfig, userConfig);
        assertFalse("Should NOT have request log config", config.hasRequestLogConfig());
        assertNull("No config specified",
                     config.getAbsolutePathToRequestLog());
    }
    
    @Test
    public void canGetConnectionURL() {
        doConnectionUrlTest("http://[1fff:0:a88:85a3::ac1f]:8999/thermostat/storage", "[1fff:0:a88:85a3::ac1f]:8999", false);
        doConnectionUrlTest("http://host1.example.com:8888/thermostat/storage", "host1.example.com:8888", false);
        doConnectionUrlTest("https://host1.example.com:8998/thermostat/storage", "host1.example.com:8998", true);
    }
    
    @Test
    public void canGetContextPath() {
        String expectedContextPath = "/thermostat/storage";
        EmbeddedServletContainerConfiguration config = new EmbeddedServletContainerConfiguration((Properties)null /* unused */, (Properties)null /* unused */);
        assertEquals(expectedContextPath, config.getContextPath());
    }
    
    private void doConnectionUrlTest(String expectedUrl, String hostPortToken, boolean enableSSL) {
        Properties systemConfig = new Properties();
        Properties userConfig = new Properties();
        systemConfig.put(ConfigKeys.SERVLET_CONTAINER_BIND_ADDRESS.name(), hostPortToken);
        if (enableSSL) {
            systemConfig.put(ConfigKeys.USE_SSL.name(), Boolean.TRUE.toString());
        }
        EmbeddedServletContainerConfiguration config = new EmbeddedServletContainerConfiguration(systemConfig, userConfig);
        assertEquals(expectedUrl, config.getConnectionUrl());
    }
    
    @Test
    public void canGetPathToJaasConfig() throws InvalidConfigurationException {
        CommonPaths paths = mock(CommonPaths.class);
        String fooThHome = "/foo/path/etc";
        File fooThermostatHome = new File(fooThHome);
        when(paths.getSystemConfigurationDirectory()).thenReturn(fooThermostatHome);
        
        // Any non-null file will do for user/system config files
        File irrelevantForTest = new File("irrelevant");
        
        EmbeddedServletContainerConfiguration config = new EmbeddedServletContainerConfiguration(paths, irrelevantForTest, irrelevantForTest);
        String actual = config.getAbsolutePathToJaasConfig();
        assertEquals(fooThHome + "/thermostat_jaas.conf", actual);
    }
}
