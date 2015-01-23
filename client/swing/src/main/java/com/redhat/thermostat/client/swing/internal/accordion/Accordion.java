/*
 * Copyright 2012-2015 Red Hat, Inc.
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

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import com.redhat.thermostat.client.swing.components.ThermostatScrollPane;
import com.redhat.thermostat.client.swing.components.VerticalLayout;
import com.redhat.thermostat.client.ui.Palette;

/**
 * An Accordion widget. H and C types represent Header and Component and are
 * added and managed for the accordion via the {@link AccordionModel} class.
 * 
 * <br /><br />
 * 
 * H and C do not have to be widgets themselves, since the
 * {@link AccordionComponentFactory} is responsible to create the specific
 * widget to be used inside the Accordion.
 * 
 */
@SuppressWarnings("serial")
public class Accordion<H, C> extends JPanel {
    
    public static final Color BASE_COLOR = Palette.WHITE.getColor();
    
    public static final int ICON_SIZE = 24;
    
    static final int MIN_HEIGHT = ICON_SIZE;
    static final int MIN_WIDTH = 200;
    
    private JScrollPane scrollPane;
    
    private AccordionModel<H, C> model;
    private HashMap<H, TitledPane> panes;
    private HashMap<H, Map<C, AccordionComponent>> components;
    
    private AccordionComponentFactory<H, C> componentFactory;
    
    private AccordionContentPane contentPane;
    
    private AccordionComponentController componentController;
    
    public Accordion(AccordionComponentFactory<H, C> componentFactory) {
        
        setBackground(BASE_COLOR);
        
        this.componentFactory = componentFactory;
        componentController = new AccordionComponentController();
        
        setLayout(new BorderLayout());

        contentPane = new AccordionContentPane();
        contentPane.setBackground(BASE_COLOR);
        contentPane.setLayout(new VerticalLayout());
        
        model = new AccordionModel<>();
        model.addAccordionModelChangeListener(new AccordionModelChangeListenerImpl());
        panes = new HashMap<>();
        components = new HashMap<>();
        
        scrollPane = new ThermostatScrollPane(contentPane);
        
        add(scrollPane, BorderLayout.CENTER);
    }

    public AccordionModel<H, C> getModel() {
        return model;
    }
    
    @Override
    public Dimension getPreferredSize() {
        Dimension dim = new Dimension();
        dim.width = Accordion.MIN_WIDTH;
        for (Component comp : getComponents()) {
            Dimension pref = comp.getPreferredSize();
            if (dim.width < pref.width) {
                dim.width =  pref.width;
            }
            dim.height += pref.height;
        }
        return dim;
    }
    
    private class AccordionComponentClickEventModel extends MouseAdapter {
        private AccordionComponent referenceComponent;
        public AccordionComponentClickEventModel(AccordionComponent referenceComponent) {
            this.referenceComponent = referenceComponent;
        }

        @Override
        public void mouseClicked(MouseEvent e) {
            componentController.setSelectedItem(referenceComponent);
        }
    }
    
    private class AccordionHeaderClickEventModel extends MouseAdapter {
        private TitledPane referenceComponent;
        public AccordionHeaderClickEventModel(TitledPane referenceComponent) {
            this.referenceComponent = referenceComponent;
        }

        @Override
        public void mouseClicked(MouseEvent e) {
            if (e.getClickCount() >= 2) {
                referenceComponent.setExpanded(!referenceComponent.isExpanded());
            }
        }
    }
    
    private class AccordionModelChangeListenerImpl implements AccordionModelChangeListener<H, C> {

        @Override
        public void headerAdded(AccordionHeaderEvent<H> e) {
            H header = e.getHeader();
            TitledPane pane = componentFactory.createHeader(header);
            
            pane.addMouseListener(new AccordionComponentClickEventModel(pane));
            pane.addMouseListener(new AccordionHeaderClickEventModel(pane));
            panes.put(header, pane);
            
            Accordion.this.contentPane.add(pane);
            Accordion.this.contentPane.revalidate();
        }

        @Override
        public void componentAdded(AccordionComponentEvent<H, C> e) {
            C component = e.getComponent();
            H header = e.getHeader();
            AccordionComponent comp = componentFactory.createComponent(header, component);
            
            TitledPane pane = panes.get(header);
            JComponent content = pane.getContent();
            if (content == null) {
                content = new AccordionContentPane();
                content.setLayout(new VerticalLayout());
                pane.setContent(content);
            }
            
            Component contentUnit = comp.getUiComponent();
            contentUnit.addMouseListener(new AccordionComponentClickEventModel(comp));
            
            content.add(contentUnit);
            content.revalidate();
            
            Map<C, AccordionComponent> componentsMap = components.get(header);
            if (componentsMap == null) {
                componentsMap = new HashMap<>();
                components.put(header, componentsMap);
            }
            componentsMap.put(component, comp);
        }

        @Override
        public void componentRemoved(AccordionComponentEvent<H, C> e) {
            C component = e.getComponent();
            H header = e.getHeader();
            
            Map<C, AccordionComponent> componentsMap = components.get(header);
            if (componentsMap.isEmpty()) {
                components.remove(header);
            }

            AccordionComponent contentUnit = componentsMap.remove(component);
            
            TitledPane pane = panes.get(header);
            JComponent content = pane.getContent();
            content.remove(contentUnit.getUiComponent());
            content.revalidate();

            componentFactory.removeComponent(contentUnit, header, component);
            
            Accordion.this.contentPane.revalidate();
        }

        @Override
        public void headerRemoved(AccordionHeaderEvent<H> e) {
            H header = e.getHeader();
            
            TitledPane pane = panes.remove(header);
            Accordion.this.contentPane.remove(pane);
            
            Map<C, AccordionComponent> componentsMap = components.remove(header);
            if (componentsMap != null) {
                for (C component : componentsMap.keySet()) {
                    AccordionComponent contentUnit = componentsMap.get(component);
                    componentFactory.removeComponent(contentUnit, header, component);
                }
            }
            
            Accordion.this.contentPane.revalidate();
        }
    }
    
    public void addAccordionItemSelectedChangeListener(AccordionItemSelectedChangeListener l) {
        componentController.addAccordionItemSelectedChangeListener(l);
    }
    
    public void removeAccordionItemSelectedChangeListener(AccordionItemSelectedChangeListener l) {
        componentController.removeAccordionItemSelectedChangeListener(l);
    }
    
    public AccordionComponent getSelectedComponent() {
        return componentController.getSelectedComponent();
    }

    public void setSelectedComponent(AccordionComponent component) {
        componentController.setSelectedItem(component);
    }
    
    public boolean isExpanded(H header) {
        return !panes.containsKey(header) || panes.get(header).isExpanded();
    }
    
    public void setExpanded(H header, boolean expanded) {
        if (panes.containsKey(header)) {
            panes.get(header).setExpanded(expanded);
            repaint();
        }
    }
}

