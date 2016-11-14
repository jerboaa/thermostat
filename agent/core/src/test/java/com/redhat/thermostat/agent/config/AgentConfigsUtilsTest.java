/*
 * Copyright 2012-2016 Red Hat, Inc.
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

import java.io.File;
import java.io.IOException;
import java.util.Properties;

import org.junit.After;
import org.junit.Assert;
import org.junit.Test;

import com.redhat.thermostat.common.utils.HostPortPair;
import com.redhat.thermostat.shared.config.InvalidConfigurationException;
import com.redhat.thermostat.testutils.TestUtils;

public class AgentConfigsUtilsTest {
    
    private File tmpDir;

    @After
    public void tearDown() throws IOException {
        if (tmpDir != null) {
            TestUtils.deleteRecursively(tmpDir);
        }
        AgentConfigsUtils.setConfigFiles(null, null);
        tmpDir = null;
    }
    
    @Test
    public void testSystemDbUrl() throws InvalidConfigurationException, IOException {
        Properties sysProps = createSystemProperties();
        setConfigs(sysProps, new Properties());
        AgentStartupConfiguration config = AgentConfigsUtils.createAgentConfigs();

        Assert.assertEquals("http://1.2.3.4:9001/hello", config.getDBConnectionString());
    }
    
    @Test
    public void testSystemPurgeProp() throws InvalidConfigurationException, IOException {
        Properties sysProps = createSystemProperties();
        setConfigs(sysProps, new Properties());
        AgentStartupConfiguration config = AgentConfigsUtils.createAgentConfigs();

        Assert.assertFalse(config.purge());
    }
    
    @Test
    public void testSystemAddressProp() {
        Properties sysProps = createSystemProperties();
        setConfigs(sysProps, new Properties());
        AgentStartupConfiguration config = AgentConfigsUtils.createAgentConfigs();

        HostPortPair hostPorts = config.getConfigListenAddress();
        Assert.assertEquals("42.42.42.42", hostPorts.getHost());
        Assert.assertEquals(42, hostPorts.getPort());
    }
    
    @Test
    public void testUserDbUrl() throws InvalidConfigurationException, IOException {
        Properties sysProps = createSystemProperties();
        Properties userProps = createUserProperties();
        setConfigs(sysProps, userProps);
        AgentStartupConfiguration config = AgentConfigsUtils.createAgentConfigs();        

        Assert.assertEquals("http://5.6.7.8:9002/world", config.getDBConnectionString());
    }
    
    @Test
    public void testUserPurgeProp() throws InvalidConfigurationException, IOException {
        Properties sysProps = createSystemProperties();
        Properties userProps = createUserProperties();
        setConfigs(sysProps, userProps);
        AgentStartupConfiguration config = AgentConfigsUtils.createAgentConfigs();        

        Assert.assertTrue(config.purge());
    }
    
    @Test
    public void testUserAddressProp() {
        Properties sysProps = createSystemProperties();
        Properties userProps = createUserProperties();
        setConfigs(sysProps, userProps);
        AgentStartupConfiguration config = AgentConfigsUtils.createAgentConfigs();        

        HostPortPair hostPorts = config.getConfigListenAddress();
        Assert.assertEquals("24.24.24.24", hostPorts.getHost());
        Assert.assertEquals(24, hostPorts.getPort());
    }
    
    @Test
    public void canParseIpv6ConfigAddress() {
        String ipV6AddressPair = "[::1]:12000";
        Properties sysProps = createSystemProperties(ipV6AddressPair);
        setConfigs(sysProps, new Properties());
        AgentStartupConfiguration config = AgentConfigsUtils.createAgentConfigs();        

        HostPortPair hostPorts = config.getConfigListenAddress();
        Assert.assertEquals("::1", hostPorts.getHost());
        Assert.assertEquals(12000, hostPorts.getPort());
    }
    
    private Properties createSystemProperties(String configListenAddress) {
        return doCreateSystemProperties(configListenAddress);
    }
    
    private Properties createSystemProperties() {
        return createSystemProperties("42.42.42.42:42");
    }
    
    private Properties createUserProperties() {
        Properties agentProperties = new Properties();
        agentProperties.setProperty("DB_URL", "http://5.6.7.8:9002/world");
        agentProperties.setProperty("SAVE_ON_EXIT", "false");
        agentProperties.setProperty("CONFIG_LISTEN_ADDRESS", "24.24.24.24:24");
        return agentProperties;
    }
    
    private Properties doCreateSystemProperties(String configListenAddress) {
        Properties agentProperties = new Properties();
        agentProperties.setProperty("DB_URL", "http://1.2.3.4:9001/hello");
        agentProperties.setProperty("SAVE_ON_EXIT", "true");
        agentProperties.setProperty("CONFIG_LISTEN_ADDRESS", configListenAddress);
        return agentProperties;
    }
    
    private void setConfigs(Properties sysProps, Properties userProps) {
        try {
            String tmpDirPath = TestUtils.setupAgentConfigs(sysProps, userProps);
            tmpDir = new File(tmpDirPath);
            File sysAgentConf = TestUtils.getAgentConfFile();
            File userAgentConf = TestUtils.getUserAgentConfFile();
            AgentConfigsUtils.setConfigFiles(sysAgentConf, userAgentConf);
        } catch (IOException e) {
            throw new AssertionError("Unable to create agent configuration", e);
        }
    }
}

