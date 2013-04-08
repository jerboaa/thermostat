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

package com.redhat.thermostat.main.impl;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.ServiceLoader;

import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.framework.launch.Framework;
import org.osgi.framework.launch.FrameworkFactory;
import org.osgi.util.tracker.ServiceTracker;

import com.redhat.thermostat.common.Launcher;
import com.redhat.thermostat.common.config.Configuration;
import com.redhat.thermostat.launcher.BundleManager;

public class FrameworkProvider {

    private static final String DEBUG_PREFIX = "FrameworkProvider: ";
    private static final String PROPS_FILE = "/com/redhat/thermostat/main/impl/bootstrapbundles.properties";
    private static final String BUNDLELIST = "bundles";

    private Configuration configuration;
    private boolean printOSGiInfo;
    // The framework cache location; Must not be shared between apps!
    private Path osgiCacheStorage;

    public FrameworkProvider(Configuration config) {
        this.configuration = config;
        printOSGiInfo = config.getPrintOSGiInfo();
    }

    // This is our ticket into OSGi land. Unfortunately, we to use a bit of reflection here.
    // The launcher and bundleloader are instantiated from within their bundles, ie loaded
    // by the bundle classloader.
    public void start(String[] args) {
        try {
            Framework framework = makeFramework();
            prepareFramework(framework);
            loadBootstrapBundles(framework);
            setLoaderVerbosity(framework);
            runLauncher(framework, args);
        } catch (InterruptedException | BundleException | IOException e) {
            throw new RuntimeException("Could not start framework.", e);
        }
    }

    private String getOSGiPublicPackages() throws FileNotFoundException, IOException {
        File osgiBundleDefinitions = new File(configuration.getConfigurationDir(), "osgi-export.properties");

        Properties bundles = new Properties();
        bundles.load(new FileInputStream(osgiBundleDefinitions));

        StringBuilder publicPackages = new StringBuilder();
        /*
         * Packages the launcher requires
         */
        //publicPackages.append("com.redhat.thermostat.common.services");
        boolean firstPackage = true;
        for (Object bundle : bundles.keySet()) {
            if (!firstPackage) {
                publicPackages.append(",\n");
            }
            firstPackage = false;
            publicPackages.append(bundle);
            String bundleVersion = (String) bundles.get(bundle);
            if (!bundleVersion.isEmpty()) {
                publicPackages.append("; version=").append(bundleVersion);
            }
        }

        return publicPackages.toString();
    }

    private void prepareFramework(final Framework framework) throws BundleException, IOException {
        framework.init();
        framework.start();
        if (printOSGiInfo) {
            System.out.println(DEBUG_PREFIX + "OSGi framework has started.");
        }

        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                try {
                    framework.stop();
                    framework.waitForStop(0);
                    if (printOSGiInfo) {
                        System.out.println(DEBUG_PREFIX + "OSGi framework has shut down.");
                    }
                    recursivelyDeleteDirectory(osgiCacheStorage.toFile());
                    if (printOSGiInfo) {
                        System.out.println(DEBUG_PREFIX + "Removed OSGi cache directory: "
                                + osgiCacheStorage.toFile().getAbsolutePath());
                    }
                } catch (Exception e) {
                    throw new RuntimeException("Error shutting down framework.", e);
                }
            }
        });

    }

    private void recursivelyDeleteDirectory(File directory) {
        for (File file: directory.listFiles()) {
            if (file.isDirectory()) {
                recursivelyDeleteDirectory(file);
            }
            file.delete();
        }
        directory.delete();
    }

    private Framework makeFramework() throws FileNotFoundException, IOException {
        File osgiCacheDir = new File(configuration.getThermostatHome(), "osgi-cache");

        // Create temporary directory which will be used as cache for OSGi bundles. See
        // http://www.osgi.org/javadoc/r4v43/core/org/osgi/framework/Constants.html#FRAMEWORK_STORAGE
        // for details about what this location is used for.
        // 
        // Agent, swing gui client application must not use the same location as this tricks the framework
        // into thinking that some bundles are installed and loaded when that might not actually be the case.
        // Note that we do not specify the org.osgi.framework.storage.clean property and the default is to NOT
        // clean the cache, which is when we might run into trouble if this location is shared. This
        // temp directory will be deleted on VM shutdown.
        // 
        // This fixes Thermostat BZ 1110.
        osgiCacheStorage = Files.createTempDirectory(osgiCacheDir.toPath(), null);
        if (printOSGiInfo) {
            System.out.println(DEBUG_PREFIX + "OSGi cache location: "
                    + osgiCacheStorage.toFile().getAbsolutePath());
        }
        
        ServiceLoader<FrameworkFactory> loader = ServiceLoader.load(FrameworkFactory.class,
                getClass().getClassLoader());
        Map<String, String> bundleConfigurations = new HashMap<String, String>();
        String extraPackages = getOSGiPublicPackages();
        bundleConfigurations.put(Constants.FRAMEWORK_SYSTEMPACKAGES_EXTRA, extraPackages);
        bundleConfigurations.put(Constants.FRAMEWORK_STORAGE, osgiCacheStorage.toFile().getAbsolutePath());
        Iterator<FrameworkFactory> factories = loader.iterator();
        if (factories.hasNext()) {
            // we just want the first found
            return factories.next().newFramework(bundleConfigurations);
        } else {
            throw new InternalError("ServiceLoader cannot find a FrameworkFactory!");
        }   
    }

    private void loadBootstrapBundles(Framework framework) throws BundleException, InterruptedException {
        Properties bootstrapProps = new Properties();
        // the properties file should be in the same package as this class
        InputStream res = getClass().getResourceAsStream(PROPS_FILE);
        if (res != null) {
            try {
                bootstrapProps.load(res);
            } catch (IOException e) {
                throw new RuntimeException("Could not load bootstrap bundle properties.", e);
            }
        }
        String[] bundles = bootstrapProps.getProperty(BUNDLELIST).split(",");
        List<String> locations = new ArrayList<>();
        for (String bundle : bundles) {
            String trimmed = bundle.trim();
            if (trimmed != null && trimmed.length() > 0) {
                String location = actualLocation(trimmed);
                locations.add(location);
            }
        }
        BundleManager.preLoadBundles(framework, locations, printOSGiInfo);
    }

    private void setLoaderVerbosity(Framework framework) throws InterruptedException {
        Object loader = getService(framework, BundleManager.class.getName());
        callVoidReflectedMethod(loader, "setPrintOSGiInfo", printOSGiInfo);
    }

    private void runLauncher(Framework framework, String[] args) throws InterruptedException {
        Object launcher = getService(framework, Launcher.class.getName());
        callVoidReflectedMethod(launcher, "run", args, false);
    }

    private Object getService(Framework framework, String name) throws InterruptedException {
        Object service = null;
        @SuppressWarnings({ "unchecked", "rawtypes" })
        ServiceTracker tracker = new ServiceTracker(framework.getBundleContext(), name, null);
        tracker.open();
        service = tracker.waitForService(0);
        tracker.close();
        return service;
    }

    /**
     * Call {@code object}.{@code name} with {@code args} as the arguments. The
     * return value is ignored. The types of the method arguments must exactly
     * match the types of the supplied arguments, but primitives are used unboxed.
     */
    private void callVoidReflectedMethod(Object object, String name, Object... args) {
        Class<?> clazz = object.getClass();
        Class<?>[] classes = new Class<?>[args.length];
        for (int i = 0; i < args.length; i++) {
            classes[i] = preferPrimitiveClass(args[i].getClass());
        }

        try {
            Method m = clazz.getMethod(name, classes);
            m.invoke(object, args);
        } catch (IllegalAccessException | NoSuchMethodException |
                IllegalArgumentException | InvocationTargetException e) {
            // It's pretty evil to just swallow these exceptions.  But, these can only
            // really come up in Really Bad Code Errors, which testing will catch early.
            // Right?  Right.  Of course it will.
            e.printStackTrace();
        }
    }

    private static <T> Class<T> preferPrimitiveClass(Class<T> boxedPrimitive) {
        HashMap<Class<?>, Class<?>> map = new HashMap<>();
        map.put(Byte.class, byte.class);
        map.put(Short.class, short.class);
        map.put(Integer.class, int.class);
        map.put(Long.class, long.class);
        map.put(Float.class, float.class);
        map.put(Double.class, double.class);
        map.put(Boolean.class, boolean.class);
        map.put(Character.class, char.class);

        if (map.containsKey(boxedPrimitive)) {
            return (Class<T>) map.get(boxedPrimitive);
        } else  {
            return boxedPrimitive;
        }
    }

    private String actualLocation(String resourceName) {
        return new File(configuration.getLibRoot(), resourceName).toURI().toString();
    }
}

