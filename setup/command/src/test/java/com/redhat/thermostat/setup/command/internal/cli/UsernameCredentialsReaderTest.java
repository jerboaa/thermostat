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

package com.redhat.thermostat.setup.command.internal.cli;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;

import org.junit.Before;
import org.junit.Test;

import com.redhat.thermostat.common.cli.Console;
import com.redhat.thermostat.shared.locale.LocalizedString;

public class UsernameCredentialsReaderTest {

    private Console console;
    private UsernameCredentialsReader credsReader;
    private LocalizedString userPrompt = new LocalizedString("tell me the username: ");
    private ByteArrayOutputStream berr;
    private ByteArrayOutputStream bout;
    
    @Before
    public void setup() {
        console = mock(Console.class);
        berr = new ByteArrayOutputStream();
        when(console.getError()).thenReturn(new PrintStream(berr));
        bout = new ByteArrayOutputStream();
        when(console.getOutput()).thenReturn(new PrintStream(bout));
        credsReader = new UsernameCredentialsReader(console, userPrompt);
    }
    
    @Test
    public void canGetUsername() throws IOException {
        String input = "foo-user\n";
        when(console.getInput()).thenReturn(new ByteArrayInputStream(input.getBytes()));
        String username = credsReader.read();
        assertEquals("foo-user", username);
        assertEquals("Expected no errors", "", new String(berr.toByteArray()));
        assertEquals("tell me the username: foo-user\n", new String(bout.toByteArray()));
    }
    
    @Test
    public void testUsernameInvalidAtFirst() throws IOException {
        String input = "\ntry-second-time\n";
        when(console.getInput()).thenReturn(new ByteArrayInputStream(input.getBytes()));
        String username = credsReader.read();
        assertEquals("try-second-time", username);
        assertEquals("Chosen username '' invalid!\n", new String(berr.toByteArray()));
        assertEquals("tell me the username: \ntell me the username: try-second-time\n", new String(bout.toByteArray()));
    }
}
