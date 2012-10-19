/*
 * Copyright 2012 Red Hat, Inc.
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

import junit.framework.Assert;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.redhat.thermostat.common.TestUtils;
import com.redhat.thermostat.common.cli.SimpleArguments;
import com.redhat.thermostat.common.config.InvalidConfigurationException;

public class AgentOptionParserTest {
    
    private static File tmpFile;
    
    @BeforeClass
    public static void setup() throws IOException {
        tmpFile = new File(TestUtils.setupAgentConfigs());
    }
    
    @AfterClass
    public static void shutdown() {
        tmpFile.delete();
    }
    
    @Test
    public void testConfigs1() throws IOException, InvalidConfigurationException {
        
        SimpleArguments args = new SimpleArguments();
        args.addArgument("dbUrl", "testURL");
        args.addArgument("debug", "--debug");
        
        AgentStartupConfiguration configs = AgentConfigsUtils.createAgentConfigs();
        AgentOptionParser parser = new AgentOptionParser(configs, args);
        parser.parse();
        
        Assert.assertEquals("testURL", configs.getDBConnectionString());
        Assert.assertTrue(configs.isDebugConsole());
        Assert.assertFalse(configs.purge());
    }
    
    @Test
    public void testConfigs2() throws IOException, InvalidConfigurationException {
        
        SimpleArguments args = new SimpleArguments();
        args.addArgument("dbUrl", "testURL2");
        args.addArgument("saveOnExit", "--saveOnExit");
        
        AgentStartupConfiguration configs = new AgentStartupConfiguration();
        AgentOptionParser parser = new AgentOptionParser(configs, args);
        parser.parse();
        
        Assert.assertEquals("testURL2", configs.getDBConnectionString());
        Assert.assertFalse(configs.isDebugConsole());
        Assert.assertFalse(configs.purge());
    }
}
