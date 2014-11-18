/*
 * Copyright 2012-2014 Red Hat, Inc.
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

import java.lang.instrument.Instrumentation;
import java.lang.instrument.UnmodifiableClassException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

public class Main {

    private Instrumentation instrumentation;

    public Main(Instrumentation instrumentation) {
        this.instrumentation = instrumentation;
    }

    public void run() {
        // System.out.println("AGENT: My classloader is " + this.getClass().getClassLoader());
        handleInitializationTasks();
    }

    public void handleInitializationTasks() {
        long start = System.nanoTime();

        // TODO defer any action till later to make sure the agent gets
        // installed. Later actions can fail without hanging things.

        addShutdownHookToPrintStatsOnEnd();
        ProfilerInstrumentor profiler = installProfiler(instrumentation);

        instrumentAlreadyLoadedClasses(instrumentation, profiler);
        long end = System.nanoTime();
        System.out.println("AGENT: done in : " + (end - start) + "ns");
    }

    private static ProfilerInstrumentor installProfiler(Instrumentation instrumentation) {
        ProfilerInstrumentor inst = new AsmBasedInstrumentor();
        instrumentation.addTransformer(inst, true);
        return inst;
    }

    private static void instrumentAlreadyLoadedClasses(Instrumentation instrumentation, ProfilerInstrumentor profiler) {
        long start = System.nanoTime();

        List<Class<?>> toTransform = new ArrayList<>();

        for (Class<?> klass : instrumentation.getAllLoadedClasses()) {
            boolean skipThisClass = false;
            if (!instrumentation.isModifiableClass(klass)) {
                skipThisClass = true;
            }
            if (!profiler.shouldInstrument(klass)) {
                skipThisClass = true;
            }

            if (skipThisClass) {
                continue;
            }

            toTransform.add(klass);
        }

        if (toTransform.size() > 0) {
            System.out.println("AGENT: Retransforming " + toTransform.size() + " classes");
            try {
                instrumentation.retransformClasses(toTransform.toArray(new Class<?>[toTransform.size()]));
            } catch (UnmodifiableClassException e) {
                throw new AssertionError("Tried to modify an unmodifiable class", e);
            } catch (InternalError e) {
                e.printStackTrace();
                System.err.println("Error retransforming already loaded classes.");
            }
        }
        long end = System.nanoTime();
        System.out.println("AGENT: Retansforming took: " + (end - start) + "ns");
    }

    private static void addShutdownHookToPrintStatsOnEnd() {
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                System.out.println("=====");
                System.out.println("Collected stats");
                System.out.format("%15s\t%s%n", "Total time (ns)", "Method");
                Map<String, AtomicLong> data = ProfileRecorder.getInstance().getData();
                for (Map.Entry<String, AtomicLong> entry : data.entrySet()) {
                    System.out.format("%15d\t%s%n", entry.getValue().get(), entry.getKey());
                }
                System.out.println("=====");
            }
        });
    }

}
