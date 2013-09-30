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

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;

import com.redhat.thermostat.client.swing.components.EmptyIcon;
import com.redhat.thermostat.client.swing.components.Icon;
import com.redhat.thermostat.client.swing.internal.vmlist.ReferenceComponent;
import com.redhat.thermostat.client.swing.internal.vmlist.ReferenceTitle;
import com.redhat.thermostat.client.swing.internal.vmlist.UIDefaults;
import com.redhat.thermostat.client.swing.internal.vmlist.controller.DecoratorProviderExtensionListener.Action;
import com.redhat.thermostat.client.ui.Decorator;
import com.redhat.thermostat.client.ui.DecoratorProvider;
import com.redhat.thermostat.client.ui.IconDescriptor;
import com.redhat.thermostat.common.ActionEvent;
import com.redhat.thermostat.common.ActionListener;
import com.redhat.thermostat.storage.core.HostRef;
import com.redhat.thermostat.storage.core.Ref;
import com.redhat.thermostat.storage.core.VmRef;

public class DecoratorManager {

    private DecoratorProviderExtensionListener<HostRef> hostDecorator = new DecoratorProviderExtensionListener<>();
    private DecoratorProviderExtensionListener<VmRef> vmDecorator = new DecoratorProviderExtensionListener<>();
    
    private Map<Decorator, Icon> decoratorsCache = new HashMap<>();
    
    public void registerAndSetIcon(final ReferenceComponent component) {
        component.setIcon(createIcon(vmDecorator, (VmRef) component.getReference()));
        // FIXME: this is a leak
        vmDecorator.addDecoratorChangeListener(new ActionListener<DecoratorProviderExtensionListener.Action>() {
            @Override
            public void actionPerformed(ActionEvent<Action> actionEvent) {
                component.setIcon(createIcon(vmDecorator, (VmRef) component.getReference()));
            }
        });
    }

    public void registerAndSetIcon(final ReferenceTitle pane) {
        pane.setIcon(createIcon(hostDecorator, (HostRef) pane.getReference()));
        // FIXME: this is a leak
        hostDecorator.addDecoratorChangeListener(new ActionListener<DecoratorProviderExtensionListener.Action>() {
            @Override
            public void actionPerformed(ActionEvent<Action> actionEvent) {
                pane.setIcon(createIcon(hostDecorator, (HostRef) pane.getReference()));
            }
        });
    }
    
    private <R extends Ref> Icon createIcon(DecoratorProviderExtensionListener<R> listener, R ref)
    {
        UIDefaults uiDefaults = UIDefaults.getInstance();
        int size = uiDefaults.getIconSize();
        Icon canvas = new EmptyIcon(size, size);
        
        // FIXME: this logic is broken, since we simply iterate over
        // icon locations, instead we need to at least sort them
        // in painter's order
        for (DecoratorProvider<R> provider : listener.getDecorators()) {
            
            if (!provider.getFilter().matches(ref)) {
                continue;
            }
            
            Decorator decorator = provider.getDecorator();
            Icon icon = getIconFromCache(decorator);
            if (icon == null) {
                continue;
            }
            
            switch (decorator.getQuadrant()) {
            case MAIN:
            case TOP_LEFT:
                canvas = createCustomIcon(canvas, icon, 0);
                break;

            case BOTTOM_LEFT:
                int y = canvas.getIconHeight() - icon.getIconHeight();
                canvas = createCustomIcon(canvas, icon, y);
                break;
                
            default:
                // FIXME: log me?
                break;
            }

        }
        
        return canvas;
    }
    
    Icon getIconFromCache(Decorator decorator) {
        Icon icon = decoratorsCache.get(decorator);
        if (icon == null) {
            IconDescriptor iconDescriptor = decorator.getIconDescriptor();
            if (iconDescriptor != null) {
                icon = new Icon(iconDescriptor);
                decoratorsCache.put(decorator, icon);
            }
        }
        return icon;
    }
    
    private Icon createCustomIcon(Icon source, Icon newIcon, int y) {
        BufferedImage image = new BufferedImage(source.getIconWidth(),
                                                source.getIconHeight(),
                                                BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = (Graphics2D) image.getGraphics();
        source.paintIcon(null, graphics, 0, 0);
        graphics.drawImage(newIcon.getImage(), 0, y, null);
        
        return new Icon(image);
    }
    
    DecoratorProviderExtensionListener<HostRef> getHostDecoratorListener() {
        return hostDecorator;
    }
    
    DecoratorProviderExtensionListener<VmRef> getVmDecoratorListener() {
        return vmDecorator;
    }
}
