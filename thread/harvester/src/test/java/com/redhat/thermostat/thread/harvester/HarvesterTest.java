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

package com.redhat.thermostat.thread.harvester;

import com.redhat.thermostat.agent.utils.management.MXBeanConnection;
import com.redhat.thermostat.agent.utils.management.MXBeanConnectionPool;
import com.redhat.thermostat.storage.core.WriterID;
import com.redhat.thermostat.thread.dao.ThreadDao;
import com.redhat.thermostat.thread.model.ThreadSession;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class HarvesterTest {

    private WriterID writerId;
    private int pid;

    private ScheduledExecutorService executor;
    private ThreadDao dao;
    private MXBeanConnectionPool pool;
    private MXBeanConnection connection;
    private DeadlockHelper deadlockHelper;

    @Before
    public void setup() throws Exception {
        writerId = mock(WriterID.class);
        pid = 42;

        int pid = 42;

        deadlockHelper = mock(DeadlockHelper.class);

        executor = mock(ScheduledExecutorService.class);
        dao = mock(ThreadDao.class);
        pool = mock(MXBeanConnectionPool.class);
        connection = mock(MXBeanConnection.class);
        when(pool.acquire(pid)).thenReturn(connection);
    }

    @Test
    public void testStart() throws Exception {

        ArgumentCaptor<Runnable> arg0 = ArgumentCaptor.forClass(Runnable.class);
        ArgumentCaptor<Long> arg1 = ArgumentCaptor.forClass(Long.class);
        ArgumentCaptor<Long> arg2 = ArgumentCaptor.forClass(Long.class);
        ArgumentCaptor<TimeUnit> arg3 = ArgumentCaptor.forClass(TimeUnit.class);

        when(executor.scheduleAtFixedRate(arg0.capture(), arg1.capture(),
                                          arg2.capture(), arg3.capture())).
            thenReturn(null);

        HarvesterHelper helper = mock(HarvesterHelper.class);
        Harvester harvester = new Harvester(pid, executor, pool, helper,
                                            deadlockHelper);

        harvester.start();

        verify(executor).scheduleAtFixedRate(any(Runnable.class), anyLong(),
                                             anyLong(), any(TimeUnit.class));
        verify(pool).acquire(pid);

        assertTrue(arg1.getValue() == Harvester.DEFAULT_INITIAL_DELAY);
        assertTrue(arg2.getValue() == Harvester.DEFAULT_PERIOD);
        assertEquals(Harvester.DEFAULT_TIME_UNIT, arg3.getValue());

        Runnable action = arg0.getValue();
        assertNotNull(action);
        assertTrue(harvester.isConnected());
    }

    @Test
    public void testStartTwice() throws Exception {
        // Mostly the same as testStart, but we call harvester.start() twice

        HarvesterHelper helper = mock(HarvesterHelper.class);
        Harvester harvester = new Harvester(pid, executor, pool, helper,
                                            deadlockHelper);

        harvester.start();
        harvester.start();

        verify(executor, times(1)).scheduleAtFixedRate(any(Runnable.class),
                                                       anyLong(),
                                                       anyLong(),
                                                       any(TimeUnit.class));
        assertTrue(harvester.isConnected());
        verify(pool, times(1)).acquire(pid);
    }

    @Test
    public void testStopAfterStarting() throws Exception {
        // Calls start and then stop

        ScheduledFuture future = mock(ScheduledFuture.class);
        when(executor.scheduleAtFixedRate(any(Runnable.class), anyLong(),
                                          anyLong(), any(TimeUnit.class))).
            thenReturn(future);

        HarvesterHelper helper = mock(HarvesterHelper.class);
        Harvester harvester = new Harvester(pid, executor, pool, helper,
                                            deadlockHelper);

        harvester.start();
        assertTrue(harvester.isConnected());
        verify(pool).acquire(pid);

        harvester.stop();
        assertFalse(harvester.isConnected());
        verify(pool).release(pid, connection);
        verify(future).cancel(false);
    }

    @Test
    public void testStopTwiceAfterStarting() throws Exception {
        // like before, but stop is called twice

        ScheduledFuture future = mock(ScheduledFuture.class);
        when(executor.scheduleAtFixedRate(any(Runnable.class), anyLong(),
                                          anyLong(), any(TimeUnit.class))).
            thenReturn(future);

        HarvesterHelper helper = mock(HarvesterHelper.class);
        Harvester harvester = new Harvester(pid, executor, pool, helper,
                                            deadlockHelper);

        harvester.start();
        assertTrue(harvester.isConnected());
        verify(pool).acquire(pid);

        harvester.stop();
        harvester.stop();

        assertFalse(harvester.isConnected());
        verify(pool, times(1)).release(pid, connection);
        verify(future, times(1)).cancel(false);
    }

    @Test
    public void testStopNotStarted() throws Exception {
        // calls stop on an harvester that was never started

        ScheduledFuture future = mock(ScheduledFuture.class);
        when(executor.scheduleAtFixedRate(any(Runnable.class), anyLong(),
                                          anyLong(), any(TimeUnit.class))).
            thenReturn(future);

        HarvesterHelper helper = mock(HarvesterHelper.class);
        Harvester harvester = new Harvester(pid, executor, pool, helper,
                                            deadlockHelper);

        assertFalse(harvester.isConnected());

        harvester.stop();

        assertFalse(harvester.isConnected());
        verify(pool, times(0)).release(pid, connection);
        verify(future, times(0)).cancel(false);
    }

    @Test
    public void testHarvestingLoopWithSunBean() throws Exception {
        // test that the harvester loop calls the appropriate helpers

        com.sun.management.ThreadMXBean sunBean =
                mock(com.sun.management.ThreadMXBean.class);

        ArgumentCaptor<Runnable> arg0 = ArgumentCaptor.forClass(Runnable.class);
        ScheduledFuture future = mock(ScheduledFuture.class);
        when(executor.scheduleAtFixedRate(arg0.capture(), anyLong(),
                                          anyLong(), any(TimeUnit.class))).
            thenReturn(future);


        // simulate the sun bean first
        when(connection.createProxy(ManagementFactory.THREAD_MXBEAN_NAME,
                                    com.sun.management.ThreadMXBean.class)).
            thenReturn(sunBean);


        HarvesterHelper helper = mock(HarvesterHelper.class);
        Harvester harvester = new Harvester(pid, executor, pool, helper,
                                            deadlockHelper);

        harvester.start();

        Runnable harvesterRunnable = arg0.getValue();
        assertNotNull(harvesterRunnable);

        harvesterRunnable.run();

        verify(helper).collectAndSaveThreadData(any(ThreadSession.class), eq(sunBean));
    }

    @Test
    public void testHarvestingLoopWithMXBean() throws Exception {
        // test that the harvester loop calls the appropriate helpers,
        // this time with the standard mxbean

        ThreadMXBean mxBean = mock(ThreadMXBean.class);

        ArgumentCaptor<Runnable> arg0 = ArgumentCaptor.forClass(Runnable.class);
        ScheduledFuture future = mock(ScheduledFuture.class);
        when(executor.scheduleAtFixedRate(arg0.capture(), anyLong(),
                                          anyLong(), any(TimeUnit.class))).
            thenReturn(future);

        // simulate the sun bean first
        when(connection.createProxy(ManagementFactory.THREAD_MXBEAN_NAME,
                                    com.sun.management.ThreadMXBean.class)).
                thenReturn(null);
        when(connection.createProxy(ManagementFactory.THREAD_MXBEAN_NAME,
                                    ThreadMXBean.class)).
                thenReturn(mxBean);

        HarvesterHelper helper = mock(HarvesterHelper.class);

        Harvester harvester = new Harvester(pid, executor, pool, helper,
                                            deadlockHelper);

        harvester.start();

        Runnable harvesterRunnable = arg0.getValue();
        assertNotNull(harvesterRunnable);

        harvesterRunnable.run();

        verify(helper).collectAndSaveThreadData(any(ThreadSession.class), eq(mxBean));
    }

    @Test
    public void testSaveDeadlockData() {

        final ThreadMXBean mxBean = mock(ThreadMXBean.class);
        HarvesterHelper helper = mock(HarvesterHelper.class);

        Harvester harvester = new Harvester(pid, executor, pool, helper,
                                            deadlockHelper)
        {
            @Override
            ThreadMXBean getDataCollectorBean(MXBeanConnection connection) {
                return mxBean;
            }

            @Override
            synchronized boolean isConnected() {
                return true;
            }
        };

        harvester.saveDeadLockData();

        verify(deadlockHelper).saveDeadlockInformation(mxBean);
    }

    @Test
    public void testSaveDeadlockDataUnconnectedWillDisconnectAgain() throws Exception {

        final ThreadMXBean mxBean = mock(ThreadMXBean.class);
        HarvesterHelper helper = mock(HarvesterHelper.class);

        Harvester harvester = new Harvester(pid, executor, pool, helper,
                                            deadlockHelper)
        {
            @Override
            ThreadMXBean getDataCollectorBean(MXBeanConnection connection) {
                return mxBean;
            }
        };

        harvester.saveDeadLockData();

        verify(pool).acquire(pid);
        verify(deadlockHelper).saveDeadlockInformation(mxBean);
        verify(pool).release(pid, connection);
    }
}

