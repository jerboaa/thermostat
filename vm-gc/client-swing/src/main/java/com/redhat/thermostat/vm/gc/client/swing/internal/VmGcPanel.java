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

package com.redhat.thermostat.vm.gc.client.swing.internal;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;

import com.redhat.thermostat.common.utils.LoggingUtils;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.DateAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.event.ChartProgressEvent;
import org.jfree.chart.event.ChartProgressListener;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.StandardXYBarPainter;
import org.jfree.chart.renderer.xy.XYBarRenderer;
import org.jfree.data.RangeType;
import org.jfree.data.xy.IntervalXYDataset;

import com.redhat.thermostat.client.core.experimental.Duration;
import com.redhat.thermostat.client.swing.SwingComponent;
import com.redhat.thermostat.client.swing.components.FontAwesomeIcon;
import com.redhat.thermostat.client.swing.components.HeaderPanel;
import com.redhat.thermostat.client.swing.components.SectionHeader;
import com.redhat.thermostat.client.swing.components.experimental.RecentTimeControlPanel;
import com.redhat.thermostat.client.swing.components.experimental.SingleValueChartPanel;
import com.redhat.thermostat.client.swing.experimental.ComponentVisibilityNotifier;
import com.redhat.thermostat.client.ui.RecentTimeSeriesChartController;
import com.redhat.thermostat.client.ui.SampledDataset;
import com.redhat.thermostat.common.ActionListener;
import com.redhat.thermostat.common.ActionNotifier;
import com.redhat.thermostat.gc.remote.client.common.RequestGCAction;
import com.redhat.thermostat.gc.remote.client.swing.ToolbarGCButton;
import com.redhat.thermostat.gc.remote.common.command.GCAction;
import com.redhat.thermostat.shared.locale.LocalizedString;
import com.redhat.thermostat.shared.locale.Translate;
import com.redhat.thermostat.storage.model.IntervalTimeData;
import com.redhat.thermostat.vm.gc.client.core.VmGcView;
import com.redhat.thermostat.vm.gc.client.locale.LocaleResources;
import com.redhat.thermostat.vm.gc.common.GcCommonNameMapper.CollectorCommonName;
import com.redhat.thermostat.vm.gc.common.params.GcParam;
import com.redhat.thermostat.vm.gc.common.params.GcParamsMapper;
import com.redhat.thermostat.vm.gc.common.params.JavaVersionRange;

public class VmGcPanel extends VmGcView implements SwingComponent {

    private static final Logger logger = LoggingUtils.getLogger(VmGcPanel.class);
    private static final Translate<LocaleResources> translator = LocaleResources.createLocalizer();
    private static final String GC_ALGO_LABEL_NAME = translator.localize(LocaleResources.VM_GC_CONFIGURED_COLLECTOR).getContents();

    private static final Color WHITE = new Color(255,255,255,0);
    private static final Color BLACK = new Color(0,0,0,0);
    private static final float TRANSPARENT = 0.0f;

    private static final int DEFAULT_VALUE = 10;
    private static final TimeUnit DEFAULT_UNIT = TimeUnit.MINUTES;

    private static final char INFO_CIRCLE_ICON_ID = '\uf05a';
    private static final int INFO_CIRCLE_ICON_SIZE = 12;

    private HeaderPanel visiblePanel = new HeaderPanel();
    private JPanel chartPanelContainer = new JPanel();
    private JPanel containerPanel = new JPanel();
    private JLabel gcAlgoLabelDescr;
    private JLabel commonNameLabel;
    private JButton gcAlgoInfoButton;
    private CollectorCommonName collectorCommonName;
    private JavaVersionRange javaVersionRange;

    private GcParamsMapper gcParamsMapper = GcParamsMapper.getInstance();

    private ToolbarGCButton toolbarGCButton;
    private RequestGCAction requestGCAction;

    private ActionNotifier<UserAction> userActionNotifier = new ActionNotifier<>(this);

    private final Map<String, SampledDataset> dataset = new HashMap<>();
    private final Map<String, List<IntervalTimeData<Double>>> addedData = new HashMap<>();
    private final Map<String, JPanel> subPanels = new HashMap<>();
    private final Map<String, Duration> subPanelDurations = new HashMap<>();

    private final GridBagConstraints gcPanelConstraints;

    public VmGcPanel() {
        super();

        gcAlgoInfoButton = new JButton(new FontAwesomeIcon(INFO_CIRCLE_ICON_ID, INFO_CIRCLE_ICON_SIZE));
        gcAlgoInfoButton.setVisible(false);
        gcAlgoInfoButton.setBorderPainted(false);
        gcAlgoInfoButton.setContentAreaFilled(false);
        gcAlgoInfoButton.setToolTipText(translator.localize(LocaleResources.VM_GC_INFO_BUTTON_TOOLTIP).getContents());
        gcAlgoInfoButton.addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                if (collectorCommonName == CollectorCommonName.UNKNOWN_COLLECTOR) {
                    showCollectorInfoErrorDialog(LocaleResources.VM_GC_UNKNOWN_COLLECTOR);
                    return;
                }
                if (javaVersionRange == null) {
                    showCollectorInfoErrorDialog(LocaleResources.VM_GC_UNKNOWN_JAVA_VERSION);
                    return;
                }

                List<GcParam> params;
                params = gcParamsMapper.getParams(collectorCommonName, javaVersionRange);
                StringBuilder paramsMessage = new StringBuilder();
                for (GcParam param : params) {
                    paramsMessage.append(System.lineSeparator()).append(param.getFlag());
                }
                JOptionPane optionPane = new JOptionPane();
                optionPane.setMessageType(JOptionPane.INFORMATION_MESSAGE);
                String message = translator.localize(LocaleResources.VM_GC_PARAMETERS_MESSAGE,
                        paramsMessage.toString())
                        .getContents();
                optionPane.setMessage(message);
                JDialog dialog = optionPane.createDialog(gcAlgoInfoButton,
                        translator.localize(LocaleResources.VM_GC_PARAMETERS_TITLE).getContents());
                dialog.setModal(false);
                dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
                dialog.setVisible(true);
            }
        });

        initializePanel();
        gcPanelConstraints = new GridBagConstraints();
        gcPanelConstraints.gridx = 0;
        gcPanelConstraints.gridy = 0;
        gcPanelConstraints.fill = GridBagConstraints.BOTH;
        gcPanelConstraints.weightx = 1;
        gcPanelConstraints.weighty = 1;

        requestGCAction = new RequestGCAction();
        toolbarGCButton = new ToolbarGCButton(requestGCAction);
        toolbarGCButton.setName("gcButton");
        visiblePanel.addToolBarButton(toolbarGCButton);

        new ComponentVisibilityNotifier().initialize(visiblePanel, notifier);
    }

    private void showCollectorInfoErrorDialog(LocaleResources resource) {
        String message = translator.localize(resource).getContents();
        JOptionPane optionPane = new JOptionPane();
        optionPane.setMessageType(JOptionPane.ERROR_MESSAGE);
        optionPane.setMessage(message);
        JDialog dialog = optionPane.createDialog(gcAlgoInfoButton,
                translator.localize(LocaleResources.VM_GC_PARAMETERS_TITLE).getContents());
        dialog.setModal(true);
        dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        dialog.setVisible(true);
    }

    @Override
    public void addUserActionListener(ActionListener<UserAction> listener) {
        userActionNotifier.addActionListener(listener);
    }

    @Override
    public void removeUserActionListener(ActionListener<UserAction> listener) {
        userActionNotifier.removeActionListener(listener);
    }

    @Override
    public void addActionListener(ActionListener<Action> listener) {
        notifier.addActionListener(listener);
    }

    @Override
    public void removeActionListener(ActionListener<Action> listener) {
        notifier.removeActionListener(listener);
    }

    @Override
    public Component getUiComponent() {
        return visiblePanel;
    }

    private void initializePanel() {
        visiblePanel.setContent(containerPanel);
        visiblePanel.setHeader(translator.localize(LocaleResources.VM_GC_TITLE));
        containerPanel.setLayout(new GridBagLayout());
        GridBagConstraints commonNameConstraints = getWeightedGridBagConstraint(0.03);
        commonNameConstraints.gridy = 0;
        GridBagConstraints chartPanelConstraints = getWeightedGridBagConstraint(0.97);
        chartPanelConstraints.gridy = 1;
        chartPanelContainer.setLayout(new GridBagLayout());
        JPanel commonNamePanel = createCollectorsCommonPanel();
        containerPanel.add(commonNamePanel, commonNameConstraints);
        containerPanel.add(chartPanelContainer, chartPanelConstraints);
    }
    
    private GridBagConstraints getWeightedGridBagConstraint(double weightY) {
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 0;
        c.weightx = 1;
        c.weighty = weightY;
        c.fill = GridBagConstraints.BOTH;
        return c;
    }
    
    private JPanel createCollectorsCommonPanel() {
        JPanel commonPanel = new JPanel();
        FlowLayout layout = new FlowLayout();
        layout.setAlignment(FlowLayout.LEFT);
        layout.setHgap(3);
        commonPanel.setLayout(layout);
        // Common name for a GC algo might not be shown at all. Thus
        // fill with empty labels first and set a visible text only once
        // we know there is a known mapping.
        this.gcAlgoLabelDescr = new JLabel(""); // intentionally empty string
        this.commonNameLabel = new JLabel(""); // intentionally empty string
        commonPanel.add(gcAlgoLabelDescr);
        commonPanel.add(commonNameLabel);
        commonPanel.add(gcAlgoInfoButton);
        return commonPanel;
    }

    private JPanel createCollectorDetailsPanel(IntervalXYDataset collectorData, LocalizedString title, String units, final String tag) {
        JPanel detailsPanel = new JPanel();
        detailsPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        detailsPanel.setLayout(new BorderLayout());

        detailsPanel.add(new SectionHeader(title), BorderLayout.NORTH);

        JFreeChart chart = ChartFactory.createHistogram(
            null,
            translator.localize(LocaleResources.VM_GC_COLLECTOR_CHART_REAL_TIME_LABEL).getContents(),
            translator.localize(LocaleResources.VM_GC_COLLECTOR_CHART_GC_TIME_LABEL, units).getContents(),
            collectorData,
            PlotOrientation.VERTICAL,
            false,
            false,
            false);

        ((XYBarRenderer)(chart.getXYPlot().getRenderer())).setBarPainter(new StandardXYBarPainter());

        setupPlotAxes(chart.getXYPlot());

        chart.getXYPlot().setDomainCrosshairLockedOnData(true);
        chart.getXYPlot().setDomainCrosshairVisible(true);

        chart.getPlot().setBackgroundPaint(WHITE);
        chart.getPlot().setBackgroundImageAlpha(TRANSPARENT);
        chart.getPlot().setOutlinePaint(BLACK);

        Duration defaultDuration = new Duration(DEFAULT_VALUE, DEFAULT_UNIT);
        final RecentTimeSeriesChartController chartController = new RecentTimeSeriesChartController(chart);
        final SingleValueChartPanel chartPanel = new SingleValueChartPanel(chart, defaultDuration);
        subPanelDurations.put(tag, defaultDuration);
        chart.addProgressListener(new ChartProgressListener() {

            @Override
            public void chartProgress(ChartProgressEvent event) {
                if (event.getType() != ChartProgressEvent.DRAWING_FINISHED) {
                    return;
                }

                double rangeCrossHairValue = event.getChart().getXYPlot().getRangeCrosshairValue();
                chartPanel.setDataInformationLabel(String.valueOf(rangeCrossHairValue));
            }
        });

        chartPanel.addPropertyChangeListener(RecentTimeControlPanel.PROPERTY_VISIBLE_TIME_RANGE, new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                VmGcPanel.this.userActionNotifier.fireAction(UserAction.USER_CHANGED_TIME_RANGE);
                Duration duration = (Duration) evt.getNewValue();
                subPanelDurations.put(tag, duration);
                chartController.setTime(duration.value, duration.unit);
            }
        });

        detailsPanel.add(chartPanel, BorderLayout.CENTER);

        return detailsPanel;
    }

    private void setupPlotAxes(XYPlot plot) {
        setupDomainAxis(plot);
        setupRangeAxis(plot);
    }

    private void setupDomainAxis(XYPlot plot) {
        plot.setDomainAxis(new DateAxis());
    }

    private void setupRangeAxis(XYPlot plot) {
        NumberAxis rangeAxis = (NumberAxis) plot.getRangeAxis();

        rangeAxis.setRangeType(RangeType.POSITIVE);
        rangeAxis.setAutoRange(true);
        rangeAxis.setAutoRangeMinimumSize(1);
    }

    @Override
    public void addChart(final String tag, final LocalizedString title, final String units) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                SampledDataset newData = new SampledDataset();
                dataset.put(tag, newData);
                addedData.put(tag, new ArrayList<IntervalTimeData<Double>>());
                JPanel subPanel = createCollectorDetailsPanel(newData, title, units, tag);
                subPanels.put(tag, subPanel);
                chartPanelContainer.add(subPanel, gcPanelConstraints);
                gcPanelConstraints.gridy++;
                containerPanel.revalidate();
                containerPanel.repaint();
            }
        });
    }

    @Override
    public void removeChart(final String tag) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                dataset.remove(tag);
                addedData.remove(tag);
                JPanel subPanel = subPanels.remove(tag);
                subPanelDurations.remove(tag);
                chartPanelContainer.remove(subPanel);
                gcPanelConstraints.gridy--;
                containerPanel.revalidate();
                containerPanel.repaint();
            }
        });
    }

    @Override
    public void addData(final String tag, List<IntervalTimeData<Double>> data) {
        final List<IntervalTimeData<Double>> copy = new ArrayList<>(data);
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                SampledDataset series = dataset.get(tag);
                List<IntervalTimeData<Double>> data = addedData.get(tag);
                for (IntervalTimeData<Double> timeData: copy) {
                    if (!data.contains(timeData)) {
                        data.add(timeData);
                        series.add(timeData.getStartTimeInMillis(), timeData.getEndTimeInMillis(), timeData.getData());
                    }
                }
                series.fireSeriesChanged();
            }
        });
    }

    @Override
    public void clearData(final String tag) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                SampledDataset series = dataset.get(tag);
                series.clear();
            }
        });
    }


    @Override
    public void setCollectorInfo(final CollectorCommonName commonName, final String rawJavaVersion) {
        // only set values if we are able to show more info about the in-use
        // GC-algo.
        this.collectorCommonName = commonName;
        JavaVersionRange javaVersionRange;
        try {
            javaVersionRange = JavaVersionRange.fromString(rawJavaVersion);
        } catch (JavaVersionRange.InvalidJavaVersionFormatException | IllegalArgumentException e) {
            logger.warning(translator.localize(LocaleResources.VM_GC_ERROR_CANNOT_PARSE_JAVA_VERSION, rawJavaVersion).getContents());
            javaVersionRange = null;
        }
        this.javaVersionRange = javaVersionRange;
        if (commonName != CollectorCommonName.UNKNOWN_COLLECTOR) {
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    gcAlgoLabelDescr.setText(GC_ALGO_LABEL_NAME);
                    commonNameLabel.setText(collectorCommonName.getHumanReadableString());
                    gcAlgoInfoButton.setVisible(VmGcPanel.this.javaVersionRange != null);
                }
            });
        }
    }

    @Override
    public void setEnableGCAction(final boolean enable) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                toolbarGCButton.setEnabled(enable);
            }
        });
    }

    @Override
    public void addGCActionListener(ActionListener<GCAction> listener) {
        requestGCAction.addActionListener(listener);
    }

    @Override
    public Duration getUserDesiredDuration() {
        //Return the greatest duration of all controllers
        long timestamp = 0l;
        Duration maxDuration = new Duration(10, TimeUnit.MINUTES); //Default of 10 minutes if there are no controllers

        for (Duration duration: subPanelDurations.values()) {
            long time = duration.unit.toMillis(duration.value);
            if (time > timestamp) {
                timestamp = time;
                maxDuration = duration;
            }
        }
        return maxDuration;
    }

    @Override
    public void displayWarning(LocalizedString string) {
        JOptionPane.showMessageDialog(visiblePanel, string.getContents(), "Warning", JOptionPane.WARNING_MESSAGE);
    }
}

