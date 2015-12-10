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

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import com.redhat.thermostat.backend.VmUpdate;
import com.redhat.thermostat.backend.VmUpdateException;
import com.redhat.thermostat.common.Clock;
import com.redhat.thermostat.thread.dao.LockInfoDao;
import com.redhat.thermostat.thread.model.LockInfo;

public class LockInfoUpdaterTest {

    private LockInfoUpdater lockInfoUpdater;

    private Clock clock;
    private LockInfoDao lockInfoDao;
    private VmUpdate update;

    private static final String AGENT_ID = "agent-id";
    private static final String VM_ID = "vm-id";
    private static final long TIMESTAMP = 1234l;

    @Before
    public void setup() throws VmUpdateException {
        clock = mock(Clock.class);
        lockInfoDao = mock(LockInfoDao.class);
        lockInfoUpdater = new LockInfoUpdater(clock, lockInfoDao, AGENT_ID, VM_ID);

        update = mock(VmUpdate.class);
    }

    @Test
    public void verifyMissingValuesPopulatesWithUnknown() throws Exception {
        when(clock.getRealTimeMillis()).thenReturn(TIMESTAMP);

        VmUpdateException counterNotFound = new VmUpdateException();
        when(update.getPerformanceCounterLong(anyString())).thenThrow(counterNotFound);

        ArgumentCaptor<LockInfo> runCaptor = ArgumentCaptor.forClass(LockInfo.class);
        lockInfoUpdater.countersUpdated(update);

        verify(lockInfoDao).saveLockInfo(runCaptor.capture());

        LockInfo value = runCaptor.getValue();
        assertEquals(AGENT_ID, value.getAgentId());
        assertEquals(VM_ID, value.getVmId());
        assertEquals(TIMESTAMP, value.getTimeStamp());
        assertEquals(LockInfo.UNKNOWN, value.getContendedLockAttempts());
        assertEquals(LockInfo.UNKNOWN, value.getDeflations());
        assertEquals(LockInfo.UNKNOWN, value.getEmptyNotifications());
        assertEquals(LockInfo.UNKNOWN, value.getFailedSpins());
        assertEquals(LockInfo.UNKNOWN, value.getFutileWakeups());
        assertEquals(LockInfo.UNKNOWN, value.getInflations());
        assertEquals(LockInfo.UNKNOWN, value.getMonExtant());
        assertEquals(LockInfo.UNKNOWN, value.getMonInCirculation());
        assertEquals(LockInfo.UNKNOWN, value.getMonScavenged());
        assertEquals(LockInfo.UNKNOWN, value.getNotifications());
        assertEquals(LockInfo.UNKNOWN, value.getParks());
        assertEquals(LockInfo.UNKNOWN, value.getPrivateA());
        assertEquals(LockInfo.UNKNOWN, value.getPrivateB());
        assertEquals(LockInfo.UNKNOWN, value.getSlowEnter());
        assertEquals(LockInfo.UNKNOWN, value.getSlowExit());
        assertEquals(LockInfo.UNKNOWN, value.getSlowNotify());
        assertEquals(LockInfo.UNKNOWN, value.getSlowNotifyAll());
        assertEquals(LockInfo.UNKNOWN, value.getSuccessfulSpins());
    }

}
