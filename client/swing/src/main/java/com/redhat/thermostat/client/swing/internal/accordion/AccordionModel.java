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

package com.redhat.thermostat.client.swing.internal.accordion;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.swing.event.EventListenerList;

/**
 * A Model class for the {@link Accordion} widget. The model represents the
 * collection of H (header) and C (components under header) instances that
 * are added to the {@link Accordion}. As noted in the {@link Accordion}
 * javadoc, H and C do not have to be widgets themselves, since the
 * {@link AccordionComponentFactory} is responsible to create the specific
 * widget to be used inside the Accordion. 
 * 
 * <strong>Note</strong>: H and C must be usable as hash keys.
 */
public class AccordionModel<H, C> {

    protected EventListenerList listenerList;
    
    private HashMap<H, List<C>> components;
    
    public AccordionModel() {
        components = new HashMap<>();
        listenerList = new EventListenerList();
    }
   
    private List<C> addOrGetHeader(H header) {
        List<C> _components = components.get(header);
        if (_components == null) {
            _components = new ArrayList<>();
            components.put(header, _components);

            fireHeaderAddedEvent(header);
        }
        return _components;
    }
    
    public boolean addHeader(H header) {
        boolean result = components.containsKey(header);
        addOrGetHeader(header);
        return result;
    }
    
    public boolean removeHeader(H header) {
        List<C> _components = components.remove(header);
        boolean result = (_components != null);
        if (result) {
            for (C component :_components) {
                fireComponentRemovedEvent(header, component);
            }
            fireHeaderRemovedEvent(header);
        }
        return result;
    }
    
    public boolean addComponent(H header, C component) {
        List<C> _components = addOrGetHeader(header);
        boolean result = _components.add(component);
        fireComponentAddedEvent(header, component);
        return result;
    }
    
    public boolean removeComponent(H header, C component) {
        List<C> _components = components.get(header);
        boolean result = false;
        if (_components != null) {
            result = _components.remove(component);
            fireComponentRemovedEvent(header, component);
        }
        return result;
    }
    
    public int headerSize() {
        return components.size();
    }
    
    public int size() {
        int size = components.size();
        for (List<C> comp : components.values()) {
            size += comp.size();
        }
        return size;
    }
    
    public void addAccordionModelChangeListener(AccordionModelChangeListener l) {
        listenerList.add(AccordionModelChangeListener.class, l);
    }
    
    private void fireHeaderRemovedEvent(H header) {
        Object[] listeners = listenerList.getListenerList();

        AccordionHeaderEvent<H> event = new AccordionHeaderEvent<>(header);
        
        for (int i = listeners.length - 2; i >= 0; i -= 2) {
            if (listeners[i] == AccordionModelChangeListener.class) {
                ((AccordionModelChangeListener) listeners[i + 1]).headerRemoved(event);
            }
        }
    }
    
    private void fireHeaderAddedEvent(H header) {
        Object[] listeners = listenerList.getListenerList();

        AccordionHeaderEvent<H> event = new AccordionHeaderEvent<>(header);
        
        for (int i = listeners.length - 2; i >= 0; i -= 2) {
            if (listeners[i] == AccordionModelChangeListener.class) {
                ((AccordionModelChangeListener) listeners[i + 1]).headerAdded(event);
            }
        }
    }
    
    private void fireComponentAddedEvent(H header, C component) {
        Object[] listeners = listenerList.getListenerList();

        AccordionComponentEvent<H, C> event = new AccordionComponentEvent<>(header, component);
        
        for (int i = listeners.length - 2; i >= 0; i -= 2) {
            if (listeners[i] == AccordionModelChangeListener.class) {
                ((AccordionModelChangeListener) listeners[i + 1]).componentAdded(event);
            }
        }
    }
    
    private void fireComponentRemovedEvent(H header, C component) {
        Object[] listeners = listenerList.getListenerList();

        AccordionComponentEvent<H, C> event = new AccordionComponentEvent<>(header, component);
        
        for (int i = listeners.length - 2; i >= 0; i -= 2) {
            if (listeners[i] == AccordionModelChangeListener.class) {
                ((AccordionModelChangeListener) listeners[i + 1]).componentRemoved(event);
            }
        }
    }
}
