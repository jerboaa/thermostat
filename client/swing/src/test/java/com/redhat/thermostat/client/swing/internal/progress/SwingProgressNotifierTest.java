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

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.junit.Assert.assertTrue;

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

public class SwingProgressNotifierTest {

    private AggregateNotificationPanel aggregateNotificationArea;
    private ProgressNotificationArea notificationArea;
    private ThermostatGlassPane glassPane;
    
    @BeforeClass
    public void setUpOnce() {
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
        glassPane = mock(ThermostatGlassPane.class);
    }
    
    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Test
    public void testNotifier() throws InterruptedException {
        ProgressNotifier notifier =
                new SwingProgressNotifier(aggregateNotificationArea,
                                          notificationArea, glassPane, false);
        
        ProgressHandle handle = mock(ProgressHandle.class);
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
        
        AggregateProgressComponent aggregateComponent = notificationAreaCaptor.getValue();
        
        event = new ActionEvent<ProgressHandle.Status>(handle, Status.STOPPED);
        listener.actionPerformed(event);
        
        verify(aggregateNotificationArea).removeProgress(aggregateComponent);
        verify(notificationArea).reset();
        verify(notificationArea).setHasMore(false);
        
        assertTrue(result[0]);
    }
}
