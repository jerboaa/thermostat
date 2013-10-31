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

package com.redhat.thermostat.client.swing.internal.vmlist.controller;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertEquals;

import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import javax.swing.JComponent;
import javax.swing.RepaintManager;
import javax.swing.SwingUtilities;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import com.redhat.thermostat.client.swing.internal.accordion.AccordionComponent;
import com.redhat.thermostat.client.swing.internal.vmlist.ReferenceProvider;
import com.redhat.thermostat.client.swing.internal.vmlist.controller.ContextActionController.ContextAction;
import com.redhat.thermostat.client.swing.internal.vmlist.controller.ContextActionController.Payload;
import com.redhat.thermostat.client.ui.ReferenceContextAction;
import com.redhat.thermostat.common.ActionEvent;
import com.redhat.thermostat.common.ActionListener;
import com.redhat.thermostat.storage.core.HostRef;

public class ContextActionControllerTest {

    private JComponent jcomponent;
    private AccordionComponent component;
    private ReferenceProvider provider;
    
    @BeforeClass
    public static void setUpOnce() {
        // This is needed because some other test may have installed the
        // EDT violation checker repaint manager.
        RepaintManager.setCurrentManager(new RepaintManager());
    }
    
    @Before
    public void setUp() {
        component = mock(AccordionComponent.class);
        provider = mock(ReferenceProvider.class);
        jcomponent = mock(JComponent.class);
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
    
    @Test
    public void testRegister() {
        
        ArgumentCaptor<MouseListener> captor = ArgumentCaptor.forClass(MouseListener.class);
        MouseEvent e = mock(MouseEvent.class);

        when(e.getX()).thenReturn(5);
        when(e.getY()).thenReturn(10);
        
        HostRef ref = new HostRef("0", "0");
        
        when(e.isPopupTrigger()).thenReturn(true);
        when(component.getUiComponent()).thenReturn(jcomponent);
        when(provider.getReference()).thenReturn(ref);
        doNothing().when(jcomponent).addMouseListener(captor.capture());
        
        final ContextActionController.Payload[] payload = new ContextActionController.Payload[1]; 
        ActionListener<ContextActionController.ContextAction> l =
                new ActionListener<ContextActionController.ContextAction>() {
            @Override
            public void actionPerformed(ActionEvent<ContextAction> actionEvent) {
                payload[0] = (Payload) actionEvent.getPayload();
            }
        };
        
        ContextActionController controller = new ContextActionController();
        controller.register(component, provider);
        controller.addContextActionListener(l);
        
        waitForSwing();
        
        MouseListener mouseListener = captor.getValue();
        mouseListener.mousePressed(e);
        
        assertNotNull(payload[0]);
        assertEquals(ref, payload[0].ref);
        assertEquals(component, payload[0].component);
        assertEquals(5, payload[0].x);
        assertEquals(10, payload[0].y);
    }
    
    @Test
    public void testExecute() {
        ContextActionController controller = new ContextActionController();
        
        ContextHandler.Payload payload = new ContextHandler.Payload();
        payload.action = mock(ReferenceContextAction.class);
        payload.reference = new HostRef("0", "0");

        ActionEvent<ContextHandler.ContextHandlerAction> event =
                new ActionEvent<ContextHandler.ContextHandlerAction>(this,
                        ContextHandler.ContextHandlerAction.ACTION_PERFORMED);
        event.setPayload(payload);
        
        controller.actionPerformed(event);
        
        verify(payload.action).execute(payload.reference);
    }
}
