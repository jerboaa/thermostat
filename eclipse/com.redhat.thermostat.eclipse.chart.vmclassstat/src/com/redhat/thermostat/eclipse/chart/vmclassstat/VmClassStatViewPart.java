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

package com.redhat.thermostat.eclipse.chart.vmclassstat;

import org.eclipse.swt.widgets.Composite;

import com.redhat.thermostat.client.vmclassstat.core.VmClassStatController;
import com.redhat.thermostat.client.vmclassstat.core.VmClassStatViewProvider;
import com.redhat.thermostat.common.dao.VmClassStatDAO;
import com.redhat.thermostat.common.dao.VmRef;
import com.redhat.thermostat.common.utils.OSGIUtils;
import com.redhat.thermostat.eclipse.SWTComponent;
import com.redhat.thermostat.eclipse.views.VmRefViewPart;

public class VmClassStatViewPart extends VmRefViewPart {

    @Override
    protected SWTComponent createControllerView(VmRef ref, Composite parent) {
        SWTVmClassStatViewProvider viewProvider = (SWTVmClassStatViewProvider) OSGIUtils
                .getInstance().getService(VmClassStatViewProvider.class);
        viewProvider.setParent(parent);
        VmClassStatDAO classStatDAO = OSGIUtils.getInstance().getService(
                VmClassStatDAO.class);
        VmClassStatController controller = createController(classStatDAO, ref,
                viewProvider);
        SWTComponent view = (SWTComponent) controller.getView();
        return view;
    }

    public VmClassStatController createController(VmClassStatDAO classStatDao,
            VmRef ref, VmClassStatViewProvider viewProvider) {
        return new VmClassStatController(classStatDao, ref, viewProvider);
    }

}