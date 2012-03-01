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

import static com.redhat.thermostat.client.Translate.localize;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.AbstractButton;
import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JTable;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.data.time.TimeSeriesCollection;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import com.redhat.thermostat.client.DiscreteTimeData;
import com.redhat.thermostat.client.HostPanelFacade;
import com.redhat.thermostat.client.MemoryType;
import com.redhat.thermostat.client.ui.SimpleTable.Key;
import com.redhat.thermostat.client.ui.SimpleTable.Section;
import com.redhat.thermostat.client.ui.SimpleTable.TableEntry;
import com.redhat.thermostat.client.ui.SimpleTable.Value;

public class HostPanel extends JPanel {

    /*
     * This entire class needs to be more dynamic. We should try to avoid
     * creating objects and should just update them when necessary
     */

    private static final long serialVersionUID = 4835316442841009133L;

    private final HostPanelFacade facade;

    public HostPanel(final HostPanelFacade facade) {
        this.facade = facade;

        init();
        addHierarchyListener(new AsyncFacadeManager(facade));
    }

    private void init() {
        setLayout(new BorderLayout());

        JTabbedPane tabPane = new JTabbedPane();

        tabPane.insertTab(localize("HOST_INFO_TAB_OVERVIEW"), null, createOverviewPanel(), null, 0);
        tabPane.insertTab(localize("HOST_INFO_TAB_CPU"), null, createCpuStatisticsPanel(), null, 1);
        tabPane.insertTab(localize("HOST_INFO_TAB_MEMORY"), null, createMemoryStatisticsPanel(), null, 2);

        // TODO additional tabs provided by plugins
        // tabPane.insertTab(title, icon, component, tip, 3)

        this.add(tabPane);

    }

    private JPanel createOverviewPanel() {

        TableEntry entry;
        List<Section> allSections = new ArrayList<Section>();

        Section basics = new Section(localize("HOST_OVERVIEW_SECTION_BASICS"));
        allSections.add(basics);

        entry = new TableEntry(localize("HOST_INFO_HOSTNAME"), facade.getHostName());
        basics.add(entry);

        Section hardware = new Section(localize("HOST_OVERVIEW_SECTION_HARDWARE"));
        allSections.add(hardware);

        entry = new TableEntry(localize("HOST_INFO_CPU_MODEL"), facade.getCpuModel());
        hardware.add(entry);
        entry = new TableEntry(localize("HOST_INFO_CPU_COUNT"), facade.getCpuCount());
        hardware.add(entry);
        entry = new TableEntry(localize("HOST_INFO_MEMORY_TOTAL"), facade.getTotalMemory());
        hardware.add(entry);

        JTable networkTable = new JTable(facade.getNetworkTableModel());

        JPanel networkPanel = new JPanel(new BorderLayout());
        networkPanel.add(networkTable.getTableHeader(), BorderLayout.PAGE_START);
        networkPanel.add(networkTable, BorderLayout.CENTER);

        Key key = new Key(localize("HOST_INFO_NETWORK"));
        hardware.add(new TableEntry(key, new Value(networkPanel)));

        Section software = new Section(localize("HOST_OVERVIEW_SECTION_SOFTWARE"));
        allSections.add(software);

        entry = new TableEntry(localize("HOST_INFO_OS_NAME"), facade.getOsName());
        software.add(entry);
        entry = new TableEntry(localize("HOST_INFO_OS_KERNEL"), facade.getOsKernel());
        software.add(entry);

        SimpleTable simpleTable = new SimpleTable();
        JPanel table = simpleTable.createTable(allSections);
        table.setBorder(Components.smallBorder());
        return table;
    }

    private JPanel createCpuStatisticsPanel() {

        JPanel contentArea = new JPanel();
        contentArea.setLayout(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.anchor = GridBagConstraints.FIRST_LINE_START;
        c.fill = GridBagConstraints.NONE;
        c.gridx = 0;
        c.gridy = 0;

        List<Section> allSections = new ArrayList<Section>();

        Section cpuBasics = new Section(localize("HOST_CPU_SECTION_OVERVIEW"));
        allSections.add(cpuBasics);

        TableEntry entry;
        entry = new TableEntry(localize("HOST_INFO_CPU_MODEL"), facade.getCpuModel());
        cpuBasics.add(entry);
        entry = new TableEntry(localize("HOST_INFO_CPU_COUNT"), facade.getCpuCount());
        cpuBasics.add(entry);

        final SimpleTable simpleTable = new SimpleTable();
        JPanel table = simpleTable.createTable(allSections);
        table.setBorder(Components.smallBorder());
        contentArea.add(table, c);

        TimeSeriesCollection dataset = facade.getCpuLoadDataSet();
        JFreeChart chart = ChartFactory.createTimeSeriesChart(
                null,
                localize("HOST_CPU_USAGE_CHART_TIME_LABEL"),
                localize("HOST_CPU_USAGE_CHART_VALUE_LABEL"),
                dataset,
                false, false, false);

        JPanel chartPanel = new RecentTimeSeriesChartPanel(new RecentTimeSeriesChartController(chart));

        c.gridy++;
        c.fill = GridBagConstraints.BOTH;
        c.weightx = 1;
        c.weighty = 1;

        contentArea.add(chartPanel, c);
        return contentArea;
    }

    private JPanel createMemoryStatisticsPanel() {
        JPanel contentArea = new JPanel();
        // contentArea.setLayout(new GridBagLayout());
        contentArea.setLayout(new BorderLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.anchor = GridBagConstraints.FIRST_LINE_START;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridx = 0;
        c.gridy = 0;

        List<Section> allSections = new ArrayList<Section>();

        Section memoryBasics = new Section(localize("HOST_MEMORY_SECTION_OVERVIEW"));
        allSections.add(memoryBasics);

        TableEntry entry;
        entry = new TableEntry(localize("HOST_INFO_MEMORY_TOTAL"), facade.getTotalMemory());
        memoryBasics.add(entry);

        SimpleTable simpleTable = new SimpleTable();
        JPanel table = simpleTable.createTable(allSections);
        table.setBorder(Components.smallBorder());
        contentArea.add(table, BorderLayout.PAGE_START);

        JFreeChart chart = createMemoryChart(facade);

        ChartPanel chartPanel = new ChartPanel(chart);
        // make this chart non-interactive
        chartPanel.setDisplayToolTips(true);
        chartPanel.setDoubleBuffered(true);
        chartPanel.setMouseZoomable(false);
        chartPanel.setPopupMenu(null);

        c.gridy++;
        c.fill = GridBagConstraints.BOTH;
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.weightx = 1;
        c.weighty = 1;
        contentArea.add(chartPanel, BorderLayout.CENTER);

        JPanel memoryPanel = new JPanel(new WrapLayout(FlowLayout.LEADING));
        contentArea.add(memoryPanel, BorderLayout.PAGE_END);

        for (MemoryType type : MemoryType.values()) {
            JCheckBox checkBox = new JCheckBox(type.getLabel(), facade.isMemoryTypeDisplayed(type));
            checkBox.addActionListener(new UpdateMemoryGraph(facade, chartPanel, type));
            memoryPanel.add(checkBox);
        }

        return contentArea;
    }

    private static JFreeChart createMemoryChart(HostPanelFacade facade) {
        XYSeriesCollection dataset = new XYSeriesCollection();

        // FIXME associate a fixed color with each type

        for (MemoryType type : facade.getMemoryTypesToDisplay()) {
            XYSeries series = new XYSeries(type.name());
            DiscreteTimeData<Long>[] data = facade.getMemoryUsage(type);
            for (DiscreteTimeData<Long> point : data) {
                series.add(point.getTimeInMillis(), point.getData());
            }
            dataset.addSeries(series);
        }

        JFreeChart chart = ChartFactory.createTimeSeriesChart(
                localize("HOST_MEMORY_CHART_TITLE"), // Title
                localize("HOST_MEMORY_CHART_TIME_LABEL"), // x-axis Label
                localize("HOST_MEMORY_CHART_SIZE_LABEL"), // y-axis Label
                dataset, // Dataset
                false, // Show Legend
                false, // Use tooltips
                false // Configure chart to generate URLs?
                );
        return chart;
    }

    private static class UpdateMemoryGraph implements ActionListener {

        private final HostPanelFacade facade;
        private final MemoryType type;
        private final ChartPanel chartPanel;

        public UpdateMemoryGraph(HostPanelFacade facade, ChartPanel chartPanel, MemoryType type) {
            this.facade = facade;
            this.chartPanel = chartPanel;
            this.type = type;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            AbstractButton abstractButton = (AbstractButton) e.getSource();
            boolean selected = abstractButton.getModel().isSelected();
            facade.setDisplayMemoryType(type, selected);
            chartPanel.setChart(createMemoryChart(facade));
        }
    }

}
