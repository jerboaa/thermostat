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

package com.redhat.thermostat.launcher.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Properties;

import org.junit.Before;
import org.junit.Test;

public class CommandInfoTest {

    private Path tempThermostatHome, someJarName1, someJarName2, missingJarName;

    @Before
    public void setUp() throws IOException {
        tempThermostatHome = Files.createTempDirectory("test");
        tempThermostatHome.toFile().deleteOnExit();
        System.setProperty("THERMOSTAT_HOME", tempThermostatHome.toString());

        File tempLibs = new File(tempThermostatHome.toFile(), "libs");
        tempLibs.mkdirs();
        tempLibs.deleteOnExit();

        File someJar1 = new File(tempLibs, "thermostat-osgi-fluff1.jar");
        someJar1.createNewFile();
        someJar1.deleteOnExit();
        someJarName1 = someJar1.toPath();
        
        File someJar2 = new File(tempLibs, "thermostat-osgi-fluff2.jar");
        someJar2.createNewFile();
        someJar2.deleteOnExit();
        someJarName2 = someJar2.toPath();

        File missingJar = new File(tempLibs, "thisjar_noexist.jar");
        missingJarName = missingJar.toPath();
    }

    private String resolvedJar(Path jar) {
        return "file:" + jar.toString();
    }

    @Test
    public void verifyGetName() {
        Properties props = new Properties();
        String name = "name";
        CommandInfo info = new CommandInfo(name, props, "");

        String commandName = info.getName();
        assertEquals(name, commandName);
    }

    @Test
    public void verifySingleResource() {
        Properties props = new Properties();
        props.setProperty("bundles", someJarName1.getFileName().toString());
        String name = "name";
        CommandInfo info = new CommandInfo(name, props, tempThermostatHome.toString());

        List<String> resources = info.getDependencyResourceNames();
        assertEquals(1, resources.size());
        assertTrue(resources.contains(resolvedJar(someJarName1)));
    }

    @Test
    public void verifyMultipleResources() {
        Properties props = new Properties();
        props.setProperty("bundles", someJarName1.getFileName() + "," + someJarName2.getFileName());
        String name = "name";
        CommandInfo info = new CommandInfo(name, props, tempThermostatHome.toString());

        List<String> resources = info.getDependencyResourceNames();
        assertEquals(2, resources.size());
        assertTrue(resources.contains(resolvedJar(someJarName1)));
        assertTrue(resources.contains(resolvedJar(someJarName2)));
    }

    @Test
    public void verifyMissingResource() {
        Properties props = new Properties();
        props.setProperty("bundles", missingJarName.getFileName().toString());
        String name = "name";
        CommandInfo info = new CommandInfo(name, props, tempThermostatHome.toString());

        List<String> resources = info.getDependencyResourceNames();
        assertEquals(0, resources.size());
        assertFalse(resources.contains(resolvedJar(missingJarName)));
    }
}
