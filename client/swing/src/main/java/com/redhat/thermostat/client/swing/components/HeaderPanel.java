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

package com.redhat.thermostat.client.swing.components;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.lang.reflect.InvocationTargetException;

import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

/**
 * A component that host a panel with a nicely rendered header.
 */
@SuppressWarnings("serial")
public class HeaderPanel extends JPanel {
    
    private String header;
    
    private JPanel contentPanel;
    private JLabel headerLabel;
    private JPanel headerPanel;
    private JPanel controlPanel;
    
    public HeaderPanel() {
        this("");
    }
    
    public HeaderPanel(String header) {
        
        this.header = header;
        
        setLayout(new BorderLayout(0, 0));

        headerLabel = new ShadowLabel(header, new EmptyIcon(5, 5));
        headerPanel = new GradientPanel(Color.WHITE, getBackground());
        headerPanel.setPreferredSize(new Dimension(HeaderPanel.this.getWidth(), 40));
        
        headerPanel.setLayout(new BorderLayout(0, 0));
        headerPanel.add(headerLabel, BorderLayout.WEST);
        add(headerPanel, BorderLayout.NORTH);
        
        controlPanel = new JPanel();
        controlPanel.setOpaque(false);
        controlPanel.setLayout(new FlowLayout(FlowLayout.RIGHT, 2, 10));
        
        headerPanel.add(controlPanel, BorderLayout.EAST);
        
        contentPanel = new JPanel();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.X_AXIS));
        
        add(contentPanel, BorderLayout.CENTER);
    }
   
    public String getHeader() {
        return header;
    }
    
    public void setHeader(String header) {
        this.header = header;
        headerLabel.setText(header);
    }
    
    public void setContent(JComponent content) {
        contentPanel.removeAll();
        contentPanel.add(content);
        contentPanel.revalidate();
        repaint();
    }

    public void addToolBarButton(ToolbarButton button) {
        controlPanel.add(button.getToolbarButton());
    }
    
    public static void main(String[] args) throws InvocationTargetException, InterruptedException {
        SwingUtilities.invokeAndWait(new Runnable() {
            
            @Override
            public void run() {
               JFrame frame = new JFrame();
               frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
               
               HeaderPanel header = new HeaderPanel();
               header.setHeader("Test");
               frame.getContentPane().add(header);
               frame.setSize(500, 500);
               frame.setVisible(true);
            }
        });
    }
}
