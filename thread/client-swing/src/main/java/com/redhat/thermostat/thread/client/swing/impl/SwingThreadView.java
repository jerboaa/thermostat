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

package com.redhat.thermostat.thread.client.swing.impl;

import com.redhat.thermostat.client.swing.ComponentVisibleListener;
import com.redhat.thermostat.client.swing.SwingComponent;
import com.redhat.thermostat.client.swing.UIDefaults;
import com.redhat.thermostat.client.swing.components.ThermostatTabbedPane;
import com.redhat.thermostat.common.ApplicationService;
import com.redhat.thermostat.shared.locale.LocalizedString;
import com.redhat.thermostat.shared.locale.Translate;
import com.redhat.thermostat.thread.client.common.ThreadTableBean;
import com.redhat.thermostat.thread.client.common.locale.LocaleResources;
import com.redhat.thermostat.thread.client.common.view.ThreadCountView;
import com.redhat.thermostat.thread.client.common.view.ThreadTableView;
import com.redhat.thermostat.thread.client.common.view.ThreadTimelineView;
import com.redhat.thermostat.thread.client.common.view.ThreadView;
import com.redhat.thermostat.thread.client.common.view.VmDeadLockView;

import java.awt.Component;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.JOptionPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;

public class SwingThreadView extends ThreadView implements SwingComponent {
    
    private String DIVIDER_LOCATION_KEY;
    
    private ThreadMainPanel panel;
    
    private SwingThreadCountView threadCountView;
    private SwingThreadTableView threadTableView;
    private SwingVmDeadLockView vmDeadLockView;
    private SwingThreadTimelineView threadTimelineView;
    private SwingThreadDetailsView threadDetailsView;

    private JTabbedPane topPane;
    private JTabbedPane bottomPane;
    
    private static final Translate<LocaleResources> t = LocaleResources.createLocalizer();

    private boolean skipNotification = false;
    
    private int threadDetailsPaneID = 0;
    
    private UIDefaults uiDefaults;

    public SwingThreadView(UIDefaults uiDefaults) {
        
        this.uiDefaults = uiDefaults;

        panel = new ThreadMainPanel();
        // TODO use ComponentVisiblityNotifier instead
        // sadly, the BasicView.notifier field can not be accessed here
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
        
        panel.getToggleButton().setToolTipText(t.localize(LocaleResources.START_RECORDING).getContents());
        panel.getToggleButton().setText(t.localize(LocaleResources.THREAD_MONITOR_SWITCH).getContents());
        panel.getToggleButton().addItemListener(new ItemListener()
        {
            @Override
            public void itemStateChanged(ItemEvent e) {
                
                ThreadAction action = null;                
                if (e.getStateChange() == ItemEvent.SELECTED) {
                    action = ThreadAction.START_LIVE_RECORDING;
                    panel.getToggleButton().setToolTipText(t.localize(LocaleResources.STOP_RECORDING).getContents());
                } else {
                    action = ThreadAction.STOP_LIVE_RECORDING;
                    panel.getToggleButton().setToolTipText(t.localize(LocaleResources.START_RECORDING).getContents());
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

        setupTopPane();
        setupBottomPane();
    }
    
    private void setupTopPane() {
        topPane = new ThermostatTabbedPane();
        topPane.setName("topTabbedPane");
        
        threadCountView = new SwingThreadCountView();
        Component comp = threadCountView.getUiComponent();
        comp.setName("count");
        topPane.addTab(t.localize(LocaleResources.THREAD_COUNT).getContents(), comp);
        
        threadTimelineView = new SwingThreadTimelineView(uiDefaults);
        comp = threadTimelineView.getUiComponent();
        comp.setName("timeline");
        topPane.addTab(t.localize(LocaleResources.TIMELINE).getContents(), comp);
        
        panel.getSplitPane().setTopComponent(topPane);
    }
    
    private void setupBottomPane() {
        bottomPane = new ThermostatTabbedPane();
        bottomPane.setName("bottomTabbedPane");
        
        threadTableView = new SwingThreadTableView();
        bottomPane.addTab(t.localize(LocaleResources.TABLE).getContents(), threadTableView.getUiComponent());
        
        threadDetailsView = new SwingThreadDetailsView();
        bottomPane.addTab(t.localize(LocaleResources.DETAILS).getContents(), threadDetailsView.getUiComponent());
        threadDetailsPaneID = 1;

        vmDeadLockView = new SwingVmDeadLockView();
        bottomPane.addTab(t.localize(LocaleResources.VM_DEADLOCK).getContents(), vmDeadLockView.getUiComponent());

        panel.getSplitPane().setBottomComponent(bottomPane);
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
    public void setEnableRecordingControl(final boolean enable) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                panel.getToggleButton().setEnabled(enable);
            }
        });
    }
    
    @Override
    public void setRecording(final boolean recording, final boolean notify) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                if (!notify) skipNotification = true;
                panel.getToggleButton().setSelected(recording);
                if (!notify) skipNotification = false;
            }
        });
    }

    @Override
    public VmDeadLockView createDeadLockView() {
        return vmDeadLockView;
    }
    
    @Override
    public ThreadTableView createThreadTableView() {
        return threadTableView;
    }
    
    @Override
    public void displayWarning(final LocalizedString warning) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                JOptionPane.showMessageDialog(panel.getParent(), warning.getContents(), "", JOptionPane.WARNING_MESSAGE);
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
    
    @Override
    public void displayThreadDetails(final ThreadTableBean thread) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                bottomPane.setSelectedIndex(threadDetailsPaneID);
                threadDetailsView.setDetails(thread);
            }
        });
    }
    
    @Override
    public ThreadTimelineView createThreadTimelineView() {
        return threadTimelineView;
    }

    @Override
    public ThreadCountView createThreadCountView() {
        return threadCountView;
    }
}

