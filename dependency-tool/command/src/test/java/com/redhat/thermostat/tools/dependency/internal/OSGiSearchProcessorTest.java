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

package com.redhat.thermostat.tools.dependency.internal;

import com.redhat.thermostat.common.cli.CommandContext;
import com.redhat.thermostat.tools.dependency.internal.utils.TestHelper;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.nio.file.Path;

/**
 */
public class OSGiSearchProcessorTest {
    private File underneathTheBrdige;

    private JarLocations paths;

    private CommandContext ctx;

    private Path a;
    private Path b;
    private Path c;
    private Path d;
    private Path e;

    @Before
    public void setUp() throws Exception {
        underneathTheBrdige = TestHelper.createTestDirectory();

        paths = new JarLocations();
        paths.getLocations().add(underneathTheBrdige.toPath());

        a = TestHelper.createJar("a", null,  underneathTheBrdige.toPath());
        b = TestHelper.createJar("b", "a",   underneathTheBrdige.toPath());
        c = TestHelper.createJar("c", "b",   underneathTheBrdige.toPath());
        d = TestHelper.createJar("d", "b,c", underneathTheBrdige.toPath());
        e = TestHelper.createJar("e", "d",   underneathTheBrdige.toPath());
    }

    @Test
    public void testSearchExportedPackage() {
        PathProcessorHandler handler = new PathProcessorHandler(paths);
        OSGiSearchProcessor search = new OSGiSearchProcessor("a");
        handler.process(search);

        OSGiSearchProcessor.BundleInfo result = search.getBundleInfo();
        Assert.assertEquals(a, result.library);

        search = new OSGiSearchProcessor("c");
        handler.process(search);
        result = search.getBundleInfo();
        Assert.assertEquals(c, result.library);

        search = new OSGiSearchProcessor("f");
        handler.process(search);
        result = search.getBundleInfo();
        Assert.assertNull(result);
    }
}
