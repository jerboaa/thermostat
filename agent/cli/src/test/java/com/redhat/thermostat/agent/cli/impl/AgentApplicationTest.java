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

package com.redhat.thermostat.agent.cli.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import com.redhat.thermostat.agent.cli.impl.AgentApplication;
import com.redhat.thermostat.agent.cli.impl.AgentApplication.ConfigurationCreator;
import com.redhat.thermostat.agent.cli.impl.AgentApplication.DAOFactoryCreator;
import com.redhat.thermostat.agent.config.AgentStartupConfiguration;
import com.redhat.thermostat.common.cli.Arguments;
import com.redhat.thermostat.common.cli.CommandContext;
import com.redhat.thermostat.common.cli.CommandException;
import com.redhat.thermostat.common.config.InvalidConfigurationException;
import com.redhat.thermostat.common.dao.DAOFactory;
import com.redhat.thermostat.storage.core.Connection;
import com.redhat.thermostat.storage.core.Connection.ConnectionListener;
import com.redhat.thermostat.storage.core.Connection.ConnectionStatus;
import com.redhat.thermostat.test.StubBundleContext;

public class AgentApplicationTest {

    // TODO: Test i18nized versions when they come.

    private StubBundleContext context;
    private DAOFactory daoFactory;
    private Connection connection;

    private AgentApplication agent;

    private ArgumentCaptor<ConnectionListener> listenerCaptor;
    
    @Before
    public void setUp() throws InvalidConfigurationException {
        
        context = new StubBundleContext();
        
        AgentStartupConfiguration config = mock(AgentStartupConfiguration.class);
        when(config.getDBConnectionString()).thenReturn("test string; please ignore");

        ConfigurationCreator configCreator = mock(ConfigurationCreator.class);
        when(configCreator.create()).thenReturn(config);

        listenerCaptor = ArgumentCaptor.forClass(ConnectionListener.class);

        connection = mock(Connection.class);
        doNothing().when(connection).addListener(listenerCaptor.capture());

        daoFactory = mock(DAOFactory.class);
        when(daoFactory.getConnection()).thenReturn(connection);

        DAOFactoryCreator daoCreator = mock(DAOFactoryCreator.class);
        when(daoCreator.create(config)).thenReturn(daoFactory);

        agent = new AgentApplication(context, configCreator, daoCreator);
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
    public void testAgentStartupRegistersDAOs() throws CommandException {

        doThrow(new ThatsAllThatWeCareAbout()).when(connection).connect();

        Arguments args = mock(Arguments.class);
        CommandContext commandContext = mock(CommandContext.class);

        when(commandContext.getArguments()).thenReturn(args);

        try {
            agent.run(commandContext);
            fail("not supposed to get here");
        } catch (ThatsAllThatWeCareAbout done) {
            // agent.run() is so convoluted that we just want to test a part of
            // it
        }

        ConnectionListener listener = listenerCaptor.getValue();
        listener.changed(ConnectionStatus.CONNECTED);

        verify(daoFactory).registerDAOsAndStorageAsOSGiServices();
    }

    // FIXME test the rest of AgentApplication

    private static class ThatsAllThatWeCareAbout extends RuntimeException {

    }
}
