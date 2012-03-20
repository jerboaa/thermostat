/*
 * Copyright 2012 Red Hat, Inc.
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

package com.redhat.thermostat.client.ui;

import static com.redhat.thermostat.client.locale.Translate.localize;

import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;

import javax.swing.JPanel;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.category.DefaultCategoryDataset;

import com.redhat.thermostat.client.locale.LocaleResources;

public class VmMemoryPanel extends JPanel implements VmMemoryView {

    private static final long serialVersionUID = -2882890932814218436L;

    private final DefaultCategoryDataset dataset = new DefaultCategoryDataset();

    public VmMemoryPanel() {
        initializePanel();
    }

    @Override
    public void setMemoryRegionSize(String name, long used, long allocated, long max) {
        dataset.addValue(used, localize(LocaleResources.VM_CURRENT_MEMORY_CHART_USED), name);
        dataset.addValue(allocated - used,
                localize(LocaleResources.VM_CURRENT_MEMORY_CHART_CAPACITY), name);
        dataset.addValue(max - allocated,
                localize(LocaleResources.VM_CURRENT_MEMORY_CHART_MAX_CAPACITY), name);
    }

    private void initializePanel() {
        JPanel panel = this;
        panel.setLayout(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 0;
        c.fill = GridBagConstraints.BOTH;
        c.weightx = 1;
        c.weighty = 1;
        panel.add(createCurrentMemoryDisplay(), c);
        c.gridy++;
        panel.add(createMemoryHistoryPanel(), c);
    }

    private Component createCurrentMemoryDisplay() {

        JFreeChart chart = ChartFactory.createStackedBarChart(
                null,
                localize(LocaleResources.VM_CURRENT_MEMORY_CHART_SPACE),
                localize(LocaleResources.VM_CURRENT_MEMORY_CHART_SIZE),
                dataset,
                PlotOrientation.HORIZONTAL, true, false, false);

        ChartPanel chartPanel = new ChartPanel(chart);
        // make this chart non-interactive
        chartPanel.setDisplayToolTips(true);
        chartPanel.setDoubleBuffered(true);
        chartPanel.setMouseZoomable(false);
        chartPanel.setPopupMenu(null);

        return chartPanel;
    }

    private Component createMemoryHistoryPanel() {
        JPanel historyPanel = new JPanel();
        // TODO implement this
        return historyPanel;
    }

    @Override
    public Component getUiComponent() {
        return this;
    }

}
