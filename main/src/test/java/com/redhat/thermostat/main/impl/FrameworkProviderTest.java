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

package com.redhat.thermostat.main.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.framework.launch.Framework;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.redhat.thermostat.launcher.BundleInformation;
import com.redhat.thermostat.launcher.BundleManager;
import com.redhat.thermostat.launcher.Launcher;
import com.redhat.thermostat.shared.config.CommonPaths;
import com.redhat.thermostat.shared.config.internal.CommonPathsImpl;

@RunWith(PowerMockRunner.class)
@PrepareForTest({FrameworkProvider.class})
public class FrameworkProviderTest {

    private static final String THERMOSTAT_HOME_PROPERTY = "THERMOSTAT_HOME";
    private static final String USER_THERMOSTAT_HOME_PROPERTY = "USER_THERMOSTAT_HOME";

    private BundleContext mockContext;

    private Framework framework;

    private FakeBundleManager bundleManager;
    private Launcher launcher;
    private CommonPaths paths;
    private String savedHome, savedUserHome;

    static class FakeBundleManager extends BundleManager /* extends BundleManagerImpl */ {

        private boolean printOSGiInfo;
        private boolean ignoreBundleVersion;

        @Override
        public void loadBundlesByName(List<BundleInformation> bundles) throws BundleException, IOException {
            // do nothing
        }

        @Override
        public CommonPaths getCommonPaths() {
            return null;
        }

        // @Override
        public void setPrintOSGiInfo(boolean print) {
            this.printOSGiInfo = print;
        }

        // @Override
        public void setIgnoreBundleVersions(boolean ignore) {
            this.ignoreBundleVersion = ignore;
        }

    }

    @SuppressWarnings("rawtypes")
    @Before
    public void setUp() throws Exception {
        Path tempDir;
        tempDir = Files.createTempDirectory("FrameworkProviderTest");
        tempDir.toFile().deleteOnExit();
        savedHome = System.setProperty(THERMOSTAT_HOME_PROPERTY, tempDir.toString());
        savedUserHome = System.setProperty(USER_THERMOSTAT_HOME_PROPERTY, tempDir.toString());
        paths = new CommonPathsImpl();

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

        framework = mock(Framework.class);
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
        bundleManager = new FakeBundleManager();
        when(registryTracker.waitForService(0)).thenReturn(bundleManager);
        ServiceTracker launcherTracker = mock(ServiceTracker.class);
        launcher = mock(Launcher.class);
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

        Path osgiDir;

        osgiDir = tempDir.resolve("osgi-cache");
        osgiDir.toFile().mkdirs();
        osgiDir.toFile().deleteOnExit();
        assertTrue(osgiDir.toFile().exists());
    }

    @After
    public void clearSystemProperties() {
        if (savedHome == null) {
            System.clearProperty(THERMOSTAT_HOME_PROPERTY);
        } else {
            System.setProperty(THERMOSTAT_HOME_PROPERTY, savedHome);
            savedHome = null;
        }
        if (savedUserHome == null) {
            System.clearProperty(USER_THERMOSTAT_HOME_PROPERTY);
        } else {
            System.setProperty(USER_THERMOSTAT_HOME_PROPERTY, savedUserHome);
            savedUserHome = null;
        }
        paths = null;
    }

    @Test
    public void testStartRunsOSGiFramework() throws Exception {
        FrameworkProvider provider = new FrameworkProvider(paths, false, false, null);

        provider.start(new String[] {});

        verify(framework).init();
        verify(framework).start();
    }

    @Test
    public void testStartRunsLauncher() throws Exception {
        FrameworkProvider provider = new FrameworkProvider(paths, false, false, null);

        provider.start(new String[] {});

        verify(launcher).run(new String[] {}, false);
    }

    @Test
    public void testPrintOSGiInfoParameterIsPassedToBundleManager() {
        FrameworkProvider provider = new FrameworkProvider(paths, true, false, null);

        provider.start(new String[] {});

        assertEquals(true, bundleManager.printOSGiInfo);
    }

    @Test
    public void testIgnoreBundleVersionsParameterIsPassedToBundleManager() {
        FrameworkProvider provider = new FrameworkProvider(paths, false, true, null);

        provider.start(new String[] {});

        assertEquals(true, bundleManager.ignoreBundleVersion);
    }

    @Test(expected=RuntimeException.class)
    public void testErrorThrownOnEmptyBootDelegationParameter() {
        FrameworkProvider provider = new FrameworkProvider(paths, false, true, "");

        provider.start(new String[] {});
    }

    @Test
    public void testNullBootDelegationIsNotSetInConfiguration() {
        FrameworkProvider provider = new FrameworkProvider(paths, false, false, null);

        provider.start(new String[] {});

        Map<String, String> config = TestFrameworkFactory.getConfig();
        assertFalse(config.containsKey(Constants.FRAMEWORK_BOOTDELEGATION));
    }

    @Test
    public void testPackagesListedInBootDelegationArePassedToFramework() {
        FrameworkProvider provider = new FrameworkProvider(paths, false, true, "foo");

        provider.start(new String[] {});

        Map<String, String> config = TestFrameworkFactory.getConfig();
        assertEquals("foo", config.get(Constants.FRAMEWORK_BOOTDELEGATION));
    }
}
