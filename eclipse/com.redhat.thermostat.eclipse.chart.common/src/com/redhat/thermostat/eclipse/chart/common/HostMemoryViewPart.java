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

package com.redhat.thermostat.eclipse.chart.common;

import org.eclipse.swt.widgets.Composite;

import com.redhat.thermostat.common.dao.HostInfoDAO;
import com.redhat.thermostat.common.dao.HostRef;
import com.redhat.thermostat.common.dao.MemoryStatDAO;
import com.redhat.thermostat.common.utils.OSGIUtils;
import com.redhat.thermostat.eclipse.SWTComponent;
import com.redhat.thermostat.eclipse.views.HostRefViewPart;
import com.redhat.thermostat.host.memory.client.core.HostMemoryController;
import com.redhat.thermostat.host.memory.client.core.HostMemoryViewProvider;

public class HostMemoryViewPart extends HostRefViewPart {

    @Override
    protected SWTComponent createControllerView(HostRef ref, Composite parent) {
        HostInfoDAO hostInfoDao = OSGIUtils.getInstance().getService(
                HostInfoDAO.class);
        MemoryStatDAO memoryStatDao = OSGIUtils.getInstance().getService(
                MemoryStatDAO.class);
        SWTHostMemoryViewProvider viewProvider = (SWTHostMemoryViewProvider) OSGIUtils
                .getInstance().getService(HostMemoryViewProvider.class);
        viewProvider.setParent(parent);
        HostMemoryController controller = createController(hostInfoDao,
                memoryStatDao, ref, viewProvider);
        SWTComponent view = (SWTComponent) controller.getView();
        return view;
    }

    public HostMemoryController createController(HostInfoDAO hostInfoDao,
            MemoryStatDAO memoryStatDao, HostRef ref,
            HostMemoryViewProvider viewProvider) {
        return new HostMemoryController(hostInfoDao, memoryStatDao, ref,
                viewProvider);
    }

}
