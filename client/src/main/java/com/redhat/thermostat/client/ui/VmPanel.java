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

import static com.redhat.thermostat.client.Translate._;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JPanel;
import javax.swing.JTabbedPane;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.data.time.TimeSeriesCollection;

import com.redhat.thermostat.client.VmPanelFacade;
import com.redhat.thermostat.client.ui.SimpleTable.Section;
import com.redhat.thermostat.client.ui.SimpleTable.TableEntry;

public class VmPanel extends JPanel {

    private static final long serialVersionUID = 2816226547554943368L;

    private final VmPanelFacade facade;

    public VmPanel(final VmPanelFacade facade) {
        this.facade = facade;
        createUI();

        addHierarchyListener(new AsyncFacadeManager(facade));
    }

    public void createUI() {
        setLayout(new BorderLayout());

        JTabbedPane tabPane = new JTabbedPane();

        tabPane.insertTab(_("VM_INFO_TAB_OVERVIEW"), null, createOverviewPanel(), null, 0);
        tabPane.insertTab(_("VM_INFO_TAB_MEMORY"), null, createMemoryPanel(), null, 1);
        tabPane.insertTab(_("VM_INFO_TAB_GC"), null, createGcPanel(), _("GARBAGE_COLLECTION"), 2);

        // TODO additional tabs provided by plugins
        // tabPane.insertTab(title, icon, component, tip, 3)

        this.add(tabPane);
    }

    public JPanel createOverviewPanel() {
        JPanel panel = new JPanel();
        panel.setBorder(Components.smallBorder());
        panel.setLayout(new BorderLayout());

        TableEntry entry;
        List<Section> allSections = new ArrayList<Section>();

        Section processSection = new Section(_("VM_INFO_SECTION_PROCESS"));
        allSections.add(processSection);

        entry = new TableEntry(_("VM_INFO_PROCESS_ID"), facade.getVmPid());
        processSection.add(entry);
        entry = new TableEntry(_("VM_INFO_START_TIME"), facade.getStartTimeStamp());
        processSection.add(entry);
        entry = new TableEntry(_("VM_INFO_STOP_TIME"), facade.getStopTimeStamp());
        processSection.add(entry);

        Section javaSection = new Section(_("VM_INFO_SECTION_JAVA"));
        allSections.add(javaSection);

        entry = new TableEntry(_("VM_INFO_MAIN_CLASS"), facade.getMainClass());
        javaSection.add(entry);
        entry = new TableEntry(_("VM_INFO_COMMAND_LINE"), facade.getJavaCommandLine());
        javaSection.add(entry);
        entry = new TableEntry(_("VM_INFO_JAVA_VERSION"), facade.getJavaVersion());
        javaSection.add(entry);
        entry = new TableEntry(_("VM_INFO_VM"), facade.getVmNameAndVersion());
        javaSection.add(entry);
        entry = new TableEntry(_("VM_INFO_VM_ARGUMENTS"), facade.getVmArguments());
        javaSection.add(entry);

        SimpleTable simpleTable = new SimpleTable();
        JPanel table = simpleTable.createTable(allSections);
        table.setBorder(Components.smallBorder());
        panel.add(table, BorderLayout.PAGE_START);

        return panel;
    }

    private Component createMemoryPanel() {
        JPanel panel = new JPanel();
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
        return panel;
    }

    private Component createCurrentMemoryDisplay() {
        DefaultCategoryDataset data = facade.getCurrentMemory();

        JFreeChart chart = ChartFactory.createStackedBarChart(
                null,
                _("VM_CURRENT_MEMORY_CHART_SPACE"),
                _("VM_CURRENT_MEMORY_CHART_SIZE"),
                data,
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

        return historyPanel;
    }

    private Component createGcPanel() {
        JPanel gcPanel = new JPanel();
        gcPanel.setLayout(new GridBagLayout());

        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 0;
        c.fill = GridBagConstraints.BOTH;
        c.weightx = 1;
        c.weighty = 1;

        String[] collectorNames = facade.getCollectorNames();
        for (int i = 0; i < collectorNames.length; i++) {
            String collectorName = collectorNames[i];
            gcPanel.add(createCollectorDetailsPanel(collectorName), c);
            c.gridy++;
        }

        return gcPanel;
    }

    private Component createCollectorDetailsPanel(String collectorName) {
        JPanel detailsPanel = new JPanel();
        detailsPanel.setBorder(Components.smallBorder());
        detailsPanel.setLayout(new BorderLayout());

        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.fill = GridBagConstraints.BOTH;

        detailsPanel.add(Components.header(_("VM_GC_COLLECTOR_OVER_GENERATION", collectorName, facade.getCollectorGeneration(collectorName))), BorderLayout.NORTH);

        TimeSeriesCollection dataset = facade.getCollectorDataSet(collectorName);
        JFreeChart chart = ChartFactory.createTimeSeriesChart(
                null,
                _("VM_GC_COLLECTOR_CHART_REAL_TIME_LABEL"),
                _("VM_GC_COLLECTOR_CHART_GC_TIME_LABEL"),
                dataset,
                false, false, false);

        JPanel chartPanel = new RecentTimeSeriesChartPanel(new RecentTimeSeriesChartController(chart));

        detailsPanel.add(chartPanel, BorderLayout.CENTER);

        return detailsPanel;
    }

}
