/*
 * Copyright 2012 Red Hat, Inc.
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

package com.redhat.thermostat.client.ui;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.LinkedList;
import java.util.List;

import org.junit.Test;

import com.redhat.thermostat.client.appctx.ApplicationContext;
import com.redhat.thermostat.common.dao.DAOFactory;
import com.redhat.thermostat.common.dao.HostInfoDAO;
import com.redhat.thermostat.common.dao.HostRef;
import com.redhat.thermostat.common.dao.MemoryStatDAO;
import com.redhat.thermostat.common.dao.MongoDAOFactory;
import com.redhat.thermostat.common.model.HostInfo;
import com.redhat.thermostat.common.model.MemoryStat;

public class HostMemoryControllerTest {

    @Test
    public void testUpdate() {
        HostInfo hostInfo = new HostInfo("someHost", "someOS", "linux_0.0.1", "lreally_fast_cpu", 2, 1024);
        HostInfoDAO hostInfoDAO = mock(HostInfoDAO.class);
        when(hostInfoDAO.getHostInfo(any(HostRef.class))).thenReturn(hostInfo);

        MemoryStat memoryStat = new MemoryStat(1, 2, 3, 4, 5, 6, 7, 8);
        List<MemoryStat> memoryStats = new LinkedList<>();
        memoryStats.add(memoryStat);
        MemoryStatDAO memoryStatDAO = mock(MemoryStatDAO.class);
        when(memoryStatDAO.getLatestMemoryStats(any(HostRef.class))).thenReturn(memoryStats);

        DAOFactory daoFactory = mock(MongoDAOFactory.class);
        when(daoFactory.getHostInfoDAO()).thenReturn(hostInfoDAO);
        when(daoFactory.getMemoryStatDAO()).thenReturn(memoryStatDAO);
        ApplicationContext.getInstance().setDAOFactory(daoFactory);

        HostRef ref = mock(HostRef.class);
        final HostMemoryView view = mock(HostMemoryView.class);
        // TODO: Consider to pass the ClassesView or a factory for it to the controller instead.
        HostMemoryController controller = new HostMemoryController(ref) {
            @Override
            protected HostMemoryView createView() {
                return view;
            }
        };

        controller.start();

        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            // Get out of here ASAP.
            return;
        }

        verify(view, atLeast(1)).setTotalMemory(any(String.class));
        verify(view, atLeast(6)).addMemoryData(any(String.class), any(List.class));
    }
}
