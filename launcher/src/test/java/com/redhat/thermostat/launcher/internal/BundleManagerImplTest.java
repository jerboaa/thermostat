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

package com.redhat.thermostat.launcher.internal;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.whenNew;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.launch.Framework;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.redhat.thermostat.launcher.BundleInformation;
import com.redhat.thermostat.shared.config.CommonPaths;

@RunWith(PowerMockRunner.class)
@PrepareForTest({BundleManagerImpl.class, FrameworkUtil.class})
public class BundleManagerImplTest {

    private static final String jar1Name = "/one.jar";
    private static final String jar2Name = "/two.jar";
    private static final String jar3Name = "/three.jar";

    private Bundle b1, b2, b3;
    private List<String> bundleLocs;

    private Framework theFramework;
    private BundleContext theContext;

    private BundleLoader loader;
    private CommonPaths paths;

    private Path testRoot;
    
    @Before
    public void setUp() throws Exception {
        testRoot = Files.createTempDirectory("thermostat");
        Path pluginRootDir = testRoot.resolve("plugins");
        Files.createDirectory(pluginRootDir);
        Path jarRootDir = testRoot.resolve("libs");
        Files.createDirectories(jarRootDir);

        paths = mock(CommonPaths.class);
        when(paths.getSystemLibRoot()).thenReturn(jarRootDir.toFile());
        when(paths.getSystemPluginRoot()).thenReturn(pluginRootDir.toFile());

        theContext = mock(BundleContext.class);
        theFramework = mock(Framework.class);
        when(theFramework.getBundleContext()).thenReturn(theContext);
        when(theContext.getBundle(0)).thenReturn(theFramework);

        bundleLocs = Arrays.asList(jar1Name, jar2Name, jar3Name);
        b1 = mock(Bundle.class);
        when(b1.getLocation()).thenReturn(jar1Name);
        when(b1.getState()).thenReturn(Bundle.ACTIVE);
        b2 = mock(Bundle.class);
        when(b2.getLocation()).thenReturn(jar2Name);
        when(b2.getState()).thenReturn(Bundle.ACTIVE);
        b3 = mock(Bundle.class);
        when(b3.getLocation()).thenReturn(jar3Name);
        when(b3.getState()).thenReturn(Bundle.ACTIVE);
        List<Bundle> installed = Arrays.asList(b1, b2, b3);

        loader = mock(BundleLoader.class);
        when(loader.installAndStartBundles(any(Framework.class), eq(bundleLocs))).
                thenReturn(installed);
        whenNew(BundleLoader.class)
            .withNoArguments()
            .thenReturn(loader);
    }

    @After
    public void tearDown() throws IOException {
        Files.walkFileTree(testRoot, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Files.delete(file);
                return FileVisitResult.CONTINUE;
            }
            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                if (exc == null) {
                    Files.delete(dir);
                    return FileVisitResult.CONTINUE;
                } else {
                    throw exc;
                }
            }
        });
    }

    @Test
    public void verifyInstallAndStartBundles() throws Exception {
        Bundle theBundle = b2;
        when(theContext.getBundles()).thenReturn(new Bundle[] {});
        when(theBundle.getBundleContext()).thenReturn(theContext);

        mockStatic(FrameworkUtil.class);

        when(FrameworkUtil.getBundle(any(Class.class))).thenReturn(theBundle);

        BundleManagerImpl registry = new BundleManagerImpl(paths);
        registry.loadBundlesByPath(bundleLocs);
        verify(loader).installAndStartBundles(any(Framework.class), eq(bundleLocs));
    }

    @Test
    public void verifyLoadBundleByNameAndVersionWithBundleNotFound() throws Exception {
        Bundle theBundle = b2;
        when(theContext.getBundles()).thenReturn(new Bundle[] {});
        when(theBundle.getBundleContext()).thenReturn(theContext);

        mockStatic(FrameworkUtil.class);

        when(FrameworkUtil.getBundle(any(Class.class))).thenReturn(theBundle);

        BundleManagerImpl registry = new BundleManagerImpl(paths);
        Map<BundleInformation, Path> bundleToPath = new HashMap<>();

        registry.setKnownBundles(bundleToPath);

        registry.loadBundlesByName(Arrays.asList(new BundleInformation("foo", "1.0")));

        verify(loader).installAndStartBundles(theFramework, Arrays.<String>asList());
    }

    @Test
    public void verifyLoadBundleByNameAndVersion() throws Exception {

        Bundle theBundle = b2;
        when(theContext.getBundles()).thenReturn(new Bundle[] {});
        when(theBundle.getBundleContext()).thenReturn(theContext);

        mockStatic(FrameworkUtil.class);

        when(FrameworkUtil.getBundle(any(Class.class))).thenReturn(theBundle);

        BundleManagerImpl registry = new BundleManagerImpl(paths);
        Map<BundleInformation, Path> bundleToPath = new HashMap<>();
        bundleToPath.put(new BundleInformation("foo", "1.0"), Paths.get(jar1Name));
        registry.setKnownBundles(bundleToPath);

        registry.loadBundlesByName(Arrays.asList(new BundleInformation("foo", "1.0")));

        verify(loader).installAndStartBundles(theFramework, Arrays.asList(new File(jar1Name).toURI().toURL().toString()));
    }

    @Test
    public void verifyLoadBundleByNameAndLatestVersion() throws Exception {

        Bundle theBundle = b2;
        when(theContext.getBundles()).thenReturn(new Bundle[] {});
        when(theBundle.getBundleContext()).thenReturn(theContext);

        mockStatic(FrameworkUtil.class);

        when(FrameworkUtil.getBundle(any(Class.class))).thenReturn(theBundle);

        BundleManagerImpl registry = new BundleManagerImpl(paths);
        registry.setIgnoreBundleVersions(true);
        Map<BundleInformation, Path> bundleToPath = new HashMap<>();
        bundleToPath.put(new BundleInformation("foo", "1.0"), Paths.get(jar1Name));
        bundleToPath.put(new BundleInformation("foo", "2.0"), Paths.get(jar2Name));

        registry.setKnownBundles(bundleToPath);

        registry.loadBundlesByName(Arrays.asList(new BundleInformation("foo", "3.0")));

        verify(loader).installAndStartBundles(theFramework, Arrays.asList(new File(jar2Name).toURI().toURL().toString()));
    }

    @Test
    public void verifyLoadBundleByNameAndLatestVersionWithoutSupportForLatestVersion() throws Exception {

        Bundle theBundle = b2;
        when(theContext.getBundles()).thenReturn(new Bundle[] {});
        when(theBundle.getBundleContext()).thenReturn(theContext);

        mockStatic(FrameworkUtil.class);

        when(FrameworkUtil.getBundle(any(Class.class))).thenReturn(theBundle);

        BundleManagerImpl registry = new BundleManagerImpl(paths);
        Map<BundleInformation, Path> bundleToPath = new HashMap<>();
        bundleToPath.put(new BundleInformation("foo", "1.0"), Paths.get(jar1Name));
        bundleToPath.put(new BundleInformation("foo", "2.0"), Paths.get(jar2Name));

        registry.setKnownBundles(bundleToPath);

        registry.loadBundlesByName(Arrays.asList(new BundleInformation("foo", "3.0")));

        verify(loader).installAndStartBundles(theFramework, new ArrayList<String>());
    }

    @Test
    public void verifyAlreadyLoadedBundlesNotReloaded() throws Exception {

        Bundle theBundle = b2;
        when(theContext.getBundles()).thenReturn(new Bundle[] {b1, b2});
        when(theContext.getBundle(jar1Name)).thenReturn(b1);
        when(theContext.getBundle(jar2Name)).thenReturn(b2);

        when(theBundle.getBundleContext()).thenReturn(theContext);
        mockStatic(FrameworkUtil.class);
        when(FrameworkUtil.getBundle(any(Class.class))).thenReturn(theBundle);

        BundleManagerImpl registry = new BundleManagerImpl(paths);
        registry.loadBundlesByPath(bundleLocs);
        verify(loader).installAndStartBundles(theFramework, Arrays.asList(jar3Name));
    }

    @Test
    public void verifySetOSGiVerbosityByReflection() throws Exception {

        // All this fluff is just so constructor doesn't NPE.
        Bundle theBundle = b2;
        BundleContext theContext = mock(BundleContext.class);
        when(theContext.getBundles()).thenReturn(new Bundle[]{});
        Framework theFramework = mock(Framework.class);
        when(theFramework.getBundleContext()).thenReturn(theContext);
        when(theContext.getBundle(0)).thenReturn(theFramework);
        when(theBundle.getBundleContext()).thenReturn(theContext);
        mockStatic(FrameworkUtil.class);
        when(FrameworkUtil.getBundle(any(Class.class))).thenReturn(theBundle);

        Object registry = new BundleManagerImpl(paths);
        Class<?> clazz = registry.getClass();
        Method m = clazz.getMethod("setPrintOSGiInfo", Boolean.TYPE);
        m.invoke(registry, true); // If this fails, then API has changed in ways that break FrameworkProvider.
    }

}

