/*
 * Copyright 2012-2017 Red Hat, Inc.
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

package com.redhat.thermostat.vm.overview.client.core.internal;

import java.text.DateFormat;
import java.util.Date;

import com.redhat.thermostat.client.core.controllers.InformationServiceController;
import com.redhat.thermostat.client.core.views.UIComponent;
import com.redhat.thermostat.common.Clock;
import com.redhat.thermostat.shared.locale.LocalizedString;
import com.redhat.thermostat.shared.locale.Translate;
import com.redhat.thermostat.storage.core.VmRef;
import com.redhat.thermostat.storage.dao.VmInfoDAO;
import com.redhat.thermostat.storage.model.VmInfo;
import com.redhat.thermostat.vm.overview.client.core.VmOverviewView;
import com.redhat.thermostat.vm.overview.client.core.VmOverviewViewProvider;
import com.redhat.thermostat.vm.overview.client.locale.LocaleResources;

public class VmOverviewController implements InformationServiceController<VmRef> {

    private static final Translate<LocaleResources> translator = LocaleResources.createLocalizer();
    static final DateFormat VM_RUNNING_TIME_FORMAT = Clock.DEFAULT_DATE_FORMAT;

    private final VmRef ref;
    private final VmInfoDAO dao;
    private final VmOverviewView view;

    public VmOverviewController(VmInfoDAO vmDao, VmRef vmRef, VmOverviewViewProvider provider) {
        this.ref = vmRef;
        this.view = provider.createView();
        this.dao = vmDao;

        updateView();
    }

    void updateView() {
        VmInfo info = dao.getVmInfo(ref);
        view.setVmPid(((Integer) info.getVmPid()).toString());
        long actualStartTime = info.getStartTimeStamp();
        view.setVmStartTimeStamp(VM_RUNNING_TIME_FORMAT.format(new Date(actualStartTime)));
        long actualStopTime = info.getStopTimeStamp();
        if (actualStopTime >= actualStartTime) {
            view.setVmStopTimeStamp(VM_RUNNING_TIME_FORMAT.format(new Date(actualStopTime)));
        } else {
            view.setVmStopTimeStamp(translator.localize(LocaleResources.VM_INFO_RUNNING).getContents());
        }
        view.setJavaVersion(info.getJavaVersion());
        view.setJavaHome(info.getJavaHome());
        view.setMainClass(info.getMainClass());
        view.setJavaCommandLine(info.getJavaCommandLine());
        view.setVmName(info.getVmName());
        view.setVmVersion(info.getVmVersion());
        view.setVmArguments(info.getVmArguments());
        view.setUsername(info.getUsername());
        view.setUserId(info.getUid());
    }

    public UIComponent getView() {
        return view;
    }

    @Override
    public LocalizedString getLocalizedName() {
        return translator.localize(LocaleResources.VM_INFO_TAB_OVERVIEW);
    }

}

