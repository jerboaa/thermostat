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

package com.redhat.thermostat.client.heap.swing;

import java.awt.Color;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.RenderingHints;
import java.lang.reflect.InvocationTargetException;

import javax.swing.BoxLayout;
import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.LayoutStyle.ComponentPlacement;
import javax.swing.SwingUtilities;

import sun.swing.SwingUtilities2;

/**
 * A component that hot a panel
 */
@SuppressWarnings("restriction")
public class HeaderPanel extends JPanel {

    private String header;
    private boolean open;
    
    private JPanel contentPanel;
    
    public HeaderPanel(String header) {
        this();
        this.header = header;
    }
    
    public HeaderPanel() {
        
        this.open = true;
        
        JPanel headerPanel = new TopPanel();
        
        contentPanel = new JPanel();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.X_AXIS));
        
        GroupLayout groupLayout = new GroupLayout(this);
        groupLayout.setHorizontalGroup(
            groupLayout.createParallelGroup(Alignment.LEADING)
                .addComponent(headerPanel, GroupLayout.DEFAULT_SIZE, 450, Short.MAX_VALUE)
                .addComponent(contentPanel, GroupLayout.DEFAULT_SIZE, 450, Short.MAX_VALUE)
        );
        groupLayout.setVerticalGroup(
            groupLayout.createParallelGroup(Alignment.LEADING)
                .addGroup(groupLayout.createSequentialGroup()
                    .addComponent(headerPanel, GroupLayout.PREFERRED_SIZE, 30, GroupLayout.PREFERRED_SIZE)
                    .addPreferredGap(ComponentPlacement.RELATED)
                    .addComponent(contentPanel, GroupLayout.DEFAULT_SIZE, 260, Short.MAX_VALUE))
        );
        setLayout(groupLayout);
    }
    
    public String getHeader() {
        return header;
    }
    
    public void setHeader(String header) {
        this.header = header;
    }
    
    public void setContent(JComponent content) {
        contentPanel.removeAll();
        contentPanel.add(content);
        contentPanel.revalidate();
        repaint();
    }
    
    @SuppressWarnings({ "serial" })
    private class TopPanel extends JPanel {
        @Override
        protected void paintComponent(Graphics g) {
            
            Graphics2D graphics = (Graphics2D) g.create();
            
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            
            Paint gradient = new GradientPaint(0, 0, Color.WHITE, 0, getHeight(), getBackground());
            graphics.setPaint(gradient);
            graphics.fillRect(0, 0, getWidth() - 1, getHeight() - 1);
            
            if (header != null) {
                int currentHeight = getHeight();
                
                // paint it twice to give a subtle drop shadow effect
                Font font = getFont();
                int height = SwingUtilities2.getFontMetrics(this, font).getAscent()/2 + currentHeight/2 - 1;
                
                graphics.setColor(new Color(0f, 0f, 0f, 0.1f));
                SwingUtilities2.drawString(this, graphics, header, 6, height + 1);
                
                graphics.setColor(getForeground());
                SwingUtilities2.drawString(this, graphics, header, 5, height);
                
            }
            
            graphics.dispose();
        }
    }
    
    public static void main(String[] args) throws InvocationTargetException, InterruptedException {
        SwingUtilities.invokeAndWait(new Runnable() {
            
            @Override
            public void run() {
               JFrame frame = new JFrame();
               HeaderPanel header = new HeaderPanel();
               header.setHeader("Test");
               frame.add(header);
               frame.setSize(500, 500);
               frame.setVisible(true);
            }
        });
    }
}
