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

package com.redhat.thermostat.vm.profiler.client.core;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.redhat.thermostat.common.utils.LoggingUtils;
import com.redhat.thermostat.common.utils.MethodDescriptorConverter;
import com.redhat.thermostat.common.utils.MethodDescriptorConverter.MethodDeclaration;
import com.redhat.thermostat.vm.profiler.client.core.ProfilingResult.MethodInfo;

/**
 * Results are expected to be in this format, one result per line:
 *
 * <pre>
 * [methodTime] [method]
 * </pre>
 *
 * Where {@code methodTime} is the total time, in nanoseconds, that the method
 * took. {@code method} is the method name followed by the method
 * descriptor, as defined by the <a href=
 * "http://docs.oracle.com/javase/specs/jvms/se7/html/jvms-4.html#jvms-4.3.3">
 * Java Language</a>. An example is {@code foo(I)I} to indicate
 * {@code int foo(int)}.
 */
public class ProfilingResultParser {

    private static final Logger logger = LoggingUtils.getLogger(ProfilingResultParser.class);

    public ProfilingResult parse(InputStream in) {
        Map<String, Long> methodAndTimes = readData(in);
        return convertToResult(methodAndTimes);
    }

    private Map<String, Long> readData(InputStream in) {
        Map<String, Long> result = new HashMap<String, Long>();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split("\\s+");
                long time = TimeUnit.NANOSECONDS.toMillis(Long.valueOf(parts[0]));
                String name = parts[1];
                result.put(name, time);
            }
        } catch (IOException e) {
            logger.log(Level.WARNING, "Unable to parse profiling data: ", e);
        }

        return result;
    }

    private ProfilingResult convertToResult(Map<String, Long> results) {
        ArrayList<MethodInfo> info = new ArrayList<>();
        long totalTime = 0;
        for (Entry<String, Long> entry : results.entrySet()) {
            totalTime += entry.getValue().longValue();
        }

        for (Entry<String, Long> entry : results.entrySet()) {
            String methodNameAndDescriptor = entry.getKey();
            MethodDeclaration declaration = breakDownMethod(methodNameAndDescriptor);
            MethodInfo method = new MethodInfo(declaration, entry.getValue(), (entry.getValue() * 1.0 / totalTime) * 100);
            info.add(method);
        }

        Collections.sort(info, new Comparator<MethodInfo>() {
            @Override
            public int compare(MethodInfo o1, MethodInfo o2) {
                return Long.compare(o1.totalTimeInMillis, o2.totalTimeInMillis);
            }
        });

        return new ProfilingResult(info);
    }

    private MethodDeclaration breakDownMethod(String name) {
        int startDescriptor = name.indexOf('(');
        if (startDescriptor == -1) {
            // handle malformed method descriptor by returning it as it is
            return new MethodDeclaration(name, new ArrayList<String>(), "");
        }
        String methodClassName = name.substring(0, startDescriptor);

        return MethodDescriptorConverter.toJavaDeclaration(methodClassName, name.substring(startDescriptor));
    }

}
