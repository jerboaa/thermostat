/*
 * Copyright 2012-2016 Red Hat, Inc.
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

import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.Collection;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import com.redhat.thermostat.common.ActionEvent;
import com.redhat.thermostat.common.ActionListener;
import com.redhat.thermostat.common.ActionNotifier;
import com.redhat.thermostat.common.cli.AbstractStateNotifyingCommand;
import com.redhat.thermostat.common.cli.CommandContext;
import com.redhat.thermostat.common.cli.CommandException;
import com.redhat.thermostat.common.cli.Console;
import com.redhat.thermostat.common.tools.ApplicationState;
import com.redhat.thermostat.launcher.Launcher;
import com.redhat.thermostat.testutils.StubBundleContext;

public class ServiceCommandTest {

    private ByteArrayOutputStream stdErrOut;
    private Launcher mockLauncher;
    private ServiceCommand serviceCommand;
    private CommandContext mockCommandContext;

    private static ActionEvent<ApplicationState> mockActionEvent;
    private static Collection<ActionListener<ApplicationState>> listeners;

    private static final String[] STORAGE_START_ARGS = { "storage", "--start" };
    private static final String[] STORAGE_STOP_ARGS = { "storage", "--stop" };
    private static final String[] AGENT_ARGS = {"agent", "-d", "Test String"};
    private static final String AGENT_ID = "Test ID";

    @SuppressWarnings("unchecked")
    @Before
    public void setUp() {
        StubBundleContext bundleContext = new StubBundleContext();
        mockLauncher = mock(Launcher.class);
        bundleContext.registerService(Launcher.class, mockLauncher, null);
        serviceCommand = new ServiceCommand(bundleContext);
        
        AbstractStateNotifyingCommand mockStorageCommand = mock(AbstractStateNotifyingCommand.class);
        mockActionEvent = mock(ActionEvent.class);
        when(mockActionEvent.getSource()).thenReturn(mockStorageCommand);
        mockCommandContext = mock(CommandContext.class);
        Console console = mock(Console.class);
        stdErrOut = new ByteArrayOutputStream();
        PrintStream err = new PrintStream(stdErrOut);
        when(console.getError()).thenReturn(err);
        when(mockCommandContext.getConsole()).thenReturn(console);
        
        ActionNotifier<ApplicationState> mockNotifier = mock(ActionNotifier.class);
        when(mockStorageCommand.getNotifier()).thenReturn(mockNotifier);
        when(mockActionEvent.getPayload()).thenReturn(new String("Test String"));
    }

    @After
    public void tearDown() {
        listeners = null;
        mockLauncher = null;
        serviceCommand = null;
        mockActionEvent = null;
        mockCommandContext = null;
    }

    @SuppressWarnings("unchecked")
    @Test(timeout=1000)
    public void testRunOnce() throws CommandException, InterruptedException {
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

        doAnswer(new Answer<Void>() {
            public Void answer(InvocationOnMock invocation) throws Throwable {
                Object[] args = invocation.getArguments();
                listeners = (Collection<ActionListener<ApplicationState>>)args[1];

                when(mockActionEvent.getActionId()).thenReturn(ApplicationState.START);
                when(mockActionEvent.getPayload()).thenReturn(AGENT_ID);

                for(ActionListener<ApplicationState> listener : listeners) {
                    listener.actionPerformed(mockActionEvent);
                }
                return null;
            }
        }).when(mockLauncher).run(eq(AGENT_ARGS), isA(Collection.class), anyBoolean());

        final boolean[] result = new boolean[2];
        final String[] agentIdFound = new String[1];
        serviceCommand.getNotifier().addActionListener(new ActionListener<ApplicationState>() {
            @SuppressWarnings("incomplete-switch")
            @Override
            public void actionPerformed(ActionEvent<ApplicationState> actionEvent) {
                switch (actionEvent.getActionId()) {
                    case FAIL:
                        result[0] = false;
                        break;
                    case START:
                        result[0] = true;
                        agentIdFound[0] = (String) actionEvent.getPayload();
                        break;
                    case STOP:
                        result[1] = true;
                        break;
                }
            }
        });

        boolean exTriggered = false;
        try {
            serviceCommand.run(mockCommandContext);
        } catch (CommandException e) { 
            exTriggered = true;
        }
        Assert.assertFalse(exTriggered);
        Assert.assertTrue("Agent expected to fire START event", result[0]);
        Assert.assertTrue("Agent expected to fire STOP event", result[1]);
        Assert.assertEquals("Payload does not contain AgentId matching the agent started", agentIdFound[0], AGENT_ID);

        verify(mockLauncher, times(1)).run(eq(STORAGE_START_ARGS), isA(Collection.class), anyBoolean());
        verify(mockLauncher, times(1)).run(eq(STORAGE_STOP_ARGS), anyBoolean());
        verify(mockLauncher, times(1)).run(eq(AGENT_ARGS), isA(Collection.class), anyBoolean());
        verify(mockActionEvent, times(2)).getActionId();
    }

    @Test(timeout=1000)
    public void testStorageStartUnknownPath()  throws CommandException {
        doAnswer(new Answer<Void>() {
            public Void answer(InvocationOnMock invocation) throws Throwable {
                Object[] args = invocation.getArguments();
                listeners = (Collection<ActionListener<ApplicationState>>)args[1];

                when(mockActionEvent.getActionId()).thenReturn(ApplicationState.START);
                // Return a null payload in order to trigger unknown path
                when(mockActionEvent.getPayload()).thenReturn(null);

                for(ActionListener<ApplicationState> listener : listeners) {
                    listener.actionPerformed(mockActionEvent);
                }
                return null;
            }
        }).when(mockLauncher).run(eq(STORAGE_START_ARGS), isA(Collection.class), anyBoolean());

        final boolean[] result = new boolean[1];
        serviceCommand.getNotifier().addActionListener(new ActionListener<ApplicationState>() {
            @SuppressWarnings("incomplete-switch")
            @Override
            public void actionPerformed(ActionEvent<ApplicationState> actionEvent) {
                switch (actionEvent.getActionId()) {
                    case FAIL:
                        result[0] = true;
                        break;
                    case START:
                        result[0] = false;
                        break;
                    case STOP:
                        result[0] = false;
                        break;
                }
            }
        });

        boolean exTriggered = false;
        try {
            serviceCommand.run(mockCommandContext);
        } catch (CommandException e) {
            exTriggered = true;
        }
        Assert.assertFalse(exTriggered);
        Assert.assertEquals("Unexpected result from storage.\n", stdErrOut.toString());
        Assert.assertTrue("Agent expected to fire FAIL event", result[0]);

        verify(mockLauncher, times(1)).run(eq(STORAGE_START_ARGS), isA(Collection.class), anyBoolean());
        verify(mockLauncher, times(1)).run(eq(STORAGE_STOP_ARGS), anyBoolean());
        verify(mockLauncher, never()).run(eq(AGENT_ARGS), isA(Collection.class), anyBoolean());
        verify(mockActionEvent, times(1)).getActionId();
    }

    @SuppressWarnings("unchecked")
    @Test(timeout=1000)
    public void testStorageFailStart() throws CommandException, InterruptedException {
        doAnswer(new Answer<Void>() {
            public Void answer(InvocationOnMock invocation) throws Throwable {
                Object[] args = invocation.getArguments();
                listeners = (Collection<ActionListener<ApplicationState>>)args[1];
                
                when(mockActionEvent.getActionId()).thenReturn(ApplicationState.FAIL);
                when(mockActionEvent.getPayload()).thenReturn(new Exception("Test Exception"));
                
                for(ActionListener<ApplicationState> listener : listeners) {
                    listener.actionPerformed(mockActionEvent);
                }
                return null;
            }
        }).when(mockLauncher).run(eq(STORAGE_START_ARGS), isA(Collection.class), anyBoolean());

        final boolean[] result = new boolean[1];
        serviceCommand.getNotifier().addActionListener(new ActionListener<ApplicationState>() {
            @SuppressWarnings("incomplete-switch")
            @Override
            public void actionPerformed(ActionEvent<ApplicationState> actionEvent) {
                switch (actionEvent.getActionId()) {
                    case FAIL:
                        result[0] = true;
                        break;
                    case START:
                        result[0] = false;
                        break;
                    case STOP:
                        result[0] = false;
                        break;
                }
            }
        });

        boolean exTriggered = false;
        try {
            serviceCommand.run(mockCommandContext);
        } catch (CommandException e) {
            exTriggered = true;
        }
        Assert.assertTrue(exTriggered);
        Assert.assertEquals("Test Exception\n", stdErrOut.toString());
        Assert.assertTrue("Agent expected to fire FAIL event", result[0]);

        verify(mockLauncher, times(1)).run(eq(STORAGE_START_ARGS), isA(Collection.class), anyBoolean());
        verify(mockLauncher, never()).run(eq(STORAGE_STOP_ARGS), anyBoolean());
        verify(mockLauncher, never()).run(eq(AGENT_ARGS), isA(Collection.class), anyBoolean());
        verify(mockActionEvent, times(1)).getActionId();
    }

    @Test(timeout=1000)
    public void testStorageFailStartUnknownPath()  throws CommandException {
        doAnswer(new Answer<Void>() {
            public Void answer(InvocationOnMock invocation) throws Throwable {
                Object[] args = invocation.getArguments();
                listeners = (Collection<ActionListener<ApplicationState>>)args[1];

                when(mockActionEvent.getActionId()).thenReturn(ApplicationState.FAIL);
                // Return a null payload in order to trigger unknown path
                when(mockActionEvent.getPayload()).thenReturn(null);

                for(ActionListener<ApplicationState> listener : listeners) {
                    listener.actionPerformed(mockActionEvent);
                }
                return null;
            }
        }).when(mockLauncher).run(eq(STORAGE_START_ARGS), isA(Collection.class), anyBoolean());

        final boolean[] result = new boolean[1];
        serviceCommand.getNotifier().addActionListener(new ActionListener<ApplicationState>() {
            @SuppressWarnings("incomplete-switch")
            @Override
            public void actionPerformed(ActionEvent<ApplicationState> actionEvent) {
                switch (actionEvent.getActionId()) {
                    case FAIL:
                        result[0] = true;
                        break;
                    case START:
                        result[0] = false;
                        break;
                    case STOP:
                        result[0] = false;
                        break;
                }
            }
        });

        boolean exTriggered = false;
        try {
            serviceCommand.run(mockCommandContext);
        } catch (CommandException e) {
            exTriggered = true;
        }
        Assert.assertTrue(exTriggered);
        Assert.assertEquals("Unexpected result from storage.\n", stdErrOut.toString());
        Assert.assertTrue("Agent expected to fire FAIL event", result[0]);

        verify(mockLauncher, times(1)).run(eq(STORAGE_START_ARGS), isA(Collection.class), anyBoolean());
        verify(mockLauncher, never()).run(eq(STORAGE_STOP_ARGS), anyBoolean());
        verify(mockLauncher, never()).run(eq(AGENT_ARGS), isA(Collection.class), anyBoolean());
        verify(mockActionEvent, times(1)).getActionId();
    }
    
    @Test
    public void testAgentStartFail()  throws CommandException {
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
        }).when(mockLauncher).run(eq(AGENT_ARGS), isA(Collection.class), anyBoolean());

        final boolean[] result = new boolean[1];
        serviceCommand.getNotifier().addActionListener(new ActionListener<ApplicationState>() {
            @SuppressWarnings("incomplete-switch")
            @Override
            public void actionPerformed(ActionEvent<ApplicationState> actionEvent) {
                switch (actionEvent.getActionId()) {
                    case FAIL:
                        result[0] = true;
                        break;
                    case START:
                        result[0] = false;
                        break;
                    case STOP:
                        result[0] = false;
                        break;
                }
            }
        });

        boolean exTriggered = false;
        try {
            serviceCommand.run(mockCommandContext);
        } catch (CommandException e) {
            exTriggered = true;
        }
        Assert.assertFalse(exTriggered);
        Assert.assertEquals("Thermostat agent failed to start. See logs for details.\n", stdErrOut.toString());
        Assert.assertTrue("Agent expected to fire FAIL event", result[0]);

        verify(mockLauncher, times(1)).run(eq(STORAGE_START_ARGS), isA(Collection.class), anyBoolean());
        verify(mockLauncher, times(1)).run(eq(STORAGE_STOP_ARGS), anyBoolean());
        verify(mockLauncher, times(1)).run(eq(AGENT_ARGS), isA(Collection.class), anyBoolean());
        verify(mockActionEvent, times(2)).getActionId();
    }

}

