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

package com.redhat.thermostat.agent.cli.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.whenNew;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.redhat.thermostat.agent.Agent;
import com.redhat.thermostat.agent.cli.internal.AgentApplication.ConfigurationCreator;
import com.redhat.thermostat.agent.command.ConfigurationServer;
import com.redhat.thermostat.agent.config.AgentStartupConfiguration;
import com.redhat.thermostat.backend.BackendRegistry;
import com.redhat.thermostat.common.ExitStatus;
import com.redhat.thermostat.common.LaunchException;
import com.redhat.thermostat.common.cli.Arguments;
import com.redhat.thermostat.common.cli.CommandContext;
import com.redhat.thermostat.common.cli.CommandException;
import com.redhat.thermostat.common.cli.DependencyServices;
import com.redhat.thermostat.common.utils.HostPortPair;
import com.redhat.thermostat.shared.config.InvalidConfigurationException;
import com.redhat.thermostat.shared.config.SSLConfiguration;
import com.redhat.thermostat.storage.core.Connection.ConnectionListener;
import com.redhat.thermostat.storage.core.Connection.ConnectionStatus;
import com.redhat.thermostat.storage.core.ConnectionException;
import com.redhat.thermostat.storage.core.DbService;
import com.redhat.thermostat.storage.core.DbServiceFactory;
import com.redhat.thermostat.storage.core.Storage;
import com.redhat.thermostat.storage.core.StorageCredentials;
import com.redhat.thermostat.storage.core.WriterID;
import com.redhat.thermostat.storage.dao.AgentInfoDAO;
import com.redhat.thermostat.storage.dao.BackendInfoDAO;
import com.redhat.thermostat.testutils.StubBundleContext;

@RunWith(PowerMockRunner.class)
public class AgentApplicationTest {

    private static final String COMMAND_CHANNLE_BIND_HOST = "test";
    private static final int COMMAND_CHANNEL_BIND_PORT = 10101;

    private StubBundleContext context;

    private ConfigurationServer configServer;
    private DbService dbService;
    private ConfigurationCreator configCreator;
    private ExitStatus exitStatus;
    private DbServiceFactory dbServiceFactory;
    private WriterID writerId;
    
    @Before
    public void setUp() throws InvalidConfigurationException {
        
        context = new StubBundleContext();
        
        AgentStartupConfiguration config = mock(AgentStartupConfiguration.class);
        when(config.getDBConnectionString()).thenReturn("test string; please ignore");
        HostPortPair hostPort = new HostPortPair(COMMAND_CHANNLE_BIND_HOST, COMMAND_CHANNEL_BIND_PORT);
        when(config.getConfigListenAddress()).thenReturn(hostPort);

        configCreator = mock(ConfigurationCreator.class);
        when(configCreator.create()).thenReturn(config);

        Storage storage = mock(Storage.class);
        context.registerService(Storage.class, storage, null);
        AgentInfoDAO agentInfoDAO = mock(AgentInfoDAO.class);
        context.registerService(AgentInfoDAO.class.getName(), agentInfoDAO, null);
        BackendInfoDAO backendInfoDAO = mock(BackendInfoDAO.class);
        context.registerService(BackendInfoDAO.class.getName(), backendInfoDAO, null);
        configServer = mock(ConfigurationServer.class);
        context.registerService(ConfigurationServer.class.getName(), configServer, null);
        dbServiceFactory = mock(DbServiceFactory.class);
        dbService = mock(DbService.class);
        writerId = mock(WriterID.class);
        when(dbServiceFactory.createDbService(anyString(), any(StorageCredentials.class), any(SSLConfiguration.class))).thenReturn(dbService);

        exitStatus = mock(ExitStatus.class);
    }

    @After
    public void tearDown() {
        context = null;
        configServer = null;
        dbService = null;
        configCreator = null;
        dbServiceFactory = null;
        exitStatus = null;
    }

    @Test
    public void testAgentStartup() throws CommandException, InterruptedException {
        final AgentApplication agent = new AgentApplication(context, exitStatus, writerId, mock(SSLConfiguration.class), new DependencyServices(), configCreator, dbServiceFactory);
        agent.setStorageCredentials(mock(StorageCredentials.class));
        final CountDownLatch latch = new CountDownLatch(1);
        final CommandException[] ce = new CommandException[1];
        final long timeoutMillis = 5000L;
        
        startAgentRunThread(timeoutMillis, agent, ce, latch);
        
        boolean ret = latch.await(timeoutMillis, TimeUnit.MILLISECONDS);
        if (ce[0] != null) {
            throw ce[0];
        }
        if (!ret) {
            fail("Timeout expired!");
        }
    }
    
    @Test
    public void testAgentStartupConnectFailure() throws CommandException, InterruptedException {
        final AgentApplication agent = new AgentApplication(context, exitStatus, writerId, mock(SSLConfiguration.class), new DependencyServices(), configCreator, dbServiceFactory);
        agent.setStorageCredentials(mock(StorageCredentials.class));
        
        Arguments args = mock(Arguments.class);
        final CommandContext commandContext = mock(CommandContext.class);
        when(commandContext.getArguments()).thenReturn(args);
        
        // Throw a ConnectionException when we try to connect to storage
        doThrow(new ConnectionException()).when(dbService).connect();
        
        agent.run(commandContext);
        
        // Ensure we shut down command channel server
        verify(configServer).stopListening();
    }
    
    /*
     * Having the PrepareForTest annotation on method level does not seem to
     * deadlock the test, which seems to be more or less reliably reproducible
     * if this annotation is at class level instead. Steps to reproduce the
     * deadlock is:
     * 1. Attach the PrepareForTest annotation to the class (over the test
     *    method)
     * 2. Run the test multiple times. 5-20 times seemed sufficient for me to
     *    make the deadlock show up. This deadlock does not seem to happen
     *    otherwise (can run up to 30 times head-to-head without deadlock).
     *    
     */
    @PrepareForTest({ AgentApplication.class })
    @SuppressWarnings("unchecked")
    @Test
    public void verifyBackendRegistryProblemsSetsExitStatus() throws Exception {
        whenNew(BackendRegistry.class).withParameterTypes(BundleContext.class)
                .withArguments(any(BundleContext.class))
                .thenThrow(InvalidSyntaxException.class);
        final AgentApplication agent = new AgentApplication(context,
                exitStatus, writerId,  mock(SSLConfiguration.class),
                mock(DependencyServices.class), configCreator, dbServiceFactory);
        try {
            agent.startAgent(null, null, null);
        } catch (RuntimeException e) {
            assertEquals(InvalidSyntaxException.class, e.getCause().getClass());
        }
        verify(exitStatus).setExitStatus(ExitStatus.EXIT_ERROR);
    }
    
    @PrepareForTest({ AgentApplication.class })
    @Test
    public void verifyAgentLaunchExceptionSetsExitStatus() throws Exception {
        whenNew(BackendRegistry.class).withParameterTypes(BundleContext.class)
                .withArguments(any(BundleContext.class))
                .thenReturn(mock(BackendRegistry.class));
        Agent mockAgent = mock(Agent.class);
        whenNew(Agent.class).withParameterTypes(BackendRegistry.class,
                AgentStartupConfiguration.class, Storage.class,
                AgentInfoDAO.class, BackendInfoDAO.class, WriterID.class).withArguments(
                any(BackendRegistry.class),
                any(AgentStartupConfiguration.class), any(Storage.class),
                any(AgentInfoDAO.class), any(BackendInfoDAO.class), 
                any(WriterID.class)).thenReturn(mockAgent);
        doThrow(LaunchException.class).when(mockAgent).start();
        final AgentApplication agent = new AgentApplication(context,
                exitStatus, writerId,  mock(SSLConfiguration.class),
                mock(DependencyServices.class), configCreator, dbServiceFactory);
        try {
            agent.startAgent(null, null, null);
        } catch (RuntimeException e) {
            fail("Should not have thrown RuntimeException");
        }
        verify(exitStatus).setExitStatus(ExitStatus.EXIT_ERROR);
    }

    private void startAgentRunThread(final long timoutMillis, final AgentApplication agent, final CommandException[] ce, final CountDownLatch latch) {
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

        // Run agent in a new thread so we can timeout on failure
        Thread t = new Thread(new Runnable() {
            
            @Override
            public void run() {
                // Finish when config server starts listening
                try {
                    doAnswer(new Answer<Void>() {

                        @Override
                        public Void answer(InvocationOnMock invocation) throws Throwable {
                            latch.countDown();
                            return null;
                        }
                    }).when(configServer).startListening(COMMAND_CHANNLE_BIND_HOST, COMMAND_CHANNEL_BIND_PORT);
                } catch (IOException e1) {
                    fail("a mock should not throw an exception");
                }
                
                try {
                    agent.run(commandContext);
                } catch (CommandException e) {
                    ce[0] = e;
                }
            }
        });
        
        t.start();
    }

}

