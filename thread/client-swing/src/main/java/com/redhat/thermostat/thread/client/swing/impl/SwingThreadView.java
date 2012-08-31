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

package com.redhat.thermostat.thread.client.swing.impl;

import java.awt.Component;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;

import com.redhat.thermostat.client.osgi.service.ApplicationService;
import com.redhat.thermostat.client.ui.ComponentVisibleListener;
import com.redhat.thermostat.client.ui.SwingComponent;
import com.redhat.thermostat.common.locale.Translate;
import com.redhat.thermostat.swing.ChartPanel;
import com.redhat.thermostat.thread.client.common.ThreadTableView;
import com.redhat.thermostat.thread.client.common.ThreadView;
import com.redhat.thermostat.thread.client.common.VMThreadCapabilitiesView;
import com.redhat.thermostat.thread.client.common.chart.LivingDaemonThreadDifferenceChart;
import com.redhat.thermostat.thread.client.common.locale.LocaleResources;

public class SwingThreadView extends ThreadView implements SwingComponent {
    
    private String DIVIDER_LOCATION_KEY;
    
    private ThreadMainPanel panel;
    private ThreadAliveDaemonTimelinePanel timelinePanel;
    
    private SwingThreadTableView threadTable;
    private SwingVMThreadCapabilitiesView vmCapsView;
    
    private static final Translate t = LocaleResources.createLocalizer();

    private boolean skipNotification = false;
    
    public SwingThreadView() {
        
        panel = new ThreadMainPanel();
        panel.addHierarchyListener(new ComponentVisibleListener() {
            
            @Override
            public void componentShown(Component component) {
                SwingThreadView.this.notify(Action.VISIBLE);
                restoreDivider();
            }
            
            @Override
            public void componentHidden(Component component) {
                SwingThreadView.this.notify(Action.HIDDEN);
            }
        });
        
        panel.getSplitPane().addPropertyChangeListener(JSplitPane.DIVIDER_LOCATION_PROPERTY,
                                                       new PropertyChangeListener()
        {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                JSplitPane sourceSplitPane = (JSplitPane) evt.getSource();
                saveDivider(sourceSplitPane.getDividerLocation());
            }
        });
        
        timelinePanel = new ThreadAliveDaemonTimelinePanel();

        timelinePanel.setToggleText(t.localize(LocaleResources.START_RECORDING) + ":");
        timelinePanel.getRecordButton().addItemListener(new ItemListener()
        {
            @Override
            public void itemStateChanged(ItemEvent e) {
                                
                ThreadAction action = null;
                if (e.getStateChange() == ItemEvent.SELECTED) {
                    action = ThreadAction.START_LIVE_RECORDING;
                    timelinePanel.setToggleText(t.localize(LocaleResources.STOP_RECORDING) + ":");
                } else {
                    action = ThreadAction.STOP_LIVE_RECORDING;
                    timelinePanel.setToggleText(t.localize(LocaleResources.START_RECORDING) + ":");
                }
                
                if (skipNotification) return;
                
                final ThreadAction toNotify = action;
                SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
                    @Override
                    protected Void doInBackground() throws Exception {
                        notifier.fireAction(toNotify);
                        return null;
                    }
                };
                worker.execute();
            }
        });

        panel.getSplitPane().setTopComponent(timelinePanel);
        
        vmCapsView = new SwingVMThreadCapabilitiesView();
        JTabbedPane pane = new JTabbedPane();
        pane.addTab(t.localize(LocaleResources.VM_CAPABILITIES), vmCapsView.getUiComponent());
        
        threadTable = new SwingThreadTableView();
        pane.addTab(t.localize(LocaleResources.TABLE), threadTable.getUiComponent());
        
        panel.getSplitPane().setBottomComponent(pane);
    }
    
    @Override
    public Component getUiComponent() {
        return panel;
    }
    
    @Override
    public void setApplicationService(ApplicationService appService, String uniqueId) {
        super.setApplicationService(appService, uniqueId);
        DIVIDER_LOCATION_KEY = "divider." + uniqueId;
    }
    
    @Override
    public void setRecording(final boolean recording, final boolean notify) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                if (!notify) skipNotification = true;
                timelinePanel.getRecordButton().setSelected(recording);
                if (!notify) skipNotification = false;
            }
        });
    }
    
    @Override
    public void setDaemonThreads(final String daemonThreads) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                timelinePanel.getDaemonThreads().setText(daemonThreads);
            }
        });
    }
    
    public void setLiveThreads(final String liveThreads) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                timelinePanel.getLiveThreads().setText(liveThreads);
            }
        });
    };
    
    @Override
    public void updateLivingDaemonTimeline(final LivingDaemonThreadDifferenceChart model)
    {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                JPanel pane = timelinePanel.getTimelinePanel();
                pane.removeAll();
                
                ChartPanel charts = new ChartPanel(model);
                pane.add(charts);
                pane.revalidate();
                pane.repaint();
            }
        });
    }
    
    @Override
    public VMThreadCapabilitiesView createVMThreadCapabilitiesView() {
        return vmCapsView;
    }
    
    @Override
    public ThreadTableView createThreadTableView() {
        return threadTable;
    }
    
    @Override
    public void displayWarning(final String warning) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                JOptionPane.showMessageDialog(panel.getParent(), warning, "", JOptionPane.WARNING_MESSAGE);
            }
        });
    }
    
    private void restoreDivider() {
        int location = (int) ((double) (panel.getSplitPane().getHeight() - panel.getSplitPane().getDividerSize()) * 0.80);
        if (appService != null) {
            Object _location = appService.getApplicationCache().getAttribute(DIVIDER_LOCATION_KEY);
            if (_location != null) {
                location = (Integer) _location;
            }
        }
        panel.getSplitPane().setDividerLocation(location);
    }
    
    private void saveDivider(int location) {
        if (appService != null) {
            appService.getApplicationCache().addAttribute(DIVIDER_LOCATION_KEY, location);
        }
    }
}
