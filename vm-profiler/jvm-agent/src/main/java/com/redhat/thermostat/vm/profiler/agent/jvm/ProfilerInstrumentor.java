/*
 * Copyright 2012-2016 Red Hat, Inc.
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

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public abstract class ProfilerInstrumentor implements ClassFileTransformer {

    private static List<Pattern> ignorePackageRegexps = new ArrayList<Pattern>();

    static {
        // jdk packages
        ignorePackageRegexps.add(Pattern.compile("java\\..*"));
        ignorePackageRegexps.add(Pattern.compile("javax\\..*"));
        ignorePackageRegexps.add(Pattern.compile("com\\.sun\\..*"));
        ignorePackageRegexps.add(Pattern.compile("sun\\..*"));
        ignorePackageRegexps.add(Pattern.compile("jdk\\..*"));

        // this class
        ignorePackageRegexps.add(Pattern.compile("com\\.redhat\\.thermostat\\.vm\\.profiler\\.agent\\.jvm\\..*"));

        // our dependencies
        ignorePackageRegexps.add(Pattern.compile("org\\.objectweb\\.asm\\..*"));
    }

    @Override
    public byte[] transform(ClassLoader loader, String className,
            Class<?> classBeingRedefined,
            ProtectionDomain protectionDomain, byte[] classfileBuffer)
            throws IllegalClassFormatException {

        className = className.replace('/', '.');
        if (!shouldInstrument(className)) {
            return null;
        }

        // System.out.println("transforming '" + className + "'");

        return transform(loader, className, classfileBuffer);
    }

    public boolean shouldInstrument(Class<?> clazz) {
        if (clazz.isArray()) {
            return false;
        }

        // TODO what to do with anonymous classes?

        return shouldInstrument(clazz.getName());
    }

    public boolean shouldInstrument(String className) {
        if (className == null) {
            return true;
        }
        for (Pattern packagePattern : ignorePackageRegexps) {
            if (packagePattern.matcher(className).matches()) {
                return false;
            }
        }

        if (className.startsWith(ProfilerInstrumentor.class.getName())) {
            return false;
        }

        return true;
    }

    public abstract byte[] transform(ClassLoader cl, String className, byte[] classBytes);

}

