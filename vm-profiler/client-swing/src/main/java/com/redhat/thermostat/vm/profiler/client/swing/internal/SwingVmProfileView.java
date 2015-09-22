/*
 * Copyright 2012-2015 Red Hat, Inc.
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
import javax.swing.JSplitPane;
import javax.swing.JToggleButton;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.WindowConstants;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableModel;

import com.redhat.thermostat.client.swing.EdtHelper;
import com.redhat.thermostat.client.swing.ModelUtils;
import com.redhat.thermostat.client.swing.NonEditableTableModel;
import com.redhat.thermostat.client.swing.SwingComponent;
import com.redhat.thermostat.client.swing.components.HeaderPanel;
import com.redhat.thermostat.client.swing.components.ThermostatScrollPane;
import com.redhat.thermostat.client.swing.components.ThermostatTable;
import com.redhat.thermostat.client.swing.experimental.ComponentVisibilityNotifier;
import com.redhat.thermostat.client.ui.Palette;
import com.redhat.thermostat.common.ActionEvent;
import com.redhat.thermostat.common.ActionListener;
import com.redhat.thermostat.common.utils.MethodDescriptorConverter.MethodDeclaration;
import com.redhat.thermostat.common.utils.StringUtils;
import com.redhat.thermostat.shared.locale.Translate;
import com.redhat.thermostat.vm.profiler.client.core.ProfilingResult;
import com.redhat.thermostat.vm.profiler.client.core.ProfilingResult.MethodInfo;

public class SwingVmProfileView extends VmProfileView implements SwingComponent {

    private static final Translate<LocaleResources> translator = LocaleResources.createLocalizer();

    private static final double SPLIT_PANE_RATIO = 0.3;

    private final CopyOnWriteArrayList<ActionListener<ProfileAction>> listeners = new CopyOnWriteArrayList<>();

    private HeaderPanel mainContainer;

    private JToggleButton startButton;
    private JToggleButton stopButton;

    private DefaultListModel<Profile> listModel;
    private JList<Profile> profileList;

    private DefaultTableModel tableModel;

    private JLabel currentStatusLabel;

    static class ProfileItemRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list,
                Object value, int index, boolean isSelected,
                boolean cellHasFocus) {
            if (value instanceof Profile) {
                Profile profile = (Profile) value;
                value = translator
                        .localize(LocaleResources.PROFILER_LIST_ITEM,
                                profile.name, new Date(profile.timeStamp).toString())
                        .getContents();
            }
            return super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
        }
    }

    public SwingVmProfileView() {
        listModel = new DefaultListModel<>();

        mainContainer = new HeaderPanel(translator.localize(LocaleResources.PROFILER_HEADING));
        new ComponentVisibilityNotifier().initialize(mainContainer, notifier);

        JPanel contentContainer = new JPanel(new BorderLayout());
        mainContainer.setContent(contentContainer);

        JComponent actionsPanel = createActionsPanel();
        contentContainer.add(actionsPanel, BorderLayout.PAGE_START);

        JComponent profilingResultsPanel = createInformationPanel();
        contentContainer.add(profilingResultsPanel, BorderLayout.CENTER);
    }

    private JPanel createActionsPanel() {
        GridBagLayout layout = new GridBagLayout();
        JPanel actionsPanel = new JPanel(layout);

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
        actionsPanel.add(descriptionLabel, constraints);

        constraints.gridy = 1;
        constraints.gridx = 0;
        constraints.gridwidth = 1;
        currentStatusLabel = new JLabel("Current Status: {0}");
        actionsPanel.add(currentStatusLabel, constraints);

        constraints.fill = GridBagConstraints.NONE;
        constraints.gridx = GridBagConstraints.RELATIVE;
        constraints.weightx = 0.0;
        startButton = new JToggleButton(translator.localize(LocaleResources.START_PROFILING).getContents());
        startButton.addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                JToggleButton button = (JToggleButton) e.getSource();
                if (button.isSelected()) {
                    fireProfileAction(ProfileAction.START_PROFILING);
                }
            }
        });
        actionsPanel.add(startButton, constraints);
        stopButton = new JToggleButton(translator.localize(LocaleResources.STOP_PROFILING).getContents());
        stopButton.addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                JToggleButton button = (JToggleButton) e.getSource();
                if (button.isSelected()) {
                    fireProfileAction(ProfileAction.STOP_PROFILING);
                }
            }
        });
        actionsPanel.add(stopButton, constraints);
        return actionsPanel;
    }

    private JComponent createInformationPanel() {
        if (!SwingUtilities.isEventDispatchThread()) {
            throw new AssertionError("Not in the EDT!");
        }

        profileList = new JList<>(listModel);
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

        Vector<String> columnNames = new Vector<>();
        columnNames.add(translator.localize(LocaleResources.PROFILER_RESULTS_METHOD).getContents());
        columnNames.add(translator.localize(LocaleResources.PROFILER_RESULTS_PERCENTAGE_TIME).getContents());
        columnNames.add(translator.localize(LocaleResources.PROFILER_RESULTS_TIME, "ms").getContents());
        tableModel = new NonEditableTableModel(columnNames, 0) {
            @Override
            public java.lang.Class<?> getColumnClass(int columnIndex) {
                switch (columnIndex) {
                case 0:
                    return String.class;
                case 1:
                    return Double.class;
                case 2:
                    return Long.class;
                default:
                    throw new AssertionError("Unknown column index");
                }
            }
        };

        ThermostatTable profileTable = new ThermostatTable(tableModel);

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                profileListPane, profileTable.wrap());
        splitPane.setDividerLocation(SPLIT_PANE_RATIO);
        splitPane.setResizeWeight(0.5);

        return splitPane;
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
        if (!SwingUtilities.isEventDispatchThread()) {
            throw new AssertionError("Not in the EDT!");
        }

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
    public void enableStartProfiling(final boolean start) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                startButton.setEnabled(start);
            }
        });
    }

    @Override
    public void enableStopProfiling(final boolean stop) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                stopButton.setEnabled(stop);
            }
        });
    }

    @Override
    public void setProfilingStatus(final String text, final boolean active) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                currentStatusLabel.setText(text);
                if (active) {
                    startButton.setSelected(true);
                    stopButton.setSelected(false);
                } else {
                    startButton.setSelected(false);
                    stopButton.setSelected(true);
                }
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
    public void setProfilingDetailData(final ProfilingResult results) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                // delete all existing data
                tableModel.setRowCount(0);

                if (results.getMethodInfo().size() == 0) {
                    String noResultsMessage = translator
                            .localize(LocaleResources.PROFILER_NO_RESULTS)
                            .getContents();
                    tableModel.addRow(new Object[] { noResultsMessage, null, null });
                    return;
                }

                for (MethodInfo methodInfo: results.getMethodInfo()) {
                    Object[] data = new Object[] {
                            syntaxHighlightMethod(methodInfo.decl),
                            methodInfo.percentageTime,
                            methodInfo.totalTimeInMillis,
                    };
                    tableModel.addRow(data);
                }
            }
        });
    }

    private String syntaxHighlightMethod(MethodDeclaration decl) {
        final Color METHOD_COLOR = Palette.PALE_RED.getColor();
        final Color PARAMETER_COLOR = Palette.AZUREUS.getColor();
        final Color RETURN_TYPE_COLOR = Palette.SKY_BLUE.getColor();

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

    private String htmlColorText(String unescapedText, Color color) {
        return "<font color='" + ("#" + Integer.toHexString(color.getRGB() & 0x00ffffff)) + "'>"
                + StringUtils.htmlEscape(unescapedText) + "</font>";
    }

    @Override
    public Component getUiComponent() {
        return mainContainer;
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
                data.add(new MethodInfo(new MethodDeclaration("foo", list("int"), "int"), 1000, 1.0));
                data.add(new MethodInfo(new MethodDeclaration("bar", list("foo.bar.Baz", "int"), "Bar"), 100000, 100));
                ProfilingResult results = new ProfilingResult(data);
                view.setProfilingDetailData(results);
            }

            private List<String> list(String... args) {
                return Arrays.asList(args);
            }
        });
    }

}
