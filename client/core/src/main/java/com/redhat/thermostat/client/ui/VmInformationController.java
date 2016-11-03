/*
 * Copyright 2012-2016 Red Hat, Inc.
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

import java.util.Collections;
import java.util.List;

import com.redhat.thermostat.client.core.InformationService;
import com.redhat.thermostat.client.core.controllers.InformationServiceController;
import com.redhat.thermostat.client.core.views.BasicView;
import com.redhat.thermostat.client.core.views.VmInformationView;
import com.redhat.thermostat.client.core.views.VmInformationViewProvider;
import com.redhat.thermostat.common.OrderedComparator;
import com.redhat.thermostat.shared.locale.LocalizedString;
import com.redhat.thermostat.storage.core.VmRef;

public class VmInformationController implements ContentProvider {

    private final List<InformationService<VmRef>> vmInfoServices;
    private final VmRef vmRef;
    private final VmInformationView view;

    private int selectedID;

    public VmInformationController(List<InformationService<VmRef>> vmInfoServices,
                                   VmRef vmRef,
                                   VmInformationViewProvider provider)
    {
        this.vmInfoServices = vmInfoServices;
        this.vmRef = vmRef;
        this.selectedID = 0;

        view = provider.createView();
        rebuild();
    }

    void rebuild() {

        view.clear();

        Collections.sort(vmInfoServices, new OrderedComparator<InformationService<VmRef>>());
        for (InformationService<VmRef> vmInfoService : vmInfoServices) {
            if (vmInfoService.getFilter().matches(vmRef)) {
                InformationServiceController<VmRef> ctrl = vmInfoService.getInformationServiceController(vmRef);
                LocalizedString name = ctrl.getLocalizedName();
                view.addChildView(name, ctrl.getView());
            }
        }

        view.selectChildID(selectedID);
    }

    public int getSelectedChildID() {
        return view.getSelectedChildID();
    }

    public boolean selectChildID(int id) {
        selectedID = id;
        return view.selectChildID(id);
    }

    public int getNumChildren() {
        return view.getNumChildren();
    }

    public BasicView getView() {
        rebuild();
        return view;
    }

}

