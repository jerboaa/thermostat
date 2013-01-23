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

package com.redhat.thermostat.backend.system;

import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.redhat.thermostat.common.utils.LoggingUtils;
import com.redhat.thermostat.utils.ProcDataSource;

public class ProcessEnvironmentBuilder {

    private static final Logger logger = LoggingUtils.getLogger(ProcessEnvironmentBuilder.class);

    private final ProcDataSource dataSource;

    public ProcessEnvironmentBuilder(ProcDataSource dataSource) {
        this.dataSource = dataSource;
    }

    public Map<String, String> build(int pid) {
        try (Reader reader = dataSource.getEnvironReader(pid)) {
            return build(reader);
        } catch (IOException ioe) {
            logger.log(Level.WARNING, "error reading env", ioe);
        }

        return Collections.emptyMap();
    }

    private Map<String,String> build(Reader reader) throws IOException {

        Map<String, String> env = new HashMap<String, String>();

        char[] fileBuffer = new char[1024];
        int fileBufferIndex = 0;
        char[] buffer = new char[1024];
        int read = 0;
        while (true) {
            read = reader.read(buffer);
            if (read == -1) {
                break;
            }

            if (read + fileBufferIndex > fileBuffer.length) {
                char[] newFileBuffer = new char[fileBuffer.length * 2];
                System.arraycopy(fileBuffer, 0, newFileBuffer, 0, fileBufferIndex);
                fileBuffer = newFileBuffer;
            }
            System.arraycopy(buffer, 0, fileBuffer, fileBufferIndex, read);
            fileBufferIndex = fileBufferIndex + read;

        }
        List<String> parts = getParts(fileBuffer, fileBufferIndex);
        for (String part : parts) {
            int splitterPos = part.indexOf("=");
            String key = part.substring(0, splitterPos);
            String value = part.substring(splitterPos + 1);
            env.put(key, value);
        }

        return env;
    }

    /**
     * Split a char array, where items are separated by a null into into a list
     * of strings
     */
    private List<String> getParts(char[] nullSeparatedBuffer, int bufferLength) {
        int maxLength = Math.min(nullSeparatedBuffer.length, bufferLength);
        List<String> parts = new ArrayList<String>();

        int lastStart = 0;
        for (int i = 0; i < maxLength; i++) {
            if (nullSeparatedBuffer[i] == '\0') {
                String string = new String(nullSeparatedBuffer, lastStart, (i - lastStart));
                parts.add(string);
                lastStart = i + 1;
            }
        }
        return parts;
    }

}

