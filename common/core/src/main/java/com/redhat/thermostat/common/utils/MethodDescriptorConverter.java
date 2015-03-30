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

package com.redhat.thermostat.common.utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Convert JVM-internal descriptors of methods to standard Java-style method
 * descriptions.
 *
 * @see DescriptorConverter
 */
public class MethodDescriptorConverter {

    public static final String UNKNOWN_METHOD_NAME = "???";

    private static final Map<Character, String> lookupTable = new HashMap<>();

    static {
        lookupTable.put('Z', "boolean");
        lookupTable.put('B', "byte");
        lookupTable.put('C', "char");
        lookupTable.put('S', "short");
        lookupTable.put('I', "int");
        lookupTable.put('J', "long");
        lookupTable.put('F', "float");
        lookupTable.put('D', "double");
        lookupTable.put('V', "void");
    }

    public static String toJavaType(String descriptor) {
        return toJavaType(UNKNOWN_METHOD_NAME, descriptor);
    }

    public static String toJavaType(String methodName, String descriptor) {
        final int NOT_FOUND = -1;

        int start = descriptor.indexOf('(');
        int end = descriptor.indexOf(')');
        if (start == NOT_FOUND || end == NOT_FOUND) {
            throw new IllegalArgumentException("Malformed descriptor: " + descriptor);
        }

        String parameterPart = descriptor.substring(start+1, end);
        List<String> decodedParameters = convertParameters(parameterPart);
        String parameters = StringUtils.join(", ", decodedParameters);

        String returnPart = descriptor.substring(end+1);
        String returnType = DescriptorConverter.toJavaType(returnPart, lookupTable);

        return returnType + " " + methodName.replace('/', '.') + "(" + parameters + ")";
    }

    private static List<String> convertParameters(String parameterPart) {
        List<String> decodedParameters = new ArrayList<>();

        int index = 0;
        while (index < parameterPart.length()) {
            int arrayDimensions = 0;
            char code = parameterPart.charAt(index);
            while (code == '[') {
                arrayDimensions++;
                index++;
                code = parameterPart.charAt(index);
            }

            if (null != lookupTable.get(code)) {
                decodedParameters.add(lookupTable.get(code) + StringUtils.repeat("[]", arrayDimensions));
                index++;
            } else if (code == 'L') {
                int endIndex = parameterPart.indexOf(';', index+1);
                if (endIndex == -1) {
                    throw new IllegalArgumentException("Malformed descriptor: " + code);
                }
                String commonClassName = parameterPart.substring(index+1, endIndex).replace('/', '.');
                decodedParameters.add(commonClassName + StringUtils.repeat("[]", arrayDimensions));
                index = endIndex + 1;
            } else {
                throw new IllegalArgumentException("Unrecognized descriptor : " + code);
            }
        }
        return decodedParameters;
    }

}
