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

package com.redhat.thermostat.client.swing.internal.sidepane;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JLabel;
import javax.swing.JPanel;

import com.redhat.thermostat.client.swing.GraphicsUtils;
import com.redhat.thermostat.client.swing.components.DebugBorder;
import com.redhat.thermostat.client.swing.components.FontAwesomeIcon;
import com.redhat.thermostat.client.swing.components.Icon;
import com.redhat.thermostat.client.swing.internal.vmlist.UIDefaultsImpl;

@SuppressWarnings("serial")
class TopSidePane extends JPanel {

    static final String COLLAPSED_PROPERTY = "collapsed";
    
    static int ICON_WIDTH = 24;

    static Color ICON_BG_COLOR = UIDefaultsImpl.getInstance().getReferenceFieldIconColor();
    
    static Color FG_COLOR = ThermostatSidePanel.FG_COLOR;
    private Color currentFGColor = ThermostatSidePanel.FG_COLOR;
    
    public TopSidePane() {
        setBackground(Color.WHITE);
        setLayout(new BorderLayout());
        
        final Icon mainIcon = new FontAwesomeIcon('\uf100', ICON_WIDTH, FG_COLOR);
        final Icon hover = new FontAwesomeIcon('\uf100', ICON_WIDTH, ICON_BG_COLOR);
        
        final JLabel expander = new JLabel(mainIcon);
        expander.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                setState(ICON_BG_COLOR, hover);
            }
            @Override
            public void mouseExited(MouseEvent e) {
                setState(FG_COLOR, mainIcon);
            }
            @Override
            public void mouseClicked(MouseEvent e) {
                setState(FG_COLOR, mainIcon);
                firePropertyChange(COLLAPSED_PROPERTY, false, true);
            }
            
            private void setState(Color color, Icon icon) {
                currentFGColor = color;
                expander.setIcon(icon);
                repaint();
            }
        });

        add(expander, BorderLayout.EAST);
    
        setBorder(new TopSidePaneBorder());
    }
    
    @Override
    protected void paintComponent(Graphics g) {
        
        GraphicsUtils utils = GraphicsUtils.getInstance();
        Graphics2D graphics = utils.createAAGraphics(g);
        
        graphics.setColor(getBackground());
        graphics.fillRect(0, 0, getWidth(), getHeight());
        
        graphics.dispose();
    }
    
    private class TopSidePaneBorder extends DebugBorder {
        @Override
        public void paintBorder(Component c, Graphics g, int x, int y,
                                int width, int height)
        {
            g.setColor(GraphicsUtils.getInstance().deriveWithAlpha(currentFGColor, 100));
            g.drawLine(x, y + height - 1, x + width, y + height - 1);
        }
    }
}

