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

package com.redhat.thermostat.vm.memory.client.core;

import com.redhat.thermostat.common.Size.Unit;

import javax.swing.BoxLayout;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import java.awt.Dimension;

import com.redhat.thermostat.client.swing.components.ThermostatScrollPane;

public class MemoryMeterDemo {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                JFrame frame = new JFrame();
                frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                frame.setMinimumSize(new Dimension(200, 200));

                MemoryMeter meter = new MemoryMeter();

                JPanel graphPanel = new JPanel();
                graphPanel.setLayout(new BoxLayout(graphPanel, BoxLayout.Y_AXIS));

                graphPanel.add(meter);

                Payload payload = new Payload();
                payload.setName("Test");
                payload.setUsedUnit(Unit.KiB);
                payload.setCapacityUnit(Unit.KiB);
                payload.setCapacity(1.7);
                payload.setMaxCapacity(2.0);
                meter.setMemoryGraphProperties(payload);

                meter = new MemoryMeter();
                graphPanel.add(meter);

                payload = new Payload();
                payload.setName("Test2");
                payload.setUsedUnit(Unit.KiB);
                payload.setCapacityUnit(Unit.KiB);
                meter.setMemoryGraphProperties(payload);

                ThermostatScrollPane scrollPane = new ThermostatScrollPane(graphPanel);
                frame.setContentPane(scrollPane);

                frame.setVisible(true);
            }
        });
    }
}