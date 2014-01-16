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

package com.redhat.thermostat.agent.config;

import java.io.File;
import java.io.IOException;
import java.util.Properties;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import com.redhat.thermostat.shared.config.InvalidConfigurationException;
import com.redhat.thermostat.testutils.TestUtils;

public class AgentConfigsUtilsTest {

    @BeforeClass
    public static void setUpOnce() {

        Properties agentProperties = new Properties();
        agentProperties.setProperty("SAVE_ON_EXIT", "true");
        agentProperties.setProperty("CONFIG_LISTEN_ADDRESS", "42.42.42.42:42");

        try {
            TestUtils.setupAgentConfigs(agentProperties);
            File agentConf = TestUtils.getAgentConfFile();
            // By default system config == user config
            AgentConfigsUtils.setConfigFiles(agentConf, agentConf);
        } catch (IOException e) {
            throw new AssertionError("Unable to create agent configuration", e);
        }
    }
    
    @Test
    public void testCreateAgentConfigs() throws InvalidConfigurationException, IOException {
        AgentStartupConfiguration config = AgentConfigsUtils.createAgentConfigs();        

        Assert.assertFalse(config.purge());
        Assert.assertEquals("42.42.42.42:42", config.getConfigListenAddress());
    }
}

