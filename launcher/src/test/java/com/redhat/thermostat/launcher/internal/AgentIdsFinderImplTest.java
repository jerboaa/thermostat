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

package com.redhat.thermostat.launcher.internal;

import com.redhat.thermostat.common.cli.CompletionInfo;
import com.redhat.thermostat.storage.dao.AgentInfoDAO;
import com.redhat.thermostat.storage.model.AgentInformation;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class AgentIdsFinderImplTest {

    private AgentIdsFinderImpl finder;

    @Before
    public void setup() {
        finder = new AgentIdsFinderImpl();
    }

    @Test
    public void testFindIds() {
        AgentInfoDAO agentInfoDAO = mock(AgentInfoDAO.class);
        finder.bindAgentInfoDao(agentInfoDAO);

        String id1 = "012345-56789";
        String id2 = "111111-22222";
        String id3 = "98765-543210";
        String id4 = "abcdef-01234564-848156";
        AgentInformation agentInfo1 = mock(AgentInformation.class);
        agentInfo1.setAgentId(id1);
        AgentInformation agentInfo2 = mock(AgentInformation.class);
        agentInfo2.setAgentId(id2);
        AgentInformation agentInfo3 = mock(AgentInformation.class);
        agentInfo3.setAgentId(id3);
        AgentInformation agentInfo4 = mock(AgentInformation.class);
        agentInfo4.setAgentId(id4);

        Collection<AgentInformation> collection = new ArrayList<>();
        collection.add(agentInfo1);
        collection.add(agentInfo2);
        collection.add(agentInfo3);
        collection.add(agentInfo4);
        when(agentInfoDAO.getAllAgentInformation()).thenReturn((List<AgentInformation>) collection);

        List<CompletionInfo> result = finder.findCompletions();
        assertEquals(4, result.size());
        assertEquals(id1, result.get(0).getActualCompletion());
        assertEquals(id2, result.get(1).getActualCompletion());
        assertEquals(id3, result.get(2).getActualCompletion());
        assertEquals(id4, result.get(3).getActualCompletion());
    }

}
