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

package com.redhat.thermostat.client.swing.views;

import java.awt.BorderLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.swing.DefaultListModel;
import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.LayoutStyle.ComponentPlacement;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableModel;

import com.redhat.thermostat.client.core.views.AgentInformationDisplayView;
import com.redhat.thermostat.client.locale.LocaleResources;
import com.redhat.thermostat.client.swing.components.LabelField;
import com.redhat.thermostat.client.swing.components.SectionHeader;
import com.redhat.thermostat.client.swing.components.ValueField;
import com.redhat.thermostat.common.ActionEvent;
import com.redhat.thermostat.common.ActionListener;
import com.redhat.thermostat.common.locale.Translate;

public class AgentInformationDisplayFrame extends AgentInformationDisplayView {

    private static final Translate<LocaleResources> translate = LocaleResources.createLocalizer();

    private static final String[] BACKEND_TABLE_COLUMN_NAMES = new String[] {
        translate.localize(LocaleResources.AGENT_INFO_BACKEND_NAME_COLUMN),
        translate.localize(LocaleResources.AGENT_INFO_BACKEND_STATUS_COLUMN),
    };

    private final CopyOnWriteArrayList<ActionListener<ConfigurationAction>> listeners = new CopyOnWriteArrayList<>();

    private final JFrame frame;

    private final ConfigurationCompleteListener configurationComplete;
    private final AgentChangedListener agentChanged;
    private final WindowClosingListener windowListener;

    private final JButton closeButton;

    private final JList<String> agentList;
    private final DefaultListModel<String> listModel;

    private final ValueField currentAgentName;
    private final ValueField currentAgentId;
    private final ValueField currentAgentCommandAddress;
    private final ValueField currentAgentStartTime;
    private final ValueField currentAgentStopTime;

    private final JTable backendsTable;
    private final DefaultTableModel backendsTableModel;
    private final ValueField backendDescription;

    public AgentInformationDisplayFrame() {
        assertInEDT();

        configurationComplete = new ConfigurationCompleteListener();
        agentChanged = new AgentChangedListener();
        windowListener = new WindowClosingListener();

        frame = new JFrame();
        frame.setTitle(translate.localize(LocaleResources.AGENT_INFO_WINDOW_TITLE));
        frame.addWindowListener(windowListener);

        closeButton = new JButton(translate.localize(LocaleResources.BUTTON_CLOSE));
        closeButton.addActionListener(configurationComplete);
        closeButton.setName("close");

        JSplitPane splitPane = new JSplitPane();
        splitPane.setResizeWeight(0.35);

        GroupLayout mainLayout = new GroupLayout(frame.getContentPane());
        mainLayout.setHorizontalGroup(
            mainLayout.createParallelGroup(Alignment.TRAILING)
                .addGroup(mainLayout.createSequentialGroup()
                    .addContainerGap()
                    .addGroup(mainLayout.createParallelGroup(Alignment.TRAILING)
                        .addComponent(splitPane, GroupLayout.DEFAULT_SIZE, 664, Short.MAX_VALUE)
                        .addComponent(closeButton))
                    .addContainerGap()));

        mainLayout.setVerticalGroup(
            mainLayout.createParallelGroup(Alignment.TRAILING)
                .addGroup(mainLayout.createSequentialGroup()
                    .addContainerGap()
                    .addComponent(splitPane, GroupLayout.DEFAULT_SIZE, 472, Short.MAX_VALUE)
                    .addPreferredGap(ComponentPlacement.UNRELATED)
                    .addComponent(closeButton)
                    .addContainerGap()));

        JPanel agentListPanel = new JPanel();
        splitPane.setLeftComponent(agentListPanel);

        JLabel agentLabel = new JLabel(translate.localize(LocaleResources.AGENT_INFO_AGENTS_LIST));

        JScrollPane scrollPane = new JScrollPane();

        listModel = new DefaultListModel<String>();
        agentList = new JList<String>(listModel);
        agentList.setName("agentList");
        agentList.addListSelectionListener(agentChanged);
        agentListPanel.setLayout(new BorderLayout());

        scrollPane.setViewportView(agentList);
        agentListPanel.add(scrollPane);
        agentListPanel.add(agentLabel, BorderLayout.NORTH);

        JPanel agentConfigurationPanel = new JPanel();
        splitPane.setRightComponent(agentConfigurationPanel);

        SectionHeader agentSectionTitle = new SectionHeader(translate.localize(LocaleResources.AGENT_INFO_AGENT_SECTION_TITLE));

        LabelField agentNameLabel = new LabelField(translate.localize(LocaleResources.AGENT_INFO_AGENT_NAME_LABEL));
        LabelField agentIdLabel = new LabelField(translate.localize(LocaleResources.AGENT_INFO_AGENT_ID_LABEL));
        LabelField agentConfigurationAddressLabel = new LabelField(translate.localize(LocaleResources.AGENT_INFO_AGENT_COMMAND_ADDRESS_LABEL));
        LabelField agentStartTimeLabel = new LabelField(translate.localize(LocaleResources.AGENT_INFO_AGENT_START_TIME_LABEL));
        LabelField agentStopTimeLabel = new LabelField(translate.localize(LocaleResources.AGENT_INFO_AGENT_STOP_TIME_LABEL));

        String notAvailable = translate.localize(LocaleResources.INFORMATION_NOT_AVAILABLE);

        currentAgentName = new ValueField(notAvailable);
        currentAgentName.setName("agentName");
        currentAgentId = new ValueField(notAvailable);
        currentAgentId.setName("agentId");
        currentAgentCommandAddress = new ValueField(notAvailable);
        currentAgentCommandAddress.setName("commandAddress");
        currentAgentStartTime = new ValueField(notAvailable);
        currentAgentStartTime.setName("startTime");
        currentAgentStopTime = new ValueField(notAvailable);
        currentAgentStopTime.setName("stopTime");

        SectionHeader backendSectionTitle = new SectionHeader(translate.localize(LocaleResources.AGENT_INFO_BACKENDS_SECTION_TITLE));

        backendsTableModel = new DefaultTableModel();
        backendsTableModel.setColumnIdentifiers(BACKEND_TABLE_COLUMN_NAMES);

        backendsTable = new JTable(backendsTableModel);
        backendsTable.setName("backends");
        backendsTable.setCellSelectionEnabled(false);
        backendsTable.setColumnSelectionAllowed(false);
        backendsTable.setRowSelectionAllowed(true);
        backendsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        backendsTable.getSelectionModel().addListSelectionListener(new BackendSelectionListener());

        JScrollPane backendsTableScollPane = new JScrollPane(backendsTable);

        JLabel backendDescriptionLabel = new JLabel(translate.localize(LocaleResources.AGENT_INFO_BACKEND_DESCRIPTION_LABEL));
        backendDescription = new ValueField(notAvailable);
        backendDescription.setName("backendDescription");

        GroupLayout agentConfigurationPanelLayout = new GroupLayout(agentConfigurationPanel);
        agentConfigurationPanelLayout.setHorizontalGroup(
            agentConfigurationPanelLayout.createParallelGroup()
                .addComponent(agentSectionTitle, 0, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGroup(agentConfigurationPanelLayout.createSequentialGroup()
                    .addGroup(agentConfigurationPanelLayout.createParallelGroup(Alignment.LEADING, true)
                        .addComponent(agentNameLabel, 0, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(agentIdLabel, 0, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(agentConfigurationAddressLabel, 0, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(agentStartTimeLabel, 0, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(agentStopTimeLabel, 0, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                    .addGroup(agentConfigurationPanelLayout.createParallelGroup(Alignment.LEADING, true)
                        .addComponent(currentAgentName, 0, GroupLayout.PREFERRED_SIZE, Short.MAX_VALUE)
                        .addComponent(currentAgentId, 0, GroupLayout.PREFERRED_SIZE, Short.MAX_VALUE)
                        .addComponent(currentAgentCommandAddress, 0, GroupLayout.PREFERRED_SIZE, Short.MAX_VALUE)
                        .addComponent(currentAgentStartTime, 0, GroupLayout.PREFERRED_SIZE, Short.MAX_VALUE)
                        .addComponent(currentAgentStopTime, 0, GroupLayout.PREFERRED_SIZE, Short.MAX_VALUE)))
                .addComponent(backendSectionTitle, 0, GroupLayout.PREFERRED_SIZE, Short.MAX_VALUE)
                .addComponent(backendsTableScollPane, 0, GroupLayout.PREFERRED_SIZE, Short.MAX_VALUE)
                .addComponent(backendDescriptionLabel, 0, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(backendDescription, 0, GroupLayout.PREFERRED_SIZE, Short.MAX_VALUE));

        agentConfigurationPanelLayout.setVerticalGroup(
            agentConfigurationPanelLayout.createSequentialGroup()
                .addComponent(agentSectionTitle)
                .addGroup(agentConfigurationPanelLayout.createParallelGroup(Alignment.BASELINE, false)
                    .addComponent(agentNameLabel, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                    .addComponent(currentAgentName, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
                .addGroup(agentConfigurationPanelLayout.createParallelGroup(Alignment.BASELINE, false)
                    .addComponent(agentIdLabel, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                    .addComponent(currentAgentId, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
                .addGroup(agentConfigurationPanelLayout.createParallelGroup(Alignment.BASELINE, false)
                    .addComponent(agentConfigurationAddressLabel, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                    .addComponent(currentAgentCommandAddress, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
                .addGroup(agentConfigurationPanelLayout.createParallelGroup(Alignment.BASELINE, false)
                    .addComponent(agentStartTimeLabel, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                    .addComponent(currentAgentStartTime, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
                .addGroup(agentConfigurationPanelLayout.createParallelGroup(Alignment.BASELINE, false)
                    .addComponent(agentStopTimeLabel, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                    .addComponent(currentAgentStopTime, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
                .addComponent(backendSectionTitle)
                .addComponent(backendsTableScollPane)
                .addComponent(backendDescriptionLabel, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE)
                .addComponent(backendDescription, 30, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE));

        agentConfigurationPanelLayout.setAutoCreateGaps(true);
        agentConfigurationPanelLayout.setAutoCreateContainerGaps(true);
        agentConfigurationPanel.setLayout(agentConfigurationPanelLayout);

        frame.getContentPane().setLayout(mainLayout);

    }

    @Override
    public void addConfigurationListener(ActionListener<ConfigurationAction> listener) {
        listeners.add(listener);
    }

    @Override
    public void removeConfigurationListener(ActionListener<ConfigurationAction> listener) {
        listeners.remove(listener);
    }

    @Override
    public void addAgent(final String agentName) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                listModel.addElement(agentName);
                if (agentList.getSelectedIndex() == -1) {
                    agentList.setSelectedIndex(0);
                }
            }
        });
    }

    @Override
    public String getSelectedAgent() {
        assertInEDT();
        return agentList.getSelectedValue();
    }

    @Override
    public void clearAllAgents() {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                listModel.clear();
            }
        });
    }

    @Override
    public void setSelectedAgentName(final String agentName) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                currentAgentName.setText(agentName);
            }
        });
    }

    @Override
    public void setSelectedAgentId(final String agentId) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                currentAgentId.setText(agentId);
            }
        });
    }

    @Override
    public void setSelectedAgentCommandAddress(final String address) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                currentAgentCommandAddress.setText(address);
            }
        });
    }

    @Override
    public void setSelectedAgentStartTime(final String startTime) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                currentAgentStartTime.setText(startTime);
            }
        });
    }

    @Override
    public void setSelectedAgentStopTime(final String stopTime) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                currentAgentStopTime.setText(stopTime);
            }
        });
    }

    @Override
    public void setSelectedAgentBackendStatus(final Map<String, String> backendStatus) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                int i = 0;
                for (Entry<String, String> entry : backendStatus.entrySet()) {
                    String backendName = entry.getKey();
                    String status = entry.getValue();
                    int rowCount = backendsTableModel.getRowCount();
                    if (i >= rowCount) {
                        Object[] rowData = new String[] { backendName, status };
                        backendsTableModel.insertRow(i, rowData);
                    } else {
                        backendsTableModel.setValueAt(backendName, i, 0);
                        backendsTableModel.setValueAt(status, i, 1);
                    }
                    i++;
                }

                if (backendsTable.getRowCount() > 0 && backendsTable.getSelectedRow() == -1) {
                    backendsTable.setRowSelectionInterval(0, 0);
                }
            }
        });
    }

    @Override
    public void setSelectedAgentBackendDescription(final String description) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                backendDescription.setText(description);
            }
        });
    }

    @Override
    public void showDialog() {
        assertInEDT();

        frame.pack();
        frame.setVisible(true);

        agentList.setSelectedIndex(0);
    }

    @Override
    public void hideDialog() {
        assertInEDT();

        frame.setVisible(false);
        frame.dispose();
    }

    /** This is for tests only */
    JFrame getFrame() {
        return frame;
    }

    private void fireAction(ActionEvent<ConfigurationAction> actionEvent) {
        for (ActionListener<ConfigurationAction> l : listeners) {
            l.actionPerformed(actionEvent);
        }
    }

    private static void assertInEDT() {
        if (!SwingUtilities.isEventDispatchThread()) {
            throw new IllegalStateException("must be called from within the swing EDT");
        }
    }

    private class ConfigurationCompleteListener implements java.awt.event.ActionListener {
        @Override
        public void actionPerformed(java.awt.event.ActionEvent e) {
            Object source = e.getSource();
            if (source == closeButton) {
                fireAction(new ActionEvent<>(AgentInformationDisplayFrame.this, ConfigurationAction.CLOSE));
            }
        }
    }

    private class WindowClosingListener extends WindowAdapter {
        @Override
        public void windowClosing(WindowEvent e) {
            fireAction(new ActionEvent<>(AgentInformationDisplayFrame.this, ConfigurationAction.CLOSE));
        }
    }

    private class AgentChangedListener implements ListSelectionListener {
        @Override
        public void valueChanged(ListSelectionEvent e) {
            if (e.getSource() == agentList) {
                if (e.getValueIsAdjusting()) {
                    return;
                }
                fireAction(new ActionEvent<>(AgentInformationDisplayFrame.this, ConfigurationAction.SWITCH_AGENT));
            } else {
                throw new IllegalStateException("unknown trigger");
            }
        }
    }

    private class BackendSelectionListener implements ListSelectionListener {

        @Override
        public void valueChanged(ListSelectionEvent e) {
            if (e.getValueIsAdjusting()) {
                return;
            }

            int rowIndex = e.getFirstIndex();
            String backendName = (String) backendsTableModel.getValueAt(rowIndex, 0);
            ActionEvent<ConfigurationAction> event = new ActionEvent<>(AgentInformationDisplayFrame.this,
                    ConfigurationAction.SHOW_BACKEND_DESCRIPTION);
            event.setPayload(backendName);
            fireAction(event);
        }

    }

}
