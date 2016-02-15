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

import java.util.HashMap;

import com.redhat.thermostat.client.swing.components.Icon;
import com.redhat.thermostat.client.swing.internal.vmlist.ReferenceComponent;
import com.redhat.thermostat.client.swing.internal.vmlist.ReferenceTitle;
import com.redhat.thermostat.client.swing.internal.vmlist.controller.DecoratorNotifier.DecorationEvent;
import com.redhat.thermostat.client.ui.ReferenceFieldIconDecorator;
import com.redhat.thermostat.client.ui.ReferenceFieldLabelDecorator;
import com.redhat.thermostat.common.ActionEvent;
import com.redhat.thermostat.common.ActionListener;
import com.redhat.thermostat.storage.core.Ref;

import javax.swing.SwingUtilities;

public class DecoratorManager {

    private class Icons {
        Icon main;
        Icon selected;
    }
    
    private DecoratorListener<ReferenceFieldLabelDecorator> infoLabelDecorator;
    private DecoratorListener<ReferenceFieldLabelDecorator> mainLabelDecorator;

    private DecoratorListener<ReferenceFieldIconDecorator> iconDecorator;

    private HashMap<Ref, ListenerPayload> listeners;
    
    public DecoratorManager() {
        mainLabelDecorator = new DecoratorListener<>(ReferenceFieldLabelDecorator.class);
        infoLabelDecorator = new DecoratorListener<>(ReferenceFieldLabelDecorator.class);
        iconDecorator = new DecoratorListener<>(ReferenceFieldIconDecorator.class);
        
        listeners = new HashMap<>();
    }

    public void unregister(ReferenceComponent component) {
        Ref ref = component.getReference();

        ListenerPayload payload = listeners.remove(ref);
        infoLabelDecorator.removeDecoratorChangeListener(payload.info);
        mainLabelDecorator.removeDecoratorChangeListener(payload.main);
        iconDecorator.removeDecoratorChangeListener(payload.icon);
    }
    
    public void unregister(ReferenceTitle pane) {
        unregister(pane.getReferenceComponent());
    }
    
    public void registerAndSetIcon(ReferenceComponent component) {
        Ref ref = component.getReference();
        component.setInfoLabelText(createComponentLabel(infoLabelDecorator, ref, ""));
        component.setMainLabelText(createComponentLabel(mainLabelDecorator, ref, ""));
        setIcons(component);
        
        ListenerPayload payload = new ListenerPayload();

        InfoLabelListener info = new InfoLabelListener(component);
        infoLabelDecorator.addDecoratorChangeListener(info);
        payload.info = info;
        
        MainLabelListener main = new MainLabelListener(component);
        mainLabelDecorator.addDecoratorChangeListener(main);
        payload.main = main;
        
        MainIconListener icon = new MainIconListener(component);
        iconDecorator.addDecoratorChangeListener(icon);
        payload.icon = icon;
        
        listeners.put(ref, payload);
    }

    private void setIcons(ReferenceComponent component) {
        Icons icons = createComponentIcon(iconDecorator, component.getReference());
        
        component.setIcon(icons.main, icons.selected);
    }
    
    public void registerAndSetIcon(ReferenceTitle pane) {
        registerAndSetIcon(pane.getReferenceComponent());
    }
    
    private <R extends Ref> Icons createComponentIcon(DecoratorListener<ReferenceFieldIconDecorator> listener,
                                                      R reference)
    {
        Icons originalIcons = new Icons();
        
        for (ReferenceFieldIconDecorator decorator : listener.getDecorators()) {
            originalIcons.main = (Icon) decorator.getIcon(originalIcons.main, reference);
            originalIcons.selected = (Icon) decorator.getSelectedIcon(originalIcons.selected, reference);
        }
        return originalIcons;
    }

    private <R extends Ref> String createComponentLabel(DecoratorListener<ReferenceFieldLabelDecorator> listener,
                                                        R reference,
                                                        String label)
    {        
        for (ReferenceFieldLabelDecorator decorator : listener.getDecorators()) {
            label = decorator.getLabel(label, reference);
        }
        return label;
    }
    
    public DecoratorListener<ReferenceFieldLabelDecorator> getInfoLabelDecoratorListener() {
        return infoLabelDecorator;
    }

    public DecoratorListener<ReferenceFieldLabelDecorator> getMainLabelDecoratorListener() {
        return mainLabelDecorator;
    }
    
    public DecoratorListener<ReferenceFieldIconDecorator> getIconDecoratorListener() {
        return iconDecorator;
    }

    private class ListenerPayload {
        ActionListener<DecoratorNotifier.DecorationEvent> main;
        ActionListener<DecoratorNotifier.DecorationEvent> info;
        ActionListener<DecoratorNotifier.DecorationEvent> icon;
    }
    
    private class InfoLabelListener implements ActionListener<DecoratorNotifier.DecorationEvent> {
        
        private ReferenceComponent component;
        InfoLabelListener(ReferenceComponent component) {
            this.component = component;
        }
        
        @Override
        public void actionPerformed(ActionEvent<DecorationEvent> actionEvent) {
            Ref ref = component.getReference();
            final String label = createComponentLabel(infoLabelDecorator, ref, "");
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    component.setInfoLabelText(label);
                }
            });
        }
    }
    
    private class MainLabelListener implements ActionListener<DecoratorNotifier.DecorationEvent> {
        private ReferenceComponent component;

        MainLabelListener(ReferenceComponent component) {
            this.component = component;
        }
        
        @Override
        public void actionPerformed(ActionEvent<DecorationEvent> actionEvent) {
            Ref ref = component.getReference();
            final String label = createComponentLabel(mainLabelDecorator, ref, "");
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    component.setMainLabelText(label);
                }
            });
        }
    }
    
    private class MainIconListener implements ActionListener<DecoratorNotifier.DecorationEvent> {
        private ReferenceComponent component;
        
        MainIconListener(ReferenceComponent component) {
            this.component = component;
        }
        
        @Override
        public void actionPerformed(ActionEvent<DecorationEvent> actionEvent) {
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    setIcons(component);
                }
            });
        }
    }
}

