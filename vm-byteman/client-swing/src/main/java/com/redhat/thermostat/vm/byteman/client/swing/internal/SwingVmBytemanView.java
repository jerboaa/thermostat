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
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.JSplitPane;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.border.LineBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.plaf.basic.BasicSplitPaneDivider;
import javax.swing.plaf.basic.BasicSplitPaneUI;

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
import com.redhat.thermostat.client.swing.experimental.ComponentVisibilityNotifier;
import com.redhat.thermostat.common.ActionEvent;
import com.redhat.thermostat.common.ActionListener;
import com.redhat.thermostat.common.Duration;
import com.redhat.thermostat.common.Pair;
import com.redhat.thermostat.common.utils.LoggingUtils;
import com.redhat.thermostat.shared.locale.LocalizedString;
import com.redhat.thermostat.shared.locale.Translate;
import com.redhat.thermostat.vm.byteman.common.BytemanMetric;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.SymbolAxis;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.DefaultKeyedValues;
import org.jfree.data.category.CategoryDataset;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.data.general.DefaultPieDataset;
import org.jfree.data.general.PieDataset;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

public class SwingVmBytemanView extends VmBytemanView implements SwingComponent {

    private static final Logger logger = LoggingUtils.getLogger(SwingVmBytemanView.class);
    private static final Translate<LocaleResources> t = LocaleResources.createLocalizer();
    private static final Icon START_ICON = IconResource.SAMPLE.getIcon();
    private static final Icon STOP_ICON = new FontAwesomeIcon('\uf28e', START_ICON.getIconHeight());
    private static final Icon ARROW_LEFT = IconResource.ARROW_LEFT.getIcon();
    private static final Icon ARROW_RIGHT = IconResource.ARROW_RIGHT.getIcon();
    private static final String EMPTY_STR = "";
    
    static final String NO_METRICS_AVAILABLE = t.localize(LocaleResources.NO_METRICS_AVAILABLE).getContents();
    
    // Names for buttons used in testing
    static final String TOGGLE_BUTTON_NAME = "TOGGLE_RULE_BUTTON";
    static final String RULES_INJECTED_TEXT_NAME = "RULES_INJECTED_TEXT";
    static final String RULES_UNLOADED_TEXT_NAME = "RULES_UNLOADED_TEXT";
    static final String METRICS_TEXT_NAME = "METRICS_TEXT";
    
    private String injectedRuleContent;
    private String unloadedRuleContent;
    private boolean generateRuleToggle;
    private final JTextArea metricsText;
    private final JTextArea unloadedRulesText;
    private final JTextArea injectedRulesText;
    private final JButton injectRuleButton;
    private final JButton unloadRuleButton;
    private JPanel graphMainPanel;
    private ChartPanel graphPanel;
    private JFreeChart graph;
    private final JTabbedPane tabbedPane;
    private final HeaderPanel mainContainer;
    private final ActionToggleButton toggleButton;
    private final CopyOnWriteArrayList<ActionListener<InjectAction>> injectListeners = new CopyOnWriteArrayList<>();
    private final CopyOnWriteArrayList<ActionListener<TabbedPaneAction>> tabbedPaneListeners = new CopyOnWriteArrayList<>();
    private final CopyOnWriteArrayList<ActionListener<GenerateAction>> generateListeners = new CopyOnWriteArrayList<>();
    
    // graph configurtaion choices from user
    // ideally these ought to be stored as
    // fields of a separate model instance

    String xkey = null;
    String ykey = null;
    String filter = null;
    String value = null;
    String graphtype = null;

    // duration over which to search for metrics

    Duration duration = new Duration(5, TimeUnit.MINUTES);

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
        final JPanel metricsPanel = new JPanel();
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
        c.fill = GridBagConstraints.BOTH;
        c.gridx = 0;
        c.gridy = 1;
        c.weighty = yWeightRow2;
        c.weightx = xWeightFullWidth;
        // add a panel to control selection of metrics time interval
        RecentTimeControlPanel graphTimeControlPanel = new RecentTimeControlPanel(duration, RecentTimeControlPanel.UnitRange.MEDIUM);
        graphTimeControlPanel.addPropertyChangeListener(RecentTimeControlPanel.PROPERTY_VISIBLE_TIME_RANGE, new PropertyChangeListener() {
            @Override
            public void propertyChange(final PropertyChangeEvent evt) {
                duration = (Duration) evt.getNewValue();
                fireTabSelectedEvent(TabbedPaneAction.METRICS_TAB_SELECTED);
            }
        });

        graphTimeControlPanel.setPreferredSize(buttonHolder.getPreferredSize());
        metricsPanel.add(graphTimeControlPanel, c);

        // graph tab
        graphMainPanel = new JPanel();
        // add a panel to control display of the graph
        JPanel graphControlHolder =  new JPanel();
        JPanel subHolder1 = new JPanel();
        JPanel subHolder2 = new JPanel();
        layout = new FlowLayout();
        layout.setAlignment(FlowLayout.RIGHT);
        layout.setHgap(5);
        layout.setVgap(0);
        subHolder1.setLayout(layout);
        subHolder1.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);

        // insert two labelled text fields to allow axis selection
        JLabel xlabel = new JLabel("x:");
        JLabel ylabel = new JLabel("y:");
        final JTextField xtext = new JTextField(30);
        final JTextField ytext = new JTextField(30);

        xtext.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {

            @Override
            public void insertUpdate(DocumentEvent e)
            {
                xkey = xtext.getText();
            }
            @Override
            public void removeUpdate(DocumentEvent e)
            {
                xkey = xtext.getText();
            }
            @Override
            public void changedUpdate(DocumentEvent e)
            {
                xkey = xtext.getText();
            }
        });

        ytext.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {

            @Override
            public void insertUpdate(DocumentEvent e) {
                ykey = ytext.getText();
            }
            @Override
            public void removeUpdate(DocumentEvent e) {
                ykey = ytext.getText();
            }
            @Override
            public void changedUpdate(DocumentEvent e) {
                ykey = ytext.getText();
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
        subHolder1.add(generateGraphButton);
        subHolder1.add(ytext);
        subHolder1.add(ylabel);
        subHolder1.add(xtext);
        subHolder1.add(xlabel);
        subHolder1.setAlignmentX(Component.RIGHT_ALIGNMENT);

        JLabel filterlabel = new JLabel("filter:");
        JLabel valuelabel = new JLabel("==");
        final JTextField filterText = new JTextField(30);
        final JTextField valueText = new JTextField(30);

        filterText.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {

            @Override
            public void insertUpdate(DocumentEvent e) {
                filter = filterText.getText();
            }
            @Override
            public void removeUpdate(DocumentEvent e) {
                filter = filterText.getText();
            }
            @Override
            public void changedUpdate(DocumentEvent e)
            {
                filter = filterText.getText();
            }
        });

        valueText.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {

            @Override
            public void insertUpdate(DocumentEvent e) {
                value = valueText.getText();
            }
            @Override
            public void removeUpdate(DocumentEvent e) {
                value = valueText.getText();
            }
            @Override
            public void changedUpdate(DocumentEvent e) {
                value = valueText.getText();
            }
        });

        layout = new FlowLayout();
        layout.setAlignment(FlowLayout.RIGHT);
        layout.setHgap(5);
        layout.setVgap(0);
        subHolder2.setLayout(layout);
        subHolder2.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);

        subHolder2.add(valueText);
        subHolder2.add(valuelabel);
        subHolder2.add(filterText);
        subHolder2.add(filterlabel);
        subHolder2.setAlignmentX(Component.RIGHT_ALIGNMENT);

        graphControlHolder.setLayout(new GridBagLayout());
        c.fill = GridBagConstraints.BOTH;
        c.gridx = 0;
        c.gridy = 0;
        c.weighty = 0.5;
        c.weightx = xWeightFullWidth;
        graphControlHolder.add(subHolder1, c);

        c.gridx = 0;
        c.gridy = 1;
        c.weighty = 0.5;
        c.weightx = xWeightFullWidth;
        graphControlHolder.add(subHolder2, c);

        // add controls and empty graph to main panel but don't add graph panel yet
        graphMainPanel.setLayout(new GridBagLayout());
        c.fill = GridBagConstraints.BOTH;
        c.gridx = 0;
        c.gridy = 0;
        c.weighty = yWeightRow0;
        c.weightx = xWeightFullWidth;
        graphMainPanel.add(graphControlHolder, c);

        CategoryDataset categoryDataset = new DefaultCategoryDataset();
        graph = ChartFactory.createBarChart("empty", "", "",
                                            categoryDataset, PlotOrientation.VERTICAL,
                                            true, true, false);
        graphPanel = new ChartPanel(graph, true);
        c.fill = GridBagConstraints.BOTH;
        c.gridx = 0;
        c.gridy = 1;
        c.weighty = yWeightRow1 + yWeightRow2;
        c.weightx = xWeightFullWidth;
        graphMainPanel.add(graphPanel, c);

        graphMainPanel.revalidate();

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
            break;
        default:
            throw new AssertionError("Unknown event: " + action);
        }
        
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
        final String f = filter;
        final String v = value;
        final String t = graphtype;
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                System.out.println("updateGraphInView:");
                GraphDataset dataset = makeGraphDataset(ms, xk, yk, f, v);
                if (dataset != null) {
                    switchGraph(dataset, t);
                }
            }
        });
    }

    private GraphDataset makeGraphDataset(List<BytemanMetric> metrics, String xkey, String ykey, String filter, String value) {
        GraphDataset dataset = new GraphDataset(metrics, xkey, ykey, filter, value);
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
        switch (xtype) {
            case CATEGORY:
                if(ytype == CoordinateType.CATEGORY) {
                    // use a bar chart with multiple bars per category 1 value
                    // where each bar counts the frequency for the second category
                    CategoryDataset categoryDataset = dataset.getCategoryDataset();
                    graph = ChartFactory.createBarChart("Byteman Metrics", xlabel, ylabel,
                                                        categoryDataset, PlotOrientation.VERTICAL,
                                                        true, true, false);
                } else {
                    // draw as a bar chart with one bar per category where
                    // each bar sums the associated second coordinate values
                    CategoryDataset categoryDataset = dataset.getCategoryDataset();
                    graph = ChartFactory.createBarChart("Byteman Metrics", xlabel, ylabel,
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
                if(ytype == CoordinateType.CATEGORY) {
                    // we need to draw a graph of category (state) value against time
                    // with step transitions between states
                    //
                    // we create a numeric time series plot with a mapping
                    // from numeric values to symbolic keys
                    String[][] symbolsReturn = new String[1][];
                    XYDataset xydataset = dataset.getCategoryTimePlot(symbolsReturn);
                    graph = ChartFactory.createXYStepChart("Byteman Metrics", xlabel, ylabel,
                                                           xydataset, PlotOrientation.VERTICAL,
                                                           true, true, false);
                    // now we change the range axis of the xyplot to draw symbols in
                    // place of numeric values
                    graph.getXYPlot().setRangeAxis(0, new SymbolAxis(ykey, symbolsReturn[0]));
                } else {
                    // draw a graph of numeric value against time
                    XYDataset xydataset = dataset.getXYDataset();
                    graph = ChartFactory.createTimeSeriesChart("Byteman Metrics", xlabel, ylabel,
                                                               xydataset, true, true, false);
                }
                break;
            case INTEGRAL:
            case REAL:
                if(ytype == CoordinateType.CATEGORY) {
                    // we could treat the numeric values as category values (or ranges?)
                    // and draw this as a bar chart
                    CategoryDataset categoryDataset = dataset.getCategoryDataset();
                    graph = ChartFactory.createBarChart("Byteman Metrics", xlabel, ylabel,
                                                        categoryDataset, PlotOrientation.VERTICAL,
                                                        true, true, false);
                    // for now draw an empty graph
                } else if(ytype == CoordinateType.TIME) {
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
                    graph = ChartFactory.createXYLineChart("Byteman Metrics", xlabel, ylabel,
                                                           xydataset, PlotOrientation.VERTICAL,
                                                           true, true, false);
                }
                break;
        }
        if(graphPanel != null) {
            graphPanel.setVisible(false);
            graphMainPanel.remove(graphPanel);
        }
        graphPanel = new ChartPanel(graph, true);
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.BOTH;
        c.gridx = 0;
        c.gridy = 1;
        c.weighty = 0.90;
        c.weightx = 1.0;
        graphMainPanel.add(graphPanel, c);
        graphPanel.setVisible(true);
        graphMainPanel.revalidate();
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
    public long getDurationMillisecs()
    {
        return duration.asMilliseconds();
    }

    public enum CoordinateType {
        INTEGRAL,
        REAL,
        TIME,
        CATEGORY
    };

    /**
     * a special coordinate name used to identify the timestamp associated
     * with any given Byteman metric. if it is entered as the x or y coordinate
     * name in the graph dialogue then it will select the tiemstamp as the value
     * to be graphed against tehthe other chosen coordinate. timestamp values
     * are stored as longs but are displayed as time values.
     *
     * n.b. this text string really needs to be localised.
     *
     * n.b.b. it really only makes sense to use timestamp as the X axis. maybe
     * we should reject any attempt to use it for the y axis?
     */
    final public static String TIMESTAMP_KEY = "timestamp";

    /**
     * a special coordinate name used to identify the frequency count
     * of any given Byteman metric. if it is entered as the x or y coordinate
     * name in the graph dialogue then it will count 1 for each occurence of
     * other value. frequency values are stored as longs.
     *
     * n.b. this text string really needs to be localised.
     */
    final public static String FREQUENCY_KEY = "frequency";

    /**
     * a special coordinate name used to identify the marker string
     * of any given Byteman metric. if it is entered as the x or y coordinate
     * name in the graph dialogue then it will select the marker as the value
     * to be graphed against the other chosen coordinate.
     *
     * n.b. this text string really needs to be localised.
     */
    final public static String MARKER_KEY = "marker";

    public static class GraphDataset
    {
        private List<Pair<Object, Object>> data;
        String xkey;
        String ykey;
        CoordinateType xtype;
        CoordinateType ytype;
        private static CategoryDataset emptyCategoryDataset = new DefaultCategoryDataset();
        private static PieDataset emptyPieDataset = new DefaultPieDataset();
        private static XYDataset emptyXYDataset = new XYSeriesCollection();
        private static Number frequencyUnit = Long.valueOf(1);

        private Object maybeNumeric(String value) {
            if (value == null || value.length() == 0)  {
                return null;
            }
            try {
                if(value.contains(".")) {
                    return Double.valueOf(value);
                } else {
                    return Long.valueOf(value);
                }
            } catch (NumberFormatException nfe) {
                return value;
            }
        }

        public GraphDataset(List<BytemanMetric> metrics, String xkey, String ykey, String filter, String value)
        {
            this.xkey = xkey;
            this.ykey = ykey;
            xtype = CoordinateType.INTEGRAL;
            ytype = CoordinateType.INTEGRAL;
            data = new ArrayList<Pair<Object,Object>>();
            if (TIMESTAMP_KEY.equals(xkey)) {
                xtype = CoordinateType.TIME;
            } else if (FREQUENCY_KEY.equals(xkey)) {
                xtype = CoordinateType.INTEGRAL;
            } else if (MARKER_KEY.equals(xkey)) {
                xtype = CoordinateType.CATEGORY;
            }
            if (TIMESTAMP_KEY.equals(ykey)) {
                ytype = CoordinateType.TIME;
            } else if (FREQUENCY_KEY.equals(ykey)) {
                ytype = CoordinateType.INTEGRAL;
            } else if (MARKER_KEY.equals(ykey)) {
                ytype = CoordinateType.CATEGORY;
            }
            // if we have a filter value then convert it to a number if it is numeric
            Object filterValue = value;
            if (filter != null && value != null) {
                // may need to convert String to Numeric
                filterValue = maybeNumeric(value);
            }
            if (metrics != null) {
                for (BytemanMetric m : metrics) {
                    Map<String, Object> map = m.getDataAsMap();
                    // ensure that lookups for the timestamp key always retrieve
                    // the Long timestamp value associated with the metric and
                    // that lookups for the frequency key always retrieve
                    // the Long value 1.
                    map.put(TIMESTAMP_KEY, m.getTimeStamp());
                    map.put(FREQUENCY_KEY, frequencyUnit);
                    map.put(MARKER_KEY, m.getMarker());
                    // if we have a filter then check for presence of filter key
                    if (filter != null && filter.length() > 0) {
                        Object v = map.get(filter);
                        if (v == null) {
                            // skip this metric
                            continue;
                        }
                        if (filterValue != null) {
                            // may need to process String value as Numeric
                            if (v instanceof String) {
                                v = maybeNumeric((String)v);
                            }
                            if (!filterValue.equals(v)) {
                                // skip this metric
                                continue;
                            }
                        }
                    }
                    Object xval = map.get(xkey);
                    Object yval = map.get(ykey);
                    // only include records which contain values for both coordinates
                    if(xval != null && yval != null) {
                        // maybe re-present retrieved values as Numeric
                        // and/or downgrade coordinate type from INTEGRAL
                        // to REAL or even CATEGORY
                        xval = newCoordinate(xkey, xval, true);
                        yval = newCoordinate(ykey, yval, false);
                        data.add(new Pair<Object, Object>(xval, yval));
                    }
                }
            }
        }

        public int size() {
            return data.size();
        }

        public XYDataset getXYDataset()
        {
            if (xtype == CoordinateType.CATEGORY ||
                    ytype == CoordinateType.CATEGORY) {
                return emptyXYDataset;
            }

            XYSeries xyseries = new XYSeries(ykey + " against  " + xkey);

            for (Pair<Object,Object> p : data) {
                Number x = (Number)p.getFirst();
                Number y = (Number)p.getSecond();
                int idx = xyseries.indexOf(x);
                if (idx >= 0) {
                    Number y1 = xyseries.getY(idx);
                    switch (ytype) {
                    case REAL:
                        y = y.doubleValue() + y1.doubleValue();
                    default:
                        y = y.longValue() + y1.longValue();
                    }
                }
                xyseries.add(x, y);
            }
            XYSeriesCollection xycollection = new  XYSeriesCollection();
            xycollection.addSeries(xyseries);
            return xycollection;
        }

        public CategoryDataset getCategoryDataset()
        {
            if (xtype == CoordinateType.TIME) {
                return emptyCategoryDataset;
            }
            DefaultCategoryDataset dataset = new DefaultCategoryDataset();
            // treat x values as category values by calling toString
            // where they are numeric we ought to support binning them into ranges
            switch (ytype) {
            case CATEGORY:
                // graph category against category by frequency
                for (Pair<Object, Object> p : data) {
                    String first = p.getFirst().toString();
                    String second = p.getSecond().toString();
                    if(dataset.getRowKeys().contains(first) && dataset.getColumnKeys().contains(second)) {
                        dataset.incrementValue(1.0, first, second);
                    } else {
                        dataset.addValue(1.0, first, second);
                    }
                }
                break;
            case TIME:
                // bin time values into ranges and label range with start time
                // for now just drop through to treat time value as numeric
            default:
                // graph category against numeric by summing numeric values
                for (Pair<Object, Object> p : data) {
                    String first = p.getFirst().toString();
                    String second = "";
                    double increment = ((Number) p.getSecond()).doubleValue();
                    if(dataset.getRowKeys().contains(first)) {
                        dataset.incrementValue(increment, first, second);
                    } else {
                        dataset.addValue(increment, first, second);
                    }
                }
                break;
            }
            return dataset;
        }

        // alternative option for presenting category xkey with numeric ykey
        public PieDataset getPieDataset()
        {
            if (xtype != CoordinateType.CATEGORY || ytype == CoordinateType.CATEGORY) {
                return emptyPieDataset;
            }

            DefaultKeyedValues keyedValues = new DefaultKeyedValues();

            for (Pair<Object,Object> p : data) {
                String first = p.getFirst().toString();
                double second = ((Number)p.getSecond()).doubleValue();
                int index = keyedValues.getIndex(first);
                if (index >= 0) {
                    Number existing = keyedValues.getValue(first);
                    keyedValues.addValue(first, existing.doubleValue() + second);
                } else {
                    keyedValues.addValue(first, second);
                }
            }
            PieDataset pieDataset = new DefaultPieDataset(keyedValues);
            return pieDataset;
        }

        public XYDataset getCategoryTimePlot(String[][] symbolsReturn)
        {
            if (xtype != CoordinateType.TIME || ytype != CoordinateType.CATEGORY) {
                return emptyXYDataset;
            }

            // we need to display changing category state over time
            //
            // we can create an XYDataSet substituting numeric Y values
            // to encode the category data. then we provide the data
            // set with a range axis which displays the numeric
            // values symbolically.

            XYSeries xyseries = new XYSeries(ykey + " against  " + xkey);
            int count = 0;
            HashMap<String, Number> tickmap = new HashMap<String, Number>();

            for (Pair<Object,Object> p : data) {
                Number x = (Number)p.getFirst();
                String ysym = (String)p.getSecond();
                Number y = tickmap.get(ysym);
                if (y == null) {
                    y = Long.valueOf(count++);
                    tickmap.put(ysym, y);
                }
                xyseries.add(x, y);
            }
            // populate key array
            String[] symbols = new String[count];
            for (String key: tickmap.keySet()) {
                int value = tickmap.get(key).intValue();
                symbols[value] = key;
            }

            symbolsReturn[0] = symbols;

            XYSeriesCollection xycollection = new  XYSeriesCollection();
            xycollection.addSeries(xyseries);

            return xycollection;
        }

        public String getXLabel() {
            return xkey;
        }

        public String getYLabel() {
            return ykey;
        }

        public CoordinateType getXType() {
            return xtype;
        }

        public CoordinateType getYType() {
            return ytype;
        }

        /**
         * process a newly read x or y coordinate value, which is either a Long timestanp or an unparsed
         * numeric or category value String, returning a Long, parsed Numeric or String value. As a side
         * effect of attempting to parse an input String the coordinate type for the relevant coordinate
         * axis may be downgraded from INTEGRAL (assumed default) to DOUBLE or CATEGORY.
         * @param key the label for the coordinate axis which may be the special value timestamp
         * @param value the new found coordinate value which may be a Long timestamp or a String yet to be parsed
         * @param isX  true if this is an x coordinate value false if it is a y coordinate value
         * @return an Object repreenting
         */
        private Object newCoordinate(String key, Object value, boolean isX) {

            CoordinateType ctype = (isX ? xtype : ytype);
            if (ctype == CoordinateType.TIME) {
                // guaranteed already to be a Long
                return value;
            }

            boolean updateCType = false;

            if (value instanceof String && ctype != CoordinateType.CATEGORY) {
                String str = (String)value;
                // see if we can parse this as a number
                try {
                    if (str.contains(".")) {
                        value = Double.valueOf(str);
                        if (ctype != CoordinateType.REAL) {
                            ctype = CoordinateType.REAL;
                            updateCType = true;
                        }
                    } else {
                        value = Long.valueOf(str);
                    }
                } catch (NumberFormatException nfe) {
                    ctype = CoordinateType.CATEGORY;
                    updateCType = true;
                }
            }
            if (updateCType) {
                if (isX) {
                    xtype = ctype;
                } else {
                    ytype = ctype;
                }
            }
            return value;
        }
    }
}
