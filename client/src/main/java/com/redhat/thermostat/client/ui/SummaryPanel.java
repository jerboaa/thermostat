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

import java.awt.Component;
import java.util.ArrayList;
import java.util.List;

import javax.swing.AbstractListModel;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListModel;
import javax.swing.SwingUtilities;

import com.redhat.thermostat.client.locale.LocaleResources;
import com.redhat.thermostat.common.ActionListener;
import com.redhat.thermostat.common.ActionNotifier;
import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;
import javax.swing.LayoutStyle.ComponentPlacement;

public class SummaryPanel extends JPanel implements SummaryView {

    private static final long serialVersionUID = -5953027789947771737L;

    private final ActionNotifier<Action> notifier = new ActionNotifier<>(this);

    private final JLabel totalMonitoredHosts;
    private final JLabel totalMonitoredVms;

    private final List<String> issuesList;

    public SummaryPanel() {

        JLabel lblHomepanel = new JLabel(localize(LocaleResources.HOME_PANEL_SECTION_SUMMARY));

        JLabel lblTotalHosts = new JLabel(localize(LocaleResources.HOME_PANEL_TOTAL_MACHINES));

        totalMonitoredHosts = new JLabel("${TOTAL_MONITORED_HOSTS}");

        JLabel lblTotal = new JLabel(localize(LocaleResources.HOME_PANEL_TOTAL_JVMS));

        totalMonitoredVms = new JLabel("${TOTAL_MONITORED_VMS}");

        JLabel lblIssues = new JLabel(localize(LocaleResources.HOME_PANEL_SECTION_ISSUES));

        JScrollPane scrollPane = new JScrollPane();

        GroupLayout groupLayout = new GroupLayout(this);
        groupLayout.setHorizontalGroup(
            groupLayout.createParallelGroup(Alignment.LEADING)
                .addGroup(Alignment.TRAILING, groupLayout.createSequentialGroup()
                    .addContainerGap()
                    .addGroup(groupLayout.createParallelGroup(Alignment.TRAILING)
                        .addComponent(lblHomepanel, Alignment.LEADING)
                        .addGroup(Alignment.LEADING, groupLayout.createSequentialGroup()
                            .addGap(12)
                            .addGroup(groupLayout.createParallelGroup(Alignment.TRAILING)
                                .addComponent(lblTotal)
                                .addComponent(lblTotalHosts))
                            .addPreferredGap(ComponentPlacement.RELATED, 36, Short.MAX_VALUE)
                            .addGroup(groupLayout.createParallelGroup(Alignment.LEADING, false)
                                .addComponent(totalMonitoredVms, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addComponent(totalMonitoredHosts, GroupLayout.DEFAULT_SIZE, 232, Short.MAX_VALUE)))
                        .addComponent(lblIssues, Alignment.LEADING)
                        .addGroup(groupLayout.createSequentialGroup()
                            .addGap(12)
                            .addComponent(scrollPane, GroupLayout.DEFAULT_SIZE, 459, Short.MAX_VALUE)))
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
                        .addComponent(totalMonitoredHosts))
                    .addPreferredGap(ComponentPlacement.RELATED)
                    .addGroup(groupLayout.createParallelGroup(Alignment.BASELINE)
                        .addComponent(lblTotal)
                        .addComponent(totalMonitoredVms))
                    .addGap(18)
                    .addComponent(lblIssues)
                    .addPreferredGap(ComponentPlacement.RELATED)
                    .addComponent(scrollPane, GroupLayout.DEFAULT_SIZE, 180, Short.MAX_VALUE)
                    .addContainerGap())
        );

        issuesList = new ArrayList<>();
        ListModel<Object> issuesListmodel = new IssuesListModel(issuesList);
        JList<Object> issuesList = new JList<>(issuesListmodel);
        scrollPane.setViewportView(issuesList);
        setLayout(groupLayout);



        addHierarchyListener(new ComponentVisibleListener() {
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
        return this;
    }

    private static class IssuesListModel extends AbstractListModel<Object> {

        private static final long serialVersionUID = 7131506292620902850L;

        private List<? extends Object> delegate;

        private String emptyElement = localize(LocaleResources.HOME_PANEL_NO_ISSUES);

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
