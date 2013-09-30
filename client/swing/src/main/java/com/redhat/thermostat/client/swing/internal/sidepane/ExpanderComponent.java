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

package com.redhat.thermostat.client.swing.internal.sidepane;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.Box;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

import com.redhat.thermostat.client.swing.GraphicsUtils;
import com.redhat.thermostat.client.swing.components.DebugBorder;
import com.redhat.thermostat.client.swing.components.FontAwesomeIcon;
import com.redhat.thermostat.client.swing.components.Icon;
import com.redhat.thermostat.client.swing.internal.splitpane.ThermostatSplitPane;
import com.redhat.thermostat.client.ui.Palette;

@SuppressWarnings("serial")
public class ExpanderComponent extends JPanel {
    
    public static int ICON_WIDTH = 20;
    public static final String EXPANDED_PROPERTY = "ExpanderComponent_EXPANDED_PROPERTY";

    private class ExpanderComponentBorder extends DebugBorder {
        @Override
        public void paintBorder(Component c, Graphics g, int x, int y,
                                int width, int height)
        {
            Graphics2D graphics = GraphicsUtils.getInstance().createAAGraphics(g);
            Insets insets = getBorderInsets(c);
            
            int xStart = x + width - insets.right;
            graphics.translate(xStart, y);
            
            graphics.setColor(Palette.LIGHT_GRAY.getColor());
            graphics.fillRect(0, y, width, height);
            
            ThermostatSplitPane.paint(graphics, insets.right, height, Palette.LIGHT_GRAY.getColor());
            graphics.dispose();            
        }
        
        @Override
        public Insets getBorderInsets(Component c, Insets insets) {
            insets.top = 0;
            insets.left = 0;
            insets.right = 5;
            insets.bottom = 1;
            
            return insets;
        }
    }
    
    public ExpanderComponent() {
        
        setBackground(Color.BLACK);
        setLayout(new BorderLayout());

        setBorder(new ExpanderComponentBorder());
        
        final Icon mainIcon = new FontAwesomeIcon('\uf101', ICON_WIDTH, TopSidePane.FG_COLOR);
        final Icon hover = new FontAwesomeIcon('\uf101', ICON_WIDTH, ThermostatSidePanel.FG_TEXT_COLOR);
        
        final JLabel expander = new JLabel(mainIcon);
        expander.setHorizontalTextPosition(SwingConstants.LEFT);

        expander.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                expander.setIcon(hover);
            }
            @Override
            public void mouseExited(MouseEvent e) {
                expander.setIcon(mainIcon);
            }
            @Override
            public void mouseClicked(MouseEvent e) {
                expander.setIcon(mainIcon);
                firePropertyChange(EXPANDED_PROPERTY, false, true);
            }
        });
        
        add(Box.createRigidArea(new Dimension(5, 0)), BorderLayout.WEST);
        add(expander, BorderLayout.CENTER);
    }
}
