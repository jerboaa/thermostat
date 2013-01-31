/*
 * Copyright 2012, 2013 Red Hat, Inc.
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import com.redhat.thermostat.agent.cli.impl.AgentApplication;
import com.redhat.thermostat.agent.cli.impl.AgentApplication.ConfigurationCreator;
import com.redhat.thermostat.agent.command.ConfigurationServer;
import com.redhat.thermostat.agent.config.AgentStartupConfiguration;
import com.redhat.thermostat.common.cli.Arguments;
import com.redhat.thermostat.common.cli.CommandContext;
import com.redhat.thermostat.common.cli.CommandException;
import com.redhat.thermostat.common.config.InvalidConfigurationException;
import com.redhat.thermostat.storage.core.Connection.ConnectionListener;
import com.redhat.thermostat.storage.core.Connection.ConnectionStatus;
import com.redhat.thermostat.storage.core.DbService;
import com.redhat.thermostat.storage.core.DbServiceFactory;
import com.redhat.thermostat.storage.core.Storage;
import com.redhat.thermostat.storage.dao.AgentInfoDAO;
import com.redhat.thermostat.storage.dao.BackendInfoDAO;
import com.redhat.thermostat.testutils.StubBundleContext;

public class AgentApplicationTest {

    // TODO: Test i18nized versions when they come.

    private StubBundleContext context;

    private AgentApplication agent;
    private ConfigurationServer configServer;
    private DbService dbService;
    
    @Before
    public void setUp() throws InvalidConfigurationException {
        
        context = new StubBundleContext();
        
        AgentStartupConfiguration config = mock(AgentStartupConfiguration.class);
        when(config.getDBConnectionString()).thenReturn("test string; please ignore");

        ConfigurationCreator configCreator = mock(ConfigurationCreator.class);
        when(configCreator.create()).thenReturn(config);

        Storage storage = mock(Storage.class);
        context.registerService(Storage.class, storage, null);
        AgentInfoDAO agentInfoDAO = mock(AgentInfoDAO.class);
        context.registerService(AgentInfoDAO.class.getName(), agentInfoDAO, null);
        BackendInfoDAO backendInfoDAO = mock(BackendInfoDAO.class);
        context.registerService(BackendInfoDAO.class.getName(), backendInfoDAO, null);
        configServer = mock(ConfigurationServer.class);
        context.registerService(ConfigurationServer.class.getName(), configServer, null);
        DbServiceFactory dbServiceFactory = mock(DbServiceFactory.class);
        dbService = mock(DbService.class);
        when(dbServiceFactory.createDbService(anyString(), anyString(), anyString())).thenReturn(dbService);

        agent = new AgentApplication(context, configCreator, dbServiceFactory);
    }

    @After
    public void tearDown() {
        agent = null;
    }

    @Test
    public void testName() {
        String name = agent.getName();
        assertEquals("agent", name);
    }

    @Test
    public void testDescAndUsage() {
        assertNotNull(agent.getDescription());
        assertNotNull(agent.getUsage());
    }

    @Test
    public void testAgentStartup() throws CommandException, InterruptedException {
        final long timeoutMillis = 5000L;
        Arguments args = mock(Arguments.class);
        final CommandContext commandContext = mock(CommandContext.class);
        when(commandContext.getArguments()).thenReturn(args);
        
        // Immediately switch to CONNECTED state on dbService.connect
        final ArgumentCaptor<ConnectionListener> listenerCaptor = ArgumentCaptor.forClass(ConnectionListener.class);
        doNothing().when(dbService).addConnectionListener(listenerCaptor.capture());
        
        doAnswer(new Answer<Void>() {

            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                ConnectionListener listener = listenerCaptor.getValue();
                listener.changed(ConnectionStatus.CONNECTED);
                return null;
            }
            
        }).when(dbService).connect();

        final CountDownLatch latch = new CountDownLatch(1);
        
        final CommandException[] ce = new CommandException[1];
        // Run agent in a new thread so we can timeout on failure
        Thread t = new Thread(new Runnable() {
            
            @Override
            public void run() {
                // Finish when config server starts listening
                doAnswer(new Answer<Void>() {

                    @Override
                    public Void answer(InvocationOnMock invocation) throws Throwable {
                        latch.countDown();
                        return null;
                    }
                }).when(configServer).startListening(anyString());
                
                try {
                    agent.run(commandContext);
                } catch (CommandException e) {
                    ce[0] = e;
                }
            }
        });
        
        t.start();
        boolean ret = latch.await(timeoutMillis, TimeUnit.MILLISECONDS);
        if (ce[0] != null) {
            throw ce[0];
        }
        if (!ret) {
            fail("Timeout expired!");
        }
        
    }

}

