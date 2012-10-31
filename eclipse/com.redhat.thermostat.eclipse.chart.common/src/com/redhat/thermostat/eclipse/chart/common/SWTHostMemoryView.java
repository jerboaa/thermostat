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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.PlatformUI;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.data.time.FixedMillisecond;
import org.jfree.data.time.RegularTimePeriod;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;

import com.redhat.thermostat.client.core.views.HostMemoryView;
import com.redhat.thermostat.client.locale.LocaleResources;
import com.redhat.thermostat.client.ui.ChartColors;
import com.redhat.thermostat.common.locale.Translate;
import com.redhat.thermostat.common.model.DiscreteTimeData;
import com.redhat.thermostat.common.utils.DisplayableValues;
import com.redhat.thermostat.common.utils.DisplayableValues.Scale;
import com.redhat.thermostat.eclipse.SWTComponent;
import com.redhat.thermostat.eclipse.ThermostatConstants;

public class SWTHostMemoryView extends HostMemoryView implements SWTComponent {
    public static final String TEST_ID_TOTAL_MEM = "SWTHostMemoryView.totalMemory";
    public static final String TEST_ID_LEGEND_ITEM_LABEL = "SWTHostMemoryView.legendItemLabel";
    public static final String TEST_ID_LEGEND_ITEM_CHECKBOX = "SWTHostMemoryView.legendItemCheckbox";
    
    private static final String LEGEND_COLOUR_BLOCK = "\u2588";
    private static final int H_INDENT = 20;
    private static final int SPACER_WIDTH = 10;
    private static final Translate<LocaleResources> translator = LocaleResources.createLocalizer();
    
    private final TimeSeriesCollection memoryCollection;
    private final Map<String, TimeSeries> dataset;
    private final Map<String, Color> colors;
    private final CopyOnWriteArrayList<GraphVisibilityChangeListener> listeners;
    private final Map<String, Composite> checkboxes;
    private final CountDownLatch latch;
    
    private Composite parent;
    private Label totalMemory;
    private ViewVisibilityWatcher watcher;
    private JFreeChart chart;
    private Composite legendTop;
    
    public SWTHostMemoryView() {
        this.memoryCollection = new TimeSeriesCollection();
        this.dataset = Collections.synchronizedMap(new HashMap<String, TimeSeries>());
        this.colors = new HashMap<String, Color>();
        this.listeners = new CopyOnWriteArrayList<GraphVisibilityChangeListener>();
        this.checkboxes = new HashMap<String, Composite>();
        this.watcher = new ViewVisibilityWatcher(notifier);
        this.latch = new CountDownLatch(1);
        this.chart = createMemoryChart();
    }
    
    public void createControl(Composite parent) {
        this.parent = parent;
        
        Label summaryLabel = new Label(parent, SWT.LEAD);
        Font stdFont = summaryLabel.getFont();
        Font boldFont = new Font(stdFont.getDevice(),
                stdFont.getFontData()[0].getName(),
                stdFont.getFontData()[0].getHeight(), SWT.BOLD);
        
        summaryLabel.setText(translator.localize(LocaleResources.HOST_MEMORY_SECTION_OVERVIEW));
        summaryLabel.setFont(boldFont);
        
        Composite detailsTop = new Composite(parent, SWT.NONE);
        detailsTop.setLayout(new GridLayout(3, false));
        
        Label cpuModelLabel = new Label(detailsTop, SWT.TRAIL);
        cpuModelLabel.setText(translator.localize(LocaleResources.HOST_INFO_MEMORY_TOTAL));
        GridData hIndentLayoutData = new GridData();
        hIndentLayoutData.horizontalIndent = H_INDENT;
        cpuModelLabel.setLayoutData(hIndentLayoutData);
        
        Label cpuModelSpacer = new Label(detailsTop, SWT.NONE);
        cpuModelSpacer.setLayoutData(new GridData(SPACER_WIDTH, SWT.DEFAULT));
        
        totalMemory = new Label(detailsTop, SWT.LEAD);
        totalMemory.setData(ThermostatConstants.TEST_TAG, TEST_ID_TOTAL_MEM);
        totalMemory.setText("Unknown");
        
        Composite chartTop = new RecentTimeSeriesChartComposite(parent, SWT.NONE, chart);
        chartTop.setLayout(new GridLayout());
        chartTop.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
        
        legendTop = new Composite(parent, SWT.NONE);
        RowLayout legendLayout = new RowLayout(SWT.HORIZONTAL);
        legendLayout.center = true;
        legendLayout.wrap = false;
        legendLayout.marginHeight = 0;
        legendTop.setLayout(legendLayout);
        
        // Notify threads that controls are created
        latch.countDown();
        
        // Don't start giving updates until controls are created
        watcher.watch(parent, ThermostatConstants.VIEW_ID_HOST_MEMORY);
    }

    @Override
    public void setTotalMemory(final String newValue) {
        PlatformUI.getWorkbench().getDisplay().asyncExec(new Runnable() {
            @Override
            public void run() {
                if (!totalMemory.isDisposed()) {
                    totalMemory.setText(newValue);
                }
            }
        });
    }
    
    private JFreeChart createMemoryChart() {
        // FIXME associate a fixed color with each type

        JFreeChart chart = ChartFactory.createTimeSeriesChart(
                null, // Title
                translator.localize(LocaleResources.HOST_MEMORY_CHART_TIME_LABEL), // x-axis Label
                translator.localize(LocaleResources.HOST_MEMORY_CHART_SIZE_LABEL, Scale.MiB.name()), // y-axis Label
                memoryCollection, // Dataset
                false, // Show Legend
                false, // Use tooltips
                false // Configure chart to generate URLs?
                );

        chart.getPlot().setBackgroundPaint( new Color(255,255,255,0) );
        chart.getPlot().setBackgroundImageAlpha(0.0f);
        chart.getPlot().setOutlinePaint(new Color(0,0,0,0));

        NumberAxis rangeAxis = (NumberAxis) chart.getXYPlot().getRangeAxis();
        rangeAxis.setAutoRangeMinimumSize(100);

        return chart;
    }
    
    private void fireShowHideHandlers(boolean show, String tag) {
        for (GraphVisibilityChangeListener listener: listeners) {
            if (show) {
                listener.show(tag);
            } else {
                listener.hide(tag);
            }
        }
    }
    
    @Override
    public void addMemoryChart(final String tag, final String humanReadableName) {
        EventQueue.invokeLater(new Runnable() {
            @Override
            public void run() {
                int colorIndex = colors.size();
                colors.put(tag, ChartColors.getColor(colorIndex));
                TimeSeries series = new TimeSeries(tag);
                dataset.put(tag, series);

                addLegendItem(tag, humanReadableName);
                
                updateColors();
            }
        });
    }

    @Override
    public void removeMemoryChart(final String tag) {
        EventQueue.invokeLater(new Runnable() {
            @Override
            public void run() {
                TimeSeries series = dataset.remove(tag);
                memoryCollection.removeSeries(series);
                
                removeLegendItem(tag);

                updateColors();
            }
        });
    }

    @Override
    public void showMemoryChart(final String tag) {
        EventQueue.invokeLater(new Runnable() {
            @Override
            public void run() {
                TimeSeries series = dataset.get(tag);
                memoryCollection.addSeries(series);

                updateColors();
            }
        });
    }

    @Override
    public void hideMemoryChart(final String tag) {
        EventQueue.invokeLater(new Runnable() {
            @Override
            public void run() {
                TimeSeries series = dataset.get(tag);
                memoryCollection.removeSeries(series);

                updateColors();
            }
        });
    }

    @Override
    public void addMemoryData(final String tag, List<DiscreteTimeData<? extends Number>> data) {
        final List<DiscreteTimeData<? extends Number>> copy = new ArrayList<>(data);
        EventQueue.invokeLater(new Runnable() {
            @Override
            public void run() {
                final TimeSeries series = dataset.get(tag);
                for (DiscreteTimeData<? extends Number> timeData: copy) {
                    RegularTimePeriod period = new FixedMillisecond(timeData.getTimeInMillis());
                    if (series.getDataItem(period) == null) {
                        Long sizeInBytes = (Long) timeData.getData();
                        Double sizeInMegaBytes = DisplayableValues.Scale.convertTo(Scale.MiB, sizeInBytes);
                        series.add(new FixedMillisecond(timeData.getTimeInMillis()), sizeInMegaBytes, false);
                    }
                }
                series.fireSeriesChanged();
            }
        });
    }

    @Override
    public void clearMemoryData(final String tag) {
        EventQueue.invokeLater(new Runnable() {
            @Override
            public void run() {
                TimeSeries series = dataset.get(tag);
                series.clear();
            }
        });
    }
    
    @Override
    public void addGraphVisibilityListener(GraphVisibilityChangeListener listener) {
        listeners.add(listener);
    }

    @Override
    public void removeGraphVisibilityListener(GraphVisibilityChangeListener listener) {
        listeners.remove(listener);
    }

    /**
     * Adding or removing series to the series collection may change the order
     * of existing items. Plus the paint for the index is now out-of-date. So
     * let's walk through all the series and set the right paint for those.
     */
    private void updateColors() {
        XYItemRenderer itemRenderer = chart.getXYPlot().getRenderer();
        for (int i = 0; i < memoryCollection.getSeriesCount(); i++) {
            String tag = (String) memoryCollection.getSeriesKey(i);
            Color color = colors.get(tag);
            itemRenderer.setSeriesPaint(i, color);
        }
    }
    
    private Composite createLabelWithLegend(Composite parent, String text, Color color, final String tag) {
        Composite top = new Composite(parent, SWT.NONE);
        RowLayout topLayout = new RowLayout(SWT.HORIZONTAL);
        topLayout.marginHeight = 0;
        topLayout.center = true;
        top.setLayout(topLayout);
        
        final Button checkBox = new Button(top, SWT.CHECK);
        checkBox.setData(ThermostatConstants.TEST_TAG, TEST_ID_LEGEND_ITEM_CHECKBOX);
        checkBox.addSelectionListener(new SelectionListener() {
            
            @Override
            public void widgetSelected(SelectionEvent e) {
                fireShowHideHandlers(checkBox.getSelection(), tag);
            }
            
            @Override
            public void widgetDefaultSelected(SelectionEvent e) {
                widgetSelected(e);
            }
        });
        checkBox.setSelection(true);
        checkBox.setAlignment(SWT.RIGHT);
        
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
        colourText.setData(ThermostatConstants.TEST_TAG, TEST_ID_LEGEND_ITEM_LABEL);
        colourText.setText(text);
        return top;
    }

    private void addLegendItem(final String tag, final String humanReadableName) {
        // We need to wait for the controls to be fully constructed
        // before modifying the legend
        ChartUtils.runAfterCreated(latch, new Runnable() {
            @Override
            public void run() {
                Composite checkbox = createLabelWithLegend(legendTop,
                        humanReadableName, colors.get(tag), tag);
                checkboxes.put(tag, checkbox);
                parent.layout();
            }
        });
    }

    private void removeLegendItem(final String tag) {
        // We need to wait for the controls to be fully constructed
        // before modifying the legend
        ChartUtils.runAfterCreated(latch, new Runnable() {
            @Override
            public void run() {
                Composite checkbox = checkboxes.remove(tag);
                checkbox.dispose();
                parent.layout();
            }
        });
    }
    
    public JFreeChart getChart() {
        return chart;
    }
    
    public TimeSeries getSeries(String tag) {
        return dataset.get(tag);
    }

}
