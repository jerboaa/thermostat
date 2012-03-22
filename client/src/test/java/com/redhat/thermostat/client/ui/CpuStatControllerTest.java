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

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.redhat.thermostat.client.appctx.ApplicationContext;
import com.redhat.thermostat.common.CpuStat;
import com.redhat.thermostat.common.HostInfo;
import com.redhat.thermostat.common.dao.CpuStatDAO;
import com.redhat.thermostat.common.dao.DAOFactory;
import com.redhat.thermostat.common.dao.HostInfoDAO;
import com.redhat.thermostat.common.dao.HostRef;
import com.redhat.thermostat.common.dao.MongoDAOFactory;

public class CpuStatControllerTest {

    @Test
    public void testUpdate() {

        CpuStat stat = new CpuStat(10L, 5.0, 10.0, 15.0);
        List<CpuStat> stats = new ArrayList<CpuStat>();
        stats.add(stat);

        CpuStatDAO cpuStatDAO = mock(CpuStatDAO.class);
        when(cpuStatDAO.getLatestCpuStats()).thenReturn(stats).thenReturn(new ArrayList<CpuStat>());

        HostInfo hostInfo = new HostInfo("someHost", "someOS", "linux_0.0.1", "lreally_fast_cpu", 2, 1024);
        HostInfoDAO hostInfoDAO = mock(HostInfoDAO.class);
        when(hostInfoDAO.getHostInfo()).thenReturn(hostInfo);

        DAOFactory daoFactory = mock(MongoDAOFactory.class);
        when(daoFactory.getCpuStatDAO(any(HostRef.class))).thenReturn(cpuStatDAO);
        when(daoFactory.getHostInfoDAO(any(HostRef.class))).thenReturn(hostInfoDAO);

        ApplicationContext.getInstance().setDAOFactory(daoFactory);
        HostRef ref = mock(HostRef.class);

        final HostCpuView view = mock(HostCpuView.class);

        // TODO: Consider to pass the ClassesView or a factory for it to the controller instead.
        HostCpuController controller = new HostCpuController(ref) {
            @Override
            protected HostCpuView createView() {
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

        verify(view, atLeast(1)).addCpuLoadData(any(List.class));
        verify(view, atLeast(1)).setCpuCount(any(String.class));
        verify(view, atLeast(1)).setCpuModel(any(String.class));
        // We don't verify atMost() since we might increase the update rate in the future.
    }
}
