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

package com.redhat.thermostat.client.ui;

import static com.redhat.thermostat.client.locale.Translate.localize;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.swing.Box;
import javax.swing.DefaultListModel;
import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.LayoutStyle.ComponentPlacement;
import javax.swing.SwingUtilities;

import com.redhat.thermostat.client.locale.LocaleResources;
import com.redhat.thermostat.common.ActionEvent;
import com.redhat.thermostat.common.ActionListener;
import javax.swing.JScrollPane;
import javax.swing.JList;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

public class AgentConfigurationFrame extends JFrame implements AgentConfigurationView, java.awt.event.ActionListener, ListSelectionListener {

    private final CopyOnWriteArrayList<ActionListener<ConfigurationAction>> listeners = new CopyOnWriteArrayList<>();

    private final Map<String, JCheckBox> backends = Collections.synchronizedMap(new HashMap<String, JCheckBox>());

    private final JPanel availableBackendsPanel;
    private final GridBagConstraints availableBackendsPanelContstraints = new GridBagConstraints();

    private final JButton okayButton;
    private final JButton cancelButton;

    private final JList<String> agentList;
    private final DefaultListModel<String> listModel;


    public AgentConfigurationFrame() {
        assertInEDT();

        setTitle(localize(LocaleResources.CONFIGURE_AGENT_WINDOW_TITLE));

        JLabel lblEnabledisableBackends = new JLabel(localize(LocaleResources.CONFIGURE_ENABLE_BACKENDS));

        availableBackendsPanel = new JPanel();

        okayButton = new JButton(localize(LocaleResources.BUTTON_OK));
        okayButton.addActionListener(this);

        cancelButton = new JButton(localize(LocaleResources.BUTTON_CANCEL));
        cancelButton.addActionListener(this);

        JScrollPane scrollPane = new JScrollPane();

        JLabel lblAgents = new JLabel(localize(LocaleResources.CONFIGURE_AGENT_AGENTS_LIST));

        GroupLayout groupLayout = new GroupLayout(getContentPane());
        groupLayout.setHorizontalGroup(
            groupLayout.createParallelGroup(Alignment.LEADING)
                .addGroup(groupLayout.createSequentialGroup()
                    .addContainerGap()
                    .addGroup(groupLayout.createParallelGroup(Alignment.LEADING)
                        .addGroup(groupLayout.createSequentialGroup()
                            .addComponent(scrollPane, GroupLayout.PREFERRED_SIZE, 127, GroupLayout.PREFERRED_SIZE)
                            .addGap(0)
                            .addGroup(groupLayout.createParallelGroup(Alignment.LEADING)
                                .addGroup(Alignment.TRAILING, groupLayout.createSequentialGroup()
                                    .addComponent(cancelButton)
                                    .addPreferredGap(ComponentPlacement.RELATED)
                                    .addComponent(okayButton))
                                .addGroup(groupLayout.createSequentialGroup()
                                    .addGap(12)
                                    .addGroup(groupLayout.createParallelGroup(Alignment.LEADING)
                                        .addGroup(groupLayout.createSequentialGroup()
                                            .addGap(12)
                                            .addComponent(availableBackendsPanel, GroupLayout.DEFAULT_SIZE, 540, Short.MAX_VALUE))
                                        .addComponent(lblEnabledisableBackends)))))
                        .addComponent(lblAgents))
                    .addContainerGap())
        );
        groupLayout.setVerticalGroup(
            groupLayout.createParallelGroup(Alignment.TRAILING)
                .addGroup(groupLayout.createSequentialGroup()
                    .addGap(6)
                    .addComponent(lblAgents)
                    .addPreferredGap(ComponentPlacement.UNRELATED)
                    .addGroup(groupLayout.createParallelGroup(Alignment.TRAILING)
                        .addComponent(scrollPane, GroupLayout.DEFAULT_SIZE, 413, Short.MAX_VALUE)
                        .addGroup(groupLayout.createSequentialGroup()
                            .addComponent(lblEnabledisableBackends)
                            .addGap(2)
                            .addComponent(availableBackendsPanel, GroupLayout.DEFAULT_SIZE, 365, Short.MAX_VALUE)
                            .addPreferredGap(ComponentPlacement.RELATED)
                            .addGroup(groupLayout.createParallelGroup(Alignment.BASELINE)
                                .addComponent(okayButton)
                                .addComponent(cancelButton))))
                    .addContainerGap())
        );

        listModel = new DefaultListModel<String>();
        agentList = new JList<String>(listModel);
        agentList.addListSelectionListener(this);
        scrollPane.setViewportView(agentList);

        availableBackendsPanel.setLayout(new GridBagLayout());
        getContentPane().setLayout(groupLayout);

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                fireAction(new ActionEvent<>(AgentConfigurationFrame.this, ConfigurationAction.CLOSE_CANCEL));
            }
        });
    }

    private void resetConstraints() {
        availableBackendsPanelContstraints.gridwidth = 1;
        availableBackendsPanelContstraints.gridy = 0;
        availableBackendsPanelContstraints.gridx = 0;
        availableBackendsPanelContstraints.weightx = 0;
        availableBackendsPanelContstraints.weighty = 0;
        availableBackendsPanelContstraints.anchor = GridBagConstraints.LINE_START;
        availableBackendsPanelContstraints.fill = GridBagConstraints.BOTH;
    }


    @Override
    public void addActionListener(ActionListener<ConfigurationAction> listener) {
        listeners.add(listener);
    }

    @Override
    public void removeActionListener(ActionListener<ConfigurationAction> listener) {
        listeners.remove(listener);
    }

    @Override
    public void addAgent(final String agentName) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                listModel.addElement(agentName);
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
    public void setBackendStatus(final Map<String, Boolean> backendStatus) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                backends.clear();
                availableBackendsPanel.removeAll();
                resetConstraints();

                for (Entry<String, Boolean> entry: backendStatus.entrySet()) {
                    String backendName = entry.getKey();
                    boolean checked = entry.getValue();
                    JCheckBox checkBox = new JCheckBox(backendName);
                    checkBox.setSelected(checked);
                    checkBox.setActionCommand(backendName);
                    checkBox.addActionListener(AgentConfigurationFrame.this);
                    backends.put(backendName, checkBox);
                    availableBackendsPanel.add(checkBox, availableBackendsPanelContstraints);
                    availableBackendsPanelContstraints.gridy++;
                }
                availableBackendsPanelContstraints.weighty = 1.0;
                availableBackendsPanelContstraints.weightx = 1.0;
                availableBackendsPanelContstraints.fill = GridBagConstraints.BOTH;
                availableBackendsPanel.add(Box.createGlue(), availableBackendsPanelContstraints);
                AgentConfigurationFrame.this.revalidate();
            }
        });
    }

    @Override
    public Map<String, Boolean> getBackendStatus() {
        assertInEDT();

        Map<String,Boolean> latestUserSpecified = new HashMap<>();
        for (Entry<String, JCheckBox> entry: backends.entrySet()) {
            latestUserSpecified.put(entry.getKey(), entry.getValue().isSelected());
        }
        return latestUserSpecified;
    }

    @Override
    public void showDialog() {
        assertInEDT();

        pack();
        setVisible(true);

        agentList.setSelectedIndex(0);
    }

    @Override
    public void hideDialog() {
        assertInEDT();

        setVisible(false);
        dispose();
    }

    @Override
    public void actionPerformed(java.awt.event.ActionEvent e) {
        Object source = e.getSource();
        if (source == okayButton) {
            fireAction(new ActionEvent<>(this, ConfigurationAction.CLOSE_ACCEPT));
        } else if (source == cancelButton) {
            fireAction(new ActionEvent<>(this, ConfigurationAction.CLOSE_CANCEL));
        }
    }

    @Override
    public void valueChanged(ListSelectionEvent e) {
        if (e.getSource() == agentList) {
            if (e.getValueIsAdjusting()) {
                return;
            }
            fireAction(new ActionEvent<>(this, ConfigurationAction.SWITCH_AGENT));
        } else {
            throw new IllegalStateException("unknown trigger");
        }
    }

    private void fireAction(ActionEvent<ConfigurationAction> actionEvent) {
        for (ActionListener<ConfigurationAction> l: listeners) {
            l.actionPerformed(actionEvent);
        }
    }

    private static void assertInEDT() {
        if (!SwingUtilities.isEventDispatchThread()) {
            throw new IllegalStateException("must be called from within the swing EDT");
        }
    }


}
