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

package com.redhat.thermostat.client.swing.components;

import com.redhat.thermostat.shared.locale.LocalizedString;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;

/**
 */
public class ThermostatTabbedPaneDemo {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                JFrame frame = new JFrame();
                frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                frame.setMinimumSize(new Dimension(500, 500));

                HeaderPanel header = new HeaderPanel(new LocalizedString("A Simple Tab Demo"));
                frame.setContentPane(header);

                ThermostatTabbedPane tabs = new ThermostatTabbedPane();
                tabs.addTab("Content #1", createContent("Some Content"));
                tabs.addTab("Content #2", createContent("Some Other Content"));
                tabs.addTab("Content #3", createContent("Some More Content Longer"));
                tabs.addTab("Content #4", createContent("Some More Content Even Longer"));
                tabs.addTab("Content #5", createContent("Some More Content Very Very Long!!!"));
                tabs.addTab("Content #6", createContent("Some More Content The Longest One!!!!!!!!"));

                header.setContent(tabs);

                frame.setVisible(true);
            }
        });
    }

    private static Component createContent(String text) {
        JPanel panel = new JPanel();
        panel.setBackground(Color.WHITE);

        panel.setLayout(new BorderLayout());
        JLabel content = new JLabel(text);
        content.setHorizontalAlignment(SwingConstants.CENTER);

        panel.add(content);

        return panel;
    }
}