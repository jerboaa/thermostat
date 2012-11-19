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

package com.redhat.thermostat.eclipse.chart.common;

import java.awt.Color;
import java.awt.EventQueue;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.PlatformUI;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.data.time.FixedMillisecond;
import org.jfree.data.time.RegularTimePeriod;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;

import com.redhat.thermostat.client.core.views.HostCpuView;
import com.redhat.thermostat.client.locale.LocaleResources;
import com.redhat.thermostat.client.ui.ChartColors;
import com.redhat.thermostat.common.locale.Translate;
import com.redhat.thermostat.eclipse.SWTComponent;
import com.redhat.thermostat.eclipse.ThermostatConstants;
import com.redhat.thermostat.storage.model.DiscreteTimeData;

public class SWTHostCpuView extends HostCpuView implements SWTComponent {
    public static final String TEST_ID_CPU_MODEL = "SWTHostCpuView.cpuModel";
    public static final String TEST_ID_CPU_COUNT = "SWTHostCpuView.cpuCount";
    public static final String TEST_ID_LEGEND_ITEM = "SWTHostCpuView.legendItem";
    
    private static final String LEGEND_COLOUR_BLOCK = "\u2588";
    private static final int H_INDENT = 20;
    private static final int SPACER_WIDTH = 10;
    private static final Translate<LocaleResources> translator = LocaleResources.createLocalizer();
    
    private JFreeChart chart;
    private TimeSeriesCollection datasetCollection;
    private Label cpuModel;
    private Label cpuCount;
    
    private final Map<Integer, TimeSeries> datasets;
    private final Map<String, Color> colors;
    private Composite chartTop;
    private Composite legendTop;
    private Composite parent;
    
    public SWTHostCpuView(Composite parent) {
        this.parent = parent;
        datasetCollection = new TimeSeriesCollection();
        datasets = new HashMap<Integer, TimeSeries>();
        colors = new HashMap<String, Color>();
        chart = createCpuChart();
        
        Label summaryLabel = new Label(parent, SWT.LEAD);
        Font stdFont = summaryLabel.getFont();
        Font boldFont = new Font(stdFont.getDevice(),
                stdFont.getFontData()[0].getName(),
                stdFont.getFontData()[0].getHeight(), SWT.BOLD);
        
        summaryLabel.setText(translator.localize(LocaleResources.HOST_CPU_SECTION_OVERVIEW));
        summaryLabel.setFont(boldFont);
        
        Composite detailsTop = new Composite(parent, SWT.NONE);
        detailsTop.setLayout(new GridLayout(3, false));
        
        Label cpuModelLabel = new Label(detailsTop, SWT.TRAIL);
        cpuModelLabel.setText(translator.localize(LocaleResources.HOST_INFO_CPU_MODEL));
        GridData hIndentLayoutData = new GridData();
        hIndentLayoutData.horizontalIndent = H_INDENT;
        cpuModelLabel.setLayoutData(hIndentLayoutData);
        
        Label cpuModelSpacer = new Label(detailsTop, SWT.NONE);
        cpuModelSpacer.setLayoutData(new GridData(SPACER_WIDTH, SWT.DEFAULT));
        
        cpuModel = new Label(detailsTop, SWT.LEAD);
        cpuModel.setData(ThermostatConstants.TEST_TAG, TEST_ID_CPU_MODEL);
        cpuModel.setText("Unknown");
        
        Label cpuCountLabel = new Label(detailsTop, SWT.TRAIL);
        cpuCountLabel.setText(translator.localize(LocaleResources.HOST_INFO_CPU_COUNT));
        cpuCountLabel.setLayoutData(hIndentLayoutData);
        
        Label cpuCountSpacer = new Label(detailsTop, SWT.NONE);
        cpuCountSpacer.setLayoutData(new GridData(SPACER_WIDTH, SWT.DEFAULT));
        
        cpuCount = new Label(detailsTop, SWT.LEAD);
        cpuCount.setData(ThermostatConstants.TEST_TAG, TEST_ID_CPU_COUNT);
        cpuCount.setText("Unknown");
        
        chartTop = new RecentTimeSeriesChartComposite(parent, SWT.NONE, chart);
        chartTop.setLayout(new GridLayout());
        chartTop.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
        
        legendTop = new Composite(parent, SWT.NONE);
        RowLayout legendLayout = new RowLayout(SWT.HORIZONTAL);
        legendLayout.center = true;
        legendLayout.wrap = false;
        legendLayout.marginHeight = 0;
        legendTop.setLayout(legendLayout);
    }
    
    private JFreeChart createCpuChart() {
        JFreeChart chart = ChartFactory.createTimeSeriesChart(null,
                translator.localize(LocaleResources.HOST_CPU_USAGE_CHART_TIME_LABEL),
                translator.localize(LocaleResources.HOST_CPU_USAGE_CHART_VALUE_LABEL),
                datasetCollection, false, false, false);

        chart.getPlot().setBackgroundPaint(new Color(255, 255, 255, 0));
        chart.getPlot().setBackgroundImageAlpha(0.0f);
        chart.getPlot().setOutlinePaint(new Color(0, 0, 0, 0));

        return chart;
    }

    public void setCpuCount(final String count) {
        PlatformUI.getWorkbench().getDisplay().asyncExec(new Runnable() {
            @Override
            public void run() {
                if (!cpuCount.isDisposed()) {
                    cpuCount.setText(count);
                }
            }
        });
    }

    public void setCpuModel(final String model) {
        PlatformUI.getWorkbench().getDisplay().asyncExec(new Runnable() {
            @Override
            public void run() {
                if (!cpuModel.isDisposed()) {
                    cpuModel.setText(model);
                }
            }
        });
    }

    @Override
    public void addCpuUsageChart(final int cpuIndex, final String humanReadableName) {
        EventQueue.invokeLater(new Runnable() {

            @Override
            public void run() {
                TimeSeries series = new TimeSeries(humanReadableName);
                Color color = ChartColors.getColor(colors.size());
                colors.put(humanReadableName, color);

                datasets.put(cpuIndex, series);
                datasetCollection.addSeries(series);

                updateColors();

                addLegendItem(humanReadableName, color);
            }
        });
    }

    @Override
    public void addCpuUsageData(final int cpuIndex, List<DiscreteTimeData<Double>> data) {
        final ArrayList<DiscreteTimeData<Double>> copy = new ArrayList<>(data);
        EventQueue.invokeLater(new Runnable() {
            @Override
            public void run() {
                TimeSeries dataset = datasets.get(cpuIndex);
                for (DiscreteTimeData<Double> timeData: copy) {
                    RegularTimePeriod period = new FixedMillisecond(timeData.getTimeInMillis());
                    if (dataset.getDataItem(period) == null) {
                        dataset.add(period, timeData.getData(), false);
                    }
                }
                dataset.fireSeriesChanged();
            }
        });
    }

    @Override
    public void clearCpuUsageData() {
        EventQueue.invokeLater(new Runnable() {
            @Override
            public void run() {
                for (Iterator<Map.Entry<Integer, TimeSeries>> iter = datasets.entrySet().iterator(); iter.hasNext();) {
                    Map.Entry<Integer, TimeSeries> entry = iter.next();
                    datasetCollection.removeSeries(entry.getValue());
                    entry.getValue().clear();

                    iter.remove();

                }
                updateColors();
            }
        });
    }
    
    /**
     * Adding or removing series to the series collection may change the order
     * of existing items. Plus the paint for the index is now out-of-date. So
     * let's walk through all the series and set the right paint for those.
     */
    private void updateColors() {
        XYItemRenderer itemRenderer = chart.getXYPlot().getRenderer();
        for (int i = 0; i < datasetCollection.getSeriesCount(); i++) {
            String tag = (String) datasetCollection.getSeriesKey(i);
            Color color = colors.get(tag);
            itemRenderer.setSeriesPaint(i, color);
        }
    }
    
    
    private Composite createLabelWithLegend(Composite parent, String text, Color color) {
        Composite top = new Composite(parent, SWT.NONE);
        GridLayout topLayout = new GridLayout(2, false);
        topLayout.marginHeight = 0;
        top.setLayout(topLayout);
        
        Label colourBlock = new Label(top, SWT.NONE);
        colourBlock.setText(LEGEND_COLOUR_BLOCK);
        
        // Convert to SWT colour
        final org.eclipse.swt.graphics.Color swtColour = new org.eclipse.swt.graphics.Color(
                PlatformUI.getWorkbench().getDisplay(), color.getRed(),
                color.getGreen(), color.getBlue());
        colourBlock.addDisposeListener(new DisposeListener() {

            @Override
            public void widgetDisposed(DisposeEvent e) {
                swtColour.dispose();
            }
        });
        colourBlock.setForeground(swtColour);
        
        Label colourText = new Label(top, SWT.NONE);
        colourText.setData(ThermostatConstants.TEST_TAG, TEST_ID_LEGEND_ITEM);
        colourText.setText(text);
        return top;
    }
    
    private void addLegendItem(final String humanReadableName, final Color color) {
        PlatformUI.getWorkbench().getDisplay().asyncExec(new Runnable() {
            @Override
            public void run() {
                createLabelWithLegend(legendTop, humanReadableName,
                        color);
                parent.layout();
            }
        });
    }
    
    public JFreeChart getChart() {
        return chart;
    }

    @Override
    public void show() {
        notifier.fireAction(Action.VISIBLE);
    }

    @Override
    public void hide() {
        notifier.fireAction(Action.HIDDEN);
    }
    
}
