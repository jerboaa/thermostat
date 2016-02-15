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

package com.redhat.thermostat.client.core.progress;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;

import org.junit.Test;
import org.mockito.ArgumentCaptor;

import com.redhat.thermostat.common.ActionEvent;
import com.redhat.thermostat.common.ActionListener;
import com.redhat.thermostat.common.model.Range;
import com.redhat.thermostat.shared.locale.LocalizedString;

public class ProgressHandleTest {

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Test
    public void testProgressHandleStartStop() {
        ActionListener listener = mock(ActionListener.class);
        
        LocalizedString name = new LocalizedString("test #1");
        ProgressHandle handle = new ProgressHandle(name);
        
        assertEquals(name, handle.getName());
        
        handle.addProgressListener(listener);

        ArgumentCaptor<ActionEvent> captor =
                ArgumentCaptor.forClass(ActionEvent.class);        
        
        handle.start();
        
        verify(listener).actionPerformed(captor.capture());
        ActionEvent event = captor.getValue();
        assertEquals(ProgressHandle.Status.STARTED, event.getActionId());
        
        handle.stop();

        verify(listener, times(2)).actionPerformed(captor.capture());
        
        event = captor.getValue();
        assertEquals(ProgressHandle.Status.STOPPED, event.getActionId());
    }
    
    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Test
    public void testProgressHandleStatusChange() {
        ActionListener listener = mock(ActionListener.class);
        
        LocalizedString name = new LocalizedString("test #1");
        ProgressHandle handle = new ProgressHandle(name);
        
        assertEquals(name, handle.getName());
        
        handle.addProgressListener(listener);

        ArgumentCaptor<ActionEvent> captor =
                ArgumentCaptor.forClass(ActionEvent.class);        
        
        handle.setIndeterminate(true);

        verify(listener).actionPerformed(captor.capture());
        
        ActionEvent event = captor.getValue();
        assertEquals(ProgressHandle.Status.DETERMINATE_STATUS_CHANGED, event.getActionId());
        assertTrue(handle.isIndeterminate());
        assertEquals(Boolean.TRUE, event.getPayload());

        handle.setIndeterminate(false);

        verify(listener, times(2)).actionPerformed(captor.capture());
        
        event = captor.getValue();
        assertEquals(ProgressHandle.Status.DETERMINATE_STATUS_CHANGED, event.getActionId());
        assertFalse(handle.isIndeterminate());
        assertEquals(Boolean.FALSE, event.getPayload());
    }
    
    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Test
    public void testProgressHandleProgressChange() {
        ActionListener listener = mock(ActionListener.class);
        
        LocalizedString name = new LocalizedString("test #1");
        ProgressHandle handle = new ProgressHandle(name);
        
        assertEquals(name, handle.getName());
        
        handle.addProgressListener(listener);

        ArgumentCaptor<ActionEvent> captor =
                ArgumentCaptor.forClass(ActionEvent.class);        
        
        handle.setProgress(5);

        verify(listener).actionPerformed(captor.capture());
        
        ActionEvent event = captor.getValue();
        assertEquals(ProgressHandle.Status.PROGRESS_CHANGED, event.getActionId());
        assertEquals(5, handle.getProgress());
        assertEquals(Integer.valueOf(5), event.getPayload());

        handle.setProgress(15);

        verify(listener, times(2)).actionPerformed(captor.capture());
        
        event = captor.getValue();
        assertEquals(ProgressHandle.Status.PROGRESS_CHANGED, event.getActionId());
        assertEquals(15, handle.getProgress());
        assertEquals(Integer.valueOf(15), event.getPayload());
    }
    
    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Test
    public void testProgressHandleBoundsChange() {
        ActionListener listener = mock(ActionListener.class);
        
        LocalizedString name = new LocalizedString("test #1");
        ProgressHandle handle = new ProgressHandle(name);
        
        assertEquals(name, handle.getName());
        
        handle.addProgressListener(listener);

        ArgumentCaptor<ActionEvent> captor =
                ArgumentCaptor.forClass(ActionEvent.class);        
        
        Range<Integer> range = new Range<Integer>(10, 20);
        
        handle.setRange(range);

        verify(listener).actionPerformed(captor.capture());
        
        ActionEvent event = captor.getValue();
        assertEquals(ProgressHandle.Status.BOUNDS_CHANGED, event.getActionId());
        assertEquals(range, handle.getRange());
        assertEquals(range, event.getPayload());

        range = new Range<Integer>(0xCAFE, 42);
        handle.setRange(range);

        verify(listener, times(2)).actionPerformed(captor.capture());
        
        event = captor.getValue();
        assertEquals(ProgressHandle.Status.BOUNDS_CHANGED, event.getActionId());
        assertEquals(range, handle.getRange());
        assertEquals(range, event.getPayload());

    }
}

