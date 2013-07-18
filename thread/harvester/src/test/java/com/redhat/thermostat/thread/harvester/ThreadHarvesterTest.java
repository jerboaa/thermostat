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

package com.redhat.thermostat.thread.harvester;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.concurrent.ScheduledExecutorService;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import com.redhat.thermostat.common.Clock;
import com.redhat.thermostat.common.Pair;
import com.redhat.thermostat.common.command.Request;
import com.redhat.thermostat.common.command.Response;
import com.redhat.thermostat.common.command.Response.ResponseType;
import com.redhat.thermostat.thread.collector.HarvesterCommand;
import com.redhat.thermostat.thread.dao.ThreadDao;
import com.redhat.thermostat.thread.model.ThreadHarvestingStatus;
import com.redhat.thermostat.utils.management.MXBeanConnectionPool;

public class ThreadHarvesterTest {

    private MXBeanConnectionPool pool;
    private ScheduledExecutorService executor;

    @Before
    public void setUp() {
        pool = mock(MXBeanConnectionPool.class);
        executor = mock(ScheduledExecutorService.class);
    }

    @Test
    public void testStart() {
        ThreadDao dao = mock(ThreadDao.class);
        Request request = mock(Request.class);
        
        final boolean[] createHarvesterCalled = new boolean[1];
        final Harvester harverster = mock(Harvester.class);
        when(harverster.start()).thenReturn(true);
        
        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        
        when(request.getParameter(captor.capture())).
            thenReturn(HarvesterCommand.START.name()).
            thenReturn("vmId").
            thenReturn("42").
            thenReturn("0xcafe");
        
        ThreadHarvester threadHarvester = new ThreadHarvester(executor, pool) {
            @Override
            Harvester createHarvester(String vmId, int pid) {
                
                createHarvesterCalled[0] = true;
                assertEquals("vmId", vmId);
                assertEquals(42, pid);
                
                return harverster;
            }
        };
        threadHarvester.setThreadDao(dao);
        threadHarvester.receive(request);
        
        List<String> values = captor.getAllValues();
        assertEquals(3, values.size());
        
        assertEquals(HarvesterCommand.class.getName(), values.get(0));
        assertEquals(HarvesterCommand.VM_ID.name(), values.get(1));
        assertEquals(HarvesterCommand.VM_PID.name(), values.get(2));
        
        assertTrue(createHarvesterCalled[0]);
        
        verify(harverster).start();
    }

    @Test
    public void testStop() {
        ThreadDao dao = mock(ThreadDao.class);
        Request request = mock(Request.class);
        
        final Harvester harverster = mock(Harvester.class);
        
        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        
        when(request.getParameter(captor.capture())).
            thenReturn(HarvesterCommand.STOP.name()).
            thenReturn("vmId");
        
        ThreadHarvester threadHarvester = new ThreadHarvester(executor, pool) {
            { connectors.put("vmId", harverster); }
        };
        threadHarvester.setThreadDao(dao);
        threadHarvester.receive(request);
        
        List<String> values = captor.getAllValues();
        assertEquals(2, values.size());
        
        assertEquals(HarvesterCommand.class.getName(), values.get(0));
        assertEquals(HarvesterCommand.VM_ID.name(), values.get(1));
                
        verify(harverster).stop();        
    }
    
    @Test
    public void testFindDeadLocks() {
        ThreadDao dao = mock(ThreadDao.class);
        Request request = mock(Request.class);

        final boolean[] createHarvesterCalled = new boolean[1];
        final Harvester harverster = mock(Harvester.class);
        when(harverster.start()).thenReturn(true);

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);

        when(request.getParameter(captor.capture())).
                thenReturn(HarvesterCommand.FIND_DEADLOCKS.name()).
                thenReturn("vmId").
                thenReturn("42").
                thenReturn("0xcafe");

        ThreadHarvester threadHarvester = new ThreadHarvester(executor, pool) {
            @Override
            Harvester createHarvester(String vmId, int pid) {

                createHarvesterCalled[0] = true;
                assertEquals("vmId", vmId);
                assertEquals(42, pid);

                return harverster;
            }
        };
        threadHarvester.setThreadDao(dao);
        threadHarvester.receive(request);

        List<String> values = captor.getAllValues();
        assertEquals(3, values.size());

        assertEquals(HarvesterCommand.class.getName(), values.get(0));
        assertEquals(HarvesterCommand.VM_ID.name(), values.get(1));
        assertEquals(HarvesterCommand.VM_PID.name(), values.get(2));

        assertTrue(createHarvesterCalled[0]);

        verify(harverster).saveDeadLockData();
    }

    @Test
    public void testSaveVmCaps() {
        ThreadDao dao = mock(ThreadDao.class);
        
        final boolean[] createHarvesterCalled = new boolean[1];
        final Harvester harverster = mock(Harvester.class);
        
        ThreadHarvester threadHarvester = new ThreadHarvester(executor, pool) {
            @Override
            Harvester createHarvester(String vmId, int pid) {
                
                createHarvesterCalled[0] = true;
                assertEquals("vmId", vmId);
                assertEquals(42, pid);
                
                return harverster;
            }
        };
        threadHarvester.setThreadDao(dao);
        threadHarvester.saveVmCaps("vmId", 42);
        
        assertTrue(createHarvesterCalled[0]);
        
        verify(harverster).saveVmCaps();
    }    

    @Test
    public void testRecieveWithoutDaosFails() {
        ThreadHarvester harvester = new ThreadHarvester(executor, pool);
        Response response = harvester.receive(mock(Request.class));

        assertEquals(ResponseType.ERROR, response.getType());
    }

    @Test
    public void testHarvestingStatus() {
        Clock clock = mock(Clock.class);
        when(clock.getRealTimeMillis()).thenReturn(1l);
        ThreadDao dao = mock(ThreadDao.class);

        ThreadHarvester harvester = new ThreadHarvester(executor, clock, pool);
        harvester.setThreadDao(dao);

        harvester.addThreadHarvestingStatus("vmId");

        ArgumentCaptor<ThreadHarvestingStatus> statusCaptor = ArgumentCaptor.forClass(ThreadHarvestingStatus.class);
        verify(dao).saveHarvestingStatus(statusCaptor.capture());

        ThreadHarvestingStatus status = statusCaptor.getValue();
        assertEquals("vmId", status.getVmId());
        assertEquals(false, status.isHarvesting());
        assertEquals(1, status.getTimeStamp());
    }

    @Test
    public void testHarvestingStatusAfterSavingVmCaps() {
        Clock clock = mock(Clock.class);
        when(clock.getRealTimeMillis()).thenReturn(1l);
        ThreadDao dao = mock(ThreadDao.class);

        ThreadHarvester harvester = new ThreadHarvester(executor, clock, pool);
        harvester.setThreadDao(dao);

        harvester.saveVmCaps("vmId", 10);
        harvester.addThreadHarvestingStatus("vmId");

        ArgumentCaptor<ThreadHarvestingStatus> statusCaptor = ArgumentCaptor.forClass(ThreadHarvestingStatus.class);
        verify(dao).saveHarvestingStatus(statusCaptor.capture());

        ThreadHarvestingStatus status = statusCaptor.getValue();
        assertEquals("vmId", status.getVmId());
        assertEquals(false, status.isHarvesting());
        assertEquals(1, status.getTimeStamp());
    }

    @Test
    public void testStopAndRemoveAll() {
        Clock clock = mock(Clock.class);
        when(clock.getRealTimeMillis()).thenReturn(1l);
        ThreadDao dao = mock(ThreadDao.class);

        final Harvester javaHarvester = mock(Harvester.class);
        when(javaHarvester.start()).thenReturn(true);
        when(javaHarvester.stop()).thenReturn(true);
        when(javaHarvester.getPid()).thenReturn(42);

        ThreadHarvester harvester = new ThreadHarvester(executor, clock, pool) {
            @Override
            Harvester createHarvester(String vmId, int pid) {
                assertEquals("vmId", vmId);
                assertEquals(42, pid);

                return javaHarvester;
            }
        };
        
        harvester.setThreadDao(dao);
        harvester.startHarvester("vmId", 42);
        
        // Reset DAO
        harvester.setThreadDao(dao);

        assertTrue(harvester.startHarvester("vmId", 42));

        List<Pair<String, Integer>> allSaved = harvester.stopAndRemoveAllHarvesters();
        assertEquals(1, allSaved.size());
        Pair<String, Integer> saved = allSaved.get(0);
        assertEquals("vmId", saved.getFirst());
        assertEquals(42, saved.getSecond().intValue());
    }
}

