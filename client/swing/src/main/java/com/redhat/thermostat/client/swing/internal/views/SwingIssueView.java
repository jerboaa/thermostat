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

import java.awt.Color;
import java.awt.Component;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JComponent;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import com.redhat.thermostat.client.core.Severity;
import com.redhat.thermostat.client.core.views.IssueView;
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

        JComponent tablePanel = createIssuesTable();
        panel.setContent(tablePanel);

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

    @Override
    public void setIssuesState(final IssueState state) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                switch (state) {
                    case NOT_STARTED:
                        panel.setHeader(translator.localize(LocaleResources.ISSUES_NOT_STARTED));
                        break;
                    case NONE_FOUND:
                        panel.setHeader(translator.localize(LocaleResources.ISSUES_NONE_FOUND));
                        break;
                    case ISSUES_FOUND:
                        panel.setHeader(translator.localize(LocaleResources.ISSUES_FOUND));
                        break;
                    default:
                        logger.log(Level.WARNING, "Unknown IssueState: " + state);
                        break;
                }
            }
        });
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
            }
        };

        final ThermostatTable table = new ThermostatTable(tableModel);
        table.getColumnModel().getColumn(0).setMaxWidth(200);
        table.getColumnModel().getColumn(0).setPreferredWidth(100);

        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setCellSelectionEnabled(false);
        table.setRowSelectionAllowed(true);
        table.setColumnSelectionAllowed(false);

        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (!(e.getButton() == MouseEvent.BUTTON1
                        && e.getClickCount() == 2)) {
                    return;
                }
                Point point = e.getPoint();
                int index = table.rowAtPoint(point);
                if (index >= 0) {
                    fireIssueSelectionChangedEvent(index);
                }
            }
        });

        return table.wrap();
    }

    private void fireIssueSelectionChangedEvent(int index) {
        notifier.fireAction(IssueAction.SELECTION_CHANGED, index);
    }

    @Override
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

    @Override
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

    @Override
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
