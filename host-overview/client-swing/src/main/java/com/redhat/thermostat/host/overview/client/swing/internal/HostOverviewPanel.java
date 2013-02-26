/*
 * Copyright 2012, 2013 Red Hat, Inc.
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

package com.redhat.thermostat.host.overview.client.swing.internal;

import java.awt.BorderLayout;
import java.awt.Component;

import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.LayoutStyle.ComponentPlacement;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumnModel;

import com.redhat.thermostat.client.swing.ComponentVisibleListener;
import com.redhat.thermostat.client.swing.SwingComponent;
import com.redhat.thermostat.client.swing.components.LabelField;
import com.redhat.thermostat.client.swing.components.SectionHeader;
import com.redhat.thermostat.client.swing.components.ValueField;
import com.redhat.thermostat.common.ActionListener;
import com.redhat.thermostat.common.locale.Translate;
import com.redhat.thermostat.host.overview.client.core.HostOverviewView;
import com.redhat.thermostat.host.overview.client.locale.LocaleResources;

public class HostOverviewPanel extends HostOverviewView implements SwingComponent {

    private static final Translate<LocaleResources> translator = LocaleResources.createLocalizer();

    private JPanel visiblePanel;
    private JScrollPane scrollPane;

    private final ValueField hostname = new ValueField("");
    private final ValueField cpuModel = new ValueField("");
    private final ValueField cpuCount = new ValueField("");
    private final ValueField totalMemory = new ValueField("");
    private final ValueField osName = new ValueField("");
    private final ValueField osKernel = new ValueField("");

    private final DefaultTableModel networkTableModel = new DefaultTableModel() {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };

    private Object[] networkTableColumns;
    private Object[][] networkTableData;

    private JTable networkTable;

    public HostOverviewPanel() {
        super();
        initializePanel();

        visiblePanel.addHierarchyListener(new ComponentVisibleListener() {
            @Override
            public void componentShown(Component component) {
                notifier.fireAction(Action.VISIBLE);
            }

            @Override
            public void componentHidden(Component component) {
                notifier.fireAction(Action.HIDDEN);
            }
        });
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
    public void setHostName(final String newHostName) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                hostname.setText(newHostName);
            }
        });
    }

    @Override
    public void setCpuModel(final String newCpuModel) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                cpuModel.setText(newCpuModel);
            }
        });
    }

    @Override
    public void setCpuCount(final String newCpuCount) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                cpuCount.setText(newCpuCount);
            }
        });
    }

    @Override
    public void setTotalMemory(final String newTotalMemory) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                totalMemory.setText(newTotalMemory);
            }
        });
    }

    @Override
    public void setOsName(final String newOsName) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                osName.setText(newOsName);
            }
        });
    }

    @Override
    public void setOsKernel(final String newOsKernel) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                osKernel.setText(newOsKernel);
            }
        });
    }

    @Override
    public void setNetworkTableColumns(final Object[] columns) {
        this.networkTableColumns = columns;
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                networkTableModel.setColumnIdentifiers(networkTableColumns);
            }
        });
    }

    @Override
    public void setInitialNetworkTableData(final Object[][] data) {
        this.networkTableData = data;
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                networkTableModel.setDataVector(networkTableData, networkTableColumns);

                /*
                 * Compute and set initial (minimum) widths for the table. The
                 * LayoutManager will give it extra space because the table is
                 * narrower than what it has to be and that space is distributed
                 * among all the columns as extra.
                 */
                TableColumnModel columnModel = networkTable.getColumnModel();
                for (int col = 0; col < networkTable.getColumnCount(); col++) {
                    int maxWidth = Integer.MIN_VALUE;

                    for (int row = 0; row < networkTable.getRowCount(); row++) {
                        TableCellRenderer renderer = networkTable.getCellRenderer(row, col);
                        Component component = networkTable.prepareRenderer(renderer, row, col);
                        maxWidth = Math.max(maxWidth, component.getPreferredSize().width);
                    }

                    columnModel.getColumn(col).setPreferredWidth(maxWidth);
                }
            }
        });
    }

    @Override
    public void updateNetworkTableData(final int row, final int column, final String data) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                networkTableModel.setValueAt(data, row, column);
            }
        });
    }

    @Override
    public Component getUiComponent() {
        return scrollPane;
    }

    private void initializePanel() {
        visiblePanel = new JPanel();
        SectionHeader overviewSection = new SectionHeader(translator.localize(LocaleResources.HOST_OVERVIEW_SECTION_BASICS));
        LabelField hostnameLabel = new LabelField(translator.localize(LocaleResources.HOST_INFO_HOSTNAME));
        SectionHeader hardwareSection = new SectionHeader(translator.localize(LocaleResources.HOST_OVERVIEW_SECTION_HARDWARE));
        LabelField cpuModelLabel = new LabelField(translator.localize(LocaleResources.HOST_INFO_CPU_MODEL));
        LabelField cpuCountLabel = new LabelField(translator.localize(LocaleResources.HOST_INFO_CPU_COUNT));
        LabelField memoryTotalLabel = new LabelField(translator.localize(LocaleResources.HOST_INFO_MEMORY_TOTAL));
        LabelField networkLabel = new LabelField(translator.localize(LocaleResources.HOST_INFO_NETWORK));
        SectionHeader softwareSection = new SectionHeader(translator.localize(LocaleResources.HOST_OVERVIEW_SECTION_SOFTWARE));
        LabelField osNameLabel = new LabelField(translator.localize(LocaleResources.HOST_INFO_OS_NAME));
        LabelField osKernelLabel = new LabelField(translator.localize(LocaleResources.HOST_INFO_OS_KERNEL));

        JPanel panel = new JPanel();

        GroupLayout gl_visiblePanel = new GroupLayout(visiblePanel);
        gl_visiblePanel.setHorizontalGroup(
            gl_visiblePanel.createParallelGroup(Alignment.LEADING)
                .addGroup(gl_visiblePanel.createSequentialGroup()
                    .addContainerGap()
                    .addGroup(gl_visiblePanel.createParallelGroup(Alignment.LEADING)
                        .addComponent(hardwareSection, GroupLayout.DEFAULT_SIZE, 620, Short.MAX_VALUE)
                        .addComponent(overviewSection, GroupLayout.DEFAULT_SIZE, 620, Short.MAX_VALUE)
                        .addGroup(gl_visiblePanel.createSequentialGroup()
                            .addGroup(gl_visiblePanel.createParallelGroup(Alignment.TRAILING, false)
                                .addGroup(gl_visiblePanel.createSequentialGroup()
                                    .addGap(12)
                                    .addComponent(hostnameLabel, GroupLayout.DEFAULT_SIZE, 134, Short.MAX_VALUE))
                                .addComponent(cpuCountLabel, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addComponent(cpuModelLabel, GroupLayout.DEFAULT_SIZE, 134, Short.MAX_VALUE)
                                .addComponent(memoryTotalLabel, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addGroup(gl_visiblePanel.createSequentialGroup()
                                    .addGap(12)
                                    .addComponent(networkLabel, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                            .addPreferredGap(ComponentPlacement.RELATED)
                            .addGroup(gl_visiblePanel.createParallelGroup(Alignment.LEADING)
                                .addComponent(panel, GroupLayout.DEFAULT_SIZE, 462, Short.MAX_VALUE)
                                .addComponent(cpuCount, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addComponent(cpuModel, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addComponent(hostname, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addComponent(totalMemory, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                        .addComponent(softwareSection, GroupLayout.DEFAULT_SIZE, 620, Short.MAX_VALUE)
                        .addGroup(gl_visiblePanel.createSequentialGroup()
                            .addGap(12)
                            .addGroup(gl_visiblePanel.createParallelGroup(Alignment.LEADING, false)
                                .addComponent(osKernelLabel, Alignment.TRAILING, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addComponent(osNameLabel, Alignment.TRAILING, GroupLayout.DEFAULT_SIZE, 129, Short.MAX_VALUE))
                            .addPreferredGap(ComponentPlacement.RELATED)
                            .addGroup(gl_visiblePanel.createParallelGroup(Alignment.LEADING)
                                .addComponent(osKernel, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addComponent(osName, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))))
                    .addContainerGap())
        );
        gl_visiblePanel.setVerticalGroup(
            gl_visiblePanel.createParallelGroup(Alignment.LEADING)
                .addGroup(gl_visiblePanel.createSequentialGroup()
                    .addContainerGap()
                    .addComponent(overviewSection, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                    .addPreferredGap(ComponentPlacement.RELATED)
                    .addGroup(gl_visiblePanel.createParallelGroup(Alignment.LEADING, false)
                        .addComponent(hostname, GroupLayout.PREFERRED_SIZE, 15, GroupLayout.PREFERRED_SIZE)
                        .addComponent(hostnameLabel, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
                    .addPreferredGap(ComponentPlacement.UNRELATED)
                    .addComponent(hardwareSection, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                    .addPreferredGap(ComponentPlacement.RELATED)
                    .addGroup(gl_visiblePanel.createParallelGroup(Alignment.LEADING, false)
                        .addComponent(cpuModelLabel, GroupLayout.PREFERRED_SIZE, 15, GroupLayout.PREFERRED_SIZE)
                        .addComponent(cpuModel, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
                    .addPreferredGap(ComponentPlacement.RELATED)
                    .addGroup(gl_visiblePanel.createParallelGroup(Alignment.LEADING, false)
                        .addComponent(cpuCountLabel, GroupLayout.PREFERRED_SIZE, 15, GroupLayout.PREFERRED_SIZE)
                        .addComponent(cpuCount, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
                    .addPreferredGap(ComponentPlacement.RELATED)
                    .addGroup(gl_visiblePanel.createParallelGroup(Alignment.LEADING)
                        .addComponent(memoryTotalLabel, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                        .addComponent(totalMemory, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
                    .addPreferredGap(ComponentPlacement.RELATED)
                    .addGroup(gl_visiblePanel.createParallelGroup(Alignment.LEADING)
                        .addComponent(networkLabel, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                        .addComponent(panel, GroupLayout.PREFERRED_SIZE, 109, GroupLayout.PREFERRED_SIZE))
                    .addPreferredGap(ComponentPlacement.RELATED)
                    .addComponent(softwareSection, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                    .addPreferredGap(ComponentPlacement.RELATED)
                    .addGroup(gl_visiblePanel.createParallelGroup(Alignment.LEADING)
                        .addComponent(osNameLabel, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                        .addComponent(osName, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
                    .addPreferredGap(ComponentPlacement.RELATED)
                    .addGroup(gl_visiblePanel.createParallelGroup(Alignment.LEADING)
                        .addComponent(osKernelLabel, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                        .addComponent(osKernel, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
                    .addGap(128))
        );

        panel.setLayout(new BorderLayout(0, 0));

        networkTable = new JTable(networkTableModel);
        panel.add(networkTable);
        JTableHeader header = networkTable.getTableHeader();
        panel.add(header, BorderLayout.PAGE_START);

        visiblePanel.setLayout(gl_visiblePanel);

        scrollPane = new JScrollPane(visiblePanel, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
    }
}

