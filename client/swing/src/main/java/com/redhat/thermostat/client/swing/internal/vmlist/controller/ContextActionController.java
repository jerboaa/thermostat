/*
 * Copyright 2012-2016 Red Hat, Inc.
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

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.SwingUtilities;

import com.redhat.thermostat.client.swing.internal.accordion.AccordionComponent;
import com.redhat.thermostat.client.swing.internal.vmlist.ReferenceProvider;
import com.redhat.thermostat.client.ui.ReferenceContextAction;
import com.redhat.thermostat.common.ActionEvent;
import com.redhat.thermostat.common.ActionListener;
import com.redhat.thermostat.common.ActionNotifier;
import com.redhat.thermostat.common.utils.LoggingUtils;
import com.redhat.thermostat.storage.core.Ref;

public class ContextActionController implements ActionListener<ContextHandler.ContextHandlerAction>{

    public enum ContextAction {
        SHOW_CONTEXT_MENU,
    }

    public static class Payload {
        public AccordionComponent component;
        public int x;
        public int y;
        public Ref ref;
    }
    
    private static final Logger logger = LoggingUtils.getLogger(ContextActionController.class);
    
    private ActionNotifier<ContextAction> notifier;
    
    private HashMap<Ref, MenuAction> actions;
    
    public ContextActionController() {
        notifier = new ActionNotifier<>(this);
        actions = new HashMap<>();
    }
    
    public void unregister(final AccordionComponent pane,
                           final ReferenceProvider provider)
    {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                MenuAction action = actions.remove(provider.getReference());
                pane.getUiComponent().removeMouseListener(action);
            }
        });
    }
    
    public void register(final AccordionComponent pane,
                         final ReferenceProvider provider)
    {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                MenuAction action = new MenuAction(pane, provider);
                pane.getUiComponent().addMouseListener(action);
                actions.put(provider.getReference(), action);
            }
        });
    }
    
    public void addContextActionListener(ActionListener<ContextAction> l) {
        notifier.addActionListener(l);
    }
    
    public void removeContextActionListener(ActionListener<ContextAction> l) {
        notifier.removeActionListener(l);
    }
    
    @Override
    public void actionPerformed(ActionEvent<ContextHandler.ContextHandlerAction> event) {
        ContextHandler.Payload payload = (ContextHandler.Payload) event.getPayload();
        try {
            ReferenceContextAction action = payload.action;
            action.execute(payload.reference);
        } catch (Throwable error) {
            logger.log(Level.SEVERE, "error invocating context action", error);
        }
    }
    
    private class MenuAction extends MouseAdapter {
        private AccordionComponent pane;
        private ReferenceProvider provider;
        
        public MenuAction(AccordionComponent pane, ReferenceProvider provider) {
            this.pane = pane;
            this.provider = provider;
        }
    
        @Override
        public void mousePressed(MouseEvent e) {
            if (e.isPopupTrigger()) {
                Payload payload = new Payload();
                payload.ref = provider.getReference();
                payload.component = pane;
                payload.x = e.getX();
                payload.y = e.getY();
                notifier.fireAction(ContextAction.SHOW_CONTEXT_MENU, payload);
            }
        }
    }
}

