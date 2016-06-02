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
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JToggleButton;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.border.LineBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import com.redhat.thermostat.client.swing.IconResource;
import com.redhat.thermostat.client.swing.SwingComponent;
import com.redhat.thermostat.client.swing.components.ActionToggleButton;
import com.redhat.thermostat.client.swing.components.FontAwesomeIcon;
import com.redhat.thermostat.client.swing.components.HeaderPanel;
import com.redhat.thermostat.client.swing.components.Icon;
import com.redhat.thermostat.client.swing.components.ThermostatScrollPane;
import com.redhat.thermostat.client.swing.components.ThermostatTabbedPane;
import com.redhat.thermostat.client.swing.components.ThermostatTextArea;
import com.redhat.thermostat.client.swing.experimental.ComponentVisibilityNotifier;
import com.redhat.thermostat.common.ActionEvent;
import com.redhat.thermostat.common.ActionListener;
import com.redhat.thermostat.common.utils.LoggingUtils;
import com.redhat.thermostat.shared.locale.LocalizedString;
import com.redhat.thermostat.shared.locale.Translate;
import com.redhat.thermostat.vm.byteman.common.BytemanMetric;

public class SwingVmBytemanView extends VmBytemanView implements SwingComponent {

    private static final Logger logger = LoggingUtils.getLogger(SwingVmBytemanView.class);
    private static final Translate<LocaleResources> t = LocaleResources.createLocalizer();
    private static final Icon START_ICON = IconResource.SAMPLE.getIcon();
    private static final Icon STOP_ICON = new FontAwesomeIcon('\uf28e', START_ICON.getIconHeight());
    private static final String EMPTY_STR = "";
    
    static final String NO_METRICS_AVAILABLE = t.localize(LocaleResources.NO_METRICS_AVAILABLE).getContents();
    
    // Names for buttons used in testing
    static final String TOGGLE_BUTTON_NAME = "TOGGLE_RULE_BUTTON";
    static final String RULES_TEXT_NAME = "RULES_TEXT";
    static final String METRICS_TEXT_NAME = "METRICS_TEXT";
    
    private final JTextArea metricsText;
    private final JTextArea rulesText;
    private final JTabbedPane tabbedPane;
    private final HeaderPanel mainContainer;
    private final ActionToggleButton toggleButton;
    private final CopyOnWriteArrayList<ActionListener<InjectAction>> injectListeners = new CopyOnWriteArrayList<>();
    private final CopyOnWriteArrayList<ActionListener<TabbedPaneAction>> tabbedPaneListeners = new CopyOnWriteArrayList<>();
    private final CopyOnWriteArrayList<ActionListener<GenerateAction>> generateListeners = new CopyOnWriteArrayList<>();
    
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
        
        final double yWeightRow1 = 0.95;
        final double yWeightRow2 = 0.05;
        final double xWeightFullWidth = 1.0;
        final Insets paddingInsets = new Insets(5, 5, 5, 5);

        // Rules tab
        final JPanel rulesPanel = new JPanel();
        rulesPanel.setLayout(new GridBagLayout());
        rulesText = new ThermostatTextArea(EMPTY_STR);
        rulesText.setName(RULES_TEXT_NAME);
        rulesText.setMargin(paddingInsets);
        rulesText.setBackground(Color.WHITE);
        rulesText.setCursor(new Cursor(Cursor.TEXT_CURSOR));
        rulesText.setBorder(new LineBorder(Color.BLACK));
        GridBagConstraints cRules = new GridBagConstraints();
        cRules.fill = GridBagConstraints.BOTH;
        cRules.gridx = 0;
        cRules.gridy = 0;
        cRules.weighty = yWeightRow1;
        cRules.weightx = xWeightFullWidth;
        cRules.insets = paddingInsets;
        JScrollPane scrollPane = new ThermostatScrollPane(rulesText);
        rulesPanel.add(scrollPane, cRules);
        cRules.fill = GridBagConstraints.BOTH;
        cRules.gridx = 0;
        cRules.gridy = 1;
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
                fireGenerateEvent(GenerateAction.GENERATE_TEMPLATE);
            }
            
        });
        buttonHolder.add(generateRuleButton);
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
        c.weighty = yWeightRow1;
        c.weightx = xWeightFullWidth;
        c.insets = paddingInsets;
        JScrollPane metricsScroll = new ThermostatScrollPane(metricsText);
        metricsPanel.add(metricsScroll, c);
        c.fill = GridBagConstraints.BOTH;
        c.gridx = 0;
        c.gridy = 1;
        c.weighty = yWeightRow2;
        c.weightx = xWeightFullWidth;
        JPanel placeholder = new JPanel();
        // Set to the same size as rules button holder
        placeholder.setPreferredSize(buttonHolder.getPreferredSize());
        metricsPanel.add(placeholder, c);
        
        tabbedPane = new ThermostatTabbedPane();
        tabbedPane.addTab(t.localize(LocaleResources.TAB_RULES).getContents(), rulesPanel);
        tabbedPane.addTab(t.localize(LocaleResources.TAB_METRICS).getContents(), metricsPanel);
        tabbedPane.addChangeListener(new ChangeListener() {
            
            @Override
            public void stateChanged(ChangeEvent e) {
                JTabbedPane pane = (JTabbedPane)e.getSource();
                JPanel selectedPanel = (JPanel)pane.getSelectedComponent();
                if (selectedPanel == rulesPanel) {
                    fireTabSelectedEvent(TabbedPaneAction.RULES_TAB_SELECTED);
                } else if (selectedPanel == metricsPanel) {
                    fireTabSelectedEvent(TabbedPaneAction.METRICS_TAB_SELECTED);
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
        final String buttonLabel;
        if (!viewControlsEnabled) {
            buttonLabel = t.localize(LocaleResources.INJECT_RULE).getContents();
        } else if (state == BytemanInjectState.UNLOADING || state == BytemanInjectState.INJECTED) {
            buttonLabel = t.localize(LocaleResources.UNLOAD_RULE).getContents();
        } else {
            buttonLabel = t.localize(LocaleResources.INJECT_RULE).getContents();
        }
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                if (!viewControlsEnabled) {
                    toggleButton.setToggleActionState(BytemanInjectState.DISABLED);
                } else {
                    toggleButton.setToggleActionState(state);
                }
                toggleButton.setText(buttonLabel);
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
        default:
            throw new AssertionError("Unknown event: " + action);
        }
        
    }

    private void updateRuleInView(final String rule) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                rulesText.setText(rule);
            }
        });
    }

    private void updateViewWithMetrics(List<BytemanMetric> metrics) {
        final StringBuffer buffer = new StringBuffer();
        for (BytemanMetric m: metrics) {
            buffer.append(m.getData()).append("\n");
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
                    content[0] = rulesText.getText();
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

}
