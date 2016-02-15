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

package com.redhat.thermostat.tools.dependency.internal.actions;

import com.redhat.thermostat.collections.graph.Node;
import com.redhat.thermostat.common.cli.CommandContext;
import com.redhat.thermostat.tools.dependency.internal.BundleProperties;
import com.redhat.thermostat.tools.dependency.internal.JarLocations;
import com.redhat.thermostat.tools.dependency.internal.PathProcessorHandler;
import com.redhat.thermostat.tools.dependency.internal.Utils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.Attributes;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

/**
 */
public class ListDependenciesActionTest {

    private File underneathTheBrdige;

    private JarLocations paths;

    private CommandContext ctx;
    private TestUtils utils;

    private Path a;
    private Path b;
    private Path c;
    private Path d;
    private Path e;

    private static class TestUtils extends Utils {
        List<Node> results = new ArrayList<>();

        public void printHeader(CommandContext ctx, String name) {}

        public void print(CommandContext ctx, Object x) {
            if (x instanceof Node) {
                results.add((Node) x);
            }
        }

        public void cannotAcessPathError(CommandContext ctx, Path path) {}

        public void cannotFindAttributeError(CommandContext ctx, BundleProperties property, Path path) {}
    }

    @Before
    public void setUp() throws Exception {
        ctx = Mockito.mock(CommandContext.class);
        utils = new TestUtils();
        Utils.initSingletonForTest(utils);

        underneathTheBrdige = Files.createTempDirectory("underneathTheBridge").toFile();
        underneathTheBrdige.deleteOnExit();

        paths = new JarLocations();
        paths.getLocations().add(underneathTheBrdige.toPath());

        a = createJar("a", null);
        b = createJar("b", "a");
        c = createJar("c", "b");
        d = createJar("d", "b,c");
        e = createJar("e", "d");
    }

    @Test
    public void testInboundDependenciesOnLibraryWithoutImports() {
        PathProcessorHandler handler = new PathProcessorHandler(paths);

        ListDependenciesAction.execute(handler, a.toString(), ctx);
        Assert.assertTrue(utils.results.isEmpty());
    }

    @Test
    public void testOutboundDependenciesOnLibraryWithMultipleDependencies() {
        PathProcessorHandler handler = new PathProcessorHandler(paths);

        ListDependenciesAction.execute(handler, d.toString(), ctx);
        Assert.assertFalse(utils.results.isEmpty());
        Assert.assertEquals(3, utils.results.size());

        Assert.assertEquals(a.toString(), utils.results.get(0).getName());
        Assert.assertEquals(b.toString(), utils.results.get(1).getName());
        Assert.assertEquals(c.toString(), utils.results.get(2).getName());
    }
    
    @Test
    public void testOutboundDependenciesOnRootLibrary() {
        PathProcessorHandler handler = new PathProcessorHandler(paths);

        ListDependenciesAction.execute(handler, a.toString(), ctx, true);
        Assert.assertFalse(utils.results.isEmpty());
        Assert.assertEquals(4, utils.results.size());

        Assert.assertEquals(b.toString(), utils.results.get(0).getName());
        Assert.assertEquals(c.toString(), utils.results.get(1).getName());
        Assert.assertEquals(d.toString(), utils.results.get(2).getName());
        Assert.assertEquals(e.toString(), utils.results.get(3).getName());

    }

    private Path createJar(String id, String importDirective) throws Exception {
        Manifest manifest = new Manifest();
        manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
        manifest.getMainAttributes().put(new Attributes.Name(BundleProperties.EXPORT.id()), id + ";");
        if (importDirective != null) {
            manifest.getMainAttributes().put(new Attributes.Name(BundleProperties.IMPORT.id()), importDirective + ";");
        }

        Path path = Paths.get(underneathTheBrdige.getAbsoluteFile() + "/" + id + ".jar");
        FileOutputStream stream = new FileOutputStream(path.toFile());
        JarOutputStream target = new JarOutputStream(stream, manifest);
        target.close();

        return path;
    }
}