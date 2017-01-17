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

package com.redhat.thermostat.dev.ipc.test.launcher.internal;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.ServiceLoader;
import java.util.concurrent.CountDownLatch;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.BundleException;
import org.osgi.framework.BundleListener;
import org.osgi.framework.Constants;
import org.osgi.framework.launch.Framework;
import org.osgi.framework.launch.FrameworkFactory;

public class TestLauncher {
    
    private static final String SERVER_BUNDLE = "com.redhat.thermostat.dev.ipc.test.server";

    public static void main(String[] args) throws IOException, BundleException {
        Properties props = new Properties();
        InputStream in = TestLauncher.class.getResourceAsStream("bundles.properties");
        props.load(in);
        
        String bundleProp = props.getProperty("bundles");
        Objects.requireNonNull(bundleProp, "bundles property missing");
        String[] bundles = bundleProp.split(",");
        
        // Start a minimal OSGi framework to run the test IPC server
        ServiceLoader<FrameworkFactory> loader = ServiceLoader.load(FrameworkFactory.class,
                TestLauncher.class.getClassLoader());
        FrameworkFactory factory = loader.iterator().next();
        Map<String, String> config = new HashMap<>();
        config.put(Constants.FRAMEWORK_SYSTEMPACKAGES_EXTRA, "sun.misc");
        final Framework framework = factory.newFramework(config);
        framework.start();
        BundleContext context = framework.getBundleContext();
        
        // Install bundles
        final List<Bundle> installed = new ArrayList<>();
        for (String location : bundles) {
            location = location.trim();
            Bundle bundle = context.installBundle("file://" + location);
            installed.add(bundle);
            System.out.println("BundleLoader: installed bundle: \"" + 
                    location + "\" as id " + bundle.getBundleId());
        }
        
        final CountDownLatch started = new CountDownLatch(1);
        
        // Add listener to shutdown framework gracefully when the server bundle stops
        context.addBundleListener(new BundleListener() {
            
            @Override
            public void bundleChanged(BundleEvent event) {
                if (event.getType() == BundleEvent.STOPPED) {
                    Bundle target = event.getBundle();
                    if (SERVER_BUNDLE.equals(target.getSymbolicName())) {
                        printStoppingBundle(target);
                        try {
                            // Wait for framework to finish starting everything
                            started.await();
                            
                            // Stop all installed bundles, except the server bundle,
                            // which was already stopped to get to this point
                            List<Bundle> toRemove = new ArrayList<>(installed);
                            toRemove.remove(target);
                            shutdown(framework, toRemove);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            e.printStackTrace();
                        } catch (BundleException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        });
        
        // Start bundles
        for (Bundle bundle : installed) {
            if (isFragment(bundle)) {
                System.out.println("BundleLoader: not starting fragment bundle: \"" + bundle.getSymbolicName() + "\"");
            } else {
                bundle.start();
                System.out.println("BundleLoader: starting bundle: \"" + bundle.getSymbolicName() + "\"");
            }
        }
        
        // Okay to shutdown now
        started.countDown();
    }
    
    private static boolean isFragment(Bundle bundle) {
        Dictionary<String, String> headers = bundle.getHeaders();
        String fragHost = headers.get(Constants.FRAGMENT_HOST);
        return fragHost != null;
    }

    private static void shutdown(Framework framework, List<Bundle> bundles) throws BundleException {
        try {
            for (Bundle bundle : bundles) {
                if (isFragment(bundle)) {
                    System.out.println("BundleLoader: not stopping fragment bundle: \"" + bundle.getSymbolicName() + "\"");
                } else {
                    printStoppingBundle(bundle);
                    bundle.stop();
                }
            }
        } finally {
            // Shut down the framework
            framework.stop();
        }
    }

    private static void printStoppingBundle(Bundle bundle) {
        System.out.println("BundleLoader: stopping bundle: \"" + 
                bundle.getSymbolicName());
    }

}
