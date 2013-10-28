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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.swing.event.EventListenerList;

/**
 * A Model class for the {@link Accordion} widget. The model represents the
 * collection of H (header) and C (components under header) instances that
 * are added to the {@link Accordion}. As noted in the {@link Accordion}
 * javadoc, H and C do not have to be widgets themselves, since the
 * {@link AccordionComponentFactory} is responsible to create the specific
 * widget to be used inside the Accordion. 
 * 
 * <br /><br />
 * 
 * <strong>Note</strong>: H and C must be usable as hash keys.
 * 
 * <br />
 * 
 * <strong>Note</strong>: This class is <strong>not</strong> thread safe.
 */
public class AccordionModel<H, C> {

    protected EventListenerList listenerList;
    
    private HashMap<H, Set<C>> components;
    
    public AccordionModel() {
        components = new HashMap<>();
        listenerList = new EventListenerList();
    }
   
    private Set<C> addOrGetHeader(H header) {
        Set<C> _components = components.get(header);
        if (_components == null) {
            _components = new HashSet<>();
            components.put(header, _components);

            fireHeaderAddedEvent(header);
        }
        return _components;
    }
    
    /**
     * Gets a {@link List} representation of all the headers currently
     * held by this model. The {@link List} can be modified, however the
     * headers are references to the actual headers contained in this model.
     * 
     * <br /><br />
     * 
     * If this model is currently empty, an emtpy {@link List} is returned.
     */
    public List<H> getHeaders() {
        List<H> result = new ArrayList<>();
        if (components.size() != 0) {
            for (H header : components.keySet()) {
                result.add(header);
            }
        }
        return result;
    }
    
   /**
    * Gets a {@link List} representation of all the components currently
    * held by this model and relative to the passed header. The {@link List}
    * can be modified, however the components are references to the actual
    * components contained in this model.
    * 
    * <br /><br />
    * 
    * If no components exist of the given header, an emtpy {@link List} is
    * returned.
    */
    public List<C> getComponents(H header) {
        List<C> result = new ArrayList<>();
        if (components.containsKey(header)) {
            Set<C> componentSet = components.get(header);
            for (C component : componentSet) {
                result.add(component);
            }
        }
        return result;
    }
    
    /**
     * Adds this header to this model. If the header already exist in this
     * model, this is a no-op.
     */
    public boolean addHeader(H header) {
        boolean result = components.containsKey(header);
        addOrGetHeader(header);
        return result;
    }
    
    /**
     * Removes this header from this model.
     */
    public boolean removeHeader(H header) {
        Set<C> _components = components.remove(header);
        boolean result = (_components != null);
        if (result) {
            for (C component :_components) {
                fireComponentRemovedEvent(header, component);
            }
            fireHeaderRemovedEvent(header);
        }
        return result;
    }
    
    /**
     * Adds this component to the given header. If the header is not in the
     * model already, it is also added.
     */
    public boolean addComponent(H header, C component) {
        Set<C> _components = addOrGetHeader(header);
        boolean result = false;
        if (!_components.contains(component)) {
            result = _components.add(component);
            fireComponentAddedEvent(header, component);
        }
        return result;
    }
    
    /**
     * Removes the current component from the given header. If the header
     * is not contained in this model, or the component does not belong to
     * the passed header, this is a no-op.
     */
    public boolean removeComponent(H header, C component) {
        Set<C> _components = components.get(header);
        boolean result = false;
        if (_components != null) {
            result = _components.remove(component);
            fireComponentRemovedEvent(header, component);
        }
        return result;
    }
    
    /**
     * Returns the total number of header objects contained in this model,
     * not including their components.
     */
    public int headerSize() {
        return components.size();
    }
    
    /**
     * Returns the total number of objects contained in this model. The total
     * size is the sum of {@link #headerSize()} plus the number of components
     * contained under each header. 
     */
    public int size() {
        int size = components.size();
        for (Set<C> comp : components.values()) {
            size += comp.size();
        }
        return size;
    }
    
    /**
     * Adds an {@link AccordionModelChangeListener} listener to this model.
     */
    public void addAccordionModelChangeListener(AccordionModelChangeListener<H, C> listener) {
        listenerList.add(AccordionModelChangeListener.class, listener);
    }

    /**
     * Removes this {@link AccordionModelChangeListener} listener from this model.
     */
    public void removeAccordionModelChangeListener(AccordionModelChangeListener<H, C> listener) {
        listenerList.remove(AccordionModelChangeListener.class, listener);
    }
    
    @SuppressWarnings({ "unchecked", "rawtypes" })
    private void fireHeaderRemovedEvent(H header) {
        Object[] listeners = listenerList.getListenerList();

        AccordionHeaderEvent<H> event = new AccordionHeaderEvent<>(header);
        
        for (int i = listeners.length - 2; i >= 0; i -= 2) {
            if (listeners[i] == AccordionModelChangeListener.class) {
                ((AccordionModelChangeListener) listeners[i + 1]).headerRemoved(event);
            }
        }
    }
    
    @SuppressWarnings({ "unchecked", "rawtypes" })
    private void fireHeaderAddedEvent(H header) {
        Object[] listeners = listenerList.getListenerList();

        AccordionHeaderEvent<H> event = new AccordionHeaderEvent<>(header);
        
        for (int i = listeners.length - 2; i >= 0; i -= 2) {
            if (listeners[i] == AccordionModelChangeListener.class) {
                ((AccordionModelChangeListener) listeners[i + 1]).headerAdded(event);
            }
        }
    }
    
    @SuppressWarnings({ "unchecked", "rawtypes" })
    private void fireComponentAddedEvent(H header, C component) {
        Object[] listeners = listenerList.getListenerList();

        AccordionComponentEvent<H, C> event = new AccordionComponentEvent<>(header, component);
        
        for (int i = listeners.length - 2; i >= 0; i -= 2) {
            if (listeners[i] == AccordionModelChangeListener.class) {
                ((AccordionModelChangeListener) listeners[i + 1]).componentAdded(event);
            }
        }
    }
    
    @SuppressWarnings({ "unchecked", "rawtypes" })
    private void fireComponentRemovedEvent(H header, C component) {
        Object[] listeners = listenerList.getListenerList();

        AccordionComponentEvent<H, C> event = new AccordionComponentEvent<>(header, component);
        
        for (int i = listeners.length - 2; i >= 0; i -= 2) {
            if (listeners[i] == AccordionModelChangeListener.class) {
                ((AccordionModelChangeListener) listeners[i + 1]).componentRemoved(event);
            }
        }
    }

    /**
     * Clears this accordion model. This will result in the removal of all the
     * headers (and subsequently of the respective components) in this model,
     * generating 
     */
    public void clear() {
        for (H header : getHeaders()) {
            removeHeader(header);
        }
    }
}
