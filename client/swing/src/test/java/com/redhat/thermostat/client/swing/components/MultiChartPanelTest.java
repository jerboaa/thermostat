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

package com.redhat.thermostat.client.swing.components;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;

import com.redhat.thermostat.client.swing.components.MultiChartPanel.DataGroup;
import com.redhat.thermostat.shared.locale.LocalizedString;
import com.redhat.thermostat.storage.model.DiscreteTimeData;

public class MultiChartPanelTest {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                JFrame window = new JFrame("Test");
                window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

                MultiChartPanel panel = new MultiChartPanel();
                DataGroup GROUP1 = panel.createGroup();
                DataGroup GROUP2 = panel.createGroup();

                panel.setDomainAxisLabel("foobar");
                panel.addChart(GROUP1, "1", new LocalizedString("foo"));
                panel.showChart(GROUP1, "1");

                List<DiscreteTimeData<? extends Number>> data1 = new ArrayList<>();
                data1.add(new DiscreteTimeData<Number>(new Date().getTime(), 20l));
                data1.add(new DiscreteTimeData<Number>(new Date().getTime() + TimeUnit.MINUTES.toMillis(100), 1000l));
                panel.addData("1", data1);

                panel.addChart(GROUP1, "2", new LocalizedString("bar"));
                panel.showChart(GROUP1, "2");

                List<DiscreteTimeData<? extends Number>> data2 = new ArrayList<>();
                data2.add(new DiscreteTimeData<Number>(new Date().getTime(), 15));
                data2.add(new DiscreteTimeData<Number>(new Date().getTime() + TimeUnit.MINUTES.toMillis(100), 30));
                panel.addData("2", data2);

                panel.addChart(GROUP2, "3", new LocalizedString("Eggs"));
                panel.showChart(GROUP2, "3");
                List<DiscreteTimeData<? extends Number>> data3 = new ArrayList<>();
                data3.add(new DiscreteTimeData<Number>(new Date().getTime(), 1000));
                data3.add(new DiscreteTimeData<Number>(new Date().getTime() + TimeUnit.MINUTES.toMillis(100), 3000));
                panel.addData("3", data3);

                panel.addChart(GROUP2, "4", new LocalizedString("Spam"));
                panel.addChart(GROUP2, "5", new LocalizedString("Ham"));
                panel.addChart(GROUP2, "6", new LocalizedString("Sausage"));
                panel.addChart(GROUP2, "7", new LocalizedString("Baked Beans"));

                window.add(panel);
                window.pack();
                window.setVisible(true);
            }
        });
    }

}
