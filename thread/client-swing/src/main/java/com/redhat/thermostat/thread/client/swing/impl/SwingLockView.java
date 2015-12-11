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

package com.redhat.thermostat.thread.client.swing.impl;

import java.awt.BorderLayout;
import java.awt.Component;

import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;

import com.redhat.thermostat.client.swing.NonEditableTableModel;
import com.redhat.thermostat.client.swing.SwingComponent;
import com.redhat.thermostat.client.swing.components.ThermostatScrollPane;
import com.redhat.thermostat.client.swing.components.ThermostatTable;
import com.redhat.thermostat.client.swing.experimental.ComponentVisibilityNotifier;
import com.redhat.thermostat.shared.locale.Translate;
import com.redhat.thermostat.thread.client.common.locale.LocaleResources;
import com.redhat.thermostat.thread.client.common.view.LockView;
import com.redhat.thermostat.thread.model.LockInfo;

public class SwingLockView extends LockView implements SwingComponent {

    private static final Translate<LocaleResources> translator = LocaleResources.createLocalizer();

    private static final int VALUE_COLUMN = 1;

    private JPanel topPanel;
    private ThermostatTable table;
    private DefaultTableModel model;

    public SwingLockView() {
        topPanel = new JPanel();
        topPanel.setLayout(new BorderLayout());

        model = new NonEditableTableModel(18, 2);
        Object[][] dataVector = new Object[][] {
            new Object[] { translator.localize(LocaleResources.LOCK_DESCRIPTION_CONTENDED_LOCK_ATTEMPS).getContents(), 0 },
            new Object[] { translator.localize(LocaleResources.LOCK_DESCRIPTION_DEFLATIONS).getContents(), 0 },
            new Object[] { translator.localize(LocaleResources.LOCK_DESCRIPTION_EMPTY_NOTIFICATIONS).getContents(), 0 },
            new Object[] { translator.localize(LocaleResources.LOCK_DESCRIPTION_FAILED_SPINS).getContents(), 0 },
            new Object[] { translator.localize(LocaleResources.LOCK_DESCRIPTION_FUTILE_WAKEUPS).getContents(), 0 },
            new Object[] { translator.localize(LocaleResources.LOCK_DESCRIPTION_INFLATIONS).getContents(), 0 },
            new Object[] { translator.localize(LocaleResources.LOCK_DESCRIPTION_MON_EXTANT).getContents(), 0 },
            new Object[] { translator.localize(LocaleResources.LOCK_DESCRIPTION_MON_IN_CIRCULATION).getContents(), 0 },
            new Object[] { translator.localize(LocaleResources.LOCK_DESCRIPTION_MON_SCAVENGED).getContents(), 0 },
            new Object[] { translator.localize(LocaleResources.LOCK_DESCRIPTION_NOTIFICATIONS).getContents(), 0 },
            new Object[] { translator.localize(LocaleResources.LOCK_DESCRIPTION_PARKS).getContents(), 0 },
            new Object[] { translator.localize(LocaleResources.LOCK_DESCRIPTION_PRIVATE_A).getContents(), 0 },
            new Object[] { translator.localize(LocaleResources.LOCK_DESCRIPTION_PRIVATE_B).getContents(), 0 },
            new Object[] { translator.localize(LocaleResources.LOCK_DESCRIPTION_SLOW_ENTER).getContents(), 0 },
            new Object[] { translator.localize(LocaleResources.LOCK_DESCRIPTION_SLOW_EXIT).getContents(), 0 },
            new Object[] { translator.localize(LocaleResources.LOCK_DESCRIPTION_SLOW_NOTIFY).getContents(), 0 },
            new Object[] { translator.localize(LocaleResources.LOCK_DESCRIPTION_SLOW_NOTIFY_ALL).getContents(), 0 },
            new Object[] { translator.localize(LocaleResources.LOCK_DESCRIPTION_SUCCESSFUL_SPINS).getContents(), 0 },
        };

        Object[] columnIdentifiers = new Object[] {
                translator.localize(LocaleResources.LOCK_COLUMN_NAME).getContents(),
                translator.localize(LocaleResources.LOCK_COLUMN_VALUE).getContents()};
        model.setDataVector(dataVector, columnIdentifiers);

        table = new ThermostatTable(model);
        ThermostatScrollPane scrollPane = new ThermostatScrollPane(table);
        topPanel.add(scrollPane, BorderLayout.CENTER);

        new ComponentVisibilityNotifier().initialize(topPanel, notifier);
    }

    @Override
    public void setLatestLockData(final LockInfo data) {
        SwingUtilities.invokeLater(new Runnable() {
            private int row = 0;
            @Override
            public void run() {
                updateModel(data.getContendedLockAttempts());
                updateModel(data.getDeflations());
                updateModel(data.getEmptyNotifications());
                updateModel(data.getFailedSpins());
                updateModel(data.getFutileWakeups());
                updateModel(data.getInflations());
                updateModel(data.getMonExtant());
                updateModel(data.getMonInCirculation());
                updateModel(data.getMonScavenged());
                updateModel(data.getNotifications());
                updateModel(data.getParks());
                updateModel(data.getPrivateA());
                updateModel(data.getPrivateB());
                updateModel(data.getSlowEnter());
                updateModel(data.getSlowExit());
                updateModel(data.getSlowNotify());
                updateModel(data.getSlowNotifyAll());
                updateModel(data.getSuccessfulSpins());
            }

            private void updateModel(long number) {
                model.setValueAt(number, row, VALUE_COLUMN);
                row++;
            }
        });
    }
    @Override
    public Component getUiComponent() {
        return topPanel;
    }

}
