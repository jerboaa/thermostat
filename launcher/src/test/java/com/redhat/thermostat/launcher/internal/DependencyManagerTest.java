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

package com.redhat.thermostat.launcher.internal;

import com.redhat.thermostat.launcher.BundleInformation;
import com.redhat.thermostat.shared.config.CommonPaths;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.jar.Attributes;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;
import static org.junit.Assert.assertEquals;

public class DependencyManagerTest {

    private DependencyManager depManager;
    private File underneathTheBridge;
    private File userPluginRoot;
    private File systemPluginRoot;

    @Before
    public void setUp() throws Exception {
        CommonPaths paths = Mockito.mock(CommonPaths.class);
        userPluginRoot = Files.createTempDirectory("userPluginRoot").toFile();
        systemPluginRoot = Files.createTempDirectory("systemPluginRoot").toFile();
        try {
            underneathTheBridge = Files.createTempDirectory("underneathTheBridge").toFile();
        } catch (IOException e) {
            e.printStackTrace();
        }
        underneathTheBridge.deleteOnExit();
        userPluginRoot.deleteOnExit();
        systemPluginRoot.deleteOnExit();
        Path a = createJar("a", null, underneathTheBridge.toPath());
        Path b = createJar("b", "a", underneathTheBridge.toPath());
        Path c = createJar("c", "b", underneathTheBridge.toPath());
        Path d = createJar("d", "b,c", underneathTheBridge.toPath());
        Path e = createJar("e", "d", underneathTheBridge.toPath());
        when(paths.getUserPluginRoot()).thenReturn(userPluginRoot);
        when(paths.getSystemPluginRoot()).thenReturn(systemPluginRoot);
        when(paths.getSystemLibRoot()).thenReturn(underneathTheBridge);
        depManager = new DependencyManager(paths);
    }

    @After
    public void tearDown() {
        try {
            deleteDirectory(underneathTheBridge.toPath());
            deleteDirectory(userPluginRoot.toPath());
            deleteDirectory(systemPluginRoot.toPath());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testGetBundle() throws Exception {
        ArrayList<BundleInformation> bundles = new ArrayList<>(depManager.getDependencies(new BundleInformation("d", "1.0")));
        assertEquals(4, bundles.size());
        assertEquals("d", bundles.get(0).getName());
        assertEquals("1.0", bundles.get(0).getVersion());
        assertEquals("c", bundles.get(1).getName());
        assertEquals("1.0", bundles.get(1).getVersion());
        assertEquals("b", bundles.get(2).getName());
        assertEquals("1.0", bundles.get(2).getVersion());
        assertEquals("a", bundles.get(3).getName());
        assertEquals("1.0", bundles.get(3).getVersion());
    }

    @Test
    public void testMismatchBundleVersion() throws Exception {
        List<BundleInformation> bundles = depManager.getDependencies(new BundleInformation("d", "1.2"));
        assertEquals(0, bundles.size());
    }

    @Test
    public void testNonExistentDependency() {
        List<BundleInformation> bundles = depManager.getDependencies(new BundleInformation("blob", "1.x"));
        assertEquals(0, bundles.size());
    }

    /**
     * The file visitor should scan system and user plugin roots
     * recursively however it should not scan system lib root recursively.
     */
    @Test
    public void testScannedLocations() {
        CommonPaths paths = mock(CommonPaths.class);
        File systemLibRoot = mock(File.class);
        File[] libContents = {underneathTheBridge};
        when(paths.getUserPluginRoot()).thenReturn(userPluginRoot);
        when(paths.getSystemPluginRoot()).thenReturn(systemPluginRoot);
        when(paths.getSystemLibRoot()).thenReturn(systemLibRoot);
        when(systemLibRoot.listFiles()).thenReturn(libContents);
        DependencyManager testManager = new DependencyManager(paths);
        Mockito.verify(systemLibRoot, times(1)).listFiles();
        assertEquals(3, testManager.getLocations().size());
        assertEquals(systemPluginRoot.toPath(), testManager.getLocations().get(0));
        assertEquals(userPluginRoot.toPath(), testManager.getLocations().get(1));
        assertEquals(underneathTheBridge.toPath(), testManager.getLocations().get(2));
    }

    private Path createJar(String exportsDirective, String importDirective, Path base) throws Exception {
        Manifest manifest = new Manifest();
        manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
        manifest.getMainAttributes().put(new Attributes.Name("Export-Package"), exportsDirective + ";");
        if (importDirective != null) {
                manifest.getMainAttributes().put(new Attributes.Name("Import-Package"), importDirective + ";");
            }
        Path path = Paths.get(base.toFile().getAbsoluteFile() + "/" + exportsDirective + ".jar");
        manifest.getMainAttributes().put(new Attributes.Name("Bundle-SymbolicName"), exportsDirective);
        manifest.getMainAttributes().put(new Attributes.Name("Bundle-Version"), "1.0");
        FileOutputStream stream = new FileOutputStream(path.toFile());
        JarOutputStream target = new JarOutputStream(stream, manifest);
        target.close();
        return path;
    }

    void deleteDirectory(Path dir) throws IOException {
        Files.walkFileTree(dir, new FileVisitor<Path>() {

            @Override
            public FileVisitResult preVisitDirectory(Path dir,
                                                     BasicFileAttributes attrs) throws IOException {
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file,
                                             BasicFileAttributes attrs) throws IOException {
                Files.delete(file);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFileFailed(Path file, IOException exc)
                    throws IOException {
                exc.printStackTrace();
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc)
                    throws IOException {
                Files.delete(dir);
                return FileVisitResult.CONTINUE;
            }
        });
    }
}