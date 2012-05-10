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

package com.redhat.thermostat.client.osgi;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.launch.Framework;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.redhat.thermostat.client.Main;

@RunWith(PowerMockRunner.class)
@PrepareForTest({Main.class, ThermostatActivator.class})
public class ThermostatTest {

    private Path tempDir;

    private Framework mockFramework;

    private BundleContext mockContext;

    @Before
    public void setUp() throws IOException {

        tempDir = Files.createTempDirectory("test");
        tempDir.toFile().deleteOnExit();
        System.setProperty("THERMOSTAT_HOME", tempDir.toString());

	mockContext = mock(BundleContext.class);

        mockFramework = mock(Framework.class);
	when(mockFramework.getBundleContext()).thenReturn(mockContext);

        TestFrameworkFactory.setFramework(mockFramework);

        PowerMockito.mockStatic(Main.class);
    }

    @Test
    public void testCreateOSGIDir() throws Exception {
        Path osgiDir = tempDir.resolve("osgi");
        assertFalse(osgiDir.toFile().exists());
        Thermostat.main(new String[0]);
        assertTrue(osgiDir.toFile().exists());
    }

    @Test
    public void testOSGIDirExists() throws Exception {
        Path osgiDir = tempDir.resolve("osgi");
        osgiDir.toFile().mkdirs();
        assertTrue(osgiDir.toFile().exists());
        Thermostat.main(new String[0]);
        assertTrue(osgiDir.toFile().exists());
    }

    @Test(expected=InternalError.class)
    public void testCreateOSGIDirNotPossible() throws Exception {
        try {
            Path osgiDir = tempDir.resolve("osgi");
            assertFalse(osgiDir.toFile().exists());
            tempDir.toFile().setWritable(false);
            Thermostat.main(new String[0]);
        } finally {
            tempDir.toFile().setWritable(true);
        }
    }

    @Test
    public void testFrameworkConfig() throws Exception {
        Thermostat.main(new String[0]);
        Map<String,String> config = TestFrameworkFactory.getConfig();
        Path osgiDir = tempDir.resolve("osgi");
        assertEquals(osgiDir.toString(), config.get(Constants.FRAMEWORK_STORAGE));
    }

    @Test
    public void testFrameworkInitAndStart() throws Exception {
        Thermostat.main(new String[0]);
        verify(mockFramework).init();
        verify(mockFramework).start();
    }

    @Test
    public void testThermostatActivator() throws Exception {
        ThermostatActivator activator = mock(ThermostatActivator.class);
        PowerMockito.mockStatic(ThermostatActivator.class);
        when(ThermostatActivator.newInstance()).thenReturn(activator);
        Thermostat.main(new String[0]);
        verify(activator).start(mockContext);
    }
}
