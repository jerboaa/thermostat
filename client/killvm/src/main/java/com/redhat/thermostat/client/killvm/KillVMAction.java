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

package com.redhat.thermostat.client.killvm;

import com.redhat.thermostat.client.osgi.service.VMContextAction;
import com.redhat.thermostat.client.osgi.service.VMFilter;
import com.redhat.thermostat.common.dao.DAOFactory;
import com.redhat.thermostat.common.dao.VmRef;
import com.redhat.thermostat.common.model.VmInfo;
import com.redhat.thermostat.service.process.UNIXProcessHandler;
import com.redhat.thermostat.service.process.UNIXSignal;

/**
 * Implements the {@link VMContextAction} entry point to provide a kill switch
 * for the currently selected Virtual Machine. 
 */
public class KillVMAction implements VMContextAction {

    private final UNIXProcessHandler unixService;
    private final DAOFactory dao;

    public KillVMAction(UNIXProcessHandler unixService, DAOFactory dao) {
        this.unixService = unixService;
        this.dao = dao;
    }

    @Override
    public String getName() {
        return "Kill Application";
    }

    @Override
    public String getDescription() {
        return "Kill the selected VM Process";
    }

    @Override
    public void execute(VmRef reference) {
        // TODO this should be executed on the agent-side
        unixService.sendSignal(reference.getIdString(), UNIXSignal.TERM);
    }

    @Override
    public VMFilter getFilter() {
        return new LocalAndAliveFilter();
    }

    private class LocalAndAliveFilter implements VMFilter {

        @Override
        public boolean matches(VmRef vm) {
            // TODO implement local checking too
            VmInfo vmInfo = dao.getVmInfoDAO().getVmInfo(vm);
            boolean dead = vmInfo.getStartTimeStamp() < vmInfo.getStopTimeStamp();
            return !dead;
        }

    }
}
