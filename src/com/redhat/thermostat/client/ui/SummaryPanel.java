package com.redhat.thermostat.client.ui;

import static com.redhat.thermostat.client.Translate._;

import java.awt.BorderLayout;
import java.util.ArrayList;
import java.util.List;

import javax.swing.AbstractListModel;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListModel;

import com.redhat.thermostat.client.SummaryPanelFacade;
import com.redhat.thermostat.client.ui.SimpleTable.Section;
import com.redhat.thermostat.client.ui.SimpleTable.TableEntry;

public class SummaryPanel extends JPanel {

    private static final long serialVersionUID = -5953027789947771737L;

    private final SummaryPanelFacade facade;

    public SummaryPanel(SummaryPanelFacade facade) {
        this.facade = facade;

        setLayout(new BorderLayout());

        JPanel intro = new JPanel();
        intro.setLayout(new BorderLayout());
        JLabel homeIcon = new JLabel(IconResource.USER_HOME.getIcon());
        intro.add(homeIcon, BorderLayout.LINE_START);

        JLabel welcomeMessageLabel = new JLabel(new HtmlTextBuilder(_("WELCOME_MESSAGE")).toHtml());
        intro.add(welcomeMessageLabel, BorderLayout.CENTER);

        add(intro, BorderLayout.PAGE_START);

        List<Section> sections = new ArrayList<Section>();
        TableEntry entry;

        Section summarySection = new Section(_("HOME_PANEL_SECTION_SUMMARY"));
        sections.add(summarySection);

        entry = new TableEntry(_("HOME_PANEL_TOTAL_MACHINES"), String.valueOf(facade.getTotalConnectedAgents()));
        summarySection.add(entry);
        entry = new TableEntry(_("HOME_PANEL_TOTAL_JVMS"), String.valueOf(facade.getTotalConnectedVms()));
        summarySection.add(entry);

        JPanel summaryPanel = SimpleTable.createTable(sections);
        summaryPanel.setBorder(Components.smallBorder());
        add(summaryPanel, BorderLayout.CENTER);

        JPanel issuesPanel = createIssuesPanel();
        issuesPanel.setBorder(Components.smallBorder());
        add(issuesPanel, BorderLayout.PAGE_END);

    }

    public JPanel createIssuesPanel() {
        JPanel result = new JPanel(new BorderLayout());

        result.add(Components.header(_("HOME_PANEL_SECTION_ISSUES")), BorderLayout.PAGE_START);

        ListModel model = new IssuesListModel(new ArrayList<Object>());

        JList issuesList = new JList(model);
        result.add(new JScrollPane(issuesList), BorderLayout.CENTER);

        return result;
    }

    private static class IssuesListModel extends AbstractListModel {

        private static final long serialVersionUID = 7131506292620902850L;

        private List<? extends Object> delegate;

        private String emptyElement = new String(_("HOME_PANEL_NO_ISSUES"));

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
