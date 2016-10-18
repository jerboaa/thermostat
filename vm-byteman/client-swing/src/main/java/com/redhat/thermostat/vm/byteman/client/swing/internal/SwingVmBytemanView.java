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

package com.redhat.thermostat.vm.byteman.client.swing.internal;

import java.awt.Color;
import java.awt.Component;
import java.awt.ComponentOrientation;
import java.awt.Cursor;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.border.LineBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.plaf.basic.BasicSplitPaneDivider;
import javax.swing.plaf.basic.BasicSplitPaneUI;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.SymbolAxis;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.category.CategoryDataset;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.data.xy.XYDataset;

import com.redhat.thermostat.client.swing.IconResource;
import com.redhat.thermostat.client.swing.SwingComponent;
import com.redhat.thermostat.client.swing.components.ActionToggleButton;
import com.redhat.thermostat.client.swing.components.FontAwesomeIcon;
import com.redhat.thermostat.client.swing.components.HeaderPanel;
import com.redhat.thermostat.client.swing.components.Icon;
import com.redhat.thermostat.client.swing.components.ThermostatScrollPane;
import com.redhat.thermostat.client.swing.components.ThermostatTabbedPane;
import com.redhat.thermostat.client.swing.components.ThermostatTextArea;
import com.redhat.thermostat.client.swing.components.experimental.RecentTimeControlPanel;
import com.redhat.thermostat.client.swing.components.experimental.RecentTimeControlPanel.UnitRange;
import com.redhat.thermostat.client.swing.components.experimental.ThermostatChartPanel;
import com.redhat.thermostat.client.swing.components.experimental.ThermostatChartPanelBuilder;
import com.redhat.thermostat.client.swing.experimental.ComponentVisibilityNotifier;
import com.redhat.thermostat.common.ActionEvent;
import com.redhat.thermostat.common.ActionListener;
import com.redhat.thermostat.common.Duration;
import com.redhat.thermostat.common.utils.LoggingUtils;
import com.redhat.thermostat.shared.locale.LocalizedString;
import com.redhat.thermostat.shared.locale.Translate;
import com.redhat.thermostat.vm.byteman.client.swing.internal.GraphDataset.CategoryTimePlotData;
import com.redhat.thermostat.vm.byteman.client.swing.internal.GraphDataset.CoordinateType;
import com.redhat.thermostat.vm.byteman.client.swing.internal.GraphDataset.Filter;
import com.redhat.thermostat.vm.byteman.client.swing.internal.PredefinedKeysMapper.MapDirection;
import com.redhat.thermostat.vm.byteman.common.BytemanMetric;

public class SwingVmBytemanView extends VmBytemanView implements SwingComponent {

    private static final Logger logger = LoggingUtils.getLogger(SwingVmBytemanView.class);
    private static final Translate<LocaleResources> t = LocaleResources.createLocalizer();
    private static final Icon START_ICON = IconResource.SAMPLE.getIcon();
    private static final Icon STOP_ICON = new FontAwesomeIcon('\uf28e', START_ICON.getIconHeight());
    private static final Icon ARROW_LEFT = IconResource.ARROW_LEFT.getIcon();
    private static final Icon ARROW_RIGHT = IconResource.ARROW_RIGHT.getIcon();
    private static final String EMPTY_STR = "";
    private static final String BYTEMAN_CHART_LABEL = EMPTY_STR;
    private static final PredefinedKeysMapper KEYS_MAPPER = new PredefinedKeysMapper();
    
    static final String NO_METRICS_AVAILABLE = t.localize(LocaleResources.NO_METRICS_AVAILABLE).getContents();
    
    // Names for buttons used in testing
    static final String TOGGLE_BUTTON_NAME = "TOGGLE_RULE_BUTTON";
    static final String RULES_INJECTED_TEXT_NAME = "RULES_INJECTED_TEXT";
    static final String RULES_UNLOADED_TEXT_NAME = "RULES_UNLOADED_TEXT";
    static final String METRICS_TEXT_NAME = "METRICS_TEXT";
    
    private String injectedRuleContent;
    private String unloadedRuleContent;
    private ThermostatChartPanel graphPanel;
    private RecentTimeControlPanel graphTimeControlPanel;
    private boolean generateRuleToggle;
    private final JTextArea metricsText;
    private final JTextArea unloadedRulesText;
    private final JTextArea injectedRulesText;
    private final JButton injectRuleButton;
    private final JButton unloadRuleButton;
    private final JPanel graphMainPanel;
    private final JPanel metricsPanel;
    private final JTabbedPane tabbedPane;
    private final HeaderPanel mainContainer;
    private final ActionToggleButton toggleButton;
    private final CopyOnWriteArrayList<ActionListener<InjectAction>> injectListeners = new CopyOnWriteArrayList<>();
    private final CopyOnWriteArrayList<ActionListener<TabbedPaneAction>> tabbedPaneListeners = new CopyOnWriteArrayList<>();
    private final CopyOnWriteArrayList<ActionListener<GenerateAction>> generateListeners = new CopyOnWriteArrayList<>();
    
    // graph configurtaion choices from user
    // ideally these ought to be stored as
    // fields of a separate model instance

    private String xkey = null;
    private String ykey = null;
    private String filter = null;
    private String value = null;
    private String graphtype = null;
    
    // Graph widgets
    private final JComboBox<String> xCombo;
    private final JComboBox<String> yCombo;
    private final JComboBox<String> filterCombo;
    private final JTextField filterText;

    // duration over which to search for metrics
    private Duration duration = ThermostatChartPanel.DEFAULT_DATA_DISPLAY;

    // Mutable state
    private boolean viewControlsEnabled;
    
    SwingVmBytemanView() {
        toggleButton = new ActionToggleButton(START_ICON, STOP_ICON, t.localize(
                LocaleResources.INJECT_RULE));
        toggleButton.setName(TOGGLE_BUTTON_NAME);
        toggleButton.toggleText(false);
        toggleButton.addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                JToggleButton button = (JToggleButton) e.getSource();
                if (button.isSelected()) {
                    fireInjectAction(InjectAction.INJECT_RULE);
                } else {
                    fireInjectAction(InjectAction.UNLOAD_RULE);
                }
            }

        });
        mainContainer = new HeaderPanel(t.localize(LocaleResources.BYTEMAN_HEADER_TITLE));
        new ComponentVisibilityNotifier().initialize(mainContainer, notifier);
        
        final double yWeightRow0 = 0.05;
        final double yWeightRow1 = 0.90;
        final double yWeightRow2 = 0.05;
        final double xWeightFullWidth = 1.0;
        final double halfWeight = 0.5;
        final Insets paddingInsets = new Insets(5, 5, 5, 5);

        // Rules tab
        final JPanel rulesPanel = new JPanel();
        rulesPanel.setLayout(new GridBagLayout());

        // Label Descriptors
        JLabel localLabel = new JLabel(t.localize(LocaleResources.LABEL_LOCAL_RULE).getContents());
        GridBagConstraints cRules = new GridBagConstraints();
        cRules.gridx = 0;
        cRules.gridy = 0;
        cRules.anchor = GridBagConstraints.LINE_START;
        cRules.insets = paddingInsets;
        rulesPanel.add(localLabel, cRules);
        JLabel injectLabel = new JLabel(t.localize(LocaleResources.LABEL_INJECTED_RULE).getContents());
        cRules = new GridBagConstraints();
        cRules.gridx = 1;
        cRules.gridy = 0;
        cRules.anchor = GridBagConstraints.LINE_END;
        cRules.insets = paddingInsets;
        rulesPanel.add(injectLabel, cRules);

        // Unloaded Rules TextArea
        unloadedRulesText = new ThermostatTextArea(EMPTY_STR);
        unloadedRulesText.setName(RULES_UNLOADED_TEXT_NAME);
        unloadedRulesText.setMargin(paddingInsets);
        unloadedRulesText.setBackground(Color.WHITE);
        unloadedRulesText.setCursor(new Cursor(Cursor.TEXT_CURSOR));
        unloadedRulesText.setBorder(new LineBorder(Color.BLACK));
        unloadedRulesText.setText(t.localize(LocaleResources.NO_RULES_LOADED).getContents());
        JScrollPane unloadedPane = new ThermostatScrollPane(unloadedRulesText);
        unloadedPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);

        // Injected Rules TextArea
        injectedRulesText = new ThermostatTextArea(EMPTY_STR);
        injectedRulesText.setName(RULES_INJECTED_TEXT_NAME);
        injectedRulesText.setMargin(paddingInsets);
        injectedRulesText.setBackground(Color.WHITE);
        injectedRulesText.setCursor(new Cursor(Cursor.TEXT_CURSOR));
        injectedRulesText.setBorder(new LineBorder(Color.BLACK));
        injectedRulesText.setEditable(false);
        injectedRulesText.setText(t.localize(LocaleResources.NO_RULES_LOADED).getContents());
        JScrollPane injectedPane = new ThermostatScrollPane(injectedRulesText);
        injectedPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);

        // Inject & Unload Buttons
        injectRuleButton = new JButton(ARROW_RIGHT);
        injectRuleButton.addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                fireInjectAction(InjectAction.INJECT_RULE);
            }
        });
        unloadRuleButton = new JButton(ARROW_LEFT);
        unloadRuleButton.setEnabled(false);
        unloadRuleButton.addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                fireInjectAction(InjectAction.UNLOAD_RULE);
            }
        });

        // Split Pane and Divider
        JSplitPane splitPane = new JSplitPane();
        splitPane.setLeftComponent(unloadedPane);
        splitPane.setRightComponent(injectedPane);
        splitPane.setResizeWeight(halfWeight);
        BasicSplitPaneUI ui = new BasicSplitPaneUI() {
            @SuppressWarnings("serial")
            public BasicSplitPaneDivider createDefaultDivider() {
                return new BasicSplitPaneDivider(this) {
                    @Override
                    public void paint(Graphics g) {
                        g.setColor(rulesPanel.getBackground());
                        g.fillRect(0, 0, getSize().width, getSize().height);
                        super.paint(g);
                    }
                };
            }
        };
        splitPane.setUI(ui);
        BasicSplitPaneDivider divider = ui.getDivider();
        divider.setCursor(Cursor.getDefaultCursor());
        divider.setLayout(new GridBagLayout());
        divider.setDividerSize((int)(injectRuleButton.getPreferredSize().getWidth())*2);
        cRules = new GridBagConstraints();
        cRules.fill = GridBagConstraints.BOTH;
        cRules.gridx = 0;
        cRules.gridy = 1;
        divider.add(unloadRuleButton, cRules);
        cRules = new GridBagConstraints();
        cRules.fill = GridBagConstraints.BOTH;
        cRules.gridx = 1;
        cRules.gridy = 1;
        divider.add(injectRuleButton, cRules);
        cRules = new GridBagConstraints();
        cRules.weighty = halfWeight;
        cRules = new GridBagConstraints();
        cRules.fill = GridBagConstraints.BOTH;
        cRules.gridx = 0;
        cRules.gridwidth = 2;
        cRules.gridy = 1;
        cRules.weighty = yWeightRow0 + yWeightRow1;
        cRules.weightx = xWeightFullWidth;
        cRules.insets = paddingInsets;
        rulesPanel.add(splitPane, cRules);

        // Import & Generate Rule Buttons
        cRules = new GridBagConstraints();
        cRules.fill = GridBagConstraints.BOTH;
        cRules.gridx = 0;
        cRules.gridy = 2;
        cRules.gridwidth = 2;
        cRules.weighty = yWeightRow2;
        cRules.weightx = xWeightFullWidth;
        cRules.insets = paddingInsets;
        JPanel buttonHolder = new JPanel();
        FlowLayout layout = new FlowLayout();
        layout.setAlignment(FlowLayout.RIGHT);
        layout.setHgap(0);
        layout.setVgap(0);
        buttonHolder.setLayout(layout);
        buttonHolder.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        JButton generateRuleButton = new JButton(t.localize(LocaleResources.GENERATE_RULE_TEMPLATE).getContents());
        generateRuleButton.addActionListener(new java.awt.event.ActionListener() {
            
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                generateRuleToggle = true;
                fireGenerateEvent(GenerateAction.GENERATE_TEMPLATE);
            }
            
        });
        buttonHolder.add(generateRuleButton);
        JButton importRuleButton = new JButton(t.localize(LocaleResources.IMPORT_RULE).getContents());
        importRuleButton.addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                JFileChooser chooser = new JFileChooser();
                int result = chooser.showOpenDialog(rulesPanel);
                if (result == JFileChooser.APPROVE_OPTION) {
                    File file = chooser.getSelectedFile();
                    try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                        unloadedRulesText.setText(EMPTY_STR);
                        String line = reader.readLine();
                        while(line != null) {
                            unloadedRulesText.append(line + "\n");
                            line = reader.readLine();
                        }
                    } catch (FileNotFoundException fnfe) {
                        fnfe.printStackTrace();
                    } catch (IOException ioe) {
                        ioe.printStackTrace();
                    }
                }
            }
        });
        buttonHolder.add(importRuleButton);
        buttonHolder.setAlignmentX(Component.RIGHT_ALIGNMENT);
        rulesPanel.add(buttonHolder, cRules);
        
        // Metrics tab
        metricsPanel = new JPanel();
        metricsPanel.setLayout(new GridBagLayout());
        metricsText = new ThermostatTextArea(EMPTY_STR);
        metricsText.setName(METRICS_TEXT_NAME);
        metricsText.setBackground(Color.WHITE);
        metricsText.setEditable(false);
        metricsText.setMargin(paddingInsets);
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.BOTH;
        c.gridx = 0;
        c.gridy = 0;
        c.weighty = yWeightRow0 + yWeightRow1;
        c.weightx = xWeightFullWidth;
        c.insets = paddingInsets;
        JScrollPane metricsScroll = new ThermostatScrollPane(metricsText);
        metricsPanel.add(metricsScroll, c);
        // add a panel to control selection of metrics time interval
        updateGraphControlPanel(xWeightFullWidth, yWeightRow2);

        // graph tab
        Insets spacerLeftInsets = new Insets(0, 5, 0, 0);
        graphMainPanel = new JPanel();
        // add a panel to control display of the graph
        JPanel graphControlHolder =  new JPanel();

        // insert two labelled text fields to allow axis selection
        JLabel xlabel = new JLabel(t.localize(LocaleResources.X_COORD).getContents());
        JLabel ylabel = new JLabel(t.localize(LocaleResources.Y_COORD).getContents());
        final List<BytemanMetric> emptyBytemanMetrics = Collections.emptyList();
        String[] keyItems = MetricsKeysAggregator.aggregate(emptyBytemanMetrics).toArray(new String[0]);
        xCombo = new JComboBox<>(keyItems);
        xCombo.addActionListener(new java.awt.event.ActionListener() {
            
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                @SuppressWarnings("unchecked")
                JComboBox<String> combo = (JComboBox<String>)e.getSource();
                String candidate = (String)combo.getSelectedItem();
                xkey = KEYS_MAPPER.mapPredefinedKey(candidate, MapDirection.VIEW_TO_MODEL);
            }
        });
        yCombo = new JComboBox<>(keyItems);
        yCombo.addActionListener(new java.awt.event.ActionListener() {
            
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                @SuppressWarnings("unchecked")
                JComboBox<String> combo = (JComboBox<String>)e.getSource();
                String candidate = (String)combo.getSelectedItem();
                ykey = KEYS_MAPPER.mapPredefinedKey(candidate, MapDirection.VIEW_TO_MODEL);
            }
        });
        // insert button to initiate graph redraw
        JButton generateGraphButton = new JButton(t.localize(LocaleResources.GENERATE_GRAPH).getContents());
        generateGraphButton.addActionListener(new java.awt.event.ActionListener() {

            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                fireTabSelectedEvent(TabbedPaneAction.GRAPH_TAB_SELECTED);
            }

        });

        JLabel filterlabel = new JLabel(t.localize(LocaleResources.FILTER).getContents());
        JLabel valuelabel = new JLabel(t.localize(LocaleResources.FILTER_VALUE_LABEL).getContents());
        String[] filterKeyVals = buildFilterKeyVals(keyItems);
        filterCombo = new JComboBox<>(filterKeyVals);
        filterCombo.addActionListener(new java.awt.event.ActionListener() {
            
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                @SuppressWarnings("unchecked")
                JComboBox<String> combo = (JComboBox<String>)e.getSource();
                String candidate = (String)combo.getSelectedItem();
                filter = mapFilter(candidate, MapDirection.VIEW_TO_MODEL);
                updateFilterText(filter);
            }
        });
        filterText = new JTextField(30);
        filterText.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                value = filterText.getText();
            }
            @Override
            public void removeUpdate(DocumentEvent e) {
                value = filterText.getText();
            }
            @Override
            public void changedUpdate(DocumentEvent e) {
                value = filterText.getText();
            }
        });

        graphControlHolder.setLayout(new GridBagLayout());
        
        double weightShortLabel = 0.05;
        double weightTextBox = 0.3;
        GridBagConstraints graphConstraints = new GridBagConstraints();
        graphConstraints.fill = GridBagConstraints.BOTH;
        graphConstraints.gridx = 0;
        graphConstraints.gridy = 0;
        graphConstraints.weighty = 0.5;
        graphConstraints.weightx = weightShortLabel;
        graphConstraints.insets = spacerLeftInsets;
        graphControlHolder.add(xlabel, graphConstraints);
        graphConstraints.fill = GridBagConstraints.HORIZONTAL;
        graphConstraints.gridx = 1;
        graphConstraints.gridy = 0;
        graphConstraints.weighty = 0.5;
        graphConstraints.weightx = weightTextBox;
        graphConstraints.insets = spacerLeftInsets;
        graphControlHolder.add(xCombo, graphConstraints);
        graphConstraints.fill = GridBagConstraints.HORIZONTAL;
        graphConstraints.gridx = 2;
        graphConstraints.gridy = 0;
        graphConstraints.weighty = 0.5;
        graphConstraints.weightx = weightShortLabel;
        graphConstraints.insets = spacerLeftInsets;
        graphControlHolder.add(ylabel, graphConstraints);
        graphConstraints.fill = GridBagConstraints.HORIZONTAL;
        graphConstraints.gridx = 3;
        graphConstraints.gridy = 0;
        graphConstraints.weighty = 0.5;
        graphConstraints.weightx = weightTextBox;
        graphConstraints.insets = spacerLeftInsets;
        graphControlHolder.add(yCombo, graphConstraints);
        graphConstraints.fill = GridBagConstraints.HORIZONTAL;
        graphConstraints.gridx = 0;
        graphConstraints.gridy = 1;
        graphConstraints.weighty = 0.5;
        graphConstraints.weightx = weightShortLabel;
        graphConstraints.insets = spacerLeftInsets;
        graphControlHolder.add(filterlabel, graphConstraints);
        graphConstraints.fill = GridBagConstraints.HORIZONTAL;
        graphConstraints.gridx = 1;
        graphConstraints.gridy = 1;
        graphConstraints.weighty = 0.5;
        graphConstraints.weightx = weightTextBox;
        graphConstraints.insets = spacerLeftInsets;
        graphControlHolder.add(filterCombo, graphConstraints);
        graphConstraints.fill = GridBagConstraints.HORIZONTAL;
        graphConstraints.gridx = 2;
        graphConstraints.gridy = 1;
        graphConstraints.weighty = 0.5;
        graphConstraints.weightx = weightShortLabel;
        graphConstraints.insets = spacerLeftInsets;
        graphControlHolder.add(valuelabel, graphConstraints);
        graphConstraints.fill = GridBagConstraints.HORIZONTAL;
        graphConstraints.gridx = 3;
        graphConstraints.gridy = 1;
        graphConstraints.weighty = 0.5;
        graphConstraints.weightx = weightTextBox;
        graphConstraints.insets = spacerLeftInsets;
        graphControlHolder.add(filterText, graphConstraints);
        graphConstraints.fill = GridBagConstraints.HORIZONTAL;
        graphConstraints.gridx = 4;
        graphConstraints.gridy = 1;
        graphConstraints.weighty = 0.5;
        graphConstraints.weightx = weightTextBox;
        graphConstraints.insets = spacerLeftInsets;
        graphControlHolder.add(generateGraphButton, graphConstraints);

        // add controls and empty graph to main panel but don't add graph panel yet
        graphMainPanel.setLayout(new GridBagLayout());
        c.fill = GridBagConstraints.BOTH;
        c.gridx = 0;
        c.gridy = 0;
        c.weighty = yWeightRow0;
        c.weightx = xWeightFullWidth;
        graphMainPanel.add(graphControlHolder, c);

        CategoryDataset categoryDataset = new DefaultCategoryDataset();
        JFreeChart graph = ChartFactory.createBarChart(BYTEMAN_CHART_LABEL, EMPTY_STR, EMPTY_STR,
                                            categoryDataset, PlotOrientation.VERTICAL,
                                            true, true, false);
        double weighty = yWeightRow1 + yWeightRow2;
        updateGraphPanel(weighty, xWeightFullWidth, graph);

        tabbedPane = new ThermostatTabbedPane();
        tabbedPane.addTab(t.localize(LocaleResources.TAB_RULES).getContents(), rulesPanel);
        tabbedPane.addTab(t.localize(LocaleResources.TAB_METRICS).getContents(), metricsPanel);
        tabbedPane.addTab(t.localize(LocaleResources.TAB_GRAPH).getContents(), graphMainPanel);
        tabbedPane.addChangeListener(new ChangeListener() {

            @Override
            public void stateChanged(ChangeEvent e) {
                JTabbedPane pane = (JTabbedPane)e.getSource();
                JPanel selectedPanel = (JPanel)pane.getSelectedComponent();
                if (selectedPanel == rulesPanel) {
                    fireTabSelectedEvent(TabbedPaneAction.RULES_TAB_SELECTED);
                } else if (selectedPanel == metricsPanel) {
                    fireTabSelectedEvent(TabbedPaneAction.METRICS_TAB_SELECTED);
                } else if (selectedPanel == graphMainPanel) {
                    fireTabSelectedEvent(TabbedPaneAction.GRAPH_TAB_SELECTED);
                } else {
                    throw new AssertionError("Unkown tab in tabbed pane: " + selectedPanel);
                }
            }
        });
        // Always have rules tab selected first
        tabbedPane.setSelectedComponent(rulesPanel);
        
        mainContainer.setContent(tabbedPane);
        mainContainer.addToolBarButton(toggleButton);
    }

    /*
     * When there is no key selected to filter by, don't enable the value
     * input text box as this doesn't make sense.
     */
    private void updateFilterText(String filterValue) {
        if (filterValue != null && !filterValue.isEmpty()) {
            filterText.setEnabled(true);
        } else {
            filterText.setEnabled(false);
        }
    }

    private String[] buildFilterKeyVals(String[] keyItems) {
        List<String> filterKeys = new ArrayList<>(keyItems.length + 1);
        filterKeys.add(PredefinedKeysMapper.NO_FILTER_ITEM); // default to no filter
        filterKeys.addAll(Arrays.asList(keyItems));
        return filterKeys.toArray(new String[] {});
    }

    private void updateGraphControlPanel(double weightx, double weighty) {
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.BOTH;
        c.gridx = 0;
        c.gridy = 1;
        c.weighty = weighty;
        c.weightx = weightx;
        
        graphTimeControlPanel = new RecentTimeControlPanel(duration, RecentTimeControlPanel.UnitRange.MEDIUM);
        graphTimeControlPanel.addPropertyChangeListener(RecentTimeControlPanel.PROPERTY_VISIBLE_TIME_RANGE, new PropertyChangeListener() {
            @Override
            public void propertyChange(final PropertyChangeEvent evt) {
                duration = (Duration) evt.getNewValue();
                fireTabSelectedEvent(TabbedPaneAction.METRICS_TAB_SELECTED);
            }
        });
        
        metricsPanel.add(graphTimeControlPanel, c);
        metricsPanel.revalidate();
    }

    private void updateGraphPanel(final double weighty,
                                  final double weightx,
                                  JFreeChart graph) {
        ThermostatChartPanelBuilder chartBuilder = new ThermostatChartPanelBuilder();
        graphPanel = chartBuilder
            .duration(duration)
            .chart(graph)
            .xyPlotFixedAutoRange(false)
            .unitRange(UnitRange.MEDIUM)
            .build();
        graphPanel.addPropertyChangeListener(RecentTimeControlPanel.PROPERTY_VISIBLE_TIME_RANGE, new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                duration = (Duration) evt.getNewValue();
                fireGenerateEvent(GenerateAction.GENERATE_GRAPH);
            }
        });
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.BOTH;
        c.gridx = 0;
        c.gridy = 1;
        c.weighty = weighty;
        c.weightx = weightx;
        graphMainPanel.add(graphPanel, c);
        graphMainPanel.revalidate();
    }
    
    private void fireGenerateEvent(final GenerateAction action) {
        fireAction(action, generateListeners);
    }

    private void fireTabSelectedEvent(final TabbedPaneAction action) {
        fireAction(action, tabbedPaneListeners);
    }

    private void fireInjectAction(final InjectAction action) {
        fireAction(action, injectListeners);
    }
    
    private <T extends Enum<?>> void fireAction(final T action, final List<ActionListener<T>> listeners) {
        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                try {
                    ActionEvent<T> event = new ActionEvent<>(this, action);
                    for (ActionListener<T> listener : listeners) {
                        listener.actionPerformed(event);
                    }
                } catch (Throwable t) {
                    logger.log(Level.INFO, t.getMessage(), t);
                }
                return null;
            }
        }.execute();
    }
    
    @Override
    public Component getUiComponent() {
        return mainContainer;
    }

    @Override
    public void addRuleChangeListener(ActionListener<InjectAction> listener) {
        injectListeners.add(listener);
    }

    @Override
    public void addTabbedPaneChangeListener(ActionListener<TabbedPaneAction> listener) {
        tabbedPaneListeners.add(listener);
    }

    @Override
    public void setInjectState(final BytemanInjectState state) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                final String buttonLabel;
                if (!viewControlsEnabled) {
                    buttonLabel = t.localize(LocaleResources.INJECT_RULE).getContents();
                } else if (state == BytemanInjectState.UNLOADING || state == BytemanInjectState.INJECTED) {
                    buttonLabel = t.localize(LocaleResources.UNLOAD_RULE).getContents();
                } else {
                    buttonLabel = t.localize(LocaleResources.INJECT_RULE).getContents();
                }

                if (!viewControlsEnabled) {
                    toggleButton.setToggleActionState(BytemanInjectState.DISABLED);
                } else {
                    toggleButton.setToggleActionState(state);
                }
                toggleButton.setText(buttonLabel);

                if (state == BytemanInjectState.INJECTED) {
                    injectedRulesText.setText(unloadedRulesText.getText());
                    injectRuleButton.setEnabled(false);
                    unloadRuleButton.setEnabled(true);
                } else if (state == BytemanInjectState.UNLOADING) {
                    if (EMPTY_STR.equals(unloadedRulesText.getText().trim())) {
                        unloadedRulesText.setText(injectedRulesText.getText());
                    }
                } else if (state == BytemanInjectState.UNLOADED){
                    injectRuleButton.setEnabled(true);
                    unloadRuleButton.setEnabled(false);
                }
            }
        });
        
    }

    @Override
    public void setViewControlsEnabled(boolean newState) {
        this.viewControlsEnabled = newState;
        if (!viewControlsEnabled) {
            setInjectState(BytemanInjectState.DISABLED);
        }
    }

    @Override
    public void contentChanged(ActionEvent<TabbedPaneContentAction> event) {
        TabbedPaneContentAction action = event.getActionId();
        switch(action) {
        case METRICS_CHANGED:
            @SuppressWarnings("unchecked")
            List<BytemanMetric> metrics = (List<BytemanMetric>)event.getPayload();
            updateViewWithMetrics(metrics);
            break;
        case RULES_CHANGED:
            String rule = (String)event.getPayload();
            updateRuleInView(rule);
            break;
        case GRAPH_CHANGED:
            @SuppressWarnings("unchecked")
            List<BytemanMetric> graphMetrics = (List<BytemanMetric>)event.getPayload();
            updateGraphInView(graphMetrics, xkey, ykey, filter, value, graphtype);
            updateMetricsRangeInView();
            break;
        default:
            throw new AssertionError("Unknown event: " + action);
        }
        
    }

    // time range might have changed in graph view. update metrics
    // accordingly
    private void updateMetricsRangeInView() {
        SwingUtilities.invokeLater(new Runnable() {

            @Override
            public void run() {
                double weightx = 1.0;
                double weighty = 0.05;
                if (graphTimeControlPanel != null) {
                    graphTimeControlPanel.setVisible(false);
                    metricsPanel.remove(graphMainPanel);
                }
                updateGraphControlPanel(weightx, weighty);
            }
            
        });
    }

    private void updateRuleInView(final String rule) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                if (generateRuleToggle) {
                    unloadedRulesText.setText(rule);
                    generateRuleToggle = false;
                } else {
                    injectedRulesText.setText(rule);
                }
            }
        });
    }

    // package private for testing
    static DateFormat metricsDateFormat = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.LONG);

    private void updateViewWithMetrics(List<BytemanMetric> metrics) {
        final StringBuffer buffer = new StringBuffer();
        for (BytemanMetric m: metrics) {
            String marker = m.getMarker();
            long timestamp = m.getTimeStamp();
            String timestring = metricsDateFormat.format(new Date(timestamp));
            buffer.append(timestring).append(": ").append(marker).append(" ").append(m.getDataAsJson()).append("\n");
        }
        if (buffer.length() == 0) {
            buffer.append(NO_METRICS_AVAILABLE).append("\n");
        }
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                metricsText.setText(buffer.toString());
            }
        });
    }

    // Package private for testing
    String getInjectedRuleContent() throws InvocationTargetException, InterruptedException {
        injectedRuleContent = "";
        SwingUtilities.invokeAndWait(new Runnable() {
            @Override
            public void run() {
                injectedRuleContent = injectedRulesText.getText();
            }
        });
        return injectedRuleContent;
    }

    // Package private for testing
    void setUnloadedRuleContent(final String rule) throws InvocationTargetException, InterruptedException {
        SwingUtilities.invokeAndWait(new Runnable() {
            @Override
            public void run() {
                unloadedRulesText.setText(rule);
            }
        });
    }

    // Package private for testing
    String getUnloadedRuleContent() throws InvocationTargetException, InterruptedException {
        unloadedRuleContent = "";
        SwingUtilities.invokeAndWait(new Runnable() {
            @Override
            public void run() {
                unloadedRuleContent = unloadedRulesText.getText();
            }
        });
        return unloadedRuleContent;
    }

    // Package private for testing
    boolean isInjectButtonEnabled() {
        return injectRuleButton.isEnabled();
    }

    // Package private for testing
    boolean isUnloadButtonEnabled() {
        return unloadRuleButton.isEnabled();
    }

    // Package private for testing
    void enableGenerateRuleToggle() {
        generateRuleToggle = true;
    }

    private void updateGraphInView(List<BytemanMetric> metrics, String xkey, String ykey, String filter, String value, String graphtype) {
        final List<BytemanMetric> ms = metrics;
        final String xk = xkey;
        final String yk = ykey;
        final Filter dataFilter;
        if (filter != null && value != null) {
            dataFilter = new Filter(filter, value);
        } else {
            dataFilter = null;
        }
        final String t = graphtype;
        final String[] keyItems = MetricsKeysAggregator.aggregate(metrics).toArray(new String[] {});
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                String f = dataFilter == null ? null : dataFilter.getFilterKey();
                updateComboKeyItems(keyItems, xk, yk, f);
                GraphDataset dataset = makeGraphDataset(ms, xk, yk, dataFilter);
                if (dataset != null) {
                    switchGraph(dataset, t);
                }
            }

        });
    }
    
    private void updateComboKeyItems(String[] keyItems, String xkey, String ykey, String filter) {
        xCombo.removeAllItems();
        yCombo.removeAllItems();
        filterCombo.removeAllItems();
        filterCombo.addItem(PredefinedKeysMapper.NO_FILTER_ITEM); // allow for no filter
        for (String key: keyItems) {
            xCombo.addItem(key);
            yCombo.addItem(key);
            filterCombo.addItem(key);
        }
        String xSelection = KEYS_MAPPER.mapPredefinedKey(xkey, MapDirection.MODEL_TO_VIEW);
        selectItem(xCombo, xSelection);
        String ySelection = KEYS_MAPPER.mapPredefinedKey(ykey, MapDirection.MODEL_TO_VIEW);
        selectItem(yCombo, ySelection);
        String filterSelection = mapFilter(filter, MapDirection.MODEL_TO_VIEW);
        selectItem(filterCombo, filterSelection);
    }

    private String mapFilter(String filter, MapDirection direction) {
        String mapped = KEYS_MAPPER.mapPredefinedKey(filter, direction);
        if (!Objects.equals(filter, mapped)) {
            return mapped;
        }
        return KEYS_MAPPER.mapNoFilter(filter, direction);
    }

    private void selectItem(final JComboBox<String> combo, String selection) {
        if (selection != null) {
            combo.setSelectedItem(selection);
        }
    }

    private GraphDataset makeGraphDataset(List<BytemanMetric> metrics, String xkey, String ykey, Filter filter) {
        GraphDataset dataset = new GraphDataset(metrics, xkey, ykey, filter);
        if (dataset.size() == 0) {
            return null;
        }
        return dataset;
    }

    private void switchGraph(GraphDataset dataset, String graphtype)
    {
        String xlabel = dataset.getXLabel();
        String ylabel = dataset.getYLabel();
        CoordinateType xtype = dataset.getXType();
        CoordinateType ytype = dataset.getYType();
        JFreeChart graph = null;
        switch (xtype) {
            case CATEGORY:
                if (ytype == CoordinateType.CATEGORY) {
                    // use a bar chart with multiple bars per category 1 value
                    // where each bar counts the frequency for the second category
                    CategoryDataset categoryDataset = dataset.getCategoryDataset();
                    graph = ChartFactory.createBarChart(BYTEMAN_CHART_LABEL, xlabel, ylabel,
                                                        categoryDataset, PlotOrientation.VERTICAL,
                                                        true, true, false);
                } else {
                    // draw as a bar chart with one bar per category where
                    // each bar sums the associated second coordinate values
                    CategoryDataset categoryDataset = dataset.getCategoryDataset();
                    graph = ChartFactory.createBarChart(BYTEMAN_CHART_LABEL, xlabel, ylabel,
                                                        categoryDataset, PlotOrientation.VERTICAL,
                                                        true, true, false);
                    /*
                     * we can also draw this as a pie chart
                    PieDataset pieDataset = dataset.getPieDataset();
                    graph = ChartFactory.createPieChart("Byteman Metrics " + xlabel + " by " + ylabel,
                                                        pieDataset, true, true, false);
                    */
                }
                break;
            case TIME:
                if (ytype == CoordinateType.CATEGORY) {
                    // we need to draw a graph of category (state) value against time
                    // with step transitions between states
                    //
                    // we create a numeric time series plot with a mapping
                    // from numeric values to symbolic keys
                    CategoryTimePlotData categoryData = dataset.getCategoryTimePlot(); 
                    XYDataset xydataset = categoryData.getXYDataSet();
                    graph = ChartFactory.createXYStepChart(BYTEMAN_CHART_LABEL, xlabel, ylabel,
                                                           xydataset, PlotOrientation.VERTICAL,
                                                           true, true, false);
                    // now we change the range axis of the xyplot to draw symbols in
                    // place of numeric values
                    graph.getXYPlot().setRangeAxis(0, new SymbolAxis(ykey, categoryData.getSymbols()));
                } else {
                    // draw a graph of numeric value against time
                    XYDataset xydataset = dataset.getXYDataset();
                    graph = ChartFactory.createTimeSeriesChart(BYTEMAN_CHART_LABEL, xlabel, ylabel,
                                                               xydataset, true, true, false);
                }
                break;
            case INTEGRAL:
            case REAL:
                if (ytype == CoordinateType.CATEGORY) {
                    // we could treat the numeric values as category values (or ranges?)
                    // and draw this as a bar chart
                    CategoryDataset categoryDataset = dataset.getCategoryDataset();
                    graph = ChartFactory.createBarChart(BYTEMAN_CHART_LABEL, xlabel, ylabel,
                                                        categoryDataset, PlotOrientation.VERTICAL,
                                                        true, true, false);
                    // for now draw an empty graph
                } else if (ytype == CoordinateType.TIME) {
                    // we could group the time values as time ranges
                    // and draw this as a bar chart
                    //
                    // for now draw an empty graph

                    XYDataset xydataset = dataset.getXYDataset();
                    graph = ChartFactory.createXYLineChart("empty", xlabel, ylabel,
                                                           xydataset, PlotOrientation.VERTICAL,
                                                           true, true, false);
                } else {
                    // draw an xy line plot of numeric value against numeric value
                    XYDataset xydataset = dataset.getXYDataset();
                    graph = ChartFactory.createXYLineChart(BYTEMAN_CHART_LABEL, xlabel, ylabel,
                                                           xydataset, PlotOrientation.VERTICAL,
                                                           true, true, false);
                }
                break;
        }
        if (graph == null) {
            throw new AssertionError("Graph must not be null");
        }
        if (graphPanel != null) {
            graphMainPanel.remove(graphPanel);
        }
        double weighty = 0.95;
        double weightx = 1.0;
        updateGraphPanel(weighty, weightx, graph);
    }

    @Override
    public void addGenerateActionListener(ActionListener<GenerateAction> listener) {
        generateListeners.add(listener);
    }

    @Override
    public String getRuleContent() {
        final String[] content = new String[1];
        try {
            SwingUtilities.invokeAndWait(new Runnable() {

                @Override
                public void run() {
                    content[0] = unloadedRulesText.getText();
                }
                
            });
        } catch (Exception e) {
            logger.log(Level.INFO, e.getMessage(), e);
        }
        return content[0];
    }

    @Override
    public void handleError(final LocalizedString message) {
        SwingUtilities.invokeLater(new Runnable() {

            @Override
            public void run() {
                JOptionPane.showMessageDialog(getUiComponent().getParent(), message.getContents(), EMPTY_STR, JOptionPane.WARNING_MESSAGE);
            }

        });
    }
    
    @Override
    public long getDurationMillisecs() {
        return duration.asMilliseconds();
    }
}
