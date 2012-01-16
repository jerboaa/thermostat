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
import org.jfree.data.time.FixedMillisecond;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;

import com.redhat.thermostat.client.DiscreteTimeData;
import com.redhat.thermostat.client.VmPanelFacade;
import com.redhat.thermostat.client.ui.SimpleTable.Section;
import com.redhat.thermostat.client.ui.SimpleTable.TableEntry;
import com.redhat.thermostat.common.VmInfo;
import com.redhat.thermostat.common.VmMemoryStat;
import com.redhat.thermostat.common.VmMemoryStat.Generation;
import com.redhat.thermostat.common.VmMemoryStat.Space;

public class VmPanel extends JPanel {

    private static final long serialVersionUID = 2816226547554943368L;

    private final VmPanelFacade facade;

    private final VmInfo vmInfo;

    public VmPanel(VmPanelFacade facade) {
        this.facade = facade;
        this.vmInfo = facade.getVmInfo();
        createUI();
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

        entry = new TableEntry(_("VM_INFO_PROCESS_ID"), String.valueOf(vmInfo.getVmPid()));
        processSection.add(entry);
        entry = new TableEntry(_("VM_INFO_START_TIME"), String.valueOf(vmInfo.getStartTimeStamp()));
        processSection.add(entry);
        entry = new TableEntry(_("VM_INFO_STOP_TIME"), String.valueOf(vmInfo.getStopTimeStamp()));
        processSection.add(entry);

        Section javaSection = new Section(_("VM_INFO_SECTION_JAVA"));
        allSections.add(javaSection);

        entry = new TableEntry(_("VM_INFO_MAIN_CLASS"), vmInfo.getMainClass());
        javaSection.add(entry);
        entry = new TableEntry(_("VM_INFO_COMMAND_LINE"), vmInfo.getJavaCommandLine());
        javaSection.add(entry);
        entry = new TableEntry(_("VM_INFO_JAVA_VERSION"), vmInfo.getJavaVersion());
        javaSection.add(entry);
        entry = new TableEntry(_("VM_INFO_VM"), _("VM_INFO_VM_NAME_AND_VERSION", vmInfo.getVmName(), vmInfo.getVmVersion()));
        javaSection.add(entry);

        JPanel table = SimpleTable.createTable(allSections);
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
        DefaultCategoryDataset data = new DefaultCategoryDataset();

        VmMemoryStat info = facade.getLatestMemoryInfo();
        List<Generation> generations = info.getGenerations();
        for (Generation generation : generations) {
            List<Space> spaces = generation.spaces;
            for (Space space : spaces) {
                data.addValue(space.used, _("VM_CURRENT_MEMORY_CHART_USED"), space.name);
                data.addValue(space.capacity - space.used, _("VM_CURRENT_MEMORY_CHART_CAPACITY"), space.name);
                data.addValue(space.maxCapacity - space.capacity, _("VM_CURRENT_MEMORY_CHART_MAX_CAPACITY"), space.name);
            }
        }

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

        DiscreteTimeData<Long>[] cpuData = facade.getCollectorRunTime(collectorName);
        TimeSeries series = new TimeSeries("gc-runs");
        for (DiscreteTimeData<Long> data : cpuData) {
            series.add(new FixedMillisecond(data.getTimeInMillis()), data.getData());
        }
        TimeSeriesCollection dataset = new TimeSeriesCollection();
        dataset.addSeries(series);
        JFreeChart chart = ChartFactory.createTimeSeriesChart(
                null,
                _("VM_GC_COLLECTOR_CHART_REAL_TIME_LABEL"),
                _("VM_GC_COLLECTOR_CHART_GC_TIME_LABEL"),
                dataset,
                false, false, false);

        ChartPanel chartPanel = new ChartPanel(chart);
        // make this chart non-interactive
        chartPanel.setDisplayToolTips(true);
        chartPanel.setDoubleBuffered(true);
        chartPanel.setMouseZoomable(false);
        chartPanel.setPopupMenu(null);

        detailsPanel.add(chartPanel, BorderLayout.CENTER);

        return detailsPanel;
    }

}
