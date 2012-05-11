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

package com.redhat.thermostat.client.osgi;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.launch.Framework;
import org.osgi.framework.launch.FrameworkFactory;

import com.redhat.thermostat.common.config.ConfigUtils;
import com.redhat.thermostat.common.config.InvalidConfigurationException;
import com.redhat.thermostat.osgi.OSGiRegistry;

public class Thermostat {

    private File thermostatBundleHome;    
    private Thermostat() { /* nothing to do */ }
    
    private void setUp() throws InvalidConfigurationException {
        String thermostatHome = ConfigUtils.getThermostatHome();
        thermostatBundleHome = new File(thermostatHome, "osgi");
    }
    
    private void installAndStartBundles(Framework framework,
                                        List<String>bundleLocations)
        throws Exception
    {
        BundleContext bundleContext = framework.getBundleContext();
        BundleActivator hostActivator = ThermostatActivator.newInstance();
        hostActivator.start(bundleContext);
        for (String location : bundleLocations) {
            Bundle bundle = bundleContext.installBundle(location);
            bundle.start();
        }
    }
    
    private void start(String[] args) throws Exception {
        setUp();
        
        ServiceLoader<FrameworkFactory> loader =
                ServiceLoader.load(FrameworkFactory.class);
        
        Map<String, String> bundleConfigurations = new HashMap<String, String>();
        
        String publicPackages = OSGiRegistry.getOSGiPublicPackages();
        publicPackages = publicPackages + ", com.redhat.thermostat.client.osgi.service, com.redhat.thermostat.common.dao";
        bundleConfigurations.put(Constants.FRAMEWORK_SYSTEMPACKAGES_EXTRA, publicPackages);
        
        bundleConfigurations.put(Constants.FRAMEWORK_STORAGE,
                                 thermostatBundleHome.getAbsolutePath());
        
        Iterator<FrameworkFactory> factories = loader.iterator();
        if (factories.hasNext()) {
            
            // we just want the first found
            Framework framework = factories.next().newFramework(bundleConfigurations);
            framework.init();
            framework.start();
            
            List<String> bundles = OSGiRegistry.getSystemBundles();
            installAndStartBundles(framework, bundles);
            
        } else {
            throw new InternalError("Can't find factories for ServiceLoader!");
        }
    }
    
    /**
     * @param args
     * @throws Exception 
     */
    public static void main(String[] args) throws Exception {
        new Thermostat().start(args);
    }    
}
