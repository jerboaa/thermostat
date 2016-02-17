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

package com.redhat.thermostat.client.swing.internal.views;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;
import java.util.concurrent.Callable;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import com.redhat.thermostat.client.core.Severity;
import com.redhat.thermostat.client.core.views.IssueView;
import com.redhat.thermostat.client.swing.EdtHelper;
import com.redhat.thermostat.client.swing.NonEditableTableModel;
import com.redhat.thermostat.client.swing.SwingComponent;
import com.redhat.thermostat.client.swing.components.ActionButton;
import com.redhat.thermostat.client.swing.components.FontAwesomeIcon;
import com.redhat.thermostat.client.swing.components.HeaderPanel;
import com.redhat.thermostat.client.swing.components.Icon;
import com.redhat.thermostat.client.swing.components.ThermostatTable;
import com.redhat.thermostat.client.swing.internal.LocaleResources;
import com.redhat.thermostat.common.ActionListener;
import com.redhat.thermostat.common.utils.LoggingUtils;
import com.redhat.thermostat.shared.locale.Translate;

public class SwingIssueView extends IssueView implements SwingComponent {

    private static final Logger logger = LoggingUtils.getLogger(SwingIssueView.class);

    private static final Translate<LocaleResources> translator = LocaleResources.createLocalizer();

    private static final Map<Severity, Icon> SEVERITY_TO_ICON = new HashMap<>();

    private final HeaderPanel panel;

    private NonEditableTableModel tableModel;

    private JPanel contentPane;

    private SearchActionListener searchListener;

    static {
        final int SIZE = 12;
        Icon error = new FontAwesomeIcon('\uf06a', SIZE, Color.RED);
        SEVERITY_TO_ICON.put(Severity.CRITICAL, error);
        Icon warning = new FontAwesomeIcon('\uf071', SIZE, Color.YELLOW);
        SEVERITY_TO_ICON.put(Severity.WARNING, warning);
        Icon info = new FontAwesomeIcon('\uf129', SIZE, Color.GREEN);
        SEVERITY_TO_ICON.put(Severity.LOW, info);
    }

    public SwingIssueView() {
        panel = new HeaderPanel(translator.localize(LocaleResources.ISSUES_HEADER));

        contentPane = new JPanel();
        contentPane.setLayout(new BorderLayout());
        panel.setContent(contentPane);

        searchListener = new SearchActionListener();

        Icon refreshIcon = new FontAwesomeIcon('\uf021', 12);
        ActionButton button = new ActionButton(refreshIcon, translator.localize(LocaleResources.ISSUES_CHECK));
        button.addActionListener(searchListener);

        panel.addToolBarButton(button);
    }

    @Override
    public Component getUiComponent() {
        return panel;
    }

    public void showInitialView() {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                contentPane.removeAll();
                JButton initialSearchButton = new JButton(translator.localize(LocaleResources.ISSUES_CHECK).getContents());
                initialSearchButton.addActionListener(searchListener);
                contentPane.add(initialSearchButton, BorderLayout.CENTER);
                contentPane.revalidate();
            }
        });
    }

    public void showIssues() {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                contentPane.removeAll();
                JComponent tablePanel = createIssuesTable();
                contentPane.add(tablePanel, BorderLayout.CENTER);
                contentPane.revalidate();
            }
        });
    }

    public boolean isInitialView() {
        try {
            return new EdtHelper().callAndWait(new Callable<Boolean>() {
                @Override
                public Boolean call() throws Exception {
                    return tableModel == null;
                }
            });
        } catch (InvocationTargetException | InterruptedException e) {
            logger.log(Level.WARNING, "Error in isInitialView()", e);
            return false;
        }
    }

    private JComponent createIssuesTable() {
        Vector<String> columnNames = new Vector<>();
        columnNames.add(translator.localize(LocaleResources.ISSUES_COLUMN_SEVERITY).getContents());
        columnNames.add(translator.localize(LocaleResources.ISSUES_COLUMN_MESSAGE).getContents());
        columnNames.add(translator.localize(LocaleResources.ISSUES_COLUMN_AGENT).getContents());
        columnNames.add(translator.localize(LocaleResources.ISSUES_COLUMN_VM).getContents());

        Vector<Vector<?>> initialData = new Vector<>();

        tableModel = new NonEditableTableModel(initialData, columnNames) {
            // let the built-in-renderers for non-string columns do their job
            public java.lang.Class<?> getColumnClass(int columnIndex) {
                if (getRowCount() > 0) {
                    return getValueAt(0, columnIndex).getClass();
                }
                return super.getColumnClass(columnIndex);
            };
        };

        ThermostatTable table = new ThermostatTable(tableModel);
        table.getColumnModel().getColumn(0).setMaxWidth(200);
        table.getColumnModel().getColumn(0).setPreferredWidth(100);
        return table.wrap();
    }

    public void addIssue(final IssueDescription issue) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                tableModel.addRow(new Object[] {
                        getIcon(issue.severity),
                        issue.description,
                        issue.agent,
                        issue.vm,
                });
            }

        });
    }

    public void clearIssues() {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                while (tableModel.getRowCount() > 0) {
                    tableModel.removeRow(0);
                }
            }
        });
    }

    public void addIssueActionListener(ActionListener<IssueView.IssueAction> listener) {
        notifier.addActionListener(listener);
    }

    private Icon getIcon(Severity severity) {
        Icon result = SEVERITY_TO_ICON.get(severity);
        if (result == null) {
            throw new AssertionError("Icon for " + severity + " not found");
        }
        return result;
    }

    class SearchActionListener implements java.awt.event.ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            notifier.fireAction(IssueAction.SEARCH);
        }
    }

}
