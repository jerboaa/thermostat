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

package com.redhat.thermostat.vm.compiler.client.swing;

import java.awt.Component;

import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;

import com.redhat.thermostat.client.swing.NonEditableTableModel;
import com.redhat.thermostat.client.swing.SwingComponent;
import com.redhat.thermostat.client.swing.components.HeaderPanel;
import com.redhat.thermostat.client.swing.components.ThermostatScrollPane;
import com.redhat.thermostat.client.swing.components.ThermostatTable;
import com.redhat.thermostat.client.swing.experimental.ComponentVisibilityNotifier;
import com.redhat.thermostat.shared.locale.Translate;
import com.redhat.thermostat.vm.compiler.client.core.VmCompilerStatView;
import com.redhat.thermostat.vm.compiler.client.locale.LocaleResources;

public class SwingVmCompilerStatView extends VmCompilerStatView implements SwingComponent {

    private static final Translate<LocaleResources> t = LocaleResources.createLocalizer();

    private final HeaderPanel visiblePanel;

    private DefaultTableModel model;

    public SwingVmCompilerStatView() {
        visiblePanel = new HeaderPanel();

        visiblePanel.setHeader(t.localize(LocaleResources.VM_COMPILER_HEADER));

        model = createTableModel();

        ThermostatTable table = new ThermostatTable(model);
        ThermostatScrollPane tablePane = new ThermostatScrollPane(table);

        visiblePanel.setContent(tablePane);

        new ComponentVisibilityNotifier().initialize(visiblePanel, notifier);
    }

    private DefaultTableModel createTableModel() {
        DefaultTableModel model = new NonEditableTableModel(11, 2);
        Object[] columnIdentifiers = new Object[] {
                t.localize(LocaleResources.STATS_TABLE_COLUMN_NAME).getContents(),
                t.localize(LocaleResources.STATS_TABLE_COLUMN_VALUE).getContents()
        };
        Object[][] dataVector = new Object[][] {
            new Object[] { t.localize(LocaleResources.STATS_TABLE_TOTAL_COMPILES).getContents(), 0 },
            new Object[] { t.localize(LocaleResources.STATS_TABLE_TOTAL_BAILOUTS).getContents(), 0 },
            new Object[] { t.localize(LocaleResources.STATS_TABLE_TOTAL_INVALIDATES).getContents(), 0 },
            new Object[] { t.localize(LocaleResources.STATS_TABLE_COMPILATION_TIME).getContents(), 0 },
            new Object[] { t.localize(LocaleResources.STATS_TABLE_LAST_SIZE).getContents(), 0 },
            new Object[] { t.localize(LocaleResources.STATS_TABLE_LAST_TYPE).getContents(), 0 },
            new Object[] { t.localize(LocaleResources.STATS_TABLE_LAST_METHOD).getContents(), 0 },
            new Object[] { t.localize(LocaleResources.STATS_TABLE_LAST_FAILED_TYPE).getContents(), 0 },
            new Object[] { t.localize(LocaleResources.STATS_TABLE_LAST_FAILED_METHOD).getContents(), 0 },
        };
        model.setDataVector(dataVector, columnIdentifiers);
        return model;
    }

    @Override
    public void setData(final ViewData data) {
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
    public Component getUiComponent() {
        return visiblePanel;
    }

}
