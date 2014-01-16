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

package com.redhat.thermostat.client.swing.internal.progress;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;

import com.redhat.thermostat.client.core.progress.ProgressHandle;
import com.redhat.thermostat.client.swing.GraphicsUtils;
import com.redhat.thermostat.client.swing.components.DebugBorder;
import com.redhat.thermostat.client.swing.components.GradientPanel;
import com.redhat.thermostat.client.swing.components.ShadowLabel;
import com.redhat.thermostat.client.ui.Palette;

@SuppressWarnings("serial")
class AggregateProgressComponent extends GradientPanel {

    private JProgressBar progressBar;
    private ShadowLabel taskStatus;
    public AggregateProgressComponent(ProgressHandle handle) {
        
        super(Palette.WHITE.getColor(), Palette.PALE_GRAY.getColor());
        
        setLayout(new BorderLayout());
        
        setBorder(new AggregateProgressComponentBorder());
        
        JPanel panel = new JPanel(new GridLayout());
        panel.setOpaque(false);
        add(panel, BorderLayout.CENTER);
        
        ShadowLabel text = new ShadowLabel(handle.getName());
        panel.add(text);

        progressBar = new JProgressBar();
        progressBar.setName(handle.getName().getContents());
        progressBar.setStringPainted(false);
      
        progressBar.setIndeterminate(handle.isIndeterminate());
        panel.add(progressBar);
        
        JPanel currentTaskStatusPane = new JPanel(new GridLayout());
        currentTaskStatusPane.setOpaque(false);
        
        taskStatus = new ShadowLabel();

        Font defaultFont = taskStatus.getFont();
        taskStatus.setFont(defaultFont.deriveFont(defaultFont.getSize2D() - 2.5f));
        taskStatus.setText(handle.getTask().getContents());
        currentTaskStatusPane.add(taskStatus);
        
        add(currentTaskStatusPane, BorderLayout.SOUTH);
    }
    
    public JProgressBar getProgressBar() {
        return progressBar;
    }
    
    public JLabel getTaskStatus() {
        return taskStatus;
    }
    
    private static class AggregateProgressComponentBorder extends DebugBorder {
        @Override
        public void paintBorder(Component c, Graphics g, int x, int y,
                                int width, int height)
        {
            GraphicsUtils utils = GraphicsUtils.getInstance();
            Graphics2D graphics = utils.createAAGraphics(g);
            
            graphics.setColor(utils.deriveWithAlpha(Palette.EGYPTIAN_BLUE.getColor(), 120));
            
            graphics.setStroke(new BasicStroke(1.2f));
            graphics.drawLine(x, y, x + width - 1, y);
            graphics.dispose();
        }
    }
}

