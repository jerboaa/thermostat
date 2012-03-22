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

import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import org.apache.commons.lang3.ObjectUtils;
import org.fest.swing.edt.GuiActionRunner;
import org.fest.swing.edt.GuiTask;
import org.fest.swing.fixture.FrameFixture;
import org.fest.swing.fixture.JTextComponentFixture;
import org.hamcrest.Description;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.ArgumentMatcher;
import org.mockito.InOrder;

import com.redhat.thermostat.client.ChangeableText;
import com.redhat.thermostat.client.SummaryPanelFacade;
import com.redhat.thermostat.client.UiFacadeFactory;
import com.redhat.thermostat.test.GUITest;

public class MainWindowTest {

    private static class PropertyChangeEventMatcher extends ArgumentMatcher<PropertyChangeEvent> {

        private PropertyChangeEvent event;

        private PropertyChangeEventMatcher(PropertyChangeEvent ev) {
            event = ev;
        }

        @Override
        public boolean matches(Object argument) {
            PropertyChangeEvent other = (PropertyChangeEvent) argument;
            return event.getSource() == other.getSource()
                    && ObjectUtils.equals(event.getPropertyName(), other.getPropertyName())
                    && ObjectUtils.equals(event.getNewValue(), other.getNewValue())
                    && ObjectUtils.equals(event.getOldValue(), other.getOldValue());
        }

        @Override
        public void describeTo(Description description) {
            super.describeTo(description);
            description.appendText(event.getSource() + ", " + event.getPropertyName() + ", " + event.getOldValue() + ", " + event.getNewValue());
        }
    }

    private FrameFixture frameFixture;
    private MainWindow window;
    private PropertyChangeListener l;

    @Before
    public void setUp() {

        SummaryPanelFacade summaryPanelFacade = mock(SummaryPanelFacade.class);
        when(summaryPanelFacade.getTotalConnectedAgents()).thenReturn(new ChangeableText("totalConnectedAgents"));
        when(summaryPanelFacade.getTotalConnectedVms()).thenReturn(new ChangeableText("connectedVms"));

        UiFacadeFactory uiFacadeFactory = mock(UiFacadeFactory.class);
        when(uiFacadeFactory.getSummaryPanel()).thenReturn(summaryPanelFacade);

        window = new MainWindow(uiFacadeFactory);
        l = mock(PropertyChangeListener.class);
        window.addViewPropertyListener(l);

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

        InOrder inOrder = inOrder(l);
        inOrder.verify(l).propertyChange(argThat(new PropertyChangeEventMatcher(new PropertyChangeEvent(window, MainWindow.ViewProperty.HOST_VM_TREE_FILTER.toString(), null, "t"))));
        inOrder.verify(l).propertyChange(argThat(new PropertyChangeEventMatcher(new PropertyChangeEvent(window, MainWindow.ViewProperty.HOST_VM_TREE_FILTER.toString(), null, "te"))));
        inOrder.verify(l).propertyChange(argThat(new PropertyChangeEventMatcher(new PropertyChangeEvent(window, MainWindow.ViewProperty.HOST_VM_TREE_FILTER.toString(), null, "tes"))));
        inOrder.verify(l).propertyChange(argThat(new PropertyChangeEventMatcher(new PropertyChangeEvent(window, MainWindow.ViewProperty.HOST_VM_TREE_FILTER.toString(), null, "test"))));
    }

    @Category(GUITest.class)
    @Test
    public void verifyThatCloseFiresShutdownEvent() {

        frameFixture.show();

        frameFixture.close();
        frameFixture.requireNotVisible();
        verify(l).propertyChange(argThat(new PropertyChangeEventMatcher(new PropertyChangeEvent(window, MainWindow.ViewProperty.SHUTDOWN.toString(), false, true))));
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
}
