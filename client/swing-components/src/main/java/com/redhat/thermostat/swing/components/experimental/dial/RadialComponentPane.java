/*
 * Copyright 2012-2014 Red Hat, Inc.
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

package com.redhat.thermostat.swing.components.experimental.dial;

import java.awt.AWTEvent;
import java.awt.Component;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.event.AWTEventListener;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.swing.JPanel;

import com.redhat.thermostat.client.ui.Palette;
import com.redhat.thermostat.common.OrderedComparator;

/**
 * A pane that only accept {@link RadialComponent}s.
 */
@SuppressWarnings("serial")
class RadialComponentPane extends JPanel {
    
    private List<RadialComponent> radialComponentsZOrder;
    private List<RadialComponent> radialComponents;
    
    private RadialComponent lastSelected;
    
    public RadialComponentPane() {
        this(0.5f, 0.5f);
    }
    
    public RadialComponentPane(float baseX, float baseY) {
        setLayout(new CenterStackingLayout(baseX, baseY));
        setBackground(Palette.WHITE.getColor());
        setUpEventFiltering();
        
        radialComponentsZOrder = new ArrayList<>();
        radialComponents = new ArrayList<>();
    }
    
    public void addRadialComponentSelectedListener(RadialComponentSelectedListener l) {
        listenerList.add(RadialComponentSelectedListener.class, l);
    }
    
    public void removeRadialComponentSelectedListener(RadialComponentSelectedListener l) {
        listenerList.remove(RadialComponentSelectedListener.class, l);
    }
    
    private void fireRadialComponentSelectedEvent(RadialComponent source) {

        Object[] listeners = listenerList.getListenerList();

        RadialComponentSelectedEvent event = new RadialComponentSelectedEvent(source, lastSelected);
        
        for (int i = listeners.length - 2; i >= 0; i -= 2) {
            if (listeners[i] == RadialComponentSelectedListener.class) {
                ((RadialComponentSelectedListener) listeners[i + 1]).componentSelected(event);
            }
        }
    }
    
    private void setUpEventFiltering() {
        long eventMask = AWTEvent.MOUSE_EVENT_MASK;
        Toolkit.getDefaultToolkit().addAWTEventListener(new AWTEventListener() {
            @Override
            public void eventDispatched(AWTEvent awtEvent) {

                // we know this, since it's the mask above, but in case
                if ( !(awtEvent instanceof MouseEvent)) {
                    return;
                }
                
                MouseEvent event = (MouseEvent) awtEvent;
                if (event.getID() != MouseEvent.MOUSE_CLICKED) {
                    return;
                }
                
                Point coordinates = event.getPoint();
                
                // check which component will be the target for this event
                for (RadialComponent component : radialComponentsZOrder) {
                    
                    if (!component.isTargetForEvents()) {
                        continue;
                    }
                    
                    // check the bounding box first
                    Rectangle bounds = component.getBounds();
                    if (bounds.contains(coordinates)) {
                        int x = coordinates.x - bounds.x;
                        int y = coordinates.y - bounds.y;
                        
                        // maybe that's it, maybe it passes through
                        // so let's check the
                        BufferedImage mask = component.getMask();
                        int pixel = mask.getRGB(x, y);
                        if (pixel == 0xFF000000) {
                            // fire event and return
                            fireRadialComponentSelectedEvent(component);
                            lastSelected = component;
                            
                            break;
                        }
                    }
                }
            }
        }, eventMask);
    }
    
    @Override
    protected void addImpl(Component comp, Object constraints, int index) {
        if (comp instanceof RadialComponent) {
            super.addImpl(comp, constraints, index);
            
            radialComponents.add((RadialComponent) comp);
            
            radialComponentsZOrder.add((RadialComponent) comp);
            Collections.sort(radialComponentsZOrder,
                             new OrderedComparator<RadialComponent>());

            // now need to get the correct painting order
            int zorder = 0;
            for (RadialComponent component : radialComponentsZOrder) {
                setComponentZOrder(component, zorder++);
            }
        } else {
            throw new IllegalArgumentException("");
        }
    }
    
    public List<RadialComponent> getRadialComponents() {
        return radialComponents;
    }
    
    public List<RadialComponent> getRadialComponentsZOrder() {
        return radialComponentsZOrder;
    }
}

