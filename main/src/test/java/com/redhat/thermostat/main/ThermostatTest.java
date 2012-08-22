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

package com.redhat.thermostat.main;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
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

import com.redhat.thermostat.bundles.impl.OSGiRegistryImpl;
import com.redhat.thermostat.common.Configuration;
import com.redhat.thermostat.launcher.Launcher;
import com.redhat.thermostat.launcher.internal.LauncherImpl;

@RunWith(PowerMockRunner.class)
@PrepareForTest(value = Thermostat.class)
public class ThermostatTest {

    private Path tempDir;

    private Framework mockFramework;

    private BundleContext mockContext;

    @Before
    public void setUp() throws Exception {

        final OSGiRegistryImpl osgiRegistry = mock(OSGiRegistryImpl.class);
        PowerMockito.whenNew(OSGiRegistryImpl.class).withArguments(any(Configuration.class)).thenReturn(osgiRegistry);

        tempDir = Files.createTempDirectory("test");
        tempDir.toFile().deleteOnExit();
        System.setProperty("THERMOSTAT_HOME", tempDir.toString());
        
        File tempEtc = new File(tempDir.toFile(), "etc");
        tempEtc.mkdirs();
        tempEtc.deleteOnExit();
        
        File tempProps = new File(tempEtc, "osgi-export.properties");
        tempProps.createNewFile();
        tempProps.deleteOnExit();

        File tempBundleProps = new File(tempEtc, "bundles.properties");
        tempBundleProps.createNewFile();
        tempBundleProps.deleteOnExit();
        
        File tempLibs = new File(tempDir.toFile(), "libs");
        tempLibs.mkdirs();
        tempLibs.deleteOnExit();
        
        mockContext = mock(BundleContext.class);

        mockFramework = mock(Framework.class);
        when(mockFramework.getBundleContext()).thenReturn(mockContext);

        TestFrameworkFactory.setFramework(mockFramework);
    }

    // TODO These now seem to belong in OSGiRegistryTest

    /*
    @Test
    public void testOSGIDirExists() throws Exception {
        Path osgiDir = tempDir.resolve("osgi");
        osgiDir.toFile().mkdirs();
        assertTrue(osgiDir.toFile().exists());
        try {
            Thermostat.main(new String[0]);
        } catch (Exception e) {
            e.printStackTrace();
        }
        assertTrue(osgiDir.toFile().exists());
    }*/

    /*@Test
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
    }*/
}
