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

package com.redhat.thermostat.backend;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.redhat.thermostat.agent.config.AgentStartupConfiguration;
import com.redhat.thermostat.common.appctx.ApplicationContext;
import com.redhat.thermostat.common.config.InvalidConfigurationException;
import com.redhat.thermostat.common.dao.DAOFactory;
import com.redhat.thermostat.common.storage.Category;
import com.redhat.thermostat.common.storage.Storage;

public class BackendRegistryTest {

    public static class MockBackend extends Backend {
        public MockBackend() {
            super();
        }

        @Override
        protected Collection<Category> getCategories() {
            return Collections.emptyList();
        }
        @Override
        public String getConfigurationValue(String key) {
            return null;
        }
        @Override
        public boolean activate() {
            return true;
        }
        @Override
        public boolean deactivate() {
            return false;
        }
        @Override
        public boolean isActive() {
            return false;
        }
        @Override
        public boolean attachToNewProcessByDefault() {
            return false;
        }

        @Override
        protected void setDAOFactoryAction() {
            // TODO Auto-generated method stub
            
        }
    }

    private List<BackendID> backends;
    private AgentStartupConfiguration config;
    private BackendConfigurationLoader configLoader;

    @Before
    public void setUp() throws InvalidConfigurationException {
        backends = new ArrayList<>();

        config = mock(AgentStartupConfiguration.class);
        when(config.getBackends()).thenReturn(backends);

        configLoader = mock(BackendConfigurationLoader.class);
        when(configLoader.retrieveBackendConfigs(any(String.class))).thenReturn(new HashMap<String,String>());

        Storage storage = mock(Storage.class);
        DAOFactory df = mock(DAOFactory.class);
        when(df.getStorage()).thenReturn(storage);
        ApplicationContext.getInstance().setDAOFactory(df);
    }

    @After
    public void tearDown() {
        backends = null;
        config = null;
        configLoader = null;
    }

    @Test
    public void testRegisterBackend() throws BackendLoadException, InvalidConfigurationException {
        /* setup fake backend */
        backends.add(new BackendID("mock", MockBackend.class.getName()));

        BackendRegistry registry = new BackendRegistry(config, configLoader);
        assertEquals(1, registry.getAll().size());
        assertNotNull(registry.getByName("mock"));
    }

    @Test
    public void testNoBackendsRegistered() throws InvalidConfigurationException, BackendLoadException {
        BackendRegistry registry = new BackendRegistry(config, configLoader);
        assertEquals(0, registry.getAll().size());
        assertEquals(null, registry.getByName("system"));
        assertEquals(null, registry.getByName("mock"));
    }

    @Test (expected=BackendLoadException.class)
    public void testRegisterBackendTwice() throws BackendLoadException, InvalidConfigurationException {
        /* setup fake backends */

        backends.add(new BackendID("mock", MockBackend.class.getClass().getName()));
        backends.add(new BackendID("mock", MockBackend.class.getClass().getName()));

        /* load the backends */
        new BackendRegistry(config, configLoader);
    }

}
