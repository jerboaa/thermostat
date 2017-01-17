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

package com.redhat.thermostat.backend.system.internal.linux;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Map;
import java.util.Random;

import com.redhat.thermostat.shared.config.OS;
import org.junit.Assume;
import org.junit.Test;

import com.redhat.thermostat.agent.utils.linux.ProcDataSource;
import com.redhat.thermostat.testutils.TestUtils;

public class ProcessEnvironmentBuilderTest {

    private final Random r = new Random();

    @Test
    public void testBasicBuild() {
        Assume.assumeTrue(OS.IS_UNIX);
        ProcDataSource dataSource = new ProcDataSource();
        Map<String, String> result = new ProcessEnvironmentBuilderImpl(dataSource).build(TestUtils.getProcessId());
        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertTrue(result.containsKey("USER"));
    }

    @Test
    public void testCustomEnvironment() throws IOException {
        byte[] data = ("USER=test\000HOME=house\000").getBytes();

        Reader r = new InputStreamReader(new ByteArrayInputStream(data));
        ProcDataSource dataSource = mock(ProcDataSource.class);
        when(dataSource.getEnvironReader(any(Integer.class))).thenReturn(r);

        Map<String, String> result = new ProcessEnvironmentBuilderImpl(dataSource).build(0);

        verify(dataSource).getEnvironReader(eq(0));
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
        ProcDataSource dataSource = mock(ProcDataSource.class);
        when(dataSource.getEnvironReader(any(Integer.class))).thenReturn(r);

        Map<String, String> result = new ProcessEnvironmentBuilderImpl(dataSource).build(0);

        verify(dataSource).getEnvironReader(eq(0));
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
}

