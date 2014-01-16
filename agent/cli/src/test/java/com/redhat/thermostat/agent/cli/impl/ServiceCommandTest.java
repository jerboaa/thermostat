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

package com.redhat.thermostat.agent.cli.impl;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.isA;
import static org.mockito.Mockito.anyBoolean;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.never;

import java.util.Collection;

import junit.framework.Assert;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.osgi.framework.ServiceReference;

import com.redhat.thermostat.agent.cli.impl.db.DBStartupConfiguration;
import com.redhat.thermostat.agent.cli.impl.db.StorageCommand;
import com.redhat.thermostat.common.ActionEvent;
import com.redhat.thermostat.common.ActionListener;
import com.redhat.thermostat.common.ActionNotifier;
import com.redhat.thermostat.common.cli.CommandContext;
import com.redhat.thermostat.common.cli.CommandException;
import com.redhat.thermostat.common.tools.ApplicationState;
import com.redhat.thermostat.launcher.Launcher;
import com.redhat.thermostat.testutils.StubBundleContext;

public class ServiceCommandTest {

    private Launcher mockLauncher;
    private ServiceCommand serviceCommand;
    private CommandContext mockCommandContext;

    private static ActionEvent<ApplicationState> mockActionEvent;
    private static Collection<ActionListener<ApplicationState>> listeners;

    private static final String[] STORAGE_START_ARGS = { "storage", "--start" };
    private static final String[] STORAGE_STOP_ARGS = { "storage", "--stop" };
    private static final String[] AGENT_ARGS = {"agent", "-d", "Test String"};
    
    private static int count = 0;

    @SuppressWarnings("unchecked")
    @Before
    public void setUp() {
        StubBundleContext bundleContext = new StubBundleContext();
        mockLauncher = mock(Launcher.class);
        bundleContext.registerService(Launcher.class, mockLauncher, null);
        serviceCommand = new ServiceCommand(bundleContext);
        
        StorageCommand mockStorageCommand = mock(StorageCommand.class);
        mockActionEvent = mock(ActionEvent.class);
        when(mockActionEvent.getSource()).thenReturn(mockStorageCommand);
        mockCommandContext = mock(CommandContext.class);
        
        ActionNotifier<ApplicationState> mockNotifier = mock(ActionNotifier.class);
        DBStartupConfiguration mockDbsConfiguration = mock(DBStartupConfiguration.class);
        when(mockStorageCommand.getNotifier()).thenReturn(mockNotifier);
        when(mockStorageCommand.getConfiguration()).thenReturn(mockDbsConfiguration);
        when(mockDbsConfiguration.getDBConnectionString()).thenReturn(new String("Test String"));
    }

    @After
    public void tearDown() {
        count = 0;
        listeners = null;
        mockLauncher = null;
        serviceCommand = null;
        mockActionEvent = null;
        mockCommandContext = null;
    }

    @SuppressWarnings("unchecked")
    @Test(timeout=1000)
    public void testRunOnce() throws CommandException {
        doAnswer(new Answer<Void>() {
            public Void answer(InvocationOnMock invocation) throws Throwable {
                Object[] args = invocation.getArguments();
                listeners = (Collection<ActionListener<ApplicationState>>)args[1];
                
                when(mockActionEvent.getActionId()).thenReturn(ApplicationState.START);
                
                for(ActionListener<ApplicationState> listener : listeners) {
                    listener.actionPerformed(mockActionEvent);
                }
                return null;
            }
        }).when(mockLauncher).run(eq(STORAGE_START_ARGS), isA(Collection.class), anyBoolean());
        
        boolean exTriggered = false;
        try {
            serviceCommand.run(mockCommandContext);
        } catch (CommandException e) { 
            exTriggered = true;
        }
        Assert.assertFalse(exTriggered);
        
        verify(mockLauncher, times(1)).run(eq(STORAGE_START_ARGS), isA(Collection.class), anyBoolean());
        verify(mockLauncher, times(1)).run(eq(STORAGE_STOP_ARGS), anyBoolean());
        verify(mockLauncher, times(1)).run(eq(AGENT_ARGS), anyBoolean());
        verify(mockActionEvent, times(1)).getActionId();
    }

    @SuppressWarnings("unchecked")
    @Test(timeout=1000)
    public void testMultipleRun() throws CommandException {
        doAnswer(new Answer<Void>() {
            public Void answer(InvocationOnMock invocation) throws Throwable {
                Object[] args = invocation.getArguments();
                listeners = (Collection<ActionListener<ApplicationState>>)args[1];
                
                if (count == 0) {
                    when(mockActionEvent.getActionId()).thenReturn(ApplicationState.START);
                } else {
                    when(mockActionEvent.getActionId()).thenReturn(ApplicationState.FAIL);
                }
                ++count;
                
                for(ActionListener<ApplicationState> listener : listeners) {
                    listener.actionPerformed(mockActionEvent);
                }
                return null;
            }
        }).when(mockLauncher).run(eq(STORAGE_START_ARGS), isA(Collection.class), anyBoolean());
        
        boolean exTriggered = false;
        try {
            serviceCommand.run(mockCommandContext);
        } catch (CommandException e) { 
            exTriggered = true;
        }
        Assert.assertFalse(exTriggered);
        
        try {
            serviceCommand.run(mockCommandContext);
        } catch (CommandException e) {
            exTriggered = true;
            Assert.assertEquals(e.getLocalizedMessage(), "Service failed to start due to error starting storage.");
        }
        Assert.assertTrue(exTriggered);
        
        verify(mockLauncher, times(2)).run(eq(STORAGE_START_ARGS), isA(Collection.class), anyBoolean());
        verify(mockLauncher, times(1)).run(eq(STORAGE_STOP_ARGS), anyBoolean());
        verify(mockLauncher, times(1)).run(eq(AGENT_ARGS), anyBoolean());
        verify(mockActionEvent, times(2)).getActionId();
    }

    @SuppressWarnings("unchecked")
    @Test(timeout=1000)
    public void testStorageFailStart()  throws CommandException {
        doAnswer(new Answer<Void>() {
            public Void answer(InvocationOnMock invocation) throws Throwable {
                Object[] args = invocation.getArguments();
                listeners = (Collection<ActionListener<ApplicationState>>)args[1];
                
                when(mockActionEvent.getActionId()).thenReturn(ApplicationState.FAIL);
                
                for(ActionListener<ApplicationState> listener : listeners) {
                    listener.actionPerformed(mockActionEvent);
                }
                return null;
            }
        }).when(mockLauncher).run(eq(STORAGE_START_ARGS), isA(Collection.class), anyBoolean());
        
        boolean exTriggered = false;
        try {
            serviceCommand.run(mockCommandContext);
        } catch (CommandException e) {
            exTriggered = true;
        }
        Assert.assertTrue(exTriggered);
        
        verify(mockLauncher, times(1)).run(eq(STORAGE_START_ARGS), isA(Collection.class), anyBoolean());
        verify(mockLauncher, never()).run(eq(STORAGE_STOP_ARGS), anyBoolean());
        verify(mockLauncher, never()).run(eq(AGENT_ARGS), anyBoolean());
        verify(mockActionEvent, times(1)).getActionId();
    }

}

