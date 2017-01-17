/*
 * Copyright 2012-2017 Red Hat, Inc.
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

package com.redhat.thermostat.client.swing.components.experimental;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;

/**
 */
public class StepGradientTest {

    // try something smaller, i.e. 10, to see a lower resolution sampling
    private static final int STEPS = 400;
    private static final int TOTAL_HEIGHT = 500;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {

                final StepGradient stepGradient = new StepGradient(new Color(0xCA80FF), new Color(0x80FDFF), STEPS);

                JFrame frame = new JFrame("StepGradientTest");
                frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                frame.setMinimumSize(new Dimension(500, TOTAL_HEIGHT));

                JPanel contentPane = new JPanel();
                contentPane.setLayout(new BorderLayout());

                final JPanel colourPane = new JPanel() {
                    @Override
                    protected void paintComponent(Graphics g) {

                        stepGradient.reset();
                        g.setColor(getForeground());
                        g.fillRect(0, 0, getWidth(), getHeight());

                        int w = getWidth();
                        int totalH = getHeight();
                        int h = totalH / STEPS + 1;

                        int steps = h * STEPS;
                        for (int y = 0; y < steps; y += h) {
                            g.setColor(stepGradient.sample());
                            g.fillRect(0, y, w, h);
                        }
                    }
                };

                contentPane.add(colourPane, BorderLayout.CENTER);

                frame.add(contentPane);

                frame.setVisible(true);
            }
        });
    }
}