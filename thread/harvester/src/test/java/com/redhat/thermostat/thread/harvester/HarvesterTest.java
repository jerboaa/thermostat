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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import javax.management.MalformedObjectNameException;

import org.junit.Test;
import org.mockito.ArgumentCaptor;

import com.redhat.thermostat.common.Clock;
import com.redhat.thermostat.thread.dao.ThreadDao;
import com.redhat.thermostat.thread.model.ThreadInfoData;
import com.redhat.thermostat.thread.model.ThreadSummary;
import com.redhat.thermostat.thread.model.VMThreadCapabilities;
import com.redhat.thermostat.thread.model.VmDeadLockData;
import com.redhat.thermostat.utils.management.MXBeanConnection;
import com.redhat.thermostat.utils.management.MXBeanConnectionPool;

public class HarvesterTest {

    @Test
    public void testStart() {
        ScheduledExecutorService executor = mock(ScheduledExecutorService.class);
        ThreadDao dao = mock(ThreadDao.class);
        MXBeanConnectionPool pool = mock(MXBeanConnectionPool.class);

        ArgumentCaptor<Runnable> arg0 = ArgumentCaptor.forClass(Runnable.class);
        ArgumentCaptor<Long> arg1 = ArgumentCaptor.forClass(Long.class);
        ArgumentCaptor<Long> arg2 = ArgumentCaptor.forClass(Long.class);
        ArgumentCaptor<TimeUnit> arg3 = ArgumentCaptor.forClass(TimeUnit.class);
        
        final boolean [] harvestDataCalled = new boolean[1];
        
        when(executor.scheduleAtFixedRate(arg0.capture(), arg1.capture(), arg2.capture(), arg3.capture())).thenReturn(null);
        
        Harvester harvester = new Harvester(dao, executor, "42", pool) {
            @Override
            synchronized void harvestData() {
                harvestDataCalled[0] = true;
            }
        };
        
        harvester.start();
        
        verify(executor).scheduleAtFixedRate(any(Runnable.class), anyLong(), anyLong(), any(TimeUnit.class));
        
        assertTrue(arg1.getValue() == 0);
        assertTrue(arg2.getValue() == 250);
        assertEquals(TimeUnit.MILLISECONDS, arg3.getValue());
        
        Runnable action = arg0.getValue();
        assertNotNull(action);
        
        action.run();
        
        assertTrue(harvestDataCalled[0]);
        
        assertTrue(harvester.isConnected());
    }
    
    /**
     *  Mostly the same as testStart, but we call harvester.start() twice
     */
    @Test
    public void testStartOnce() throws Exception {

        ScheduledExecutorService executor = mock(ScheduledExecutorService.class);
        ThreadDao dao = mock(ThreadDao.class);
        MXBeanConnectionPool pool = mock(MXBeanConnectionPool.class);
        
        ArgumentCaptor<Runnable> arg0 = ArgumentCaptor.forClass(Runnable.class);
        ArgumentCaptor<Long> arg1 = ArgumentCaptor.forClass(Long.class);
        ArgumentCaptor<Long> arg2 = ArgumentCaptor.forClass(Long.class);
        ArgumentCaptor<TimeUnit> arg3 = ArgumentCaptor.forClass(TimeUnit.class);
        
        final boolean [] harvestDataCalled = new boolean[1];
        
        when(executor.scheduleAtFixedRate(arg0.capture(), arg1.capture(), arg2.capture(), arg3.capture())).thenReturn(null);
        
        Harvester harvester = new Harvester(dao, executor, "42", pool) {
            @Override
            synchronized void harvestData() {
                harvestDataCalled[0] = true;
            }
        };
        
        harvester.start();
        harvester.start();

        verify(executor, times(1)).scheduleAtFixedRate(any(Runnable.class), anyLong(), anyLong(), any(TimeUnit.class));
        
        assertTrue(arg1.getValue() == 0);
        assertTrue(arg2.getValue() == 250);
        assertEquals(TimeUnit.MILLISECONDS, arg3.getValue());
        
        Runnable action = arg0.getValue();
        assertNotNull(action);
        
        action.run();
        
        assertTrue(harvestDataCalled[0]);
        
        assertTrue(harvester.isConnected());
    }
    
    @Test
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public void testStopAfterStarting() throws Exception {
        
        ScheduledFuture future = mock(ScheduledFuture.class);
        
        ScheduledExecutorService executor = mock(ScheduledExecutorService.class);
        ThreadDao dao = mock(ThreadDao.class);
        MXBeanConnectionPool pool = mock(MXBeanConnectionPool.class);
        MXBeanConnection connection = mock(MXBeanConnection.class);
        when(pool.acquire(42)).thenReturn(connection);
        
        when(executor.scheduleAtFixedRate(any(Runnable.class), anyLong(), anyLong(), any(TimeUnit.class))).thenReturn(future);
        
        Harvester harvester = new Harvester(dao, executor, "42", pool);
        
        harvester.start();
        
        assertTrue(harvester.isConnected());
        
        harvester.stop();
        
        verify(future).cancel(false);
        
        verify(pool).acquire(42);
        verify(pool).release(42, connection);
        
        assertFalse(harvester.isConnected());
    }
    
    /**
     *  Mostly the same as testStopAfterStarting, but we call harvester.stop()
     *  twice
     */
    @Test
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public void testStopTwiceAfterStarting() throws Exception {
        
        ScheduledFuture future = mock(ScheduledFuture.class);
        
        ScheduledExecutorService executor = mock(ScheduledExecutorService.class);
        ThreadDao dao = mock(ThreadDao.class);
        MXBeanConnectionPool pool = mock(MXBeanConnectionPool.class);
        MXBeanConnection connection = mock(MXBeanConnection.class);
        when(pool.acquire(42)).thenReturn(connection);
        
        when(executor.scheduleAtFixedRate(any(Runnable.class), anyLong(), anyLong(), any(TimeUnit.class))).thenReturn(future);
        
        Harvester harvester = new Harvester(dao, executor, "42", pool);
        
        harvester.start();
        
        assertTrue(harvester.isConnected());
        
        harvester.stop();
        harvester.stop();

        verify(future, times(1)).cancel(false);
        
        verify(pool).acquire(42);
        verify(pool).release(42, connection);
        
        assertFalse(harvester.isConnected());
    }
    
    @Test
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public void testStopNotStarted() throws Exception {
        ScheduledExecutorService executor = mock(ScheduledExecutorService.class);
        ThreadDao dao = mock(ThreadDao.class);
        
        ScheduledFuture future = mock(ScheduledFuture.class);
        when(executor.scheduleAtFixedRate(any(Runnable.class), anyLong(), anyLong(), any(TimeUnit.class))).thenReturn(future);
        
        MXBeanConnectionPool pool = mock(MXBeanConnectionPool.class);

        Harvester harvester = new Harvester(dao, executor, "42", pool);
        
        verify(executor, times(0)).scheduleAtFixedRate(any(Runnable.class), anyLong(), anyLong(), any(TimeUnit.class));
        
        assertFalse(harvester.isConnected());
        
        harvester.stop();

        assertFalse(harvester.isConnected());
        
        verify(future, times(0)).cancel(false);
    }
    
    @Test
    public void testHarvestData() {
        
        long ids[] = new long [] {
            0, 1
        };
        
        ThreadInfo info1 = mock(ThreadInfo.class);
        when(info1.getThreadName()).thenReturn("fluff1");
        when(info1.getThreadId()).thenReturn(1l);
        when(info1.getThreadState()).thenReturn(Thread.State.RUNNABLE);
        
        ThreadInfo info2 = mock(ThreadInfo.class);
        when(info2.getThreadName()).thenReturn("fluff2");
        when(info2.getThreadId()).thenReturn(2l);
        when(info2.getThreadState()).thenReturn(Thread.State.BLOCKED);

        ThreadInfo[] infos = new ThreadInfo[] {
            info1,
            info2
        };

        MXBeanConnectionPool pool = mock(MXBeanConnectionPool.class);
        ScheduledExecutorService executor = mock(ScheduledExecutorService.class);

        ArgumentCaptor<ThreadSummary> summaryCapture = ArgumentCaptor.forClass(ThreadSummary.class);

        ThreadDao dao = mock(ThreadDao.class);
        doNothing().when(dao).saveSummary(summaryCapture.capture());

        ArgumentCaptor<ThreadInfoData> threadInfoCapture = ArgumentCaptor.forClass(ThreadInfoData.class);        
        doNothing().when(dao).saveThreadInfo(threadInfoCapture.capture());

        final ThreadMXBean collectorBean = mock(ThreadMXBean.class);

        when(collectorBean.getThreadCount()).thenReturn(42);
        when(collectorBean.getAllThreadIds()).thenReturn(ids);
        when(collectorBean.getThreadInfo(ids, true, true)).thenReturn(infos);

        final boolean [] getDataCollectorBeanCalled = new boolean[1];
        
        Harvester harvester = new Harvester(dao, executor, "42", pool) {
            @Override
            ThreadMXBean getDataCollectorBean(MXBeanConnection connection)
                    throws MalformedObjectNameException {
                getDataCollectorBeanCalled[0] = true;
                return collectorBean;
            }
        };

        harvester.harvestData();
        
        assertTrue(getDataCollectorBeanCalled[0]);
        
        verify(collectorBean).getThreadInfo(ids, true, true);
        
        verify(dao).saveSummary(any(ThreadSummary.class));
        
        // once for each thread info
        verify(dao, times(2)).saveThreadInfo(any(ThreadInfoData.class));
        
        assertEquals(42, summaryCapture.getValue().getCurrentLiveThreads());
        assertEquals(42, summaryCapture.getValue().getVmId());
        
        assertEquals(42, summaryCapture.getValue().getCurrentLiveThreads());
        assertEquals(42, summaryCapture.getValue().getVmId());

        List<ThreadInfoData> threadInfos = threadInfoCapture.getAllValues();
        assertEquals(2, threadInfos.size());
        
        assertEquals("fluff1", threadInfos.get(0).getThreadName());
        assertEquals("fluff2", threadInfos.get(1).getThreadName());
        
        assertEquals("RUNNABLE", threadInfos.get(0).getThreadState());
        assertEquals("BLOCKED", threadInfos.get(1).getThreadState());
        assertEquals(Thread.State.RUNNABLE, threadInfos.get(0).getState());
        assertEquals(Thread.State.BLOCKED, threadInfos.get(1).getState());

        verify(collectorBean, times(1)).getThreadCpuTime(1l);
        verify(collectorBean, times(1)).getThreadCpuTime(2l);
    }
    
    @Test
    public void testSaveVmCaps() {

        MXBeanConnectionPool pool = mock(MXBeanConnectionPool.class);
        
        ScheduledExecutorService executor = mock(ScheduledExecutorService.class);

        ArgumentCaptor<VMThreadCapabilities> capsCapture = ArgumentCaptor.forClass(VMThreadCapabilities.class);

        ThreadDao dao = mock(ThreadDao.class);
        doNothing().when(dao).saveCapabilities(capsCapture.capture());
      
        final ThreadMXBean collectorBean = mock(ThreadMXBean.class);
        when(collectorBean.isThreadCpuTimeSupported()).thenReturn(true);
        when(collectorBean.isThreadContentionMonitoringSupported()).thenReturn(true);

        final boolean [] getDataCollectorBeanCalled = new boolean[1];
        
        Harvester harvester = new Harvester(dao, executor, "42", pool) {
            @Override
            ThreadMXBean getDataCollectorBean(MXBeanConnection connection)
                    throws MalformedObjectNameException {
                getDataCollectorBeanCalled[0] = true;
                return collectorBean;
            }
        };

        harvester.saveVmCaps();
        assertTrue(getDataCollectorBeanCalled[0]);
        
        verify(dao, times(1)).saveCapabilities(any(VMThreadCapabilities.class));
        assertEquals(42, capsCapture.getValue().getVmId());

        String[] features = capsCapture.getValue().getSupportedFeaturesList();
        assertEquals(2, features.length);
        assertEquals(ThreadDao.CPU_TIME, features[0]);
        assertEquals(ThreadDao.CONTENTION_MONITOR, features[1]);
    }    

    @Test
    public void testCheckForDeadLocks() {
        MXBeanConnectionPool pool = mock(MXBeanConnectionPool.class);

        ScheduledExecutorService executor = mock(ScheduledExecutorService.class);

        ArgumentCaptor<VmDeadLockData> deadLockCapture = ArgumentCaptor.forClass(VmDeadLockData.class);

        ThreadDao dao = mock(ThreadDao.class);

        ThreadInfo[] threadInfo = new ThreadInfo[0];

        final ThreadMXBean collectorBean = mock(ThreadMXBean.class);
        when(collectorBean.findDeadlockedThreads()).thenReturn(new long[] { -1, 0, 1 });
        when(collectorBean.getThreadInfo(new long[] { -1, 0, 1 }, true, true)).thenReturn(threadInfo);

        Clock clock = mock(Clock.class);
        when(clock.getRealTimeMillis()).thenReturn(101010l);

        final boolean[] getDataCollectorBeanCalled = new boolean[1];

        Harvester harvester = new Harvester(dao, executor, clock, "42", pool) {
            @Override
            ThreadMXBean getDataCollectorBean(MXBeanConnection connection)
                    throws MalformedObjectNameException {
                getDataCollectorBeanCalled[0] = true;
                return collectorBean;
            }
        };

        harvester.saveDeadLockData();
        assertTrue(getDataCollectorBeanCalled[0]);

        verify(dao).saveDeadLockStatus(deadLockCapture.capture());

        VmDeadLockData data = deadLockCapture.getValue();

        assertEquals(42, data.getVmId());
        assertEquals(101010l, data.getTimeStamp());
        assertEquals("", data.getDeadLockDescription());
    }
}

