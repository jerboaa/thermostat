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

import java.awt.Component;
import java.util.ArrayList;
import java.util.List;

import javax.swing.AbstractListModel;
import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.LayoutStyle.ComponentPlacement;
import javax.swing.ListModel;
import javax.swing.SwingUtilities;
import javax.swing.text.JTextComponent;

import com.redhat.thermostat.client.core.views.SummaryView;
import com.redhat.thermostat.client.locale.LocaleResources;
import com.redhat.thermostat.client.swing.SwingComponent;
import com.redhat.thermostat.client.swing.components.SectionHeader;
import com.redhat.thermostat.client.swing.components.ValueField;
import com.redhat.thermostat.client.ui.ComponentVisibleListener;
import com.redhat.thermostat.common.ActionListener;
import com.redhat.thermostat.common.locale.Translate;

public class SummaryPanel extends SummaryView implements SwingComponent {

    private static final Translate<LocaleResources> translator = LocaleResources.createLocalizer();

    private JPanel visiblePanel;
    
    private final JTextComponent totalMonitoredHosts;
    private final JTextComponent totalMonitoredVms;

    private final List<String> issuesList;

    public SummaryPanel() {
        super();
        visiblePanel = new JPanel();
        JLabel lblHomepanel = new SectionHeader(translator.localize(LocaleResources.HOME_PANEL_SECTION_SUMMARY));

        JLabel lblTotalHosts = new JLabel(translator.localize(LocaleResources.HOME_PANEL_TOTAL_MACHINES));

        totalMonitoredHosts = new ValueField("${TOTAL_MONITORED_HOSTS}");

        JLabel lblTotal = new JLabel(translator.localize(LocaleResources.HOME_PANEL_TOTAL_JVMS));

        totalMonitoredVms = new ValueField("${TOTAL_MONITORED_VMS}");

        JLabel lblIssues = new SectionHeader(translator.localize(LocaleResources.HOME_PANEL_SECTION_ISSUES));

        JScrollPane scrollPane = new JScrollPane();

        GroupLayout groupLayout = new GroupLayout(visiblePanel);
        groupLayout.setHorizontalGroup(
            groupLayout.createParallelGroup(Alignment.TRAILING)
                .addGroup(groupLayout.createSequentialGroup()
                    .addGroup(groupLayout.createParallelGroup(Alignment.LEADING)
                        .addGroup(groupLayout.createSequentialGroup()
                            .addContainerGap()
                            .addGroup(groupLayout.createParallelGroup(Alignment.LEADING)
                                .addComponent(lblHomepanel)
                                .addGroup(groupLayout.createSequentialGroup()
                                    .addGap(12)
                                    .addGroup(groupLayout.createParallelGroup(Alignment.TRAILING)
                                        .addComponent(lblTotal)
                                        .addComponent(lblTotalHosts))
                                    .addGap(18)
                                    .addGroup(groupLayout.createParallelGroup(Alignment.LEADING)
                                        .addComponent(totalMonitoredVms, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                        .addComponent(totalMonitoredHosts, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                                .addComponent(lblIssues)))
                        .addGroup(groupLayout.createSequentialGroup()
                            .addGap(24)
                            .addComponent(scrollPane, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                    .addContainerGap())
        );
        groupLayout.setVerticalGroup(
            groupLayout.createParallelGroup(Alignment.LEADING)
                .addGroup(groupLayout.createSequentialGroup()
                    .addContainerGap()
                    .addComponent(lblHomepanel)
                    .addPreferredGap(ComponentPlacement.RELATED)
                    .addGroup(groupLayout.createParallelGroup(Alignment.BASELINE)
                        .addComponent(lblTotalHosts)
                        .addComponent(totalMonitoredHosts, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE))
                    .addPreferredGap(ComponentPlacement.RELATED)
                    .addGroup(groupLayout.createParallelGroup(Alignment.BASELINE)
                        .addComponent(lblTotal)
                        .addComponent(totalMonitoredVms, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
                    .addGap(18)
                    .addComponent(lblIssues)
                    .addPreferredGap(ComponentPlacement.RELATED)
                    .addComponent(scrollPane, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addContainerGap())
        );

        issuesList = new ArrayList<>();
        ListModel<Object> issuesListModel = new IssuesListModel(issuesList);
        JList<Object> issuesList = new JList<>();
        issuesList.setModel(issuesListModel);
        scrollPane.setViewportView(issuesList);
        visiblePanel.setLayout(groupLayout);

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
    public void setTotalHosts(final String count) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                totalMonitoredHosts.setText(count);
            }
        });
    }

    @Override
    public void setTotalVms(final String count) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                totalMonitoredVms.setText(count);
            }
        });
    }

    @Override
    public Component getUiComponent() {
        return visiblePanel;
    }

    private static class IssuesListModel extends AbstractListModel<Object> {

        private static final long serialVersionUID = 7131506292620902850L;

        private List<? extends Object> delegate;

        private String emptyElement = translator.localize(LocaleResources.HOME_PANEL_NO_ISSUES);

        public IssuesListModel(List<? extends Object> actualList) {
            this.delegate = actualList;
            // TODO observe the delegate for changes
        }

        @Override
        public int getSize() {
            if (delegate.isEmpty()) {
                return 1;
            }
            return delegate.size();
        }

        @Override
        public Object getElementAt(int index) {
            if (delegate.isEmpty()) {
                return emptyElement;
            }
            return delegate.get(index);
        }
    }
}
