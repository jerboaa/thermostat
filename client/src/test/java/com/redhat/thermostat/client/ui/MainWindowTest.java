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
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.fest.swing.annotation.GUITest;
import org.fest.swing.edt.GuiActionRunner;
import org.fest.swing.edt.GuiTask;
import org.fest.swing.fixture.FrameFixture;
import org.fest.swing.fixture.JMenuItemFixture;
import org.fest.swing.fixture.JTextComponentFixture;
import org.fest.swing.junit.v4_5.runner.GUITestRunner;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;

import com.redhat.thermostat.client.ChangeableText;
import com.redhat.thermostat.client.MainView;
import com.redhat.thermostat.client.SummaryPanelFacade;
import com.redhat.thermostat.client.UiFacadeFactory;
import com.redhat.thermostat.common.ActionEvent;
import com.redhat.thermostat.common.ActionListener;

@RunWith(GUITestRunner.class)
public class MainWindowTest {

    private FrameFixture frameFixture;
    private MainWindow window;
    private ActionListener<MainView.Action> l;

    @SuppressWarnings("unchecked") // mock(ActionListener.class)
    @Before
    public void setUp() {

        SummaryPanelFacade summaryPanelFacade = mock(SummaryPanelFacade.class);
        when(summaryPanelFacade.getTotalMonitoredHosts()).thenReturn(new ChangeableText("totalConnectedAgents"));
        when(summaryPanelFacade.getTotalMonitoredVms()).thenReturn(new ChangeableText("connectedVms"));

        UiFacadeFactory uiFacadeFactory = mock(UiFacadeFactory.class);
        when(uiFacadeFactory.getSummaryPanel()).thenReturn(summaryPanelFacade);

        window = new MainWindow(uiFacadeFactory);
        l = mock(ActionListener.class);
        window.addActionListener(l);

        frameFixture = new FrameFixture(window);
    }

    @After
    public void tearDown() {
        frameFixture.cleanUp();
        frameFixture = null;
        window = null;
        l = null;
    }

    @Category(GUITest.class)
    @Test
    public void testHostVMTreeFilterPropertySupport() {
        frameFixture.show();
        JTextComponentFixture hostVMTreeFilterField = frameFixture.textBox("hostVMTreeFilter");
        hostVMTreeFilterField.enterText("test");

        verify(l, times(4)).actionPerformed(new ActionEvent<MainView.Action>(window, MainView.Action.HOST_VM_TREE_FILTER));
    }

    @Category(GUITest.class)
    @Test
    public void verifyThatCloseFiresShutdownEvent() {

        frameFixture.show();

        frameFixture.close();
        frameFixture.requireNotVisible();
        verify(l).actionPerformed(new ActionEvent<MainView.Action>(window, MainView.Action.SHUTDOWN));
    }

    @Category(GUITest.class)
    @Test
    public void verifyShowMainWindowShowsWindow() {
        GuiActionRunner.execute(new GuiTask() {

            @Override
            protected void executeInEDT() throws Throwable {
                window.showMainWindow();
            }
        });
        frameFixture.requireVisible();
    }

    @Category(GUITest.class)
    @Test
    public void verifyThatClientPreferencesMenuItemTriggersEvent() {
        frameFixture.show();
        JMenuItemFixture menuItem = frameFixture.menuItem("showClientConfig");
        menuItem.click();
        frameFixture.close();
        frameFixture.requireNotVisible();

        verify(l).actionPerformed(new ActionEvent<MainView.Action>(window, MainView.Action.SHOW_CLIENT_CONFIG));

    }

    @Category(GUITest.class)
    @Test
    public void verifyThatHistorySwitchTriggersEvent() {
        frameFixture.show();
        JMenuItemFixture menuItem = frameFixture.menuItem("historyModeSwitch");
        menuItem.click();
        frameFixture.close();
        frameFixture.requireNotVisible();

        verify(l).actionPerformed(new ActionEvent<MainView.Action>(window, MainView.Action.SWITCH_HISTORY_MODE));
    }
    
    @Category(GUITest.class)
    @Test
    public void testGetHostVMTreeFilter() {
        frameFixture.show();
        JTextComponentFixture hostVMTreeFilterField = frameFixture.textBox("hostVMTreeFilter");
        hostVMTreeFilterField.enterText("test");

        assertEquals("test", window.getHostVmTreeFilter());
    }


}
