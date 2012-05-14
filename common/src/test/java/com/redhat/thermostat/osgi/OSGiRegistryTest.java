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

package com.redhat.thermostat.osgi;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Properties;

import junit.framework.Assert;

import org.junit.Before;
import org.junit.Test;

import com.redhat.thermostat.common.config.InvalidConfigurationException;

public class OSGiRegistryTest {

    private Path tempDir;
    private File someJars1;
    private File someJars2;
    
    @Before
    public void setUp() throws IOException {

        tempDir = Files.createTempDirectory("test");
        tempDir.toFile().deleteOnExit();
        System.setProperty("THERMOSTAT_HOME", tempDir.toString());
        
        File tempEtc = new File(tempDir.toFile(), "etc");
        tempEtc.mkdirs();
        tempEtc.deleteOnExit();
        
        File tempProps = new File(tempEtc, "osgi-export.properties");
        tempProps.createNewFile();
        tempProps.deleteOnExit();
        
        File tempLibs = new File(tempDir.toFile(), "libs");
        tempLibs.mkdirs();
        tempLibs.deleteOnExit();
        
        someJars1 = new File(tempLibs, "thermostat-osgi-fluff1.jar");
        someJars1.createNewFile();
        someJars1.deleteOnExit();
        
        someJars2 = new File(tempLibs, "thermostat-osgi-fluff2.jar");
        someJars2.createNewFile();
        someJars2.deleteOnExit();
        
        File tmpConfigs = new File(tempEtc, "osgi-export.properties");     
        tmpConfigs.deleteOnExit();
        
        Properties props = new Properties();            

        props.setProperty("this.is.a.fluff.package", "0.0.0");
        props.setProperty("this.is.even.more.a.fluff.package", "0.0.1");

        props.store(new FileOutputStream(tmpConfigs), "thermostat osgi public api test properties");
    }
    
    @Test
    public void testBundles() throws InvalidConfigurationException, IOException {
        
        List<String> bundles = OSGiRegistry.getSystemBundles();
        Assert.assertEquals(2, bundles.size());
        Assert.assertTrue(bundles.contains("file:" + someJars1.getAbsolutePath()));
        Assert.assertTrue(bundles.contains("file:" + someJars2.getAbsolutePath()));
        
        String publicApi = OSGiRegistry.getOSGiPublicPackages();
        Assert.assertTrue(publicApi.contains("this.is.a.fluff.package; version=0.0.0"));
        Assert.assertTrue(publicApi.contains("this.is.even.more.a.fluff.package; version=0.0.1"));
    }
}
