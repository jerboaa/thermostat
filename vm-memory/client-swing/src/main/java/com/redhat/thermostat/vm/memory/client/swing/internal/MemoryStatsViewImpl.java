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

package com.redhat.thermostat.vm.memory.client.swing.internal;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.beans.Transient;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;

import com.redhat.thermostat.client.core.views.BasicView;
import com.redhat.thermostat.client.swing.SwingComponent;
import com.redhat.thermostat.client.swing.components.HeaderPanel;
import com.redhat.thermostat.client.swing.experimental.ComponentVisibilityNotifier;
import com.redhat.thermostat.common.ActionListener;
import com.redhat.thermostat.common.ActionNotifier;
import com.redhat.thermostat.gc.remote.client.common.RequestGCAction;
import com.redhat.thermostat.gc.remote.client.swing.ToolbarGCButton;
import com.redhat.thermostat.gc.remote.common.command.GCAction;
import com.redhat.thermostat.shared.locale.LocalizedString;
import com.redhat.thermostat.shared.locale.Translate;
import com.redhat.thermostat.vm.memory.client.core.MemoryStatsView;
import com.redhat.thermostat.vm.memory.client.core.Payload;
import com.redhat.thermostat.vm.memory.client.locale.LocaleResources;

public class MemoryStatsViewImpl extends MemoryStatsView implements SwingComponent {

    private static final Translate<LocaleResources> t = LocaleResources.createLocalizer();

    //FIXME: Duplicate default timeunits : See VmCpuChartPanel.java
    private static final TimeUnit[] DEFAULT_TIMEUNITS = new TimeUnit[] { TimeUnit.DAYS, TimeUnit.HOURS, TimeUnit.MINUTES };

    private static final long REPAINT_DELAY = 500;

    private long lastRepaint;
    
    private HeaderPanel visiblePanel;
    private JPanel graphPanel;
    private JPanel contentPanel;

    private JPanel labelContainer;
    
    private final Map<String, MemoryGraphPanel> regions;
    
    private ToolbarGCButton toolbarButton;
    private RequestGCAction toolbarButtonAction;
    
    private Dimension preferredSize;

    private ActionNotifier<UserAction> userActionNotifier = new ActionNotifier<>(this);

    public MemoryStatsViewImpl(Duration duration) {
        super();
        visiblePanel = new HeaderPanel();
        regions = new HashMap<>();
 
        preferredSize = new Dimension(0, 0);
        
        visiblePanel.setHeader(t.localize(LocaleResources.MEMORY_REGIONS_HEADER));

        new ComponentVisibilityNotifier().initialize(visiblePanel, notifier);

        graphPanel = new JPanel();
        graphPanel.setLayout(new BoxLayout(graphPanel, BoxLayout.Y_AXIS));

        contentPanel = new JPanel();
        contentPanel.setLayout(new BorderLayout());

        contentPanel.add(graphPanel, BorderLayout.CENTER);
        contentPanel.add(getControlsAndAdditionalDisplay(duration), BorderLayout.SOUTH);

        visiblePanel.setContent(contentPanel);
        
        toolbarButtonAction = new RequestGCAction();
        toolbarButton = new ToolbarGCButton(toolbarButtonAction);
        toolbarButton.setName("gcButton");
        visiblePanel.addToolBarButton(toolbarButton);

    }
    
    @Transient
    public Dimension getPreferredSize() {
        return new Dimension(preferredSize);
    }
    
    @Override
    public void updateRegion(final Payload region) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                MemoryGraphPanel memoryGraphPanel = regions.get(region.getName());
                memoryGraphPanel.setMemoryGraphProperties(region);
            }
        });
    }
    
    @Override
    public void setEnableGCAction(final boolean enable) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                toolbarButton.setEnabled(enable);
            }
        });
    }

    @Override
    public void addGCActionListener(ActionListener<GCAction> listener) {
        toolbarButtonAction.addActionListener(listener);
    }

    @Override
    public void addUserActionListener(ActionListener<UserAction> listener) {
        userActionNotifier.addActionListener(listener);
    }

    @Override
    public void addRegion(final Payload region) {

        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                MemoryGraphPanel memoryGraphPanel = new MemoryGraphPanel();
                
                graphPanel.add(memoryGraphPanel);
                graphPanel.add(Box.createRigidArea(new Dimension(5, 5)));
                regions.put(region.getName(), memoryGraphPanel);
                
                // components are stacked up vertically in this panel
                Dimension memoryGraphPanelMinSize = memoryGraphPanel.getMinimumSize();
                preferredSize.height += memoryGraphPanelMinSize.height + 5;
                if (preferredSize.width < (memoryGraphPanelMinSize.width + 5)) {
                    preferredSize.width = memoryGraphPanelMinSize.width + 5;
                }

                updateRegion(region);
                graphPanel.revalidate();
            }
        });
    }

    @Override
    public void displayWarning(LocalizedString string) {
        JOptionPane.showMessageDialog(visiblePanel, string.getContents(), "Warning", JOptionPane.WARNING_MESSAGE);
    }

    @Override
    public Component getUiComponent() {
        return visiblePanel;
    }

    @Override
    public void requestRepaint() {
        // really only repaint every REPAINT_DELAY milliseconds
        long now = System.currentTimeMillis();
        if (now - lastRepaint > REPAINT_DELAY) {
            visiblePanel.repaint();
            lastRepaint = System.currentTimeMillis();
        }
    }
    
    public BasicView getView() {
        return this;
    }

    //FIXME : Duplicate method : see RecentTimeSeriesChartPanel.java or VmCpuChartPanel.java
    public Component getControlsAndAdditionalDisplay(Duration duration) {
        JPanel container = new JPanel();
        container.setOpaque(false);

        container.setLayout(new BorderLayout());

        container.add(getChartControls(duration), BorderLayout.LINE_START);
        container.add(getAdditionalDataDisplay(), BorderLayout.LINE_END);

        container.setName("since-panel");

        return container;
    }

    //FIXME : Duplicate method : see RecentTimeSeriesChartPanel.java or VmCpuChartPanel.java
    private Component getChartControls(Duration duration) {
        JPanel container = new JPanel();
        container.setOpaque(false);

        final JTextField durationSelector = new JTextField(5);
        final JComboBox<TimeUnit> unitSelector = new JComboBox<>();
        unitSelector.setModel(new DefaultComboBoxModel<>(DEFAULT_TIMEUNITS));

        TimeUnitChangeListener timeUnitChangeListener = new TimeUnitChangeListener(duration.value, duration.unit);

        durationSelector.getDocument().addDocumentListener(timeUnitChangeListener);
        unitSelector.addActionListener(timeUnitChangeListener);

        durationSelector.setText(String.valueOf(duration.value));
        unitSelector.setSelectedItem(duration.unit);

        container.add(new JLabel(t.localize(LocaleResources.CHART_DURATION_SELECTOR_LABEL).getContents()));
        container.add(durationSelector);
        container.add(unitSelector);

        return container;
    }

    //FIXME : Duplicate method : see RecentTimeSeriesChartPanel.java or VmCpuChartPanel.java
    private Component getAdditionalDataDisplay() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setOpaque(false);
        labelContainer = new JPanel();
        labelContainer.setOpaque(false);
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.fill = GridBagConstraints.BOTH;
        constraints.anchor = GridBagConstraints.CENTER;
        panel.add(labelContainer, constraints);
        return panel;
    }

    //FIXME : Duplicate class : see RecentTimeSeriesChartPanel.java or VmCpuChartPanel.java
    private class TimeUnitChangeListener implements DocumentListener, java.awt.event.ActionListener {

        private int value;
        private TimeUnit unit;

        public TimeUnitChangeListener(int defaultValue, TimeUnit defaultUnit) {
            this.value = defaultValue;
            this.unit = defaultUnit;
        }

        @Override
        public void removeUpdate(DocumentEvent event) {
            changed(event.getDocument());
        }

        @Override
        public void insertUpdate(DocumentEvent event) {
            changed(event.getDocument());
        }

        @Override
        public void changedUpdate(DocumentEvent event) {
            changed(event.getDocument());
        }

        private void changed(Document doc) {
            try {
                this.value = Integer.valueOf(doc.getText(0, doc.getLength()));
            } catch (NumberFormatException nfe) {
                // ignore
            } catch (BadLocationException ble) {
                // ignore
            }
            chartChanged();
        }

        private void chartChanged() {
            MemoryStatsViewImpl.this.userActionNotifier.fireAction(UserAction.USER_CHANGED_TIME_RANGE, new Duration(value, unit));
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            @SuppressWarnings("unchecked") // We are a TimeUnitChangeListener, specifically.
                    JComboBox<TimeUnit> comboBox = (JComboBox<TimeUnit>) e.getSource();
            TimeUnit time = (TimeUnit) comboBox.getSelectedItem();
            this.unit = time;
            chartChanged();
        }
    }

}

