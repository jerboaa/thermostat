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

package com.redhat.thermostat.host.memory.agent.internal;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import com.redhat.thermostat.common.Version;
import com.redhat.thermostat.host.memory.common.MemoryStatDAO;
import com.redhat.thermostat.host.memory.common.model.MemoryStat;
import com.redhat.thermostat.storage.core.WriterID;

public class HostMemoryBackendTest {
    
    private HostMemoryBackend backend;
    private ScheduledExecutorService executor;
    private MemoryStatDAO memoryStatDao;

    @Before
    public void setup() {
        executor = mock(ScheduledExecutorService.class);
        memoryStatDao = mock(MemoryStatDAO.class);
        
        Version version = mock(Version.class);
        when(version.getVersionNumber()).thenReturn("0.0.0");
        WriterID id = mock(WriterID.class);
        
        backend = new HostMemoryBackend(executor, memoryStatDao, version, id);
    }

    @Test
    public void testStart() {
        backend.activate();
        ArgumentCaptor<Runnable> captor = ArgumentCaptor.forClass(Runnable.class);
        verify(executor).scheduleAtFixedRate(captor.capture(), any(Long.class), any(Long.class), any(TimeUnit.class));
        assertTrue(backend.isActive());
        
        Runnable runnable = captor.getValue();
        runnable.run();
        verify(memoryStatDao).putMemoryStat(any(MemoryStat.class));
    }
    
    @Test
    public void testStop() {
        backend.activate();
        backend.deactivate();
        verify(executor).shutdown();
        assertFalse(backend.isActive());
    }
}

