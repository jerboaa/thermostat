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

package com.redhat.thermostat.client.swing.internal.sidepane;

import java.awt.BorderLayout;
import java.awt.Color;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.JComponent;
import javax.swing.JPanel;

import com.redhat.thermostat.client.ui.Palette;

@SuppressWarnings("serial")
public class ThermostatSidePanel extends JPanel {
    
    public static final Color BG_COLOR = Palette.DROID_GRAY.getColor();
    
    public static final Color FG_COLOR = Palette.EARL_GRAY.getColor();
    public static final Color FG_TEXT_COLOR = Palette.LIGHT_GRAY.getColor();
    
    public static final String COLLAPSED = "ThermostatSidePanel_collapsed";
    
    private TopSidePane top;
    private JPanel bottom;
    
    public ThermostatSidePanel() {
        setLayout(new BorderLayout());
        
        top = new TopSidePane();
        top.addPropertyChangeListener(TopSidePane.COLLAPSED_PROPERTY,
                                      new PropertyChangeListener()
        {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                firePropertyChange(COLLAPSED, evt.getOldValue(),
                                              evt.getNewValue());
            }
        });
        add(top, BorderLayout.NORTH);
        
        bottom = new JPanel();        
        bottom.setLayout(new BorderLayout());
        add(bottom, BorderLayout.CENTER);
    }
    
    public void addContent(JComponent comp) {
        addContent(comp, null);
    }
    
    public void removeComponent(JComponent comp) {
        bottom.remove(comp);
        repaint();
    }
    
    public void addContent(JComponent comp, Object contraints) {
        bottom.add(comp, contraints);
        repaint();
    }
    
    public JPanel getTopPane() {
        return top;
    }
}

