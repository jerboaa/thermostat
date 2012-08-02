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

package com.redhat.thermostat.bundles.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Properties;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class BundlePropertiesTest {

    private Path tempThermostatHome;

    private File tempEtc;
    private File tempPropsFile;

    private Path someJarName1;
    private Path someJarName2;

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Before
    public void setUp() throws IOException {

        tempThermostatHome = Files.createTempDirectory("test");
        tempThermostatHome.toFile().deleteOnExit();
        System.setProperty("THERMOSTAT_HOME", tempThermostatHome.toString());
        
        tempEtc = new File(tempThermostatHome.toFile(), "etc");
        tempEtc.mkdirs();
        tempEtc.deleteOnExit();
        
        File tempProps = new File(tempEtc, "bundles.properties");
        tempProps.createNewFile();
        tempProps.deleteOnExit();
        
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
    }

    private Properties getSingleBundleProperties() {
        Properties props = new Properties();
        props.setProperty("foo", someJarName1.getFileName().toString());
        return props;
    }

    private Properties getMultipleBundleProperties() {
        Properties props = new Properties();
        props.setProperty("foo", someJarName1.getFileName() + "," + someJarName2.getFileName());
        return props;
    }

    private Properties getPropertiesWithMultipleCommands() {
        Properties props = getSingleBundleProperties();
        props.setProperty("bar", someJarName2.getFileName().toString());
        return props;
    }

    private void writeProperties(Properties props) {
        tempPropsFile = new File(tempEtc, "bundles.properties");
        try {
            props.store(new FileOutputStream(tempPropsFile), "Nothing here matters.  It's a comment.");
        } catch (IOException e) {
            // The test setup is broken; the test hasn't started yet.
            throw new RuntimeException("Exception was thrown while setting up for test.", e);
        }
    }

    private void deletePropertiesFile() {
        if (tempPropsFile.exists()) {
            tempPropsFile.delete();
        }
    }

    private String resolvedJar(Path jar) {
        return "file:" + jar.toString();
    }

    @Test
    public void testSingleReferencedJarPresent() throws FileNotFoundException, IOException {
        Properties props = getSingleBundleProperties();
        writeProperties(props);
        BundleProperties bundles = new BundleProperties(tempThermostatHome.toString());
        List<String> jarNames = bundles.getDependencyResourceNamesFor("foo");
        assertEquals(1, jarNames.size());
        assertTrue(jarNames.contains(resolvedJar(someJarName1)));
        deletePropertiesFile();
    }

    @Test
    public void testMultipleReferencedJarPresent() throws FileNotFoundException, IOException {
        Properties props = getMultipleBundleProperties();
        writeProperties(props);
        BundleProperties bundles = new BundleProperties(tempThermostatHome.toString());
        List<String> jarNames = bundles.getDependencyResourceNamesFor("foo");
        assertEquals(2, jarNames.size());
        assertTrue(jarNames.contains(resolvedJar(someJarName1)));
        assertTrue(jarNames.contains(resolvedJar(someJarName2)));
        deletePropertiesFile();
    }

    @Test
    public void testSomeReferencedJarMissing() throws IOException {
        thrown.expect(FileNotFoundException.class);
        Properties props = getMultipleBundleProperties();
        props.setProperty("baz", "thisjar_noexist.jar");
        writeProperties(props);
        @SuppressWarnings("unused")
        BundleProperties bundles = new BundleProperties(tempThermostatHome.toString());
    }

    @Test
    public void testWithMultipleCommands() throws FileNotFoundException, IOException {
        Properties props = getPropertiesWithMultipleCommands();
        writeProperties(props);
        BundleProperties bundles = new BundleProperties(tempThermostatHome.toString());
        List<String> jarNames = bundles.getDependencyResourceNamesFor("foo");
        assertEquals(1, jarNames.size());
        assertTrue(jarNames.contains(resolvedJar(someJarName1)));
        jarNames = bundles.getDependencyResourceNamesFor("bar");
        assertEquals(1, jarNames.size());
        assertTrue(jarNames.contains(resolvedJar(someJarName2)));
        deletePropertiesFile();
    }
}
