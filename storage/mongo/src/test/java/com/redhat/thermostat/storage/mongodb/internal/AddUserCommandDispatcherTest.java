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

package com.redhat.thermostat.storage.mongodb.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.Test;
import org.osgi.framework.BundleContext;

import com.redhat.thermostat.common.cli.Arguments;
import com.redhat.thermostat.common.cli.CommandContext;
import com.redhat.thermostat.common.cli.CommandContextFactory;
import com.redhat.thermostat.common.cli.CommandException;

public class AddUserCommandDispatcherTest {

    @Test
    public void canRunUndecorated() {
        BundleContext context = mock(BundleContext.class);
        BaseAddUserCommand mockCmd = mock(BaseAddUserCommand.class);
        AddUserCommandDispatcher dispatcher = new AddUserCommandDispatcher(context, mockCmd);
        CommandContextFactory factory = new CommandContextFactory(context);
        Arguments args = mock(Arguments.class);
        when(args.hasArgument(eq("dbUrl"))).thenReturn(true);
        CommandContext ctx = factory.createContext(args);
        
        // This should not throw any exception
        try {
            dispatcher.run(ctx);
        } catch (CommandException e) {
            fail(e.getMessage());
        }
    }
    
    @Test(expected = CommandException.class )
    public void failsToRunWithNoArguments() throws CommandException {
        BundleContext context = mock(BundleContext.class);
        BaseAddUserCommand mockCmd = mock(BaseAddUserCommand.class);
        AddUserCommandDispatcher dispatcher = new AddUserCommandDispatcher(context, mockCmd);
        CommandContextFactory factory = new CommandContextFactory(context);
        Arguments args = mock(Arguments.class);
        when(args.hasArgument(any(String.class))).thenReturn(false);
        CommandContext ctx = factory.createContext(args);
        
        // this throws CommandException
        dispatcher.run(ctx);
    }
    
    @Test
    public void decoratesIfStorageStartOptionGiven() {
        BundleContext context = mock(BundleContext.class);
        BaseAddUserCommand mockCmd = mock(BaseAddUserCommand.class);
        AddUserCommandDispatcher dispatcher = new AddUserCommandDispatcher(context, mockCmd);
        CommandContextFactory factory = new CommandContextFactory(context);
        Arguments args = mock(Arguments.class);
        when(args.hasArgument(eq("startStorage"))).thenReturn(true);
        CommandContext ctx = factory.createContext(args);
        
        try {
            dispatcher.run(ctx);
            fail("CommonPaths not available, should have thrown exception");
        } catch (CommandException e) {
            boolean passed = false;
            for( StackTraceElement elmt: e.getStackTrace()) {
                // StartStopAddUserCommandDecorator should be in stack trace
                if (elmt.getClassName().equals(StartStopAddUserCommandDecorator.class.getName())) {
                    passed = true;
                    break;
                }
            }
            assertTrue("Expected mocked BaseAddUserCommand to be decorated", passed);
        }
    }
    
    @Test
    public void testCommandName() {
        assertEquals("add-mongodb-user", AddUserCommandDispatcher.COMMAND_NAME);
    }
}
