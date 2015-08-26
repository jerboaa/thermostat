/*
 * Copyright 2012-2015 Red Hat, Inc.
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

package com.redhat.thermostat.setup.command.internal.model;

import static org.junit.Assert.assertTrue;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Properties;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class PropertiesWriterTest {
    
    private static final String ROLES_PROPERTIES = "thermostat-roles.properties";
    private Path testRoot;
    private Path rolesPropertiesFile;
    
    @Before
    public void setup() throws IOException {
        testRoot = TestRootHelper.createTestRootDirectory(getClass().getName());
        Path thermostatSysHome = testRoot.resolve("system");
        Files.createDirectory(thermostatSysHome);
        Path sysConfigDir = thermostatSysHome.resolve("etc");
        Files.createDirectories(sysConfigDir);
        rolesPropertiesFile = sysConfigDir.resolve(ROLES_PROPERTIES);
    }
    
    @After
    public void tearDown() throws IOException {
        TestRootHelper.recursivelyRemoveTestRootDirectory(testRoot);
    }

    @Test
    public void testPropertiesWriter() throws IOException {
        String key = "thermostat-agent";
        String[] roles  = new String[] {
                UserRoles.LOGIN,
                UserRoles.PREPARE_STATEMENT,
                UserRoles.PURGE,
                UserRoles.REGISTER_CATEGORY,
        };
        StringBuilder rolesBuilder = new StringBuilder();
        for (int i = 0; i < roles.length - 1; i++) {
            rolesBuilder.append(roles[i] + ", " + System.getProperty("line.separator"));
        }
        rolesBuilder.append(roles[roles.length - 1]);
        String value = rolesBuilder.toString();

        Properties propsToStore = new Properties();
        propsToStore.setProperty(key, value);
        FileOutputStream roleStream = new FileOutputStream(rolesPropertiesFile.toFile());
        propsToStore.store(new PropertiesWriter(roleStream), null);

        Properties propsToLoad = new Properties();
        propsToLoad.load(new FileInputStream(rolesPropertiesFile.toFile()));
        String[] loadedRoles = propsToLoad.getProperty(key).split(",\\s+");

        assertTrue(Arrays.asList(roles).containsAll(Arrays.asList(loadedRoles)));
    }
}
