/*
 * Copyright 2012 Red Hat, Inc.
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

package com.redhat.thermostat.swing;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.beans.Transient;

import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JToggleButton;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

/**
 * A simple On/Off switch.
 */
@SuppressWarnings("serial")
public class ToggleButton extends JToggleButton {
    
    private static final int WIDTH = 40;
    private static final int HEIGHT = 24;
    
    private ImageIcon toggle;
    private ImageIcon selectedToggle;
    
    public ToggleButton() {
        
        toggle = new ImageIcon(getClass().getResource("/icons/scale-slider-vert.png"));
        selectedToggle = new ImageIcon(getClass().getResource("/icons/scale-slider-vert-backdrop.png"));
        
        setBorder(null);
    }

    @Override
    protected void paintComponent(Graphics g) {
        GraphicsUtils utils = GraphicsUtils.getInstance();
        
        Graphics2D graphics = utils.createAAGraphics(g);
        graphics.clearRect(0, 0, getWidth(), getHeight());
        
        graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        
        Color selectedcolor = null;
        Color shadow = new Color(0xE0E0E0);
        if (model.isSelected()) {
            selectedcolor = new Color(0x4A90D9);
        } else {
            selectedcolor = new Color(0xaeaeae);
        }
        
        Color bgColor = null;
        Container parent = getParent();
        if (parent != null) {
            bgColor = parent.getBackground();
        } else {
            bgColor = getBackground();
        }
        
        // external shape and main fill
        utils.setGradientPaint(graphics, 0, 10, shadow, bgColor);
        graphics.fill(utils.getRoundShape(getWidth(), getHeight()));
        
        utils.setGradientPaint(graphics, 0, getWidth() / 2, selectedcolor, bgColor);
        graphics.draw(utils.getRoundShape(getWidth(), getHeight()));
        
        // slider
        int x = (toggle.getIconWidth() / 2);
        int y = (getHeight()/2);
        int width = getWidth() - (toggle.getIconWidth()/2) - 2;
        
        graphics.setColor(selectedcolor);
        graphics.setStroke(new BasicStroke(1.2f));
        graphics.drawLine(x, y, width, y);
        
        graphics.setStroke(new BasicStroke());

        // toggle
        x = 2;
        y = (getHeight()/2) - (toggle.getIconHeight()/2);
        ImageIcon toggle = this.toggle;
        if (model.isRollover()) {
            toggle = this.selectedToggle;
        }
        
        if (model.isSelected()) {
            toggle = this.selectedToggle;
            x = getWidth() - toggle.getIconWidth() - 3;
        }
        graphics.drawImage(toggle.getImage(), x, y, null);
        
        graphics.dispose();
    }
    
    @Override
    @Transient
    public Dimension getMinimumSize() {
        if (isMinimumSizeSet()) {
            return super.getMinimumSize();
        }
        return new Dimension(WIDTH, HEIGHT);
    }
    
    @Override
    @Transient
    public Dimension getPreferredSize() {
        if (isPreferredSizeSet()) {
            return super.getPreferredSize();
        }
        return new Dimension(WIDTH, HEIGHT);
    }
    
    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            
            @Override
            public void run() {
                JFrame frame = new JFrame();
                frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                
                HeaderPanel header = new HeaderPanel();
                header.setHeader("Test");
                
                JPanel buttonPanel = new JPanel();
                ToggleButton toggle = new ToggleButton();
                
                buttonPanel.add(toggle);
                header.setContent(buttonPanel);
                
                frame.add(header);
                frame.setSize(500, 500);
                frame.setVisible(true);
            }
        });
    }
}
