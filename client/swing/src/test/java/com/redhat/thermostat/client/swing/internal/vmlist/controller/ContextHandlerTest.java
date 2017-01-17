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

package com.redhat.thermostat.client.swing.internal.vmlist.controller;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyInt;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.doNothing;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.awt.Component;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JComponent;
import javax.swing.JMenuItem;
import javax.swing.RepaintManager;
import javax.swing.SwingUtilities;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import com.redhat.thermostat.client.swing.components.ThermostatPopupMenu;
import com.redhat.thermostat.client.swing.internal.ContextActionServiceTracker;
import com.redhat.thermostat.client.swing.internal.accordion.AccordionComponent;
import com.redhat.thermostat.client.swing.internal.vmlist.controller.ContextHandler.ContextHandlerAction;
import com.redhat.thermostat.client.swing.internal.vmlist.controller.ContextHandler.Payload;
import com.redhat.thermostat.client.ui.ReferenceContextAction;
import com.redhat.thermostat.client.ui.ReferenceFilter;
import com.redhat.thermostat.common.ActionEvent;
import com.redhat.thermostat.common.Filter;
import com.redhat.thermostat.shared.locale.LocalizedString;
import com.redhat.thermostat.storage.core.HostRef;
import com.redhat.thermostat.storage.core.Ref;

/**
 *
 */
public class ContextHandlerTest {

    private ContextActionServiceTracker contextActionTracker;
    private ActionEvent actionEvent;
    
    @BeforeClass
    public static void setUpOnce() {
        // This is needed because some other test may have installed the
        // EDT violation checker repaint manager.
        RepaintManager.setCurrentManager(new RepaintManager());
    }
    
    private void waitForSwing() {
        try {
            SwingUtilities.invokeAndWait(new Runnable() {
                @Override
                public void run() {
                    // just wait :)
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private class TestActionListener implements com.redhat.thermostat.common.ActionListener<ContextHandlerAction> {
        
        ActionEvent<ContextHandlerAction> actionEvent;
        
        @Override
        public void actionPerformed(ActionEvent<ContextHandlerAction> actionEvent) {
            this.actionEvent = actionEvent;
        }
    }
    
    @Before
    public void setUp() {
        contextActionTracker = mock(ContextActionServiceTracker.class);
        actionEvent = mock(ActionEvent.class);
    }
    
    @Test
    public void testFilterEvaluatedOnlyIfApplies() {
        
        final ThermostatPopupMenu popup = mock(ThermostatPopupMenu.class);
        
        ContextActionController.Payload payload =
                new ContextActionController.Payload();
        
        HostRef host0 = new HostRef("0", "0");
        
        payload.ref = host0;
        
        ReferenceFilter hostFilter0 = mock(ReferenceFilter.class);
        when(hostFilter0.applies(host0)).thenReturn(false);
        when(hostFilter0.matches(host0)).thenReturn(true);
        
        ReferenceFilter hostFilter1 = mock(ReferenceFilter.class);
        when(hostFilter1.applies(host0)).thenReturn(true);
        when(hostFilter1.matches(host0)).thenReturn(false);
        
        ReferenceContextAction hostAction0 = mock(ReferenceContextAction.class);
        when(hostAction0.getFilter()).thenReturn(hostFilter0);
        
        ReferenceContextAction hostAction1 = mock(ReferenceContextAction.class);
        when(hostAction1.getFilter()).thenReturn(hostFilter1);
        
        when(actionEvent.getPayload()).thenReturn(payload);
        
        List<ReferenceContextAction> hostActions = new ArrayList<>();
        hostActions.add(hostAction0);
        hostActions.add(hostAction1);
        
        when(contextActionTracker.getActions()).thenReturn(hostActions);
        
        ContextHandler handler = new ContextHandler(contextActionTracker) {
            @Override
            ThermostatPopupMenu createContextPopMenu() {
                return popup;
            }
        };
        
        handler.actionPerformed(actionEvent);
        waitForSwing();

        verify(hostFilter0).applies(host0);
        verify(hostFilter0, times(0)).matches(host0);

        verifyNoMoreInteractions(hostFilter0);
        
        verify(hostFilter1).applies(host0);
        verify(hostFilter1).matches(host0);
    }
    
    @Test
    public void testActions() {
        
        ArgumentCaptor<ActionListener> captor0 = ArgumentCaptor.forClass(ActionListener.class);
        ArgumentCaptor<ActionListener> captor1 = ArgumentCaptor.forClass(ActionListener.class);
        
        final int[] invocations = new int[2]; 
        
        final ThermostatPopupMenu popup = mock(ThermostatPopupMenu.class);
        final JMenuItem menuItem0 = mock(JMenuItem.class);
        final JMenuItem menuItem1 = mock(JMenuItem.class);

        doNothing().when(menuItem0).addActionListener(captor0.capture());
        doNothing().when(menuItem1).addActionListener(captor1.capture());
        
        final JMenuItem[] items = new JMenuItem[2]; 
        items[0] = menuItem0;
        items[1] = menuItem1;
        
        ContextActionController.Payload payload = new ContextActionController.Payload();
        when(actionEvent.getPayload()).thenReturn(payload);
        
        HostRef host0 = new HostRef("0", "0");
        payload.ref = host0;
        
        JComponent accordionComponent = mock(JComponent.class);
        AccordionComponent accordion = mock(AccordionComponent.class);
        when(accordion.getUiComponent()).thenReturn(accordionComponent);
        payload.component = accordion;
        payload.x = 10;
        payload.y = 10;

        TestActionListener testListener = new TestActionListener();
        
        ContextHandler handler = new ContextHandler(contextActionTracker) {
            @Override
            ThermostatPopupMenu createContextPopMenu() {
                invocations[0]++;
                return popup;
            }
            @Override
            JMenuItem createContextMenuItem() {
                return items[invocations[1]++ % 2];
            }    
        };
        handler.addContextHandlerActionListener(testListener);
        
        // *** test no action
        
        handler.actionPerformed(actionEvent);
        
        waitForSwing();
        
        assertEquals(1, invocations[0]);
        assertEquals(0, invocations[1]);
        
        verify(contextActionTracker).getActions();
        
        // verify the popup is built from scratch
        verify(popup).removeAll();

        verify(popup, times(0)).show(any(Component.class), anyInt(), anyInt());
        
        // *** test two actions, no filter
        
        // no reason to change the event, but add actions
        ReferenceFilter hostFilter = mock(ReferenceFilter.class);
        when(hostFilter.matches(host0)).thenReturn(true).thenReturn(true);
        when(hostFilter.applies(host0)).thenReturn(true).thenReturn(true);

        LocalizedString name0 = new LocalizedString("actionName0");
        LocalizedString des0 = new LocalizedString("actionDesc0");

        ReferenceContextAction hostAction0 = mock(ReferenceContextAction.class);
        when(hostAction0.getFilter()).thenReturn(hostFilter);
        when(hostAction0.getName()).thenReturn(name0);
        when(hostAction0.getDescription()).thenReturn(des0);

        LocalizedString name1 = new LocalizedString("actionName1");
        LocalizedString des1 = new LocalizedString("actionDesc1");
        
        ReferenceContextAction hostAction1 = mock(ReferenceContextAction.class);
        when(hostAction1.getFilter()).thenReturn(hostFilter);
        when(hostAction1.getName()).thenReturn(name1);
        when(hostAction1.getDescription()).thenReturn(des1);
        
        List<ReferenceContextAction> hostActions = new ArrayList<>();
        hostActions.add(hostAction0);
        hostActions.add(hostAction1);
        
        when(contextActionTracker.getActions()).thenReturn(hostActions);
        
        handler.actionPerformed(actionEvent);
        waitForSwing();

        assertEquals(1, invocations[0]);
        assertEquals(2, invocations[1]);
     
        verify(popup, times(2)).removeAll();

        verify(popup).add(menuItem0);
        verify(popup).add(menuItem1);

        verify(popup, times(1)).show(accordionComponent, 10, 10);

        verifyNoMoreInteractions(popup);
        
        verify(menuItem0).setText(name0.getContents());
        verify(menuItem1).setText(name1.getContents());
        
        verify(menuItem0).setToolTipText(des0.getContents());
        verify(menuItem1).setToolTipText(des1.getContents());
        
        // *** check that we are notified for the correct action
        
        ActionListener l0 = captor0.getValue();
        ActionListener l1 = captor1.getValue();
        
        java.awt.event.ActionEvent fakeEvent = mock(java.awt.event.ActionEvent.class);
        l0.actionPerformed(fakeEvent);
        
        assertNotNull(testListener.actionEvent);
        
        Payload eventPayload = (Payload) testListener.actionEvent.getPayload();
        assertNotNull(eventPayload);
        assertEquals(host0, eventPayload.reference);
        assertEquals(hostAction0, eventPayload.action);

        l1.actionPerformed(fakeEvent);
        assertNotNull(testListener.actionEvent);

        eventPayload = (Payload) testListener.actionEvent.getPayload();
        assertNotNull(eventPayload);
        assertEquals(host0, eventPayload.reference);
        assertEquals(hostAction1, eventPayload.action);
        
        // *** now test again with one filtering
        
        invocations[1] = 0;
        
        when(hostFilter.matches(host0)).thenReturn(false).thenReturn(true);
        handler.actionPerformed(actionEvent);
        waitForSwing();
        
        assertEquals(1, invocations[0]);
        assertEquals(1, invocations[1]);
        
        // TODO: the numbers here take into account the previous
        // invocation this should really be a separate test, but there's too
        // much stubbing to duplicate, consider refactoring the test
        
        verify(popup, times(3)).removeAll();

        verify(popup, times(2)).add(menuItem0);
        verify(popup, times(2)).show(accordionComponent, 10, 10);

        verifyNoMoreInteractions(popup);
        
        verify(menuItem0).setText(name0.getContents());
        verify(menuItem0).setToolTipText(des0.getContents());
    }    
}

