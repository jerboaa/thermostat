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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.PrintStream;

import org.junit.Before;
import org.junit.Test;

import com.redhat.thermostat.common.cli.Console;

public class StorageAuthInfoGetterTest {

    private Console console;

    @Before
    public void setUp() throws IOException {
        console = mock(Console.class);
        when(console.getOutput()).thenReturn(mock(PrintStream.class));
    }
    
    /*
     * This must not loop infinitely.
     */
    @Test
    public void testGetPasswordEmpty() throws IOException {
        String input = "";
        ByteArrayInputStream bin = new ByteArrayInputStream(input.getBytes());
        when(console.getInput()).thenReturn(bin);
        StorageAuthInfoGetter getter = new StorageAuthInfoGetter(console);
        char[] password = getter.getPassword("no matter");
        assertNull(password);
    }
    
    @Test
    public void testGetUsernameEmpty() throws IOException {
        String input = "";
        ByteArrayInputStream bin = new ByteArrayInputStream(input.getBytes());
        when(console.getInput()).thenReturn(bin);
        StorageAuthInfoGetter getter = new StorageAuthInfoGetter(console);
        String username = getter.getUserName("no matter");
        assertNull(username);
    }

    @Test
    public void testGetUserNameCarriageReturn() throws IOException {
        testGetUsername("user\r\n", "user");
    }

    @Test
    public void testGetUserNameNewLine() throws IOException {
        testGetUsername("user\n", "user");
    }
    
    @Test
    public void testGetUserNameNoNewLine() throws IOException {
        testGetUsername("user", null);
    }

    private void testGetUsername(String input, String expectedUsername) throws IOException {
        ByteArrayInputStream bin = new ByteArrayInputStream(input.getBytes());
        when(console.getInput()).thenReturn(bin);
        StorageAuthInfoGetter getter = new StorageAuthInfoGetter(console);
        assertEquals(expectedUsername, getter.getUserName("url_doesn't_matter"));
    }

    @Test
    public void testGetPasswordCarriageReturn() throws IOException {
        testGetPassword("pass\r\n", "pass");
    }

    @Test
    public void testGetPasswordNewLine() throws IOException {
        testGetPassword("pass\n", "pass");
    }
    
    @Test
    public void testGetPasswordNoNewLine() throws IOException {
        testGetPassword("pass?", "pass?");
    }
    
    @Test
    public void testGetPasswordLongerThanIncrement() throws IOException {
        testGetPassword("pass|pass|pass|pass\n", "pass|pass|pass|pass");
    }

    private void testGetPassword(String input, String expectedPassword) throws IOException {
        ByteArrayInputStream bin = new ByteArrayInputStream(input.getBytes());
        when(console.getInput()).thenReturn(bin);
        StorageAuthInfoGetter getter = new StorageAuthInfoGetter(console);
        assertEquals(expectedPassword, new String(getter.getPassword("url_doesn't_matter")));
    }
}

