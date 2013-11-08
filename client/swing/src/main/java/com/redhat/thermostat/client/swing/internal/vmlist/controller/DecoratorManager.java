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

import com.redhat.thermostat.client.swing.components.Icon;
import com.redhat.thermostat.client.swing.internal.vmlist.ReferenceComponent;
import com.redhat.thermostat.client.swing.internal.vmlist.ReferenceTitle;
import com.redhat.thermostat.client.swing.internal.vmlist.controller.DecoratorNotifier.DecorationEvent;
import com.redhat.thermostat.client.ui.ReferenceFieldIconDecorator;
import com.redhat.thermostat.client.ui.ReferenceFieldLabelDecorator;
import com.redhat.thermostat.common.ActionEvent;
import com.redhat.thermostat.common.ActionListener;
import com.redhat.thermostat.storage.core.Ref;

public class DecoratorManager {

    private class Icons {
        Icon main;
        Icon selected;
    }
    
    private DecoratorListener<ReferenceFieldLabelDecorator> infoLabelDecorator =
            new DecoratorListener<>(ReferenceFieldLabelDecorator.class);
    private DecoratorListener<ReferenceFieldLabelDecorator> mainLabelDecorator =
            new DecoratorListener<>(ReferenceFieldLabelDecorator.class);

    private DecoratorListener<ReferenceFieldIconDecorator> iconDecorator =
            new DecoratorListener<>(ReferenceFieldIconDecorator.class);

    public void registerAndSetIcon(final ReferenceComponent component) {
        Ref ref = component.getReference();
        component.setInfoLabelText(createComponentLabel(infoLabelDecorator, ref,
                                                        component.getInfoLabelText()));
        component.setMainLabelText(createComponentLabel(mainLabelDecorator, ref,
                                                        component.getMainLabelText()));
        setIcons(component);
        
        // FIXME: this is a leak
        infoLabelDecorator.addDecoratorChangeListener(new ActionListener<DecoratorNotifier.DecorationEvent>() {
            @Override
            public void actionPerformed(ActionEvent<DecorationEvent> actionEvent) {
                component.setInfoLabelText(createComponentLabel(infoLabelDecorator,
                                                                component.getReference(),
                                                                component.getInfoLabelText()));
            }
        });
        mainLabelDecorator.addDecoratorChangeListener(new ActionListener<DecoratorNotifier.DecorationEvent>() {
            @Override
            public void actionPerformed(ActionEvent<DecorationEvent> actionEvent) {
                component.setMainLabelText(createComponentLabel(mainLabelDecorator,
                                                                component.getReference(),
                                                                component.getMainLabelText()));
            }
        });
        iconDecorator.addDecoratorChangeListener(new ActionListener<DecoratorNotifier.DecorationEvent>() {
            @Override
            public void actionPerformed(ActionEvent<DecorationEvent> actionEvent) {
                setIcons(component);
            }
        });
    }

    private void setIcons(final ReferenceComponent component) {
        Icons icons = new Icons();
        icons.main = component.getIcon();
        icons.selected = component.getSelectedIcon();
        icons = createComponentIcon(iconDecorator, component.getReference(), icons);
        
        component.setIcon(icons.main, icons.selected);
    }
    
    public void registerAndSetIcon(final ReferenceTitle pane) {
        registerAndSetIcon(pane.getReferenceComponent());
    }
    
    private <R extends Ref> Icons createComponentIcon(DecoratorListener<ReferenceFieldIconDecorator> listener,
                                                      R reference,
                                                      Icons originalIcons)
    {
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
}
