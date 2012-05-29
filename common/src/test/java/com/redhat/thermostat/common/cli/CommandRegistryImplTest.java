/*
 * Copyright 2012 Red Hat, Inc.
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

package com.redhat.thermostat.common.cli;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collection;
import java.util.Hashtable;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;

public class CommandRegistryImplTest {

    private CommandRegistryImpl commandRegistry;

    private BundleContext bundleContext;

    @Before
    public void setUp() {
        bundleContext = mock(BundleContext.class);
        commandRegistry = new CommandRegistryImpl(bundleContext);
    }

    @After
    public void tearDown() {
        bundleContext = null;
    }

    @Test
    public void testRegisterCommands() {
        Command cmd1 = mock(Command.class);
        when(cmd1.getName()).thenReturn("test1");
        Command cmd2 = mock(Command.class);
        when(cmd2.getName()).thenReturn("test2");

        commandRegistry.registerCommands(Arrays.asList(cmd1, cmd2));

        Hashtable<String,Object> props1 = new Hashtable<>();
        props1.put(Command.NAME, "test1");
        Hashtable<String,Object> props2 = new Hashtable<>();
        props2.put(Command.NAME, "test2");
        verify(bundleContext).registerService(Command.class.getName(), cmd1, props1);
        verify(bundleContext).registerService(Command.class.getName(), cmd2, props2);

        verifyNoMoreInteractions(bundleContext);

    }

    @Test
    public void testUnregisterCommand() {
        Command cmd1 = mock(Command.class);
        when(cmd1.getName()).thenReturn("test1");
        Command cmd2 = mock(Command.class);
        when(cmd2.getName()).thenReturn("test2");

        ServiceReference cmd1Reference = mock(ServiceReference.class);
        ServiceReference cmd2Reference = mock(ServiceReference.class);

        ServiceRegistration cmd1Reg = mock(ServiceRegistration.class);
        when(cmd1Reg.getReference()).thenReturn(cmd1Reference);
        ServiceRegistration cmd2Reg = mock(ServiceRegistration.class);
        when(cmd2Reg.getReference()).thenReturn(cmd2Reference);

        Hashtable<String,String> props1 = new Hashtable<>();
        props1.put(Command.NAME, cmd1.getName());
        Hashtable<String,String> props2 = new Hashtable<>();
        props2.put(Command.NAME, cmd2.getName());

        when(bundleContext.registerService(Command.class.getName(), cmd1, props1)).thenReturn(cmd1Reg);
        when(bundleContext.registerService(Command.class.getName(), cmd2, props2)).thenReturn(cmd2Reg);

        commandRegistry.registerCommands(Arrays.asList(cmd1, cmd2));

        verify(bundleContext).registerService(Command.class.getName(), cmd1, props1);
        verify(bundleContext).registerService(Command.class.getName(), cmd2, props2);

        when(bundleContext.getService(eq(cmd1Reference))).thenReturn(cmd1);
        when(bundleContext.getService(eq(cmd2Reference))).thenReturn(cmd2);

        commandRegistry.unregisterCommands();

        verify(bundleContext).getService(cmd1Reference);
        verify(cmd1).disable();
        verify(cmd1Reg).unregister();
        verify(bundleContext).getService(cmd2Reference);
        verify(cmd2).disable();
        verify(cmd2Reg).unregister();

        verifyNoMoreInteractions(bundleContext);
    }

    @Test
    public void testGetCommand() throws InvalidSyntaxException {
        ServiceReference ref1 = mock(ServiceReference.class);
        when(bundleContext.getServiceReferences(Command.class.getName(), "(&(objectclass=*)(COMMAND_NAME=test1))")).thenReturn(new ServiceReference[] { ref1 });
        Command cmd1 = mock(Command.class);
        when(bundleContext.getService(ref1)).thenReturn(cmd1);

        Command cmd = commandRegistry.getCommand("test1");

        assertSame(cmd1, cmd);
    }

    @Test
    public void testNotRegisteredCommand() throws InvalidSyntaxException {
        ServiceReference ref1 = mock(ServiceReference.class);
        Command cmd1 = mock(Command.class);
        when(bundleContext.getService(ref1)).thenReturn(cmd1);

        Command cmd = commandRegistry.getCommand("test1");

        assertNull(cmd);
    }

    @Test
    public void testNotRegisteredCommandEmptyList() throws InvalidSyntaxException {
        when(bundleContext.getServiceReferences(Command.class.getName(), "(&(objectclass=*)(COMMAND_NAME=test1))")).thenReturn(new ServiceReference[0]);

        Command cmd = commandRegistry.getCommand("test1");

        assertNull(cmd);
    }

    @Test
    public void testDoubleRegisteredCommand() throws InvalidSyntaxException {
        ServiceReference ref1 = mock(ServiceReference.class);
        ServiceReference ref2 = mock(ServiceReference.class);
        Command cmd1 = mock(Command.class);
        Command cmd2 = mock(Command.class);
        when(bundleContext.getServiceReferences(Command.class.getName(), "(&(objectclass=*)(COMMAND_NAME=test1))")).thenReturn(new ServiceReference[] { ref1, ref2 });
        when(bundleContext.getService(ref1)).thenReturn(cmd1);
        when(bundleContext.getService(ref2)).thenReturn(cmd2);

        Command cmd = commandRegistry.getCommand("test1");

        assertSame(cmd1, cmd);
    }

    @Test(expected=InternalError.class)
    public void testGetCommandInvalidSyntax() throws InvalidSyntaxException {
        when(bundleContext.getServiceReferences(Command.class.getName(), "(&(objectclass=*)(COMMAND_NAME=test1))")).thenThrow(new InvalidSyntaxException("test", "test"));

        commandRegistry.getCommand("test1");
    }

    @Test
    public void testRegisteredCommands() throws InvalidSyntaxException {
        ServiceReference ref1 = mock(ServiceReference.class);
        ServiceReference ref2 = mock(ServiceReference.class);
        Command cmd1 = mock(Command.class);
        Command cmd2 = mock(Command.class);
        when(bundleContext.getServiceReferences(Command.class.getName(), null)).thenReturn(new ServiceReference[] { ref1, ref2 });
        when(bundleContext.getService(ref1)).thenReturn(cmd1);
        when(bundleContext.getService(ref2)).thenReturn(cmd2);

        Collection<Command> cmds = commandRegistry.getRegisteredCommands();

        assertEquals(2, cmds.size());
        assertTrue(cmds.contains(cmd1));
        assertTrue(cmds.contains(cmd2));
    }
}
