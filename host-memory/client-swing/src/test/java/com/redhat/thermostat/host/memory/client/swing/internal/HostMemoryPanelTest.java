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

package com.redhat.thermostat.host.memory.client.swing.internal;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;

import com.redhat.thermostat.shared.locale.LocalizedString;
import com.redhat.thermostat.storage.model.DiscreteTimeData;

public class HostMemoryPanelTest {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                JFrame frame = new JFrame("Test");

                HostMemoryPanel classPanel = new HostMemoryPanel();

                frame.add(classPanel.getUiComponent());
                frame.setVisible(true);

                classPanel.addMemoryChart("foo", new LocalizedString("foo"));
                classPanel.showMemoryChart("foo");

                List<DiscreteTimeData<? extends Number>> data = new ArrayList<DiscreteTimeData<? extends Number>>();
                data.add(new DiscreteTimeData<>(new Date().getTime(), 10l));
                data.add(new DiscreteTimeData<>(new Date().getTime() + TimeUnit.MINUTES.toMillis(1), 1000000l));
                data.add(new DiscreteTimeData<>(new Date().getTime() + TimeUnit.MINUTES.toMillis(10), 100l));

                classPanel.addMemoryData("foo", data);
            }
        });
    }
}
