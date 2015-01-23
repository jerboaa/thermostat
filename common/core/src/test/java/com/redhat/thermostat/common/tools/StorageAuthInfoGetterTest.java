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

package com.redhat.thermostat.common.tools;

import java.io.IOException;
import java.io.InputStream;
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

import java.io.PrintStream;

import org.junit.Before;
import org.junit.Test;

import com.redhat.thermostat.common.cli.Console;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class StorageAuthInfoGetterTest {

    private StorageAuthInfoGetter getter;
    private InputStream in;
    private PrintStream out;

    @Before
    public void setUp() throws IOException {
        in = mock(InputStream.class);
        out = mock(PrintStream.class);
        Console console = mock(Console.class);
        when(console.getInput()).thenReturn(in);
        when(console.getOutput()).thenReturn(out);
        getter = new StorageAuthInfoGetter(console);
    }

    @Test
    public void testGetUserNameCarriageReturn() throws IOException {
        testGetUsername((int) '\r');
    }

    @Test
    public void testGetUserNameNewLine() throws IOException {
        testGetUsername((int) '\n');
    }

    private void testGetUsername(int newLineChar) throws IOException {
        when(in.read()).thenReturn((int) 'u')
                .thenReturn((int) 's')
                .thenReturn((int) 'e')
                .thenReturn((int) 'r')
                .thenReturn(newLineChar);
        assertEquals("user", getter.getUserName("url_doesn't_matter"));
    }

    @Test
    public void testGetPasswordCarriageReturn() throws IOException {
        testGetPassword((int) '\r');
    }

    @Test
    public void testGetPasswordNewLine() throws IOException {
        testGetPassword((int) '\n');
    }

    private void testGetPassword(int newLineChar) throws IOException {
        char[] pass = new char[] {'p', 'a', 's', 's'};
        when(in.read()).thenReturn((int) 'p')
                .thenReturn((int) 'a')
                .thenReturn((int) 's')
                .thenReturn((int) 's')
                .thenReturn((int) '\r');
        assertEquals(new String(pass), new String(getter.getPassword("url_doesn't_matter")));
    }
}

