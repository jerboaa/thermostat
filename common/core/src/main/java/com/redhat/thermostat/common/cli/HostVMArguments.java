/*
 * Copyright 2012, 2013 Red Hat, Inc.
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

package com.redhat.thermostat.common.cli;

import com.redhat.thermostat.common.dao.HostRef;
import com.redhat.thermostat.common.dao.VmRef;

public class HostVMArguments {

    static final String HOST_ID_ARGUMENT = "hostId";
    static final String VM_ID_ARGUMENT = "vmId";

    private HostRef host;
    private VmRef vm;

    public HostVMArguments(Arguments args) throws CommandException {
        this(args, true, true);
    }

    public HostVMArguments(Arguments args, boolean hostRequired, boolean vmRequired) throws CommandException {
        String hostId = args.getArgument(HOST_ID_ARGUMENT);
        String vmId = args.getArgument(VM_ID_ARGUMENT);
        if (hostRequired && hostId == null) {
            throw new CommandException("a " + HOST_ID_ARGUMENT + " is required");
        } else if (hostId == null) {
            host = null;
        } else {
            host = new HostRef(hostId, "dummy");
        }
        try {
            int parsedVmId = parseVmId(vmId);
            vm = new VmRef(host, parsedVmId, "dummy");
        } catch (CommandException ce) {
            if (vmRequired) {
                throw ce;
            }
            vm = null;
        }
    }

    private int parseVmId(String vmId) throws CommandException {
        try {
            return Integer.parseInt(vmId);
        } catch (NumberFormatException ex) {
            throw new CommandException("Invalid VM ID: " + vmId, ex);
        }
    }

    public HostRef getHost() {
        return host;
    }

    public VmRef getVM() {
        return vm;
    }
}

