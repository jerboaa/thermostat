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

package com.redhat.thermostat.client.cli.internal;

import java.io.PrintStream;

import com.redhat.thermostat.common.cli.TableRenderer;
import com.redhat.thermostat.shared.locale.Translate;
import com.redhat.thermostat.storage.core.VmRef;
import com.redhat.thermostat.storage.model.AgentInformation;
import com.redhat.thermostat.storage.model.VmInfo;

class VMListFormatter {

    private static final Translate<LocaleResources> translator = LocaleResources.createLocalizer();
    private static final int NUM_COLUMNS = 6;

    private static final String HOST_ID = translator.localize(LocaleResources.COLUMN_HEADER_HOST_ID).getContents();
    private static final String HOST = translator.localize(LocaleResources.COLUMN_HEADER_HOST).getContents();
    private static final String VM_ID = translator.localize(LocaleResources.COLUMN_HEADER_VM_ID).getContents();
    private static final String VM_PID = translator.localize(LocaleResources.COLUMN_HEADER_VM_PID).getContents();
    private static final String VM_NAME = translator.localize(LocaleResources.COLUMN_HEADER_VM_NAME).getContents();
    private static final String VM_STATUS = translator.localize(LocaleResources.COLUMN_HEADER_VM_STATUS).getContents();

    private static final String STATUS_RUNNING = translator.localize(LocaleResources.VM_STATUS_RUNNING).getContents();
    private static final String STATUS_EXITED = translator.localize(LocaleResources.VM_STATUS_EXITED).getContents();
    private static final String STATUS_UNKNOWN = translator.localize(LocaleResources.VM_STATUS_UNKNOWN).getContents();

    private final TableRenderer tableRenderer = new TableRenderer(NUM_COLUMNS);

    void addVM(VmRef vm, AgentInformation agentInfo, VmInfo info) {
        printVM(vm, agentInfo, info);
    }

    void addHeader() {
        printLine(HOST_ID, HOST, VM_ID, VM_PID, VM_STATUS, VM_NAME);
    }

    void format(PrintStream out) {
        tableRenderer.render(out);
    }

    private void printVM(VmRef vm, AgentInformation agentInfo, VmInfo info) {
        printLine(vm.getHostRef().getAgentId(),
                  vm.getHostRef().getHostName(),
                  vm.getVmId(),
                  vm.getPid().toString(),
                  getAliveStatus(info.isAlive(agentInfo)),
                  vm.getName());
    }

    private void printLine(String hostId, String host, String vmId, String pid, String status, String vmName) {
        tableRenderer.printLine(hostId, host, vmId, pid, status, vmName);
    }

    private String getAliveStatus(VmInfo.AliveStatus status) {
        switch (status) {
        case RUNNING:
            return STATUS_RUNNING;
        case EXITED:
            return STATUS_EXITED;
        case UNKNOWN:
            return STATUS_UNKNOWN;
        default:
            throw new AssertionError("Unknown VM status");
        }
    }
}

