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

import static org.junit.Assert.assertArrayEquals;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import net.java.openjdk.cacio.ctc.junit.CacioFESTRunner;

import org.fest.swing.edt.GuiActionRunner;
import org.fest.swing.edt.GuiQuery;
import org.fest.swing.edt.GuiTask;
import org.fest.swing.fixture.FrameFixture;
import org.fest.swing.fixture.JListFixture;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.redhat.thermostat.common.ActionEvent;
import com.redhat.thermostat.common.ActionListener;

@RunWith(CacioFESTRunner.class)
public class AgentConfigurationFrameTest {

    private AgentConfigurationFrame agentConfigFrame;
    private FrameFixture fixture;
    private ActionListener<AgentConfigurationView.ConfigurationAction> l;

    @Before
    public void setUp() {
        agentConfigFrame = GuiActionRunner.execute(new GuiQuery<AgentConfigurationFrame>() {

            @Override
            protected AgentConfigurationFrame executeInEDT() throws Throwable {
                 return new AgentConfigurationFrame();
            }
        });

        @SuppressWarnings("unchecked")
        ActionListener<AgentConfigurationView.ConfigurationAction> listener = mock(ActionListener.class);
        l = listener;
        agentConfigFrame.addActionListener(l);

        fixture = new FrameFixture(agentConfigFrame);
    }

    @After
    public void tearDown() {
        GuiActionRunner.execute(new GuiTask() {
            @Override
            protected void executeInEDT() throws Throwable {
                agentConfigFrame.hideDialog();
            }
        });

        fixture.requireNotVisible();
        agentConfigFrame.removeActionListener(l);

        fixture.cleanUp();
        fixture = null;
    }

    @Test
    public void testAddingAgentWorks() {
        fixture.show();
        JListFixture list = fixture.list("agentList");
        assertArrayEquals(new String[0], list.contents());

        agentConfigFrame.addAgent("test-agent");

        assertArrayEquals(new String[] {"test-agent"}, list.contents());
    }

    @Test
    public void testSelectingAgentWorks() {
        fixture.show();
        agentConfigFrame.addAgent("testAgent");
        JListFixture list = fixture.list("agentList");

        list.selectItem("testAgent");

        verify(l).actionPerformed(eq(new ActionEvent<>(agentConfigFrame, AgentConfigurationView.ConfigurationAction.SWITCH_AGENT)));
    }

    @Test
    public void testRemovingAllAgentsWorks() {
        fixture.show();
        agentConfigFrame.addAgent("test-agent");
        JListFixture list = fixture.list("agentList");

        agentConfigFrame.clearAllAgents();

        assertArrayEquals(new String[0], list.contents());
    }

    @Test
    public void testWindowClose() {
        fixture.show();

        fixture.close();

        verify(l).actionPerformed(eq(new ActionEvent<>(agentConfigFrame, AgentConfigurationView.ConfigurationAction.CLOSE_CANCEL)));
    }

}
