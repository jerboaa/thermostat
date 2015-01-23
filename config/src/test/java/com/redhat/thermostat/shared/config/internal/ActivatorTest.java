/*
 * Copyright 2012-2015 Red Hat, Inc.
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

package com.redhat.thermostat.shared.config.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.redhat.thermostat.shared.config.CommonPaths;
import com.redhat.thermostat.shared.config.SSLConfiguration;
import com.redhat.thermostat.testutils.StubBundleContext;
import com.redhat.thermostat.testutils.TestUtils;

public class ActivatorTest {

    private String originalThermostatHomeProperty;
    private File testHome;

    @Before
    public void setUp() {
        Path testHomePath = null;
        try {
            testHomePath = Files.createTempDirectory("ActivatorTest_THERMOSTAT_HOME");
        } catch (IOException e) {
            e.printStackTrace();
        }
        File testHome = testHomePath.toFile();
        originalThermostatHomeProperty = System.getProperty("THERMOSTAT_HOME");
        System.setProperty("THERMOSTAT_HOME", testHome.getAbsolutePath());
    }

    @After
    public void tearDown() throws IOException {
        if (testHome != null) {
            TestUtils.deleteRecursively(testHome);
        }
        if (originalThermostatHomeProperty != null) {
            System.setProperty("THERMOSTAT_HOME", originalThermostatHomeProperty);
        } else {
            System.clearProperty("THERMOSTAT_HOME");
        }
    }

    @Test
    public void verifyServicesRegistered() throws Exception {
        StubBundleContext ctx = new StubBundleContext();
        Activator activator = new Activator();
        activator.start(ctx);

        assertTrue(ctx.isServiceRegistered(SSLConfiguration.class.getName(),
                SSLConfigurationImpl.class));
        assertTrue(ctx.isServiceRegistered(CommonPaths.class.getName(),
                CommonPathsImpl.class));
        assertEquals(2, ctx.getAllServices().size());

    }


    
}

