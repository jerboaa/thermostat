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

package com.redhat.thermostat.client.swing.components;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.concurrent.TimeUnit;

import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import javax.swing.border.EmptyBorder;

import com.redhat.thermostat.client.swing.components.experimental.TimelineIntervalSelector;
import com.redhat.thermostat.client.swing.components.experimental.TimelineIntervalSelectorModel.ChangeListener;

public class TimelineIntervalSelectorTest {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                JFrame mainWindow = new JFrame();

                final TimelineIntervalSelector intervalSelector = new TimelineIntervalSelector();
                long now = System.currentTimeMillis();

                intervalSelector.getModel().addChangeListener(new ChangeListener() {
                    @Override
                    public void changed() {
                        // System.out.println(intervalSelector.getModel().getTotalMinimum());
                        // System.out.println(intervalSelector.getModel().getTotalMaximum());
                        //
                        // System.out.println(intervalSelector.getModel().getSelectedMinimum());
                        // System.out.println(intervalSelector.getModel().getSelectedMaximum());

                    }
                });

                intervalSelector.getModel().setTotalMinimum(now);
                intervalSelector.getModel().setTotalMaximum(now + TimeUnit.HOURS.toMillis(1));

                intervalSelector.getModel().setSelectedMinimum(now);
                intervalSelector.getModel().setSelectedMaximum(now + TimeUnit.MINUTES.toMillis(10));

                final JCheckBox enable = new JCheckBox("Enabled");
                enable.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        intervalSelector.setEnabled(enable.isSelected());
                    }
                });
                enable.setSelected(true);

                PlaceHolder actualComponent = new PlaceHolder();
                actualComponent.setBorder(BorderFactory.createLineBorder(Color.BLACK, 1));

                mainWindow.getRootPane().setBorder(new EmptyBorder(new Insets(10,10,10,10)));
                mainWindow.add(intervalSelector, BorderLayout.NORTH);
                mainWindow.add(actualComponent, BorderLayout.CENTER);
                mainWindow.add(enable, BorderLayout.SOUTH);
                mainWindow.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);

                mainWindow.setVisible(true);

            }
        });
    }

    private static class PlaceHolder extends JPanel {
        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();

            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(Color.BLACK);

            g2.drawLine(0, 0, getWidth(), getHeight());
            g2.drawLine(getWidth(), 0, 0, getHeight());

            g2.dispose();
        }
    }
}

