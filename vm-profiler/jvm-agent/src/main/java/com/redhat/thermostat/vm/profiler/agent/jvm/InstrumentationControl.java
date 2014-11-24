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

import java.io.BufferedWriter;
import java.io.IOException;
import java.lang.instrument.Instrumentation;
import java.lang.instrument.UnmodifiableClassException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

public class InstrumentationControl implements InstrumentationControlMXBean {

    private final Instrumentation instrumentation;
    private final ProfilerInstrumentor classInstrumentor;

    private boolean profiling = false;

    private String lastResults = null;

    public InstrumentationControl(Instrumentation instrumentation) {
        this.instrumentation = instrumentation;
        this.classInstrumentor = new AsmBasedInstrumentor();
    }

    @Override
    public void startProfiling() {
        System.out.println("AGENT: startProfiling()");
        if (profiling) {
            throw new IllegalStateException("Already started");
        }
        profiling = true;

        instrumentation.addTransformer(classInstrumentor, true);
        retransformAlreadyLoadedClasses(instrumentation, classInstrumentor);
    }

    @Override
    public void stopProfiling() {
        System.out.println("AGENT: stopProfiling() called");
        if (!profiling) {
            throw new IllegalStateException("Not profiling");
        }
        profiling = false;

        instrumentation.removeTransformer(classInstrumentor);
        retransformAlreadyLoadedClasses(instrumentation, classInstrumentor);

        lastResults = writeProfilingResultsToDisk();
    }
    private void retransformAlreadyLoadedClasses(Instrumentation instrumentation, ProfilerInstrumentor profiler) {
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

    private String writeProfilingResultsToDisk() {
        System.out.println("AGENT: Writing results to disk");
        try {
            Path output = createOutput();
            OpenOption[] options =
                    new OpenOption[] { StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING };

            try (BufferedWriter out = Files.newBufferedWriter(output, StandardCharsets.UTF_8, options)) {
                Map<String, AtomicLong> data = ProfileRecorder.getInstance().getData();
                for (Map.Entry<String, AtomicLong> entry : data.entrySet()) {
                    out.write(entry.getValue().get() + "\t" + entry.getKey() + "\n");
                }
                System.out.println("AGENT: profiling data written to " + output.toString());
                return output.toString();
            }
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    private Path createOutput() throws IOException {
        Set<PosixFilePermission> perm = PosixFilePermissions.fromString("rw-------");
        FileAttribute<Set<PosixFilePermission>> attributes = PosixFilePermissions.asFileAttribute(perm);
        return Files.createTempFile("thermostat", ".perfdata", attributes);
    }

    @Override
    public boolean isProfiling() {
        return profiling;
    }

    @Override
    public String getProfilingDataFile() {
        System.out.println("getProfilingDataFile() called. Returning : " + lastResults);
        return lastResults;
    }

}
