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

package org.jboss.byteman.thermostat.helper;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.regex.Pattern;

import static java.lang.System.currentTimeMillis;

/**
 * Common utility functions
 *
 * @author akashche
 */
public class Utils {
    /**
     * Regex for escaping quotes in generated JSON
     */
    private static final Pattern QUOTE_PATTERN = Pattern.compile("\"");

    /**
     * Thread#sleep call without checked exceptions
     *
     * @param millis number of milliseconds to sleep
     */
    public static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            throw new TransportException("Sleep fo millis: [" + millis +"] interrupted", e);
        }
    }

    /**
     * Escapes all quotes with "\" in specified string
     *
     * @param str input string
     * @return string with escaped quotes
     */
    public static String escapeQuotes(String str) {
        if (null != str) {
            return QUOTE_PATTERN.matcher(str).replaceAll("\\\\\"");
        }
        return str;
    }

    /**
     * Converts "key1, value1, key2, value2" array into
     * "key1->value, key2->value2" map
     *
     * @param dataArray input array with even number of elements
     * @return constructed map
     */
    public static LinkedHashMap<String, Object> toMap(Object[] dataArray){
        LinkedHashMap<String, Object> map = new LinkedHashMap<String, Object>();
        if (0 != dataArray.length % 2) {
            throw new IllegalArgumentException("Invalid odd elements count in array: [" + Arrays.toString(dataArray) + "]");
        }
        for (int i = 0; i < dataArray.length; i += 2) {
            Object objKey = dataArray[i];
            if (null == objKey) {
                objKey = "";
            }
            if (!(objKey instanceof String)) {
                throw new IllegalArgumentException("Unsupported type for key. Expected String but was " + objKey.getClass());
            }
            Object value = dataArray[i + 1];
            if (value == null || value instanceof String || value instanceof Number || value instanceof Boolean) {
                map.put((String)objKey, value);
            } else {
                throw new IllegalArgumentException("Unsupported type for value. Expected String or Number but was: " + value.getClass());
            }
        }
        return map;
    }

    /**
     * Creates temporary directory for given class
     *
     * @param clazz name of this class will be used in dir name
     * @return created directory
     * @throws IOException creation error
     */
    public static File createTmpDir(Class<?> clazz) throws IOException {
        File baseDir = new File(System.getProperty("java.io.tmpdir"));
        String baseName = clazz.getName() + "_" + currentTimeMillis() + ".tmp";
        File tmp = new File(baseDir, baseName);
        boolean res = tmp.mkdirs();
        if (!res) throw new IOException("Cannot create directory: [" + tmp.getAbsolutePath() + "]");
        return tmp;
    }

    /**
     * Closes specified closeable without throwing exceptions
     *
     * @param closeable closeable instance to close
     */
    public static void closeQuietly(Closeable closeable) {
        if (null != closeable) {
            try {
                closeable.close();
            } catch (Exception e) {
                // ignore
            }
        }
    }
}
