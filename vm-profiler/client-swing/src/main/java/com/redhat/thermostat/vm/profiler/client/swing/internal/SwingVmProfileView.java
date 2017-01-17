/*
 * Copyright 2012-2017 Red Hat, Inc.
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
import java.util.Date;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.Callable;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JLayer;
import javax.swing.JOptionPane;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JToggleButton;
import javax.swing.ListSelectionModel;
import javax.swing.OverlayLayout;
import javax.swing.RowSorter;
import javax.swing.SingleSelectionModel;
import javax.swing.SortOrder;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableModel;

import com.redhat.thermostat.client.swing.EdtHelper;
import com.redhat.thermostat.client.swing.IconResource;
import com.redhat.thermostat.client.swing.ModelUtils;
import com.redhat.thermostat.client.swing.NonEditableTableModel;
import com.redhat.thermostat.client.swing.OverlayContainer;
import com.redhat.thermostat.client.swing.SwingComponent;
import com.redhat.thermostat.client.swing.UIDefaults;
import com.redhat.thermostat.client.swing.components.ActionToggleButton;
import com.redhat.thermostat.client.swing.components.FontAwesomeIcon;
import com.redhat.thermostat.client.swing.components.HeaderPanel;
import com.redhat.thermostat.client.swing.components.Icon;
import com.redhat.thermostat.client.swing.components.OverlayPanel;
import com.redhat.thermostat.client.swing.components.SearchField;
import com.redhat.thermostat.client.swing.components.ShadowLabel;
import com.redhat.thermostat.client.swing.components.ThermostatScrollPane;
import com.redhat.thermostat.client.swing.components.ThermostatTabbedPane;
import com.redhat.thermostat.client.swing.components.ThermostatTable;
import com.redhat.thermostat.client.swing.components.ThermostatTableRenderer;
import com.redhat.thermostat.client.swing.components.ThermostatThinScrollBar;
import com.redhat.thermostat.client.swing.experimental.ComponentVisibilityNotifier;
import com.redhat.thermostat.client.ui.Palette;
import com.redhat.thermostat.client.ui.SearchProvider.SearchAction;
import com.redhat.thermostat.common.ActionEvent;
import com.redhat.thermostat.common.ActionListener;
import com.redhat.thermostat.common.utils.MethodDescriptorConverter.MethodDeclaration;
import com.redhat.thermostat.common.utils.StringUtils;
import com.redhat.thermostat.platform.swing.components.ThermostatComponent;
import com.redhat.thermostat.shared.locale.LocalizedString;
import com.redhat.thermostat.shared.locale.Translate;
import com.redhat.thermostat.ui.swing.components.Spinner;
import com.redhat.thermostat.ui.swing.components.SpinnerLayerUI;
import com.redhat.thermostat.vm.profiler.client.core.ProfilingResult;
import com.redhat.thermostat.vm.profiler.client.core.ProfilingResult.MethodInfo;

class SwingVmProfileView extends VmProfileView implements SwingComponent, OverlayContainer {

    /** these components names are for testing only */
    static final String CURRENT_STATUS_LABEL_NAME = "CURRENT_STATUS_LABEL";
    static final String PROFILES_LIST_NAME = "PROFILES_LIST";
    static final String PROFILE_TABLE_NAME = "METHOD_TABLE";
    static final String TOGGLE_BUTTON_NAME = "TOGGLE_PROFILING_BUTTON";
    static final String TOGGLE_PROFILE_LIST_NAME = "TOGGLE_PROFILE_LIST_NAME";
    static final String STACK_PANE = "STACK_PANE";
    static final String OVERLAY_PANEL = "OVERLAY_PANEL";

    private static final Icon START_ICON = IconResource.SAMPLE.getIcon();
    private static final Icon STOP_ICON = new FontAwesomeIcon('\uf28e', START_ICON.getIconHeight());
    private static final Icon LIST_SESSIONS_ICON = IconResource.HISTORY.getIcon();

    private static final Translate<LocaleResources> translator = LocaleResources.createLocalizer();

    private static final int COLUMN_METHOD_NAME = 0;
    private static final int COLUMN_METHOD_PERCENTAGE = 1;
    private static final int COLUMN_METHOD_TIME = 2;

    private final CopyOnWriteArrayList<ActionListener<ProfileAction>> listeners = new CopyOnWriteArrayList<>();

    private HeaderPanel mainContainer;
    private ThermostatTabbedPane tabPane;
    private TabbedPaneAction lastTabSelection;

    private ActionToggleButton toggleButton;

    private DefaultListModel<Profile> listModel;
    private JList<Profile> profileList;

    private ThermostatTable profileTable;
    private DefaultTableModel tableModel;

    private boolean viewControlsEnabled = true;

    private ActionListener<TabbedPaneAction> tabListener;

    private OverlayPanel overlay;
    private ActionToggleButton showRecordedSessionsButton;

    private UIDefaults uiDefaults;

    private ThermostatComponent contentContainer;

    private SpinningPanel spinner;
    private SearchField filter;

    static class ProfileItemRenderer extends DefaultListCellRenderer {

        private UIDefaults uiDefaults;

        public ProfileItemRenderer(UIDefaults uiDefaults) {
            this.uiDefaults = uiDefaults;
        }

        @Override
        public Component getListCellRendererComponent(JList<?> list,
                Object value, int index, boolean isSelected,
                boolean cellHasFocus)
        {
            if (value instanceof Profile) {
                Profile profile = (Profile) value;
                value = translator.localize(LocaleResources.PROFILER_LIST_ITEM,
                        profile.name, new Date(profile.startTimeStamp).toString(),
                                      new Date(profile.stopTimeStamp).toString()).getContents();
            } else {
                return super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            }

            JPanel panel = new JPanel();
            panel.setName(value.toString());
            panel.setLayout(new BorderLayout());
            panel.setOpaque(false);
            ShadowLabel label = new ShadowLabel();
            label.setText(value.toString());
            label.setOpaque(false);

            if (isSelected || cellHasFocus) {
                panel.setOpaque(true);
                panel.setBackground((Color) uiDefaults.getSelectedComponentBGColor());
                label.setForeground((Color) uiDefaults.getSelectedComponentFGColor());

            } else {
                label.setForeground((Color) uiDefaults.getComponentFGColor());
            }

            panel.add(label);
            return panel;
        }
    }

    public SwingVmProfileView(UIDefaults uiDefaults) {

        this.uiDefaults = uiDefaults;

        tabPane = new ThermostatTabbedPane();
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
        toggleButton.setToolTipText(translator.localize(LocaleResources.PROFILING_TOGGLE_TOOLTIP).getContents());
        toggleButton.setName(TOGGLE_BUTTON_NAME);
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

        showRecordedSessionsButton = new ActionToggleButton(LIST_SESSIONS_ICON,
                                                            translator.localize(LocaleResources.DISPLAY_SESSIONS));
        showRecordedSessionsButton.setName(TOGGLE_PROFILE_LIST_NAME);
        showRecordedSessionsButton.setToolTipText(translator.localize(LocaleResources.DISPLAY_SESSIONS_TOOLTIP).getContents());

        createOverlay();
        mainContainer = new HeaderPanel(translator.localize(LocaleResources.PROFILER_HEADING));
        mainContainer.addOverlayCloseListeners(overlay);
        new ComponentVisibilityNotifier().initialize(mainContainer, notifier);

        contentContainer = new ThermostatComponent();
        mainContainer.addToolBarButton(toggleButton);
        mainContainer.addToolBarButton(showRecordedSessionsButton);

        JComponent profilingResultsPanel = createInformationPanel();
        contentContainer.add(profilingResultsPanel, BorderLayout.CENTER);

        JPanel stack = new JPanel() {
            @Override
            public boolean isOptimizedDrawingEnabled() {
                return false;
            }
        };

        spinner = new SpinningPanel(uiDefaults);

        stack.setName(STACK_PANE);
        stack.setLayout(new OverlayLayout(stack));

        stack.add(overlay);
        stack.add(spinner);
        stack.add(contentContainer);

        stack.setOpaque(false);

        mainContainer.setContent(stack);
    }

    public SpinningPanel getSpinner() {
        return spinner;
    }

    private void createOverlay() {
        overlay = new OverlayPanel(translator.localize(LocaleResources.RECORDING_LIST), true, true);
        overlay.setName(OVERLAY_PANEL);
        overlay.setOpaque(false);
        overlay.addCloseEventListener(new OverlayPanel.CloseEventListener() {
            @Override
            public void closeRequested(OverlayPanel.CloseEvent event) {
                if (overlay.isVisible()) {
                    showRecordedSessionsButton.getToolbarButton().doClick();
                }
            }
        });

        listModel = new DefaultListModel<>();
        profileList = new JList<>(listModel);
        profileList.setOpaque(false);

        profileList.setName(PROFILES_LIST_NAME);
        profileList.setCellRenderer(new ProfileItemRenderer(uiDefaults));
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
        profileListPane.setVerticalScrollBar(new ThermostatThinScrollBar(ThermostatThinScrollBar.VERTICAL));
        profileListPane.setHorizontalScrollBar(new ThermostatThinScrollBar(ThermostatThinScrollBar.HORIZONTAL));

        overlay.add(profileListPane);

        showRecordedSessionsButton.addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                if (overlay.isVisible()) {
                    overlay.setOverlayVisible(false);
                } else {
                    fireProfileAction(ProfileAction.DISPLAY_PROFILING_SESSIONS);
                }
            }
        });
    }

    private JComponent createInformationPanel() {
        filter = new SearchField();
        filter.setLabel(translator.localize(LocaleResources.PROFILER_RESULTS_FILTER_HINT));
        filter.addSearchListener(new ActionListener<SearchAction>() {
            @Override
            public void actionPerformed(ActionEvent<SearchAction> actionEvent) {
                fireProfileAction(ProfileAction.PROFILE_TABLE_FILTER_CHANGED);
            }
        });

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
                int methodColumnIndex = tableHeader.getColumnModel().getColumnIndex(translator.localize(LocaleResources.PROFILER_RESULTS_METHOD).getContents());
                if (column == methodColumnIndex) {
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

        List <RowSorter.SortKey> sortKeys = new ArrayList<>();
        sortKeys.add(new RowSorter.SortKey(COLUMN_METHOD_TIME, SortOrder.DESCENDING));
        profileTable.getRowSorter().setSortKeys(sortKeys);

        profileTable.setName(PROFILE_TABLE_NAME);
        JScrollPane scrollPaneProfileTable = profileTable.wrap();

        JPanel tablePanel = new JPanel(new BorderLayout());
        tablePanel.add(filter, BorderLayout.PAGE_START);
        tablePanel.add(scrollPaneProfileTable, BorderLayout.CENTER);

        tabPane.addTab(translator.localize(LocaleResources.PROFILER_RESULTS_TABLE).getContents(), tablePanel);

        JPanel pane = new JPanel();
        pane.setLayout(new BorderLayout());
        pane.add(tabPane);

        return pane;
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
            buttonLabel = translator.localize(LocaleResources.START_PROFILING).getContents();
        } else if (profilingState == ProfilingState.STOPPING || profilingState == ProfilingState.STARTED) {
            buttonLabel = translator.localize(LocaleResources.STOP_PROFILING).getContents();
        } else {
            buttonLabel = translator.localize(LocaleResources.START_PROFILING).getContents();
        }
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                if (!viewControlsEnabled) {
                    toggleButton.setToggleActionState(ProfilingState.DISABLED);
                } else {
                    toggleButton.setToggleActionState(profilingState);
                }
                toggleButton.setText(buttonLabel);

                boolean enabled = false;
                switch (profilingState) {
                    case STARTED:
                    case STARTING:
                    case STOPPING:
                        enabled = true;
                        break;
                    default:
                        enabled = false;
                        break;
                }

                spinner.enableSpinner(enabled);
                contentContainer.setEnabled(!enabled);
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
    public void setDisplayProfilingRuns(final boolean display) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                overlay.setOverlayVisible(display);
            }
        });
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
    public String getProfilingDataFilter() {
        try {
            return new EdtHelper().callAndWait(new Callable<String>() {
                @Override
                public String call() throws Exception {
                    return filter.getSearchText();
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

    public void displayErrorMessage(final LocalizedString message) {
        JOptionPane.showMessageDialog(this.getUiComponent(), message.getContents(),
                translator.localize(LocaleResources.ERROR_HEADER).getContents(), JOptionPane.ERROR_MESSAGE);
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

    @Override
    public OverlayPanel getOverlay() {
        return overlay;
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

            return super.getTableCellRendererComponent(table, value.toString(),
                                                       isSelected, hasFocus, row,
                                                       column);
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
}
