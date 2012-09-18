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

package com.redhat.thermostat.client.ui;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Arrays;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.redhat.thermostat.common.appctx.ApplicationContext;
import com.redhat.thermostat.common.appctx.ApplicationContextUtil;
import com.redhat.thermostat.common.dao.AgentInfoDAO;
import com.redhat.thermostat.common.dao.BackendInfoDAO;
import com.redhat.thermostat.common.dao.DAOFactory;
import com.redhat.thermostat.common.model.AgentInformation;

public class AgentInformationDisplayModelTest {

    private AgentInformation agentInfo1 = new AgentInformation();
    private AgentInformation agentInfo2 = new AgentInformation();

    private DAOFactory daoFactory = mock(DAOFactory.class);
    private AgentInfoDAO agentInfoDao = mock(AgentInfoDAO.class);
    private BackendInfoDAO backendInfoDao = mock(BackendInfoDAO.class);

    @Before
    public void setUp() {
        ApplicationContextUtil.resetApplicationContext();

        when(daoFactory.getAgentInfoDAO()).thenReturn(agentInfoDao);
        when(daoFactory.getBackendInfoDAO()).thenReturn(backendInfoDao);

        agentInfo1.setAgentId("agent1-id");
        agentInfo1.setStartTime(0);
        agentInfo1.setStopTime(1);
        agentInfo1.setConfigListenAddress("config-address:port");
        agentInfo1.setAlive(false);

        agentInfo2.setAgentId("agent2-id");

        when(agentInfoDao.getAllAgentInformation()).thenReturn(Arrays.asList(agentInfo1));

        ApplicationContext.getInstance().setDAOFactory(daoFactory);
    }

    @After
    public void tearDown() {
        ApplicationContextUtil.resetApplicationContext();
    }

    @Test
    public void testModelInitializesItself() {
        AgentInformationDisplayModel model = new AgentInformationDisplayModel();

        AgentInformation agentInfoFromModel = model.getAgentInfo(agentInfo1.getAgentId());

        assertEquals(agentInfo1, agentInfoFromModel);
    }

    @Test
    public void testGetUnknownAgentInformation() {
        AgentInformationDisplayModel model = new AgentInformationDisplayModel();

        AgentInformation agentInfoFromModel = model.getAgentInfo("some unknown agent id");

        assertEquals(null, agentInfoFromModel);
    }

    @Test
    public void testChangesOnlyVisibleAfterRefresh() {
        AgentInformationDisplayModel model = new AgentInformationDisplayModel();
        AgentInformation agentInfoFromModel;

        agentInfoFromModel = model.getAgentInfo(agentInfo1.getAgentId());

        assertEquals(agentInfo1, agentInfoFromModel);

        when(agentInfoDao.getAllAgentInformation()).thenReturn(Arrays.asList(agentInfo2));

        agentInfoFromModel = model.getAgentInfo(agentInfo1.getAgentId());

        assertEquals(agentInfo1, agentInfoFromModel);

        model.refresh();

        agentInfoFromModel = model.getAgentInfo(agentInfo1.getAgentId());

        assertEquals(null, agentInfoFromModel);

    }
}
