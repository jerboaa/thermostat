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

package com.redhat.thermostat.thread.harvester;

import com.redhat.thermostat.common.SystemClock;
import com.redhat.thermostat.storage.core.WriterID;
import com.redhat.thermostat.thread.dao.ThreadDao;
import com.redhat.thermostat.thread.model.VmDeadLockData;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class DeadlockHelperTest {

    private static final String DEFAULT_W_ID = "0xcafe";
    private static final long TIME_IN_MILLIS = 101010l;

    private String vmId;
    private WriterID writerId;
    private ThreadDao threadDao;
    private SystemClock clock;

    private ThreadMXBean collectorBean;

    @Before
    public void setUp() throws Exception {

        clock = mock(SystemClock.class);
        when(clock.getRealTimeMillis()).thenReturn(TIME_IN_MILLIS);

        vmId = "testVM";
        writerId  = mock(WriterID.class);
        when(writerId.getWriterID()).thenReturn(DEFAULT_W_ID);

        threadDao = mock(ThreadDao.class);

        collectorBean = mock(ThreadMXBean.class);
    }

    @Test
    public void testSaveDeadlockInformation() throws Exception {

        long[] infos = new long[] { -1, 0, 1 };

        ThreadInfo[] threadInfo = new ThreadInfo[0];
        when(collectorBean.findDeadlockedThreads()).thenReturn(infos);
        when(collectorBean.getThreadInfo(infos, true, true)).thenReturn(threadInfo);

        ArgumentCaptor<VmDeadLockData> deadLockCapture = ArgumentCaptor.forClass(VmDeadLockData.class);

        DeadlockHelper deadlockHelper = new DeadlockHelper(threadDao, clock, vmId, writerId);

        deadlockHelper.saveDeadlockInformation(collectorBean);
        verify(threadDao).saveDeadLockStatus(deadLockCapture.capture());

        verify(collectorBean).getThreadInfo(infos, true, true);
        verify(collectorBean).findDeadlockedThreads();

        VmDeadLockData data = deadLockCapture.getValue();

        assertEquals(vmId, data.getVmId());
        assertEquals(TIME_IN_MILLIS, data.getTimeStamp());
        assertEquals("", data.getDeadLockDescription());
    }
}
