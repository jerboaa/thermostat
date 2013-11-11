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

import java.util.List;

import javax.swing.JMenuItem;
import javax.swing.SwingUtilities;

import com.redhat.thermostat.client.swing.components.ThermostatPopupMenu;
import com.redhat.thermostat.client.swing.internal.osgi.ContextActionServiceTracker;
import com.redhat.thermostat.client.ui.ReferenceContextAction;
import com.redhat.thermostat.client.ui.ReferenceFilter;
import com.redhat.thermostat.common.ActionEvent;
import com.redhat.thermostat.common.ActionListener;
import com.redhat.thermostat.common.ActionNotifier;
import com.redhat.thermostat.storage.core.Ref;

/**
 *
 */
public class ContextHandler implements ActionListener<ContextActionController.ContextAction> {

    public enum ContextHandlerAction {
        ACTION_PERFORMED,
    }

    public static class Payload {
        ReferenceContextAction action;
        Ref reference;
    }
    
    private ActionNotifier<ContextHandlerAction> notifier;
    
    private ThermostatPopupMenu contextMenu;
    
    private ContextActionServiceTracker contextActionTracker;
    
    public ContextHandler(ContextActionServiceTracker contextActionTracker) {
        this.contextActionTracker = contextActionTracker;
        notifier = new ActionNotifier<>(this);
    }

    @Override
    public void actionPerformed(final ActionEvent<ContextActionController.ContextAction> actionEvent) {
        SwingUtilities.invokeLater(new Runnable() {
            
            @Override
            public void run() {
                if (contextMenu == null) {
                    contextMenu = createContextPopMenu();
                }
                
                final ContextActionController.Payload payload =
                        (ContextActionController.Payload) actionEvent.getPayload();
                
                List<? extends ReferenceContextAction> actions =
                        contextActionTracker.getActions();
                contextMenu.removeAll();
                
                boolean showPopup = false;
                for (final ReferenceContextAction action : actions) {
                    ReferenceFilter filter = action.getFilter();
                    if (filter.applies(payload.ref) && filter.matches(payload.ref))  {
                     
                        showPopup = true;
                        
                        JMenuItem contextAction = createContextMenuItem();
                        contextAction.setText(action.getName().getContents());
                        contextAction.setToolTipText(action.getDescription().getContents());
                        contextAction.addActionListener(new java.awt.event.ActionListener() {
                            @Override
                            public void actionPerformed(java.awt.event.ActionEvent e) {
                                Payload actionPayload = new Payload();
                                actionPayload.reference = payload.ref;
                                actionPayload.action = action;
                                
                                notifier.fireAction(ContextHandlerAction.ACTION_PERFORMED, actionPayload);
                            }
                        });
                        
                        // the component name is for unit tests only
                        contextAction.setName(action.getName().getContents());
                        
                        contextMenu.add(contextAction);
                    }
                }
                
                if (showPopup) {
                    contextMenu.show(payload.component.getUiComponent(), payload.x, payload.y);
                }
            }
        });
    }
    
    // allow us to inject a mock for testing 
    ThermostatPopupMenu createContextPopMenu() {
        return new ThermostatPopupMenu();
    }
    
    // allow us to inject a mock for testing 
    JMenuItem createContextMenuItem() {
        return new JMenuItem();
    }
    
    public void addContextHandlerActionListener(ActionListener<ContextHandlerAction> l) {
        notifier.addActionListener(l);
    }
    
    public void removeContextHandlerActionListener(ActionListener<ContextHandlerAction> l) {
        notifier.removeActionListener(l);
    }
}
