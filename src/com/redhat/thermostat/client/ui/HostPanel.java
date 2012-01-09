package com.redhat.thermostat.client.ui;

import static com.redhat.thermostat.client.Translate._;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.swing.AbstractButton;
import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import com.redhat.thermostat.client.HostInformationFacade;
import com.redhat.thermostat.client.MemoryType;
import com.redhat.thermostat.client.ui.SimpleTable.Key;
import com.redhat.thermostat.client.ui.SimpleTable.Section;
import com.redhat.thermostat.client.ui.SimpleTable.TableEntry;
import com.redhat.thermostat.client.ui.SimpleTable.Value;
import com.redhat.thermostat.common.HostInfo;
import com.redhat.thermostat.common.NetworkInterfaceInfo;

public class HostPanel extends JPanel {

    /*
     * This entire class needs to be more dynamic. We should try to avoid
     * creating objects and should just update them when necessary
     */

    private static final long serialVersionUID = 4835316442841009133L;

    private final HostInformationFacade facade;
    private final HostInfo hostInfo;

    public HostPanel(HostInformationFacade facade) {
        this.facade = facade;
        this.hostInfo = facade.getHostInfo();
        init();
    }

    private void init() {
        setLayout(new BorderLayout());

        JTabbedPane tabPane = new JTabbedPane();

        tabPane.insertTab(_("HOST_INFO_TAB_OVERVIEW"), null, createOverviewPanel(), null, 0);
        tabPane.insertTab(_("HOST_INFO_TAB_CPU"), null, createCpuStatisticsPanel(), null, 1);
        tabPane.insertTab(_("HOST_INFO_TAB_MEMORY"), null, createMemoryStatisticsPanel(), null, 2);

        // TODO additional tabs provided by plugins
        // tabPane.insertTab(title, icon, component, tip, 3)

        this.add(tabPane);

    }

    private JPanel createOverviewPanel() {

        TableEntry entry;
        List<Section> allSections = new ArrayList<Section>();

        Section basics = new Section(_("HOST_OVERVIEW_SECTION_BASICS"));
        allSections.add(basics);

        entry = new TableEntry(_("HOST_INFO_HOSTNAME"), hostInfo.getHostname());
        basics.add(entry);

        Section hardware = new Section(_("HOST_OVERVIEW_SECTION_HARDWARE"));
        allSections.add(hardware);

        entry = new TableEntry(_("HOST_INFO_CPU_MODEL"), hostInfo.getCpuModel());
        hardware.add(entry);
        entry = new TableEntry(_("HOST_INFO_CPU_COUNT"), String.valueOf(hostInfo.getCpuCount()));
        hardware.add(entry);
        entry = new TableEntry(_("HOST_INFO_MEMORY_TOTAL"), String.valueOf(hostInfo.getTotalMemory()));
        hardware.add(entry);

        Key key = new Key(_("HOST_INFO_NETWORK"));
        List<Value> values = new ArrayList<Value>();
        for (Iterator<NetworkInterfaceInfo> iter = facade.getNetworkInfo().getInterfacesIterator(); iter.hasNext();) {
            NetworkInterfaceInfo networkInfo = iter.next();
            String ifaceName = networkInfo.getInterfaceName();
            String ipv4 = networkInfo.getIp4Addr();
            String ipv6 = networkInfo.getIp6Addr();
            values.add(new Value(_("HOST_INFO_NETWORK_INTERFACE_ADDDRESS", ifaceName, ipv4, ipv6)));
        }
        hardware.add(new TableEntry(key, values));

        Section software = new Section(_("HOST_OVERVIEW_SECTION_SOFTWARE"));
        allSections.add(software);

        entry = new TableEntry(_("HOST_INFO_OS_NAME"), hostInfo.getOsName());
        software.add(entry);
        entry = new TableEntry(_("HOST_INFO_OS_KERNEL"), hostInfo.getOsKernel());
        software.add(entry);

        JPanel table = SimpleTable.createTable(allSections);
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

        Section cpuBasics = new Section(_("HOST_CPU_SECTION_OVERVIEW"));
        allSections.add(cpuBasics);

        TableEntry entry;
        entry = new TableEntry(_("HOST_INFO_CPU_MODEL"), hostInfo.getCpuModel());
        cpuBasics.add(entry);
        entry = new TableEntry(_("HOST_INFO_CPU_COUNT"), String.valueOf(hostInfo.getCpuCount()));
        cpuBasics.add(entry);

        JPanel table = SimpleTable.createTable(allSections);
        table.setBorder(Components.smallBorder());
        contentArea.add(table, c);

        double[][] cpuData = facade.getCpuLoad();
        XYSeries series = new XYSeries("cpu-load");
        for (double[] data : cpuData) {
            series.add(data[0], data[1]);
        }
        XYSeriesCollection dataset = new XYSeriesCollection();
        dataset.addSeries(series);
        JFreeChart chart = ChartFactory.createTimeSeriesChart(
                _("HOST_CPU_USAGE_CHART_TITLE"),
                _("HOST_CPU_USAGE_CHART_TIME_LABEL"),
                _("HOST_CPU_USAGE_CHART_VALUE_LABEL"),
                dataset,
                false, false, false);

        ChartPanel chartPanel = new ChartPanel(chart);
        // make this chart non-interactive
        chartPanel.setDisplayToolTips(true);
        chartPanel.setDoubleBuffered(true);
        chartPanel.setMouseZoomable(false);
        chartPanel.setPopupMenu(null);

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

        Section memoryBasics = new Section(_("HOST_MEMORY_SECTION_OVERVIEW"));
        allSections.add(memoryBasics);

        TableEntry entry;
        entry = new TableEntry(_("HOST_INFO_MEMORY_TOTAL"), String.valueOf(hostInfo.getTotalMemory()));
        memoryBasics.add(entry);


        JPanel table = SimpleTable.createTable(allSections);
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

    private static JFreeChart createMemoryChart(HostInformationFacade facade) {
        XYSeriesCollection dataset = new XYSeriesCollection();

        // FIXME associate a fixed color with each type

        for (MemoryType type : facade.getMemoryTypesToDisplay()) {
            XYSeries series = new XYSeries(type.name());
            long[][] data = facade.getMemoryUsage(type);
            for (long[] point : data) {
                series.add(point[0], point[1]);
            }
            dataset.addSeries(series);
        }

        JFreeChart chart = ChartFactory.createTimeSeriesChart(
                _("HOST_MEMORY_CHART_TITLE"), // Title
                _("HOST_MEMORY_CHART_TIME_LABEL"), // x-axis Label
                _("HOST_MEMORY_CHART_SIZE_LABEL"), // y-axis Label
                dataset, // Dataset
                false, // Show Legend
                false, // Use tooltips
                false // Configure chart to generate URLs?
                );
        return chart;
    }

    private static class UpdateMemoryGraph implements ActionListener {

        private final HostInformationFacade facade;
        private final MemoryType type;
        private final ChartPanel chartPanel;

        public UpdateMemoryGraph(HostInformationFacade facade, ChartPanel chartPanel, MemoryType type) {
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
