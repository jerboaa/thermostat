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

package com.redhat.thermostat.vm.classstat.agent.internal;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import com.redhat.thermostat.backend.VmUpdate;
import com.redhat.thermostat.backend.VmUpdateException;
import com.redhat.thermostat.vm.classstat.common.VmClassStatDAO;
import com.redhat.thermostat.vm.classstat.common.model.VmClassStat;

public class VmClassStatVmListenerTest {

    private static final String VM_ID = "vmId";
    private static final Long LOADED_CLASSES_NON_SHARED = 1234L;
    private static final Long LOADED_CLASSES_SHARED = 1234L;
    private static final Long LOADED_CLASSES = LOADED_CLASSES_NON_SHARED + LOADED_CLASSES_SHARED;
    private static final Long LOADED_BYTES_NON_SHARED = 1234L;
    private static final Long LOADED_BYTES_SHARED = 1234L;
    private static final Long LOADED_BYTES = LOADED_BYTES_NON_SHARED + LOADED_BYTES_SHARED;
    private static final Long UNLOADED_CLASSES_NON_SHARED = 1234L;
    private static final Long UNLOADED_CLASSES_SHARED = 1234L;
    private static final Long UNLOADED_CLASSES = UNLOADED_CLASSES_NON_SHARED + UNLOADED_CLASSES_SHARED;
    private static final Long UNLOADED_BYTES_NON_SHARED = 1234L;
    private static final Long UNLOADED_BYTES_SHARED = 1234L;
    private static final Long UNLOADED_BYTES = UNLOADED_BYTES_NON_SHARED + UNLOADED_BYTES_SHARED;
    private static final Long CLASS_TIME_TICKS = 4242L;
    private static final Long FREQUENCY = 2L;
    private static final Long CLASS_TIME = CLASS_TIME_TICKS / FREQUENCY;

    private VmClassStatDAO dao;
    private VmClassStatVmListener listener;

    private VmUpdate update;

    @Before
    public void setUp() throws VmUpdateException {
        dao = mock(VmClassStatDAO.class);
        listener = new VmClassStatVmListener("foo-agent", dao, VM_ID);

        update = mock(VmUpdate.class);
        when(update.getPerformanceCounterLong("java.cls.loadedClasses")).thenReturn(LOADED_CLASSES_NON_SHARED);
        when(update.getPerformanceCounterLong("java.cls.sharedLoadedClasses")).thenReturn(LOADED_CLASSES_SHARED);

        when(update.getPerformanceCounterLong("sun.cls.loadedBytes")).thenReturn(LOADED_BYTES_NON_SHARED);
        when(update.getPerformanceCounterLong("sun.cls.sharedLoadedBytes")).thenReturn(LOADED_BYTES_SHARED);

        when(update.getPerformanceCounterLong("java.cls.unloadedClasses")).thenReturn(UNLOADED_CLASSES_NON_SHARED);
        when(update.getPerformanceCounterLong("java.cls.sharedUnloadedClasses")).thenReturn(UNLOADED_CLASSES_SHARED);

        when(update.getPerformanceCounterLong("sun.cls.unloadedBytes")).thenReturn(UNLOADED_BYTES_NON_SHARED);
        when(update.getPerformanceCounterLong("sun.cls.sharedUnloadedBytes")).thenReturn(UNLOADED_BYTES_SHARED);

        when(update.getPerformanceCounterLong("sun.cls.time")).thenReturn(CLASS_TIME_TICKS);
        when(update.getPerformanceCounterLong("sun.os.hrt.frequency")).thenReturn(FREQUENCY);
    }

    @Test
    public void testMonitorUpdatedClassStat() throws Exception {
        listener.countersUpdated(update);

        ArgumentCaptor<VmClassStat> arg = ArgumentCaptor.forClass(VmClassStat.class);
        verify(dao).putVmClassStat(arg.capture());
        VmClassStat stat = arg.getValue();
        assertEquals(LOADED_CLASSES, (Long) stat.getLoadedClasses());
        assertEquals(VM_ID, stat.getVmId());
    }

    @Test
    public void testMonitorUpdatedClassStatTwice() throws Exception {
        listener.countersUpdated(update);
        listener.countersUpdated(update);

        // This checks a bug where the Category threw an IllegalStateException because the DAO
        // created a new one on each call, thus violating the unique guarantee of Category.
    }

    @Test
    public void testMonitorUpdateFails() throws VmUpdateException {
        when(update.getPerformanceCounterLong(anyString())).thenThrow(new VmUpdateException());
        listener.countersUpdated(update);

        verifyNoMoreInteractions(dao);
    }

}

