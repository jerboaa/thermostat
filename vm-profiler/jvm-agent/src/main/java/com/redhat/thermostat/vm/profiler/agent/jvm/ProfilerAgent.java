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

package com.redhat.thermostat.vm.profiler.agent.jvm;

import java.io.IOException;
import java.lang.instrument.Instrumentation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.InvocationTargetException;
import java.util.jar.JarFile;

/**
 * This is an {@link Instrumentation} agent that sets up the classpath with jars
 * supplied as agent parameters and then invokes {@link Main}.
 * <p>
 * Agent jars (including this one) are loaded into the system classpath by
 * default. The class that records profiling events has to have a single
 * instance JVM-wide, otherwise we will miss events being recorded or won't be
 * able to query them from a different instance of the "singleton". To ensure a
 * single instance across all classloaders, we add the jars to bootstrap
 * classpath and then use that instance explicitly (via reflection).
 */
public class ProfilerAgent {

    public static void premain(String agentArgs, Instrumentation instrumentation) {
        initializeAgent(agentArgs, instrumentation);
    }

    public static void agentmain(String agentArgs, Instrumentation instrumentation) {
        initializeAgent(agentArgs, instrumentation);
    }

    private static void initializeAgent(String args, Instrumentation instrumentation) {

        System.out.println("AGENT: loaded");

        String jars = args;
        addJarsToClassPath(jars, instrumentation);

        invokeMain(instrumentation);
    }

    private static void addJarsToClassPath(String jars, Instrumentation instrumentation) throws AssertionError {
        // System.out.println("Classpath: " + System.getProperty("java.class.path"));
        boolean addToBoot = true;
        String[] jarPaths = jars.split(":");
        for (String jarPath : jarPaths) {
            JarFile jarFile = null;
            try {
                jarFile = new JarFile(jarPath);
                // This needs to be bootclassloader if it is to be loaded and visible everywhere
                // without hitting recursive classloading.
                if (addToBoot) {
                    instrumentation.appendToBootstrapClassLoaderSearch(jarFile);
                } else {
                    instrumentation.appendToSystemClassLoaderSearch(jarFile);
                }
                System.out.println("AGENT: Added '" + jarPath + "' to " + (addToBoot ? "bootstrap" : "system") + " classpath");
            } catch (IOException e) {
                throw new AssertionError(jarFile + " not found!");
            }
        }
    }

    private static void invokeMain(Instrumentation instrumentation) {
        try {
            // do this via reflection so the version in system boot classpath is used!
            Class<?> klass = ClassLoader.getSystemClassLoader().loadClass("com.redhat.thermostat.vm.profiler.agent.jvm.Main");
            Constructor<?> constructor = klass.getConstructor(Instrumentation.class);
            Object main = constructor.newInstance(instrumentation);
            Method runMethod = klass.getMethod("run");
            runMethod.invoke(main);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            System.err.println("Unable to initialize agent");
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
            System.err.println("Unable to initialize agent");
        } catch (IllegalAccessException e) {
            e.printStackTrace();
            System.err.println("Unable to initialize agent");
        } catch (InstantiationException e) {
            e.printStackTrace();
            System.err.println("Unable to initialize agent");
        } catch (InvocationTargetException e) {
            e.printStackTrace();
            System.err.println("Unable to initialize agent");
        } catch (SecurityException e) {
            e.printStackTrace();
            System.err.println("Unable to initialize agent");
        }
    }

}
