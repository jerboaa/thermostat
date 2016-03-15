/*
 * Copyright 2012-2016 Red Hat, Inc.
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

package com.redhat.thermostat.internal.utils.laf;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JPanel;
import javax.swing.OverlayLayout;
import javax.swing.RepaintManager;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.Arc2D;
import java.util.Random;

/**
 */
public class ThermostatRepaintManagerDemo {

    private static class RefreshingPanel extends JPanel {

        private Random random = new Random();

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D graphics = (Graphics2D) g;
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            graphics.setColor(getBackground());
            graphics.fillRect(0, 0, getWidth(), getHeight());

            graphics.setColor(Color.BLUE);
            double angle = (double) random.nextInt(360);
            if (angle == 0) {
                angle = 1;
            }
            Arc2D arc = new Arc2D.Double(150., 150., 100., 100., 0., angle, Arc2D.PIE);
            graphics.fill(arc);
        }
    }


    private static class TransparentTop extends JPanel {

        public TransparentTop() {
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D graphics = (Graphics2D) g;
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            graphics.setColor(new Color(200, 200, 200, 200));
            graphics.fillRect(100, 100, 200, 200);
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {

                System.err.println("STARRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRR");

                RepaintManager.setCurrentManager(new ThermostatRepaintManager());

                JFrame frame = new JFrame();
                JMenuBar bar = new JMenuBar();
                bar.add(new JMenu("ffdfd"));
                frame.setJMenuBar(bar);
                frame.setMinimumSize(new Dimension(500, 500));
                frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

                JPanel mainPanel = new JPanel() {
                    @Override
                    public boolean isOptimizedDrawingEnabled() {
                        return false;
                    }
                };
                mainPanel.setName("mainPanel");
                mainPanel.setLayout(new BorderLayout());
                mainPanel.setBackground(Color.BLACK);
                frame.add(mainPanel);

                JPanel content = new JPanel();
                content.setName("content");
                content.setLayout(new OverlayLayout(content));
                content.setBackground(Color.WHITE);

                final RefreshingPanel refreshingPanel = new RefreshingPanel();
                refreshingPanel.setName("refreshing");

                TransparentTop top = new TransparentTop();
                top.setName("top");
                top.add(new JLabel("test"));
                top.setOpaque(false);

                content.add(top);
                content.add(refreshingPanel);

                mainPanel.add(content);

                Timer timer = new Timer(1000, new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        refreshingPanel.repaint(100, 100, 100, 100);
                    }
                });

                timer.start();
                frame.setVisible(true);
            }
        });
    }
}
