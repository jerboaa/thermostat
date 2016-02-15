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

package com.redhat.thermostat.vm.profiler.client.swing.internal;

import static com.redhat.thermostat.vm.profiler.client.swing.internal.VmProfileView.TabbedPaneAction.TABLE_TAB_SELECTED;
import static com.redhat.thermostat.vm.profiler.client.swing.internal.VmProfileView.TabbedPaneAction.TREEMAP_TAB_SELECTED;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.Callable;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JToggleButton;
import javax.swing.ListSelectionModel;
import javax.swing.SingleSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.WindowConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableModel;

import com.redhat.thermostat.client.swing.EdtHelper;
import com.redhat.thermostat.client.swing.IconResource;
import com.redhat.thermostat.client.swing.ModelUtils;
import com.redhat.thermostat.client.swing.NonEditableTableModel;
import com.redhat.thermostat.client.swing.SwingComponent;
import com.redhat.thermostat.client.swing.components.ActionToggleButton;
import com.redhat.thermostat.client.swing.components.FontAwesomeIcon;
import com.redhat.thermostat.client.swing.components.HeaderPanel;
import com.redhat.thermostat.client.swing.components.Icon;
import com.redhat.thermostat.client.swing.components.ThermostatScrollPane;
import com.redhat.thermostat.client.swing.components.ThermostatTable;
import com.redhat.thermostat.client.swing.components.ThermostatTableRenderer;
import com.redhat.thermostat.client.swing.experimental.ComponentVisibilityNotifier;
import com.redhat.thermostat.client.ui.Palette;
import com.redhat.thermostat.common.ActionEvent;
import com.redhat.thermostat.common.ActionListener;
import com.redhat.thermostat.common.utils.MethodDescriptorConverter.MethodDeclaration;
import com.redhat.thermostat.common.utils.StringUtils;
import com.redhat.thermostat.shared.locale.LocalizedString;
import com.redhat.thermostat.shared.locale.Translate;
import com.redhat.thermostat.vm.profiler.client.core.ProfilingResult;
import com.redhat.thermostat.vm.profiler.client.core.ProfilingResult.MethodInfo;

public class SwingVmProfileView extends VmProfileView implements SwingComponent {

    private static final Icon START_ICON = IconResource.SAMPLE.getIcon();
    private static final Icon STOP_ICON = new FontAwesomeIcon('\uf04d', START_ICON.getIconHeight());

    private static final Translate<LocaleResources> translator = LocaleResources.createLocalizer();

    private static final double SPLIT_PANE_RATIO = 0.3;

    private static final int COLUMN_METHOD_NAME = 0;
    private static final int COLUMN_METHOD_PERCENTAGE = 1;
    private static final int COLUMN_METHOD_TIME = 2;

    private final CopyOnWriteArrayList<ActionListener<ProfileAction>> listeners = new CopyOnWriteArrayList<>();

    private HeaderPanel mainContainer;
    private JTabbedPane tabPane;
    private TabbedPaneAction lastTabSelection;

    private ActionToggleButton toggleButton;

    private DefaultListModel<Profile> listModel;
    private JList<Profile> profileList;

    private ThermostatTable profileTable;
    private DefaultTableModel tableModel;

    private JLabel currentStatusLabel;
    private boolean viewControlsEnabled = true;

    private ActionListener<TabbedPaneAction> tabListener;

    static class ProfileItemRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list,
                Object value, int index, boolean isSelected,
                boolean cellHasFocus) {
            if (value instanceof Profile) {
                Profile profile = (Profile) value;
                value = translator.localize(LocaleResources.PROFILER_LIST_ITEM,
                        profile.name, new Date(profile.startTimeStamp).toString(), new Date(profile.stopTimeStamp).toString()).getContents();
            }
            return super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
        }
    }

    public SwingVmProfileView() {
        listModel = new DefaultListModel<>();

        tabPane = new JTabbedPane();
        tabPane.getModel().addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                SingleSelectionModel model = (SingleSelectionModel) e.getSource();

                switch (model.getSelectedIndex()) {
                    case 0:
                        if (lastTabSelection != TABLE_TAB_SELECTED) {
                            fireTabbedPaneAction(TABLE_TAB_SELECTED);
                            lastTabSelection = TABLE_TAB_SELECTED;
                        }
                        break;
                    case 1:
                        if (lastTabSelection != TREEMAP_TAB_SELECTED) {
                            fireTabbedPaneAction(TREEMAP_TAB_SELECTED);
                            lastTabSelection = TREEMAP_TAB_SELECTED;
                        }
                        break;
                    default:
                        throw new AssertionError("Unexpected index " + model.getSelectedIndex());
                }
            }
        });
        lastTabSelection = TABLE_TAB_SELECTED;

        toggleButton = new ActionToggleButton(START_ICON, STOP_ICON, translator.localize(
                LocaleResources.START_PROFILING));
        toggleButton.setName("TOGGLE_PROFILING_BUTTON");
        toggleButton.toggleText(false);
        toggleButton.addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                JToggleButton button = (JToggleButton) e.getSource();
                if (button.isSelected()) {
                    fireProfileAction(ProfileAction.START_PROFILING);
                } else {
                    fireProfileAction(ProfileAction.STOP_PROFILING);
                }
            }
        });

        mainContainer = new HeaderPanel(translator.localize(LocaleResources.PROFILER_HEADING));
        new ComponentVisibilityNotifier().initialize(mainContainer, notifier);

        JPanel contentContainer = new JPanel(new BorderLayout());
        mainContainer.setContent(contentContainer);
        mainContainer.addToolBarButton(toggleButton);

        JComponent actionsPanel = createStatusPanel();
        contentContainer.add(actionsPanel, BorderLayout.PAGE_START);

        JComponent profilingResultsPanel = createInformationPanel();
        contentContainer.add(profilingResultsPanel, BorderLayout.CENTER);
    }

    private JPanel createStatusPanel() {
        GridBagLayout layout = new GridBagLayout();
        JPanel statusPanel = new JPanel(layout);

        GridBagConstraints constraints = new GridBagConstraints();
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.anchor = GridBagConstraints.PAGE_START;
        constraints.weightx = 1.0;
        constraints.gridy = 0;
        constraints.gridx = 0;
        constraints.gridwidth = 3;
        constraints.ipady = 5;

        String wrappedText = "<html>" + translator.localize(LocaleResources.PROFILER_DESCRIPTION).getContents() + "</html>";
        JLabel descriptionLabel = new JLabel(wrappedText);
        statusPanel.add(descriptionLabel, constraints);

        constraints.gridy = 1;
        constraints.gridx = 0;
        constraints.gridwidth = 1;
        currentStatusLabel = new JLabel("Current Status: {0}");
        currentStatusLabel.setName("CURRENT_STATUS_LABEL");
        statusPanel.add(currentStatusLabel, constraints);
        return statusPanel;
    }

    private JComponent createInformationPanel() {
        profileList = new JList<>(listModel);
        profileList.setName("PROFILE_LIST");
        profileList.setCellRenderer(new ProfileItemRenderer());
        profileList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        profileList.addListSelectionListener(new ListSelectionListener() {

            private Profile oldValue = null;

            @Override
            public void valueChanged(ListSelectionEvent e) {
                if (e.getValueIsAdjusting()) {
                    return;
                }

                Profile newValue = profileList.getSelectedValue();

                if (oldValue == null || !oldValue.equals(newValue)) {
                    oldValue = newValue;
                    fireProfileAction(ProfileAction.PROFILE_SELECTED);
                }

            }
        });
        ThermostatScrollPane profileListPane = new ThermostatScrollPane(profileList);
        profileListPane.setName("PROFILE_LIST_PANE");

        Vector<String> columnNames = new Vector<>();
        columnNames.add(translator.localize(LocaleResources.PROFILER_RESULTS_METHOD).getContents());
        columnNames.add(translator.localize(LocaleResources.PROFILER_RESULTS_PERCENTAGE_TIME).getContents());
        columnNames.add(translator.localize(LocaleResources.PROFILER_RESULTS_TIME, "ms").getContents());
        tableModel = new NonEditableTableModel(columnNames, 0) {
            @Override
            public java.lang.Class<?> getColumnClass(int columnIndex) {
                switch (columnIndex) {
                case COLUMN_METHOD_NAME:
                    return MethodDeclaration.class;
                case COLUMN_METHOD_PERCENTAGE:
                    return Double.class;
                case COLUMN_METHOD_TIME:
                    return Long.class;
                default:
                    throw new AssertionError("Unknown column index");
                }
            }
        };

        final SimpleTextRenderer simpleRenderer = new SimpleTextRenderer();
        final PlainTextMethodDeclarationRenderer plainRenderer = new PlainTextMethodDeclarationRenderer();
        final SyntaxHighlightedMethodDeclarationRenderer colorRenderer = new SyntaxHighlightedMethodDeclarationRenderer();

        profileTable = new ThermostatTable(tableModel) {
            public javax.swing.table.TableCellRenderer getCellRenderer(int row, int column) {
                if (column == COLUMN_METHOD_NAME) {
                    if(tableModel.getRowCount() == 1 && tableModel.getValueAt(0,0).equals(
                            translator.localize(LocaleResources.PROFILER_NO_RESULTS).getContents())) {
                        return simpleRenderer;
                    } else if (profileTable.isCellSelected(row, column)) {
                        return plainRenderer;
                    } else {
                        return colorRenderer;
                    }
                }
                return super.getCellRenderer(row, column);
            }
        };

        JScrollPane scrollPaneProfileTable = profileTable.wrap();
        scrollPaneProfileTable.setName("METHOD_TABLE");
        tabPane.addTab(translator.localize(LocaleResources.PROFILER_RESULTS_TABLE).getContents(),
                scrollPaneProfileTable);

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, profileListPane, tabPane);
        splitPane.setDividerLocation(SPLIT_PANE_RATIO);
        splitPane.setResizeWeight(0.5);

        return splitPane;
    }

    @Override
    public void setTabbedPaneActionListener(ActionListener<TabbedPaneAction> listener) {
        this.tabListener = listener;
    }

    private void fireTabbedPaneAction(final TabbedPaneAction action) {
        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                try {
                    ActionEvent<TabbedPaneAction> event =
                            new ActionEvent<>(this, action);
                    tabListener.actionPerformed(event);
                } catch (Throwable t) {
                    t.printStackTrace();
                }
                return null;
            }
        }.execute();
    }

    @Override
    public void addProfileActionListener(ActionListener<ProfileAction> listener) {
        listeners.add(listener);
    }

    @Override
    public void removeProfileActionlistener(ActionListener<ProfileAction> listener) {
        listeners.remove(listener);
    }

    private void fireProfileAction(final ProfileAction action) {
        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                try {
                    ActionEvent<ProfileAction> event = new ActionEvent<>(this, action);
                    for (ActionListener<ProfileAction> listener : listeners) {
                        listener.actionPerformed(event);
                    }
                } catch (Throwable t) {
                    t.printStackTrace();
                }
                return null;
            }
        }.execute();
    }

    @Override
    public void setProfilingState(final ProfilingState profilingState) {
        final String status, buttonLabel;
        if (!viewControlsEnabled) {
            status = translator.localize(LocaleResources.PROFILER_CURRENT_STATUS_DEAD).getContents();
            buttonLabel = translator.localize(LocaleResources.START_PROFILING).getContents();
        } else if (profilingState == ProfilingState.STOPPING || profilingState == ProfilingState.STARTED) {
            status = translator.localize(LocaleResources.PROFILER_CURRENT_STATUS_ACTIVE).getContents();
            buttonLabel = translator.localize(LocaleResources.STOP_PROFILING).getContents();
        } else {
            status = translator.localize(LocaleResources.PROFILER_CURRENT_STATUS_INACTIVE).getContents();
            buttonLabel = translator.localize(LocaleResources.START_PROFILING).getContents();
        }
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                currentStatusLabel.setText(status);
                if (!viewControlsEnabled) {
                    toggleButton.setToggleActionState(ProfilingState.DISABLED);
                } else {
                    toggleButton.setToggleActionState(profilingState);
                }
                toggleButton.setText(buttonLabel);
            }
        });
    }

    @Override
    public void setViewControlsEnabled(boolean enabled) {
        this.viewControlsEnabled = enabled;
        if (!enabled) {
            setProfilingState(ProfilingState.DISABLED);
        }
    }

    @Override
    public void setAvailableProfilingRuns(final List<Profile> data) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                DefaultListModel<Profile> listModel = (DefaultListModel<Profile>) profileList.getModel();
                ModelUtils.updateListModel(data, listModel);
            }

        });
    }

    @Override
    public Profile getSelectedProfile() {
        try {
            return new EdtHelper().callAndWait(new Callable<Profile>() {
                @Override
                public Profile call() throws Exception {
                    if (profileList.isSelectionEmpty()) {
                        throw new AssertionError("Selection is empty");
                    }
                    return profileList.getSelectedValue();
                }
            });
        } catch (InvocationTargetException | InterruptedException e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public void setProfilingDetailData(final ProfilingResult results) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                // delete all existing data
                tableModel.setRowCount(0);

                if (results.getMethodInfo().size() == 0) {
                    String noResultsMessage = translator.localize(
                            LocaleResources.PROFILER_NO_RESULTS).getContents();
                    tableModel.addRow(new Object[] { noResultsMessage, null, null });
                    return;
                }

                for (MethodInfo methodInfo: results.getMethodInfo()) {
                    Object[] data = new Object[] {
                            methodInfo.decl,
                            methodInfo.percentageTime,
                            methodInfo.totalTimeInMillis,
                    };
                    tableModel.addRow(data);
                }
            }
        });
    }

    @Override
    public void addTabToTabbedPane(final LocalizedString title, final Component component) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                tabPane.addTab(title.getContents(), component);
            }
        });
    }

    @Override
    public Component getUiComponent() {
        return mainContainer;
    }

    static class SimpleTextRenderer extends ThermostatTableRenderer {

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                       boolean hasFocus, int row, int column) {

            if (!(value instanceof String)) {
                throw new AssertionError("Unexpected value");
            }

            return super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row,
                    column);
        }
    }

    static class PlainTextMethodDeclarationRenderer extends ThermostatTableRenderer {

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus, int row, int column) {

            if (!(value instanceof MethodDeclaration)) {
                throw new AssertionError("Unexpected value");
            }

            String plainText = ((MethodDeclaration) value).toString();
            return super.getTableCellRendererComponent(table, plainText, isSelected, hasFocus, row, column);
        }
    }

    static class SyntaxHighlightedMethodDeclarationRenderer extends ThermostatTableRenderer {

        static final Color METHOD_COLOR = Palette.PALE_RED.getColor();
        static final Color PARAMETER_COLOR = Palette.VIOLET.getColor();
        static final Color RETURN_TYPE_COLOR = Palette.GRANITA_ORANGE.getColor();

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus, int row, int column) {

            if (!(value instanceof MethodDeclaration)) {
                throw new AssertionError("Unexpected value");
            }

            String syntaxHighlightedText = syntaxHighlightMethod((MethodDeclaration) value);
            return super.getTableCellRendererComponent(table, syntaxHighlightedText, isSelected, hasFocus, row, column);
        }

        private String syntaxHighlightMethod(MethodDeclaration decl) {
            String highlightedName = htmlColorText(decl.getName(), METHOD_COLOR);
            String highlightedReturnType = htmlColorText(decl.getReturnType(), RETURN_TYPE_COLOR);

            StringBuilder toReturn = new StringBuilder();
            toReturn.append("<html>");
            toReturn.append("<pre>");

            toReturn.append(highlightedReturnType);
            toReturn.append(" ");
            toReturn.append("<b>");
            toReturn.append(highlightedName);
            toReturn.append("</b>");
            toReturn.append("(");

            ArrayList<String> parameters = new ArrayList<>();
            for (String parameter : decl.getParameters()) {
                parameters.add(htmlColorText(parameter, PARAMETER_COLOR));
            }

            toReturn.append(StringUtils.join(",", parameters));

            toReturn.append(")");
            toReturn.append("</pre>");
            toReturn.append("<html>");
            return toReturn.toString();

        }

        /**
         * Package-private for testing purposes.
         */
        static String htmlColorText(String unescapedText, Color color) {
            String hexColorString = "#" + Integer.toHexString(color.getRGB() & 0x00ffffff);
            return "<font color='" + hexColorString + "'>"
                    + StringUtils.htmlEscape(unescapedText) + "</font>";
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                JFrame window = new JFrame();
                SwingVmProfileView view = new SwingVmProfileView();
                window.add(view.getUiComponent());
                window.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
                window.pack();
                window.setVisible(true);

                List<MethodInfo> data = new ArrayList<>();
                data.add(new MethodInfo(new MethodDeclaration(
                        "foo", list("int"), "int"), 1000, 1.0));
                data.add(new MethodInfo(new MethodDeclaration(
                        "bar", list("foo.bar.Baz", "int"), "Bar"), 100000, 100));
                ProfilingResult results = new ProfilingResult(data);
                view.setProfilingDetailData(results);
            }

            private List<String> list(String... args) {
                return Arrays.asList(args);
            }
        });
    }

}
