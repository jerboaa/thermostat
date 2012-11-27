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

import java.awt.EventQueue;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.PlatformUI;
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

import com.redhat.thermostat.client.ui.SampledDataset;
import com.redhat.thermostat.common.locale.Translate;
import com.redhat.thermostat.eclipse.SWTComponent;
import com.redhat.thermostat.storage.model.IntervalTimeData;
import com.redhat.thermostat.vm.gc.client.core.VmGcView;
import com.redhat.thermostat.vm.gc.client.locale.LocaleResources;

public class SWTVmGcView extends VmGcView implements SWTComponent {
    
    private static final Translate<LocaleResources> translator = LocaleResources.createLocalizer();
    
    private final Map<String, SampledDataset> dataset;
    private final Map<String, Composite> subPanels;
    private final Map<String, JFreeChart> charts;
    
    private Composite parent;

    public SWTVmGcView(Composite parent) {
        this.parent = parent;
        dataset = new HashMap<String, SampledDataset>();
        subPanels = Collections.synchronizedMap(new HashMap<String, Composite>());
        charts = new HashMap<String, JFreeChart>();
    }

    @Override
    public void addChart(final String tag, final String title, final String units) {
        EventQueue.invokeLater(new Runnable() {
            @Override
            public void run() {
                SampledDataset newData = new SampledDataset();
                dataset.put(tag, newData);
                Composite subPanel = createCollectorDetailsPanel(tag, newData, title, units);
                subPanels.put(tag, subPanel);
            }
        });
    }

    @Override
    public void removeChart(final String tag) {
        EventQueue.invokeLater(new Runnable() {
            @Override
            public void run() {
                dataset.remove(tag);
                charts.remove(tag);
                Composite subPanel = subPanels.remove(tag);
                destroyChartComposite(subPanel);
            }
        });
    }

    @Override
    public void addData(final String tag, List<IntervalTimeData<Double>> data) {
        final List<IntervalTimeData<Double>> copy = new ArrayList<>(data);
        EventQueue.invokeLater(new Runnable() {
            @Override
            public void run() {
                SampledDataset series = dataset.get(tag);
                for (IntervalTimeData<Double> timeData: copy) {
                    series.add(timeData.getStartTimeInMillis(), timeData.getEndTimeInMillis(), timeData.getData());
                }
                series.fireSeriesChanged();
            }
        });
    }

    @Override
    public void clearData(final String tag) {
        EventQueue.invokeLater(new Runnable() {
            @Override
            public void run() {
                SampledDataset series = dataset.get(tag);
                series.clear();
            }
        });
    }
    
    private Composite createCollectorDetailsPanel(String tag, IntervalXYDataset collectorData, final String title, String units) {
        // Create chart
        final JFreeChart chart = ChartFactory.createHistogram(
            null,
            translator.localize(LocaleResources.VM_GC_COLLECTOR_CHART_REAL_TIME_LABEL),
            translator.localize(LocaleResources.VM_GC_COLLECTOR_CHART_GC_TIME_LABEL, units),
            collectorData,
            PlotOrientation.VERTICAL,
            false,
            false,
            false);

        ((XYBarRenderer)(chart.getXYPlot().getRenderer())).setBarPainter(new StandardXYBarPainter());

        setupPlotAxes(chart.getXYPlot());

        chart.getXYPlot().setDomainCrosshairLockedOnData(true);
        chart.getXYPlot().setDomainCrosshairVisible(true);

        // An array so we can modify it in the UI thread
        final Composite detailsTop[] = new Composite[1];
        
        PlatformUI.getWorkbench().getDisplay().asyncExec(new Runnable() {

            @Override
            public void run() {
                detailsTop[0] = createChartComposite(title, chart);
                parent.layout();
            }
        });
        
        charts.put(tag, chart);
       
        return detailsTop[0];
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

    private Composite createChartComposite(final String title,
            final JFreeChart chart) {
        Composite detailsTop = new Composite(parent, SWT.NONE);
        detailsTop.setLayout(new GridLayout());
        detailsTop.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

        Label summaryLabel = new Label(detailsTop, SWT.LEAD);
        Font stdFont = summaryLabel.getFont();
        Font boldFont = new Font(stdFont.getDevice(),
                stdFont.getFontData()[0].getName(),
                stdFont.getFontData()[0].getHeight(), SWT.BOLD);

        summaryLabel.setText(title);
        summaryLabel.setFont(boldFont);

        final RecentTimeSeriesChartComposite chartTop = new RecentTimeSeriesChartComposite(
                detailsTop, SWT.NONE, chart);
        chartTop.setLayout(new GridLayout());
        chartTop.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

        chart.addProgressListener(new ChartProgressListener() {

            @Override
            public void chartProgress(ChartProgressEvent event) {
                if (event.getType() != ChartProgressEvent.DRAWING_FINISHED) {
                    return;
                }

                double rangeCrossHairValue = event.getChart().getXYPlot()
                        .getRangeCrosshairValue();
                chartTop.setDataInformationLabel(String
                        .valueOf(rangeCrossHairValue));
            }
        });

        return detailsTop;
    }

    private void destroyChartComposite(final Composite top) {
        PlatformUI.getWorkbench().getDisplay().asyncExec(new Runnable() {

            @Override
            public void run() {
                if (top != null && !top.isDisposed()) {
                    top.dispose();
                    parent.layout();
                }
            }
        });
    }
    
    public JFreeChart getChart(String tag) {
        return charts.get(tag);
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
