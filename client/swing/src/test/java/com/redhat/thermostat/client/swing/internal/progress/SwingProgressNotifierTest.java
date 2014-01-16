/*
 * Copyright 2012-2014 Red Hat, Inc.
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

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import javax.swing.JLabel;
import javax.swing.JProgressBar;
import javax.swing.RepaintManager;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import com.redhat.thermostat.client.core.progress.ProgressHandle;
import com.redhat.thermostat.client.core.progress.ProgressHandle.Status;
import com.redhat.thermostat.client.core.progress.ProgressNotifier;
import com.redhat.thermostat.client.swing.internal.progress.SwingProgressNotifier.PropertyChange;
import com.redhat.thermostat.common.ActionEvent;
import com.redhat.thermostat.common.ActionListener;
import com.redhat.thermostat.common.model.Range;
import com.redhat.thermostat.shared.locale.LocalizedString;

public class SwingProgressNotifierTest {

    private AggregateNotificationPanel aggregateNotificationArea;
    private ProgressNotificationArea notificationArea;
    
    @BeforeClass
    public static void setUpOnce() {
        // This is needed because some other test may have installed the
        // EDT violation checker repaint manager.
        // We don't need this check here, since we are not testing Swing
        // code but only the functionality of the notifier
        RepaintManager.setCurrentManager(new RepaintManager());
    }
    
    @Before
    public void setUp() {
        aggregateNotificationArea = mock(AggregateNotificationPanel.class);
        notificationArea = mock(ProgressNotificationArea.class);
    }
    
    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Test
    public void testNotifierStartStop() throws InterruptedException {
        ProgressNotifier notifier =
                new SwingProgressNotifier(aggregateNotificationArea,
                                          notificationArea, false);
        
        ProgressHandle handle = mock(ProgressHandle.class);
        when(handle.getName()).thenReturn(LocalizedString.EMPTY_STRING);
        when(handle.getTask()).thenReturn(LocalizedString.EMPTY_STRING);
        notifier.register(handle);
        
        final boolean [] result = new boolean[1];
        ((SwingProgressNotifier) notifier).addPropertyChangeListener(new ActionListener<SwingProgressNotifier.PropertyChange>() {
            @Override
            public void actionPerformed(ActionEvent<PropertyChange> actionEvent) {
                result[0] = true;
            }
        });
        
        ArgumentCaptor<ActionListener> captor =
                ArgumentCaptor.forClass(ActionListener.class);
        verify(handle).addProgressListener(captor.capture());
        
        ActionListener listener = captor.getValue();
        
        ActionEvent<ProgressHandle.Status> event =
                new ActionEvent<ProgressHandle.Status>(handle, Status.STARTED);
        listener.actionPerformed(event);
        
        ArgumentCaptor<AggregateProgressComponent> notificationAreaCaptor =
                ArgumentCaptor.forClass(AggregateProgressComponent.class);
        verify(aggregateNotificationArea).addProgress(notificationAreaCaptor.capture());
        verify(notificationArea).setRunningTask(handle);
        verify(notificationArea).setHasMore(false);
        
        assertTrue(notifier.hasTasks());
        
        event = new ActionEvent<ProgressHandle.Status>(handle, Status.STOPPED);
        listener.actionPerformed(event);
        
        AggregateProgressComponent aggregateComponent = notificationAreaCaptor.getValue();
        verify(aggregateNotificationArea).removeProgress(aggregateComponent);
        verify(notificationArea).reset();
        verify(notificationArea).setHasMore(false);
        
        assertTrue(result[0]);
    }
    
    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Test
    public void testHandleStatusChanges() throws InterruptedException {
        SwingProgressNotifier notifier =
                new SwingProgressNotifier(aggregateNotificationArea,
                                          notificationArea, false);

        JProgressBar progressBar = mock(JProgressBar.class);
        JLabel label = mock(JLabel.class);
        AggregateProgressComponent progressComponent =
                mock(AggregateProgressComponent.class);
        when(progressComponent.getProgressBar()).thenReturn(progressBar);
        when(progressComponent.getTaskStatus()).thenReturn(label);

        ProgressHandle handle = mock(ProgressHandle.class);
        when(handle.getName()).thenReturn(LocalizedString.EMPTY_STRING);
        when(handle.getTask()).thenReturn(LocalizedString.EMPTY_STRING);
        notifier.register(handle);

        ArgumentCaptor<ActionListener> captor =
                ArgumentCaptor.forClass(ActionListener.class);
        verify(handle).addProgressListener(captor.capture());
        
        notifier.__getTasks().put(handle, progressComponent);
        ActionListener listener = captor.getValue();
        
        LocalizedString textPayload = new LocalizedString("test");
        ActionEvent<ProgressHandle.Status> event =
                new ActionEvent<ProgressHandle.Status>(handle, Status.TASK_CHANGED);
        event.setPayload(textPayload);
        listener.actionPerformed(event);

        verify(label).setText(textPayload.getContents());

        event = new ActionEvent<ProgressHandle.Status>(handle, Status.DETERMINATE_STATUS_CHANGED);
        event.setPayload(Boolean.TRUE);
        listener.actionPerformed(event);
        
        verify(progressBar).setIndeterminate(true);

        event = new ActionEvent<ProgressHandle.Status>(handle, Status.DETERMINATE_STATUS_CHANGED);
        event.setPayload(Boolean.FALSE);
        listener.actionPerformed(event);
        
        verify(progressBar).setIndeterminate(false);
        
        event = new ActionEvent<ProgressHandle.Status>(handle, Status.PROGRESS_CHANGED);
        event.setPayload(Integer.valueOf(10));
        listener.actionPerformed(event);
        
        verify(progressBar).setValue(10);
        
        event = new ActionEvent<ProgressHandle.Status>(handle, Status.PROGRESS_CHANGED);
        event.setPayload(Integer.valueOf(99));
        listener.actionPerformed(event);
        
        verify(progressBar).setValue(99);
        
        Range<Integer> range = new Range<Integer>(5, 20);
        event = new ActionEvent<ProgressHandle.Status>(handle, Status.BOUNDS_CHANGED);
        event.setPayload(range);
        listener.actionPerformed(event);
        
        verify(progressBar).setMinimum(5);
        verify(progressBar).setMaximum(20);
        
        range = new Range<Integer>(99, 101);
        event = new ActionEvent<ProgressHandle.Status>(handle, Status.BOUNDS_CHANGED);
        event.setPayload(range);
        listener.actionPerformed(event);
        
        verify(progressBar).setMinimum(99);
        verify(progressBar).setMaximum(101);
    }
}

