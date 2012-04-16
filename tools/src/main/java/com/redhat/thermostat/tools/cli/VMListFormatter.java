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

package com.redhat.thermostat.tools.cli;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

import com.redhat.thermostat.common.dao.VmRef;

class VMListFormatter {

    // TODO: Localize.
    private static final String HOST = "HOST";

    private static final String VM_ID = "VM_ID";

    private static final String VM_NAME = "VM_NAME";

    private List<VmRef> vms = new ArrayList<>();

    private int longestHost = HOST.length();
    private int longestVmId = VM_ID.length();

    void addVM(VmRef vm) {
        vms.add(vm);
        longestHost = Math.max(longestHost, vm.getAgent().getHostName().length());
        longestVmId = Math.max(longestVmId, vm.getIdString().length());
    }

    void format(PrintStream output) {
        printHeader(output);
        for (VmRef vm : vms) {
            printVM(output, vm);
        }
    }

    private void printHeader(PrintStream output) {
        printLine(output, HOST, VM_ID, VM_NAME);
    }

    private void printVM(PrintStream output, VmRef vm) {
        printLine(output, vm.getAgent().getHostName(), vm.getId().toString(), vm.getName());
    }

    private void printLine(PrintStream output, String host, String vmId, String vmName) {
        output.print(host);
        output.print(fillSpace(longestHost - host.length() + 1));
        output.print(vmId);
        output.print(fillSpace(longestVmId - vmId.length() + 1));
        output.print(vmName);
        output.print('\n');
    }

    private String fillSpace(int n) {
        StringBuilder s = new StringBuilder();
        for (int i = 0; i < n; i++) {
            s.append(' ');
        }
        return s.toString();
    }

}
