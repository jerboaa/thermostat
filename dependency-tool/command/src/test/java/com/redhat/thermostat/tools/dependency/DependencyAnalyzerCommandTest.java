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

package com.redhat.thermostat.tools.dependency;

import com.redhat.thermostat.common.cli.CommandContext;
import com.redhat.thermostat.common.cli.Console;
import com.redhat.thermostat.tools.dependency.internal.actions.ListDependenciesAction;
import com.redhat.thermostat.tools.dependency.internal.utils.TestHelper;
import com.redhat.thermostat.tools.dependency.internal.*;
import org.junit.Before;
import org.junit.Test;
import java.io.File;
import java.io.PrintStream;
import java.nio.file.Path;
import static com.redhat.thermostat.tools.dependency.DependencyAnalyzerCommand.findJarPath;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.*;

public class DependencyAnalyzerCommandTest {

    private DependencyAnalyzerCommand cmd;
    private Console console;
    private CommandContext ctx;
    private PathProcessorHandler handler;
    private String target;
    private String expected;
    private JarLocations locations;
    private File underneathTheBridge;
    private Path testJar;

    @Before
    public void setUp() throws Exception {
        cmd = new DependencyAnalyzerCommand();
        ctx = mock(CommandContext.class);
        console = mock(Console.class);
        underneathTheBridge = TestHelper.createTestDirectory();

        locations = new JarLocations();
        locations.getLocations().add(underneathTheBridge.toPath());
        handler = new PathProcessorHandler(locations);

        testJar = TestHelper.createJarWithExports("foobar", "foobar,com.real.package", null, underneathTheBridge.toPath());

        when(ctx.getConsole()).thenReturn(console);
        when(console.getOutput()).thenReturn(mock(PrintStream.class));
    }

    @Test
    public void testValidJarPath() {
        // Case 1 - Given a valid path to a jar, assert the path is unchanged
        target = testJar.toString();
        String result = cmd.findJarPath(handler, target, ctx);
        assertEquals(target, result);
    }

    @Test
    public void testValidPackageInput() {
        // Case 2 - Given a package that resolves to a jar, assert the jar path is returned
        ListDependenciesAction.execute(handler, testJar.toString(), ctx);
        expected = testJar.toString();
        String result = findJarPath(handler, "com.real.package", ctx);
        assertEquals(expected, result);
    }

    @Test
    public void testInvalidPackageInput() {
        // Case 3 - Given a package that doesn't resolve to a jar, assert it does the right thing
        target = "com.foo.bar";
        expected = "";
        String result = cmd.findJarPath(handler, target, ctx);
        assertEquals(expected, result);
    }

    @Test
    public void testInvalidJarPath() {
        // Case 4 - Given a jar path that doesn't exist, assert that it doesn't blow up
        target = "path/to/foo/bar.jar";
        String result = cmd.findJarPath(handler, target, ctx);
        assertEquals(target, result);
    }

    @Test
    public void testNullInput() {
        // Case 5 - Given a null input, assert it doesn't blow up
        target = null;
        expected = "";
        try {
            String result = cmd.findJarPath(handler, target, ctx);
            assertEquals(expected, result);
        } catch (NullPointerException e) {
            fail("Shouldn't throw a null pointer exception");
        }
    }
}