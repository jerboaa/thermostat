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

package com.redhat.thermostat.ui.swing.components;

import com.redhat.thermostat.platform.swing.components.ThermostatComponent;

import javax.swing.JFrame;
import javax.swing.JLayer;
import javax.swing.SwingUtilities;
import java.awt.Dimension;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.awt.event.HierarchyEvent;
import java.awt.event.HierarchyListener;

/**
 */
public class Spinner extends ThermostatComponent {

    private SpinnerLayerUI spinnerLayerUI;
    private ThermostatComponent contentPane;

    public Spinner() {
        spinnerLayerUI = new SpinnerLayerUI();
        contentPane = new ThermostatComponent();

        JLayer<ThermostatComponent> spinnerLayer = new JLayer<>(contentPane, spinnerLayerUI);
        add(spinnerLayer);

        addHierarchyListener(new HierarchyListener() {
            @Override
            public void hierarchyChanged(HierarchyEvent e) {
                if ((e.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) != 0)  {
                    if (isShowing()) {
                        spinnerLayerUI.start();
                    } else {
                        spinnerLayerUI.stop();
                    }
                }
            }
        });
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {

                final Spinner spinner = new Spinner();
                JFrame frame = new JFrame();
                frame.add(spinner);

                // quick hack for putting in the center of the first screen
                // so I don't have to move the mouse too much to close this
                // window...
                Rectangle bounds =
                        GraphicsEnvironment.getLocalGraphicsEnvironment().
                                getScreenDevices()[0].getDefaultConfiguration().
                                getBounds();
                frame.setLocation(bounds.width/2-250, bounds.height/2-250);
                frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                frame.setMinimumSize(new Dimension(500, 500));
                frame.setVisible(true);
            }
        });
    }
}
