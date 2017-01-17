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

package com.redhat.thermostat.tools.dependency.internal;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

public class JarListerTest {

    private JarLocations locations;

    private File underneathTheBridge;
    private Path something;
    private Path in;
    private Path the;
    private Path way;

    @Before
    public void setUp() throws Exception {

        locations = new JarLocations();

        underneathTheBridge = Files.createTempDirectory("underneathTheBridge").toFile();
        underneathTheBridge.deleteOnExit();

        something = Files.createTempFile(underneathTheBridge.toPath(), "something", ".jar");
        something.toFile().deleteOnExit();

        in = Files.createTempFile(underneathTheBridge.toPath(), "in", ".jar");
        in.toFile().deleteOnExit();

        the = Files.createTempFile(underneathTheBridge.toPath(), "the", ".jar");
        the.toFile().deleteOnExit();

        way = Files.createTempFile(underneathTheBridge.toPath(), "way", ".jar");
        way.toFile().deleteOnExit();

        locations.getLocations().add(underneathTheBridge.toPath());
    }

    @Test
    public void testProcess() throws Exception {

        JarLister jarLister = new JarLister();

        PathProcessorHandler handler = new PathProcessorHandler(locations);
        handler.process(jarLister);

        Map<String, List<Path>> paths = jarLister.getPaths();
        Assert.assertFalse(paths.isEmpty());

        List<Path> files = paths.get(underneathTheBridge.getName());
        Assert.assertEquals(4, files.size());

        Assert.assertTrue(files.contains(something));
        Assert.assertTrue(files.contains(in));
        Assert.assertTrue(files.contains(the));
        Assert.assertTrue(files.contains(way));
    }
}
