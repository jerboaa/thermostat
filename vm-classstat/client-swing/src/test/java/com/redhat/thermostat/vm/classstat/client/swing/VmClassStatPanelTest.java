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

package com.redhat.thermostat.vm.classstat.client.swing;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;

import org.fest.swing.edt.FailOnThreadViolationRepaintManager;
import org.junit.BeforeClass;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;

import com.redhat.thermostat.annotations.internal.CacioTest;
import com.redhat.thermostat.shared.locale.LocalizedString;
import com.redhat.thermostat.storage.model.DiscreteTimeData;
import com.redhat.thermostat.vm.classstat.client.core.VmClassStatView;

import net.java.openjdk.cacio.ctc.junit.CacioFESTRunner;

@Category(CacioTest.class)
@RunWith(CacioFESTRunner.class)
public class VmClassStatPanelTest {

    @BeforeClass
    public static void setUpOnce() {
        FailOnThreadViolationRepaintManager.install();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                JFrame frame = new JFrame("Test");

                VmClassStatPanel classPanel = new VmClassStatPanel();

                List<DiscreteTimeData<? extends Number>> data = new ArrayList<DiscreteTimeData<? extends Number>>();
                data.add(new DiscreteTimeData<>(new Date().getTime(), 10l));
                data.add(new DiscreteTimeData<>(new Date().getTime() + TimeUnit.MINUTES.toMillis(1), 1000l));

                classPanel.addClassChart(VmClassStatView.Group.NUMBER, "classes", new LocalizedString("classes"));
                classPanel.addClassData("classes", data);

                List<DiscreteTimeData<? extends Number>> data2 = new ArrayList<DiscreteTimeData<? extends Number>>();
                data2.add(new DiscreteTimeData<Number>(new Date().getTime(), 10));
                data2.add(new DiscreteTimeData<Number>(new Date().getTime() + TimeUnit.MINUTES.toMillis(1), 20));
                classPanel.addClassChart(VmClassStatView.Group.SIZE, "size", new LocalizedString("size"));
                classPanel.addClassData("size", data2);

                frame.add(classPanel.getUiComponent());
                frame.pack();
                frame.setVisible(true);
                frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            }
        });
    }
}

