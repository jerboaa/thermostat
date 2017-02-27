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

package com.redhat.thermostat.common.portability.internal.linux;

import com.redhat.thermostat.common.portability.linux.ProcDataSource;
import org.mockito.Matchers;
import org.mockito.Mockito;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;
import java.util.Random;

import com.redhat.thermostat.shared.config.OS;
import org.junit.Assume;
import org.junit.Test;

import com.redhat.thermostat.testutils.TestUtils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;


public class LinuxProcessEnvironmentBuilderImplTest {

    private static final String ROOT_CGROUP_SCOPE = "/";
    private static final String PID_1_PROC_CGROUP = "/proc/1/cgroup";
    private final Random r = new Random();

    @Test
    public void testBasicBuild() {
        Assume.assumeTrue(OS.IS_LINUX);
        ProcDataSource dataSource = new ProcDataSource();
        int pid = TestUtils.getProcessId();
        Map<String, String> result = new LinuxProcessEnvironmentBuilderImpl(dataSource).build(pid);
        assertNotNull(result);
        assertFalse(result.isEmpty());
        // assert this only for the non-container case.
        if (!isContainer()) {
            assertTrue(result.containsKey("USER"));
        }
    }

    @Test
    public void testCustomEnvironment() throws IOException {
        byte[] data = ("USER=test\000HOME=house\000").getBytes();

        Reader r = new InputStreamReader(new ByteArrayInputStream(data));
        ProcDataSource dataSource = Mockito.mock(ProcDataSource.class);
        Mockito.when(dataSource.getEnvironReader(Matchers.any(Integer.class))).thenReturn(r);

        Map<String, String> result = new LinuxProcessEnvironmentBuilderImpl(dataSource).build(0);

        Mockito.verify(dataSource).getEnvironReader(Matchers.eq(0));
        assertEquals("test", result.get("USER"));
        assertEquals("house", result.get("HOME"));
    }

    @Test
    public void testLargeRandomEnvironment() throws IOException {
        int TEST_ENV_SIZE = 1024 * 1024;
        byte[] data = new byte[TEST_ENV_SIZE];
        int currentPosition = 0;
        do {
            byte[] key = generateRandomBytes();
            byte[] value = generateRandomBytes();
            if (currentPosition + key.length + value.length + 2 >= data.length) {
                break;
            }
            System.arraycopy(key, 0, data, currentPosition, key.length);
            currentPosition += key.length;
            data[currentPosition] = (byte) '=';
            currentPosition++;
            System.arraycopy(value, 0, data, currentPosition, value.length);
            currentPosition += value.length;
            data[currentPosition] = 0x00;
            currentPosition++;
        } while (true);
        Reader r = new InputStreamReader(new ByteArrayInputStream(data, 0, currentPosition));
        ProcDataSource dataSource = Mockito.mock(ProcDataSource.class);
        Mockito.when(dataSource.getEnvironReader(Matchers.any(Integer.class))).thenReturn(r);

        Map<String, String> result = new LinuxProcessEnvironmentBuilderImpl(dataSource).build(0);

        Mockito.verify(dataSource).getEnvironReader(Matchers.eq(0));
        assertNotNull(result);
    }

    private byte[] generateRandomBytes() {
        byte start = (byte) 'a';
        byte end = (byte) 'z' + 1;

        byte[] alphabet = new byte[end - start];
        for (int i = 0; i < (end-start); i++) {
            alphabet[i] = (byte) (i + start);
        }
        int size = r.nextInt(15) + 10;
        byte[] result = new byte[size];
        for (int i = 0; i < result.length; i++) {
            result[i] = alphabet[r.nextInt(alphabet.length)];
        }
        return result;
    }
    
    /*
     * Heuristic: /proc/1/cgroup => A:B:C where C == '/' outside a container.
     *                                    where C == '/system.slice/docker-<SHA256>.scope in a container
     */
    private boolean isContainer() {
        try {
            List<String> lines = Files.readAllLines(new File(PID_1_PROC_CGROUP).toPath(), Charset.forName("UTF-8"));
            String[] tokens = lines.get(0).split(":");
            if (tokens.length == 3) {
                return !(tokens[2].equals(ROOT_CGROUP_SCOPE));
            } else {
                // unknown format?
                return false;
            }
        } catch (Throwable e) {
            // Default to false
            return false;
        }
    }
}

