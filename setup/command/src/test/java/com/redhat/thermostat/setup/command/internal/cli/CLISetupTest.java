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
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;

import org.junit.Before;
import org.junit.Test;

import com.redhat.thermostat.common.cli.CommandException;
import com.redhat.thermostat.common.cli.Console;
import com.redhat.thermostat.setup.command.internal.model.ThermostatSetup;

public class CLISetupTest {

    private CLISetup cliSetup;
    private ThermostatSetup thermostatSetup;
    private Console console;
    private ByteArrayOutputStream berr;
    private ByteArrayOutputStream bout;
    
    @Before
    public void setup() {
        thermostatSetup = mock(ThermostatSetup.class);
        console = mock(Console.class);
        berr = new ByteArrayOutputStream();
        when(console.getError()).thenReturn(new PrintStream(berr));
        bout = new ByteArrayOutputStream();
        when(console.getOutput()).thenReturn(new PrintStream(bout));
        cliSetup = new CLISetup(thermostatSetup, console);
    }
    
    @Test
    public void testCancelProceedLoop() throws IOException {
        String input = "no\n";
        ByteArrayInputStream mockInStream = new ByteArrayInputStream(input.getBytes());
        when(console.getInput()).thenReturn(mockInStream);
        try {
            cliSetup.run();
            fail("Setup expected to throw CommandException on cancel");
        } catch (CommandException e) {
            assertEquals("Setup cancelled on user request.", e.getMessage());
        }
        String output = new String(bout.toByteArray());
        assertTrue(output.contains("Ready to proceed?"));
    }
    
    @Test
    public void testProceedLoopWithMoreInput() throws IOException {
        String input = "no\nsomethingMore\n";
        ByteArrayInputStream mockInStream = new ByteArrayInputStream(input.getBytes());
        when(console.getInput()).thenReturn(mockInStream);
        try {
            cliSetup.run();
            fail("Setup expected to throw CommandException on cancel");
        } catch (CommandException e) {
            assertEquals("Setup cancelled on user request.", e.getMessage());
        }
        String output = new String(bout.toByteArray());
        assertTrue(output.contains("Ready to proceed?"));
        // Verify that we can still read from the stream and we haven't read too
        // much.
        byte[] buf = new byte[input.length()];
        int retval = mockInStream.read(buf, 3, input.length() - 3);
        assertEquals("Read more bytes than are needed!", input.length() - 3, retval);
        assertEquals("Expected 'e' from somethingMor(e)", 'e', (char) buf[input.length() - 2]);
    }
    
    @Test
    public void testUnknownInputProceedLoop() throws IOException {
        String input = "somethingNotYes\nno\n";
        ByteArrayInputStream mockInStream = new ByteArrayInputStream(input.getBytes());
        when(console.getInput()).thenReturn(mockInStream);
        try {
            cliSetup.run();
            fail("Setup expected to throw CommandException on cancel");
        } catch (CommandException e) {
            assertEquals("Setup cancelled on user request.", e.getMessage());
        }
        String errBuf = new String(berr.toByteArray());
        assertTrue("Expected unknown input msg! Got " + errBuf, errBuf.startsWith("Unknown response 'somethingNotYes'"));
    }
    
    @Test
    public void testNoInputProceedLoopCancels() throws InterruptedException {
        String input = "";
        ByteArrayInputStream mockInStream = new ByteArrayInputStream(input.getBytes());
        when(console.getInput()).thenReturn(mockInStream);
        try {
            cliSetup.run();
            fail("Expected setup to cancel");
        } catch (CommandException e) {
            assertEquals("Setup cancelled on user request.", e.getMessage());
        }
    }
    
    @Test
    public void testReadMongodbCreds() throws IOException {
        String input = "somevalidusername\nt\nt\n";
        ByteArrayInputStream mockInStream = new ByteArrayInputStream(input.getBytes());
        when(console.getInput()).thenReturn(mockInStream);
        cliSetup.readMongodbCredentials();
        verify(thermostatSetup).createMongodbUser(eq("somevalidusername"), argThat(matchesPassword(new char[] { 't' })));
        String output = new String(bout.toByteArray());
        assertTrue("Expected user setup blurb", output.contains("Mongodb User Setup"));
        assertTrue("Expected somevalidusername in output. Got: " + output, output.contains("somevalidusername"));
        assertEquals("Expected no errors", "", new String(berr.toByteArray()));
    }
    
    @Test
    public void testReadMongodbCreds2() throws IOException, CommandException {
        String input = "yes\nsomevalidusername\nt\nt\n";
        ByteArrayInputStream mockInStream = new ByteArrayInputStream(input.getBytes());
        when(console.getInput()).thenReturn(mockInStream);
        when(thermostatSetup.isWebAppInstalled()).thenReturn(false);
        cliSetup.run();
        verify(thermostatSetup).createMongodbUser(eq("somevalidusername"), argThat(matchesPassword(new char[] { 't' })));
        String output = new String(bout.toByteArray());
        assertTrue("Expected user setup blurb", output.contains("Mongodb User Setup"));
        assertTrue("Expected somevalidusername in output. Got: " + output, output.contains("somevalidusername"));
        assertEquals("Expected no errors", "", new String(berr.toByteArray()));
    }
    
    @Test
    public void testReadThermostatCreds() throws IOException {
        String input = "client-user\nt\nt\nagent-user\nb\nb\n";
        ByteArrayInputStream mockInStream = new ByteArrayInputStream(input.getBytes());
        when(console.getInput()).thenReturn(mockInStream);
        cliSetup.readThermostatUserCredentials();
        verify(thermostatSetup).createAgentUser(eq("agent-user"), argThat(matchesPassword(new char[] { 'b' })));
        verify(thermostatSetup).createClientAdminUser(eq("client-user"), argThat(matchesPassword(new char[] { 't' })));
        String output = new String(bout.toByteArray());
        assertTrue("Expected user setup blurb", output.contains("Thermostat User Setup"));
        assertTrue("Expected client-user in output. Got: " + output, output.contains("client-user"));
        assertTrue("Expected agent-user in output. Got: " + output, output.contains("agent-user"));
        assertEquals("Expected no errors", "", new String(berr.toByteArray()));
    }

    @Test
    public void testReadThermostatCredsWithIdenticalUsernames() throws IOException {
        String incorrectInput = "identical-user\nt\nt\nidentical-user\nb\nb\n";
        String correctInput = "client-user\nt\nt\nagent-user\nb\nb\n";
        ByteArrayInputStream mockInStream = new ByteArrayInputStream((incorrectInput + correctInput).getBytes());
        when(console.getInput()).thenReturn(mockInStream);
        cliSetup.readThermostatUserCredentials();
        verify(thermostatSetup).createAgentUser(eq("agent-user"), argThat(matchesPassword(new char[] {'b'})));
        verify(thermostatSetup).createClientAdminUser(eq("client-user"), argThat(matchesPassword(new char[] {'t'})));
        String output = new String(bout.toByteArray());
        assertTrue("Expected client-user in output. Got: " + output, output.contains("client-user"));
        assertTrue("Expected agent-user in output. Got: " + output, output.contains("agent-user"));
        assertEquals("Both client and agent usernames cannot be 'identical-user'!\n", new String(berr.toByteArray()));
    }
    
    @Test
    public void canCreateUsersFromStdInput() throws CommandException {
        // simulate webapp being installed
        when(thermostatSetup.isWebAppInstalled()).thenReturn(true);
        
        String input = "yes\nmongodb-user\nfoo\nfoo\nclient-user\nt\nt\nagent-user\nb\nb\n";
        when(console.getInput()).thenReturn(new ByteArrayInputStream(input.getBytes()));
        cliSetup.run();
        verify(thermostatSetup).createMongodbUser(eq("mongodb-user"), argThat(matchesPassword(new char[] { 'f', 'o', 'o' })));
        verify(thermostatSetup).createAgentUser(eq("agent-user"), argThat(matchesPassword(new char[] { 'b' })));
        verify(thermostatSetup).createClientAdminUser(eq("client-user"), argThat(matchesPassword(new char[] { 't' })));
        String output = new String(bout.toByteArray());
        assertTrue("Expected user setup blurb", output.contains("Thermostat User Setup"));
        assertTrue("Expected client-user in output. Got: " + output, output.contains("client-user"));
        assertTrue("Expected agent-user in output. Got: " + output, output.contains("agent-user"));
        assertTrue("Expected mongodb-user in output. Got: " + output, output.contains("mongodb-user"));
        assertEquals("Expected no errors", "", new String(berr.toByteArray()));
    }
    
    private CharArrayMatcher matchesPassword(char[] array) {
        return new CharArrayMatcher(array);
    }
}
