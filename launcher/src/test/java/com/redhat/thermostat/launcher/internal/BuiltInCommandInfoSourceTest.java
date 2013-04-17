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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Properties;

import org.junit.Before;
import org.junit.Test;

import com.redhat.thermostat.launcher.internal.BuiltInCommandInfoSource;

public class BuiltInCommandInfoSourceTest {

    private Path tempThermostatHome;

    private File tempLibs;
    private File tempEtc;
    private File tempCommands;
    private File tempPropsFile;

    @Before
    public void setUp() throws IOException {

        tempThermostatHome = Files.createTempDirectory("test");
        tempThermostatHome.toFile().deleteOnExit();
        System.setProperty("THERMOSTAT_HOME", tempThermostatHome.toString());
        
        tempLibs = new File(tempThermostatHome.toFile(), "libs");

        tempEtc = new File(tempThermostatHome.toFile(), "etc");
        tempEtc.mkdirs();
        tempEtc.deleteOnExit();

        tempCommands = new File(tempEtc, "commands");
        tempCommands.mkdirs();
        tempCommands.deleteOnExit();

        Properties props = new Properties(); // Don't need to put anything in here.
        writeProperties(props);
    }

    private void writeProperties(Properties props) {
        tempPropsFile = new File(tempCommands, "foo.properties");
        try {
            props.store(new FileOutputStream(tempPropsFile), "Nothing here matters.  It's a comment.");
        } catch (IOException e) {
            // The test setup is broken; the test hasn't started yet.
            throw new RuntimeException("Exception was thrown while setting up for test.", e);
        }
        tempPropsFile.deleteOnExit();
    }

    @Test
    public void testGetCommandInfo() {
        BuiltInCommandInfoSource bundles =
                new BuiltInCommandInfoSource(tempCommands.toString(), tempLibs.toString());
        CommandInfo info = bundles.getCommandInfo("foo");
        assertNotNull(info);
        assertEquals("foo", info.getName());
    }

    @Test
    public void testGetCommandInfos() {
        BuiltInCommandInfoSource bundles =
                new BuiltInCommandInfoSource(tempCommands.toString(), tempLibs.toString());
        Collection<CommandInfo> infos = bundles.getCommandInfos();
        assertNotNull(infos);
        assertEquals(1, infos.size());
        CommandInfo info = infos.iterator().next();
        assertNotNull(info);
        assertEquals("foo", info.getName());
    }

}

