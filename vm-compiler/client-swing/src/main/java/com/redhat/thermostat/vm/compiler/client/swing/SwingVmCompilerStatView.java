/*
 * Copyright 2012-2016 Red Hat, Inc.
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

package com.redhat.thermostat.vm.compiler.client.swing;

import java.awt.Component;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.concurrent.Callable;

import javax.swing.JSplitPane;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;

import com.redhat.thermostat.client.swing.EdtHelper;
import com.redhat.thermostat.client.swing.NonEditableTableModel;
import com.redhat.thermostat.client.swing.SwingComponent;
import com.redhat.thermostat.client.swing.components.HeaderPanel;
import com.redhat.thermostat.client.swing.components.MultiChartPanel;
import com.redhat.thermostat.client.swing.components.MultiChartPanel.DataGroup;
import com.redhat.thermostat.client.swing.components.ThermostatScrollPane;
import com.redhat.thermostat.client.swing.components.ThermostatTable;
import com.redhat.thermostat.client.swing.experimental.ComponentVisibilityNotifier;
import com.redhat.thermostat.common.Duration;
import com.redhat.thermostat.common.model.Range;
import com.redhat.thermostat.shared.locale.Translate;
import com.redhat.thermostat.storage.model.DiscreteTimeData;
import com.redhat.thermostat.vm.compiler.client.core.VmCompilerStatView;
import com.redhat.thermostat.vm.compiler.client.locale.LocaleResources;

public class SwingVmCompilerStatView extends VmCompilerStatView implements SwingComponent {

    private static final Translate<LocaleResources> t = LocaleResources.createLocalizer();

    private final HeaderPanel visiblePanel;

    private DefaultTableModel model;
    private MultiChartPanel multiChart;

    private DataGroup numberGroup;
    private DataGroup timeGroup;

    public SwingVmCompilerStatView() {
        visiblePanel = new HeaderPanel();

        visiblePanel.setHeader(t.localize(LocaleResources.VM_COMPILER_HEADER));

        JSplitPane splitPane = new JSplitPane();
        splitPane.setOrientation(JSplitPane.VERTICAL_SPLIT);
        splitPane.setOneTouchExpandable(true);

        visiblePanel.setContent(splitPane);

        model = createTableModel();

        ThermostatTable table = new ThermostatTable(model);
        ThermostatScrollPane tablePane = new ThermostatScrollPane(table);

        splitPane.setTopComponent(tablePane);

        multiChart = new MultiChartPanel();
        numberGroup = multiChart.createGroup();
        timeGroup = multiChart.createGroup();

        splitPane.setBottomComponent(multiChart);
        splitPane.setResizeWeight(0.5);

        new ComponentVisibilityNotifier().initialize(visiblePanel, notifier);

        addChartTypes();
    }

    private DefaultTableModel createTableModel() {
        DefaultTableModel model = new NonEditableTableModel(11, 2);
        Object[] columnIdentifiers = new Object[] {
                t.localize(LocaleResources.STATS_TABLE_COLUMN_NAME).getContents(),
                t.localize(LocaleResources.STATS_TABLE_COLUMN_VALUE).getContents()
        };
        Object[][] dataVector = new Object[][] {
            new Object[] { t.localize(LocaleResources.STATS_TOTAL_COMPILES).getContents(), 0 },
            new Object[] { t.localize(LocaleResources.STATS_TOTAL_BAILOUTS).getContents(), 0 },
            new Object[] { t.localize(LocaleResources.STATS_TOTAL_INVALIDATES).getContents(), 0 },
            new Object[] { t.localize(LocaleResources.STATS_COMPILATION_TIME).getContents(), 0 },
            new Object[] { t.localize(LocaleResources.STATS_LAST_SIZE).getContents(), 0 },
            new Object[] { t.localize(LocaleResources.STATS_LAST_TYPE).getContents(), 0 },
            new Object[] { t.localize(LocaleResources.STATS_LAST_METHOD).getContents(), 0 },
            new Object[] { t.localize(LocaleResources.STATS_LAST_FAILED_TYPE).getContents(), 0 },
            new Object[] { t.localize(LocaleResources.STATS_LAST_FAILED_METHOD).getContents(), 0 },
        };
        model.setDataVector(dataVector, columnIdentifiers);
        return model;
    }

    private void addChartTypes() {
        for (Type type : Type.values()) {
            DataGroup group = getGroup(type);
            String tag = getTag(type);
            multiChart.addChart(group, tag, type.getLabel());
            multiChart.showChart(group, tag);
        }

        multiChart.getRangeAxis(numberGroup).setLabel(
                t.localize(LocaleResources.STAT_CHART_NUMBER_AXIS).getContents());
        multiChart.getRangeAxis(timeGroup).setLabel(
                t.localize(LocaleResources.STAT_CHART_TIME_AXIS).getContents());
    }

    @Override
    public void setCurrentDisplay(final ViewData data) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                final int VALUE_COLUMN = 1;

                String[] values = {
                        data.totalCompiles,
                        data.totalBailouts,
                        data.totalInvalidates,
                        data.compilationTime,
                        data.lastSize,
                        data.lastType,
                        data.lastMethod,
                        data.lastFailedType,
                        data.lastFailedMethod,
                };

                int row = 0;
                for (String value : values) {
                    model.setValueAt(value, row, VALUE_COLUMN);
                    row++;
                }
            }

        });
    }

    @Override
    public Duration getUserDesiredDuration() {
        try {
            return new EdtHelper().callAndWait(new Callable<Duration>(){
                public Duration call() {
                    return multiChart.getUserDesiredDuration();
                }
            });
        } catch (InvocationTargetException | InterruptedException e) {
            return null;
        }
    }

    @Override
    public void setAvailableDataRange(Range<Long> availableDataRange) {
        // TODO
    }

    @Override
    public void addCompilerData(final Type type, final List<DiscreteTimeData<? extends Number>> data) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                String tag = getTag(type);
                multiChart.addData(tag, data);
            }
        });
    }

    @Override
    public void clearCompilerData(final Type type) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                String tag = getTag(type);
                multiChart.clearData(tag);
            }
        });
    }

    private DataGroup getGroup(final Type type) {
        final DataGroup group;
        if (type == Type.TOTAL_BAILOUTS || type == Type.TOTAL_COMPILES || type == Type.TOTAL_INVALIDATES) {
            group = numberGroup;
        } else {
            group = timeGroup;
        }
        return group;
    }

    private String getTag(final Type type) {
        return type.name();
    }

    @Override
    public Component getUiComponent() {
        return visiblePanel;
    }

}
