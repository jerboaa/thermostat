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

package com.redhat.thermostat.vm.memory.client.swing.internal;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.Transient;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;

import com.redhat.thermostat.client.core.views.BasicView;
import com.redhat.thermostat.client.swing.SwingComponent;
import com.redhat.thermostat.client.swing.components.HeaderPanel;
import com.redhat.thermostat.client.swing.components.MultiChartPanel;
import com.redhat.thermostat.client.swing.components.MultiChartPanel.DataGroup;
import com.redhat.thermostat.client.swing.components.experimental.RecentTimeControlPanel;
import com.redhat.thermostat.client.swing.experimental.ComponentVisibilityNotifier;
import com.redhat.thermostat.common.ActionListener;
import com.redhat.thermostat.common.ActionNotifier;
import com.redhat.thermostat.common.Duration;
import com.redhat.thermostat.gc.remote.client.common.RequestGCAction;
import com.redhat.thermostat.gc.remote.client.swing.ToolbarGCButton;
import com.redhat.thermostat.gc.remote.common.command.GCAction;
import com.redhat.thermostat.shared.locale.LocalizedString;
import com.redhat.thermostat.shared.locale.Translate;
import com.redhat.thermostat.storage.model.DiscreteTimeData;
import com.redhat.thermostat.vm.memory.client.core.MemoryStatsView;
import com.redhat.thermostat.vm.memory.client.core.Payload;
import com.redhat.thermostat.vm.memory.client.locale.LocaleResources;

public class MemoryStatsViewImpl extends MemoryStatsView implements SwingComponent {

    private static final Translate<LocaleResources> t = LocaleResources.createLocalizer();

    private static final long REPAINT_DELAY = 500;

    private long lastRepaint;
    
    private HeaderPanel visiblePanel;
    private JTabbedPane rootPanel;
    private JPanel graphPanel;
    private JPanel contentPanel;
    private JPanel tlabPanel;
    
    private final Map<String, MemoryGraphPanel> regions;
    
    private ToolbarGCButton toolbarButton;
    private RequestGCAction toolbarButtonAction;
    
    private Dimension preferredSize;

    private ActionNotifier<UserAction> userActionNotifier = new ActionNotifier<>(this);

    private MultiChartPanel multiChart;
    private DataGroup numberGroup;
    private DataGroup bytesGroup;

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

        RecentTimeControlPanel recentTimeControlPanel = new RecentTimeControlPanel(duration);
        recentTimeControlPanel.addPropertyChangeListener(RecentTimeControlPanel.PROPERTY_VISIBLE_TIME_RANGE, new PropertyChangeListener() {
            @Override
            public void propertyChange(final PropertyChangeEvent evt) {
                Duration d = (Duration) evt.getNewValue();
                MemoryStatsViewImpl.this.userActionNotifier.fireAction(UserAction.USER_CHANGED_TIME_RANGE, d);
            }
        });
        contentPanel.add(recentTimeControlPanel, BorderLayout.SOUTH);

        createTlabPanel();

        rootPanel = new JTabbedPane();
        rootPanel.addTab(t.localize(LocaleResources.TAB_MEMORY).getContents(), null, contentPanel, t.localize(LocaleResources.TAB_MEMORY_TOOLTIP).getContents());
        rootPanel.addTab(t.localize(LocaleResources.TAB_TLAB).getContents(), null, tlabPanel, t.localize(LocaleResources.TAB_TLAB_TOOLTIP).getContents());

        visiblePanel.setContent(rootPanel);
        
        toolbarButtonAction = new RequestGCAction();
        toolbarButton = new ToolbarGCButton(toolbarButtonAction);
        toolbarButton.setName("gcButton");
        visiblePanel.addToolBarButton(toolbarButton);

    }

    private void createTlabPanel() {
        tlabPanel = new JPanel();
        tlabPanel.setLayout(new BorderLayout());

        multiChart = new MultiChartPanel();
        numberGroup = multiChart.createGroup();
        bytesGroup = multiChart.createGroup();
        addChartTypes();

        tlabPanel.add(multiChart, BorderLayout.CENTER);
    }

    private void addChartTypes() {
        for (Type type : Type.values()) {
            DataGroup group = getGroup(type);
            String tag = getTag(type);
            multiChart.addChart(group, tag, type.getLabel());
            multiChart.showChart(group, tag);
        }

        multiChart.getRangeAxis(numberGroup).setLabel(
                t.localize(LocaleResources.TLAB_CHART_NUMBER_AXIS).getContents());
        multiChart.getRangeAxis(bytesGroup).setLabel(
                t.localize(LocaleResources.TLAB_CHART_BYTE_AXIS).getContents());
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
                graphPanel.repaint();
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

    @Override
    public void addTlabData(final Type type, final List<DiscreteTimeData<? extends Number>> data) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                String tag = getTag(type);
                multiChart.addData(tag, data);
            }
        });
    }

    @Override
    public void clearTlabData(final Type type) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                String tag = getTag(type);
                multiChart.clearData(tag);
            }
        });
    }

    private DataGroup getGroup(final Type type) {
        final DataGroup group;
        // FIXME this is view knowing about the data internals. not desirable.
        if (type == Type.TOTAL_ALLOCATING_THREADS || type == Type.TOTAL_ALLOCATIONS
                || type == Type.TOTAL_REFILLS || type == Type.MAX_REFILLS
                || type == Type.TOTAL_SLOW_ALLOCATIONS || type == Type.MAX_SLOW_ALLOCATIONS) {
            group = numberGroup;
        } else {
            group = bytesGroup;
        }
        return group;
    }

    private String getTag(final Type type) {
        return type.name();
    }

    public BasicView getView() {
        return this;
    }

}

