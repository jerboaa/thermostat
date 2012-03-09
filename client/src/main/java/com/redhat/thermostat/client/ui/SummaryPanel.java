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

import java.awt.BorderLayout;
import java.util.ArrayList;
import java.util.List;

import javax.swing.AbstractListModel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListModel;

import com.redhat.thermostat.client.SummaryPanelFacade;
import com.redhat.thermostat.client.locale.LocaleResources;
import com.redhat.thermostat.client.ui.SimpleTable.Section;
import com.redhat.thermostat.client.ui.SimpleTable.TableEntry;

public class SummaryPanel extends JPanel {

    private static final long serialVersionUID = -5953027789947771737L;

    private final SummaryPanelFacade facade;

    public SummaryPanel(SummaryPanelFacade facade) {
        this.facade = facade;

        setLayout(new BorderLayout());

        List<Section> sections = new ArrayList<Section>();
        TableEntry entry;

        Section summarySection = new Section(localize(LocaleResources.HOME_PANEL_SECTION_SUMMARY));
        sections.add(summarySection);

        entry = new TableEntry(localize(LocaleResources.HOME_PANEL_TOTAL_MACHINES), this.facade.getTotalConnectedAgents());
        summarySection.add(entry);
        entry = new TableEntry(localize(LocaleResources.HOME_PANEL_TOTAL_JVMS), this.facade.getTotalConnectedVms());
        summarySection.add(entry);

        SimpleTable simpleTable = new SimpleTable();
        JPanel summaryPanel = simpleTable.createTable(sections);
        summaryPanel.setBorder(Components.smallBorder());
        add(summaryPanel, BorderLayout.CENTER);

        JPanel issuesPanel = createIssuesPanel();
        issuesPanel.setBorder(Components.smallBorder());
        add(issuesPanel, BorderLayout.PAGE_END);

        addHierarchyListener(new AsyncFacadeManager(facade));
    }

    public JPanel createIssuesPanel() {
        JPanel result = new JPanel(new BorderLayout());

        result.add(Components.header(localize(LocaleResources.HOME_PANEL_SECTION_ISSUES)), BorderLayout.PAGE_START);

        ListModel<Object> model = new IssuesListModel(new ArrayList<>());

        JList<Object> issuesList = new JList<>(model);
        result.add(new JScrollPane(issuesList), BorderLayout.CENTER);

        return result;
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
