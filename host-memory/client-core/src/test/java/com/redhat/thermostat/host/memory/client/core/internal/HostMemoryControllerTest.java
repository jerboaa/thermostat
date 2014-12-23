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

package com.redhat.thermostat.host.memory.client.core.internal;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.junit.Test;
import org.mockito.ArgumentCaptor;

import com.redhat.thermostat.client.core.experimental.Duration;
import com.redhat.thermostat.common.ActionEvent;
import com.redhat.thermostat.common.ActionListener;
import com.redhat.thermostat.common.ApplicationService;
import com.redhat.thermostat.common.Timer;
import com.redhat.thermostat.common.TimerFactory;
import com.redhat.thermostat.host.memory.client.core.HostMemoryView;
import com.redhat.thermostat.host.memory.client.core.HostMemoryViewProvider;
import com.redhat.thermostat.host.memory.common.MemoryStatDAO;
import com.redhat.thermostat.host.memory.common.model.MemoryStat;
import com.redhat.thermostat.storage.core.HostRef;
import com.redhat.thermostat.storage.dao.HostInfoDAO;
import com.redhat.thermostat.storage.model.HostInfo;

public class HostMemoryControllerTest {

    @SuppressWarnings({ "unchecked", "rawtypes" }) // any(List.class)
    @Test
    public void testUpdate() {
        final long TOTAL_MEMORY = 512;
        HostInfo hostInfo = new HostInfo("foo-agent", "someHost", "someOS", "linux_0.0.1", "lreally_fast_cpu", 2, TOTAL_MEMORY);
        HostInfoDAO hostInfoDAO = mock(HostInfoDAO.class);
        when(hostInfoDAO.getHostInfo(any(HostRef.class))).thenReturn(hostInfo);

        MemoryStat memoryStat = new MemoryStat("foo", 1, 2, 3, 4, 5, 6, 7, 8);
        List<MemoryStat> memoryStats = new LinkedList<>();
        memoryStats.add(memoryStat);
        MemoryStatDAO memoryStatDAO = mock(MemoryStatDAO.class);
        when(memoryStatDAO.getMemoryStats(any(HostRef.class), anyLong(), anyLong())).thenReturn(memoryStats);
        when(memoryStatDAO.getOldest(any(HostRef.class))).thenReturn(memoryStat);
        when(memoryStatDAO.getNewest(any(HostRef.class))).thenReturn(memoryStat);

        Timer timer = mock(Timer.class);
        ArgumentCaptor<Runnable> timerActionCaptor = ArgumentCaptor.forClass(Runnable.class);
        doNothing().when(timer).setAction(timerActionCaptor.capture());

        TimerFactory timerFactory = mock(TimerFactory.class);
        when(timerFactory.createTimer()).thenReturn(timer);
        ApplicationService appSvc = mock(ApplicationService.class);
        when(appSvc.getTimerFactory()).thenReturn(timerFactory);

        HostRef ref = mock(HostRef.class);

        HostMemoryView view = mock(HostMemoryView.class);
        when(view.getUserDesiredDuration()).thenReturn(new Duration(10, TimeUnit.MINUTES));
        ArgumentCaptor<ActionListener> viewArgumentCaptor = ArgumentCaptor.forClass(ActionListener.class);
        doNothing().when(view).addActionListener(viewArgumentCaptor.capture());

        HostMemoryViewProvider viewProvider = mock(HostMemoryViewProvider.class);
        when(viewProvider.createView()).thenReturn(view);

        @SuppressWarnings("unused")
        HostMemoryController controller = new HostMemoryController(appSvc, hostInfoDAO, memoryStatDAO, ref, viewProvider);

        ActionListener<HostMemoryView.Action> l = viewArgumentCaptor.getValue();

        l.actionPerformed(new ActionEvent<>(view, HostMemoryView.Action.VISIBLE));

        verify(timer).start();
        timerActionCaptor.getValue().run();

        verify(view, times(1)).setTotalMemory(eq(TOTAL_MEMORY + " B"));
        verify(view, times(6)).addMemoryData(any(String.class), any(List.class));

        l.actionPerformed(new ActionEvent<>(view, HostMemoryView.Action.HIDDEN));

        verify(timer).stop();

    }
}

