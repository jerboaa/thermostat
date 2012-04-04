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

package com.redhat.thermostat.client;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import com.redhat.thermostat.client.appctx.ApplicationContext;
import com.redhat.thermostat.client.ui.VmClassStatController;
import com.redhat.thermostat.client.ui.VmClassStatView;
import com.redhat.thermostat.common.dao.DAOFactory;
import com.redhat.thermostat.common.dao.MongoDAOFactory;
import com.redhat.thermostat.common.dao.VmClassStatDAO;
import com.redhat.thermostat.common.dao.VmRef;
import com.redhat.thermostat.common.model.VmClassStat;

public class VmClassStatControllerTest {

    @Test
    public void testChartUpdate() {

        VmClassStat stat1 = new VmClassStat(123, 12345, 1234);
        List<VmClassStat> stats = new ArrayList<VmClassStat>();
        stats.add(stat1);

        VmClassStatDAO vmClassStatDAO = mock(VmClassStatDAO.class);
        when(vmClassStatDAO.getLatestClassStats(any(VmRef.class))).thenReturn(stats).thenReturn(new ArrayList<VmClassStat>());

        DAOFactory daoFactory = mock(MongoDAOFactory.class);
        when(daoFactory.getVmClassStatsDAO()).thenReturn(vmClassStatDAO);

        ApplicationContext.getInstance().setDAOFactory(daoFactory);
        VmRef ref = mock(VmRef.class);

        final VmClassStatView view = mock(VmClassStatView.class);

        // TODO: Consider to pass the ClassesView or a factory for it to the controller instead.
        VmClassStatController controller = new VmClassStatController(ref) {
            @Override
            protected VmClassStatView createView() {
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

        verify(view, atLeast(1)).addClassCount(any(List.class));
        // We don't verify atMost() since we might increase the update rate in the future.
    }

}
