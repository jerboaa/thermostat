/*
 * Copyright 2012, 2013 Red Hat, Inc.
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

import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Hashtable;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.launch.Framework;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.redhat.thermostat.launcher.BundleManager;
import com.redhat.thermostat.launcher.Launcher;
import com.redhat.thermostat.main.impl.FrameworkProvider;

@RunWith(PowerMockRunner.class)
@PrepareForTest({FrameworkProvider.class})
public class ThermostatTest {

    private Path tempDir;

    private Framework mockFramework;

    private BundleContext mockContext;

    @SuppressWarnings("rawtypes")
    @Before
    public void setUp() throws Exception {
        tempDir = Files.createTempDirectory("test");
        tempDir.toFile().deleteOnExit();
        System.setProperty("THERMOSTAT_HOME", tempDir.toString());
        System.setProperty("USER_THERMOSTAT_HOME", tempDir.toString());
        
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

        Framework framework = mock(Framework.class);
        TestFrameworkFactory.setFramework(framework);
        when(framework.getBundleContext()).thenReturn(mockContext);
        Bundle mockBundle = mock(Bundle.class);
        when(mockContext.installBundle(any(String.class))).thenReturn(mockBundle);
        when(mockBundle.getHeaders()).thenReturn(new Hashtable<String, String>());
        ServiceTracker registryTracker = mock(ServiceTracker.class);
        PowerMockito
                .whenNew(ServiceTracker.class)
                .withParameterTypes(BundleContext.class, String.class,
                        ServiceTrackerCustomizer.class)
                .withArguments(any(BundleContext.class),
                        eq(BundleManager.class.getName()), any(ServiceTrackerCustomizer.class))
                .thenReturn(registryTracker);
        when(registryTracker.waitForService(0)).thenReturn(mock(BundleManager.class));
        ServiceTracker launcherTracker = mock(ServiceTracker.class);
        Launcher launcher = mock(Launcher.class);
        PowerMockito
                .whenNew(ServiceTracker.class)
                .withParameterTypes(BundleContext.class, String.class,
                        ServiceTrackerCustomizer.class)
                .withArguments(any(BundleContext.class),
                        eq(Launcher.class.getName()),
                        any(ServiceTrackerCustomizer.class))
                .thenReturn(launcherTracker);
        when(launcherTracker.waitForService(0))
                .thenReturn(launcher);
    }

    @Test
    public void testOSGIDirExists() throws Exception {
        Path osgiDir = tempDir.resolve("osgi-cache");
        osgiDir.toFile().mkdirs();
        osgiDir.toFile().deleteOnExit();
        assertTrue(osgiDir.toFile().exists());
        try {
            Thermostat.main(new String[0]);
        } catch (Exception e) {
            e.printStackTrace();
        }
        assertTrue(osgiDir.toFile().exists());
    }

    @Test
    public void testFrameworkInitAndStart() throws Exception {
        Path osgiDir = tempDir.resolve("osgi-cache");
        osgiDir.toFile().mkdirs();
        osgiDir.toFile().deleteOnExit();
        mockFramework = mock(Framework.class);
        when(mockFramework.getBundleContext()).thenReturn(mockContext);
        TestFrameworkFactory.setFramework(mockFramework);
        Thermostat.main(new String[0]);
        verify(mockFramework).init();
        verify(mockFramework).start();
    }
}

