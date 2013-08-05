/*
 * Copyright 2012, 2013 Red Hat, Inc.
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

package com.redhat.thermostat.client.swing.internal.progress;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.swing.SwingUtilities;

import com.redhat.thermostat.client.core.progress.ProgressHandle;
import com.redhat.thermostat.client.core.progress.ProgressNotifier;
import com.redhat.thermostat.client.swing.SwingComponent;
import com.redhat.thermostat.common.ActionEvent;
import com.redhat.thermostat.common.ActionListener;
import com.redhat.thermostat.common.ActionNotifier;
import com.redhat.thermostat.common.model.Range;
import com.redhat.thermostat.shared.locale.LocalizedString;

public class SwingProgressNotifier implements ProgressNotifier, SwingComponent {

    public enum PropertyChange {
        ALL_PROGRESS_DONE,
    }
    
    // for tests, so that code run synchronously
    private boolean runInEDT;
    
    private final ActionNotifier<SwingProgressNotifier.PropertyChange> notifier;

    public void addPropertyChangeListener(ActionListener<SwingProgressNotifier.PropertyChange> listener) {
        notifier.addActionListener(listener);
    }
    
    public void removePropertyChangeListener(ActionListener<SwingProgressNotifier.PropertyChange> listener) {
        notifier.removeActionListener(listener);
    }
    
    private Map<ProgressHandle, AggregateProgressComponent> tasks;
    
    private ProgressNotificationArea notificationArea;
    private AggregateNotificationPanel aggregateNotificationArea;
    
    public SwingProgressNotifier(AggregateNotificationPanel aggregateNotificationArea,
                                 ProgressNotificationArea notificationArea,
                                 ThermostatGlassPane glassPane)
    {
        this(aggregateNotificationArea, notificationArea, glassPane, true);
    }
    
    /**
     * For test only, allows to build a notifier that runs update outside
     * the EDT.
     */
    SwingProgressNotifier(AggregateNotificationPanel aggregateNotificationArea,
                          ProgressNotificationArea notificationArea,
                          ThermostatGlassPane glassPane, boolean runInEDT)
    {
        this.notificationArea = notificationArea;
        this.aggregateNotificationArea = aggregateNotificationArea;
        tasks = new ConcurrentHashMap<>();

        notifier = new ActionNotifier<>(this);
        
        this.runInEDT = runInEDT;
    }
    
    /**
     * For test only, access the internal map containing handles and
     * progress components currently tracked by this notifier.
     */
    Map<ProgressHandle, AggregateProgressComponent> __getTasks() {
        return tasks;
    }
    
    private void handleTask(ActionEvent<ProgressHandle.Status> status, ProgressHandle handle) {
        switch (status.getActionId()) {
        case STARTED: {
            AggregateProgressComponent progressBar = new AggregateProgressComponent(handle);
            aggregateNotificationArea.addProgress(progressBar);
            tasks.put(handle, progressBar);
            
            notificationArea.setRunningTask(handle);
            
            checkTasksNumber();
            
        } break;

        case STOPPED: {
            AggregateProgressComponent progressBar = tasks.remove(handle);
            if (progressBar != null) {
                aggregateNotificationArea.removeProgress(progressBar);                
            }
            
            if (tasks.isEmpty()) {
                notificationArea.reset();
                notifier.fireAction(PropertyChange.ALL_PROGRESS_DONE);
            } else {
                // pick another task, only if this is the task that was just
                // removed
                if (handle.equals(notificationArea.getRunningTask())) {
                   for (ProgressHandle newHandle : tasks.keySet()) {
                       notificationArea.setRunningTask(newHandle);
                       break;
                   }
                }
                checkTasksNumber();
            }
            
        } break;
        
        case TASK_CHANGED: {
            AggregateProgressComponent progressBar = tasks.get(handle);
            if (progressBar != null) {
                String text = ((LocalizedString) status.getPayload()).getContents();
                progressBar.getTaskStatus().setText(text);
            }
        
        } break;
        
        case DETERMINATE_STATUS_CHANGED: {
            AggregateProgressComponent progressBar = tasks.get(handle);
            if (progressBar != null) {
                boolean state = ((Boolean) status.getPayload()).booleanValue();
                progressBar.getProgressBar().setIndeterminate(state);                
            }
        } break;

        case BOUNDS_CHANGED: {
            AggregateProgressComponent progressBar = tasks.get(handle);
            if (progressBar != null) {
                
                @SuppressWarnings("unchecked")
                Range<Integer> range = (Range<Integer>) status.getPayload();
                progressBar.getProgressBar().setMinimum(range.getMin().intValue());
                progressBar.getProgressBar().setMaximum(range.getMax().intValue());
            }
            
        } break;
        
        case PROGRESS_CHANGED: {
            AggregateProgressComponent progressBar = tasks.get(handle);
            if (progressBar != null) {
                int value = ((Integer) status.getPayload()).intValue();
                progressBar.getProgressBar().setValue(value);
            }
        } break;
            
        default:
            // nothing here
        }
    }
    
    private void checkTasksNumber() {
        if (tasks.size() > 1) {
            notificationArea.setHasMore(true);
        } else {
            notificationArea.setHasMore(false);
        }
    }
    
    @Override
    public void register(final ProgressHandle handle) {
        
        handle.addProgressListener(new ActionListener<ProgressHandle.Status>() {
            @Override
            public void actionPerformed(final ActionEvent<ProgressHandle.Status> status) {
                if (runInEDT) {
                    SwingUtilities.invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            handleTask(status, handle);
                        }
                    });
                } else {
                    handleTask(status, handle);
                }
            }
        });
    }

    @Override
    public AggregateNotificationPanel getUiComponent() {
        return aggregateNotificationArea;
    }

    @Override
    public boolean hasTasks() {
        return !tasks.isEmpty();
    }
}
