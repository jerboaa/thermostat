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

import java.util.Arrays;
import java.util.Collection;

import com.redhat.thermostat.common.cli.ArgumentSpec;
import com.redhat.thermostat.common.cli.Arguments;
import com.redhat.thermostat.common.cli.CommandException;
import com.redhat.thermostat.common.cli.SimpleArgumentSpec;
import com.redhat.thermostat.common.dao.HostRef;
import com.redhat.thermostat.common.dao.VmRef;

class HostVMArguments {

    static final String HOST_ID_ARGUMENT = "hostId";
    static final String VM_ID_ARGUMENT = "vmId";

    private HostRef host;
    private VmRef vm;

    HostVMArguments(Arguments args) throws CommandException {
        this(args, true);
    }

    HostVMArguments(Arguments args, boolean vmRequired) throws CommandException {
        String hostId = args.getArgument(HOST_ID_ARGUMENT);
        String vmId = args.getArgument(VM_ID_ARGUMENT);
        host = new HostRef(hostId, "dummy");
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

    HostRef getHost() {
        return host;
    }

    VmRef getVM() {
        return vm;
    }

    /**
     * @return a collection of arguments for accepting hosts and vms (where both
     * are required)
     */
    static Collection<ArgumentSpec> getArgumentSpecs() {
        return getArgumentSpecs(true);
    }

    /**
     * @return a collection of arguments for accepting hosts and vms (where the
     * vm is optional)
     */
    static Collection<ArgumentSpec> getArgumentSpecs(boolean vmRequired) {
        ArgumentSpec vmId = new SimpleArgumentSpec(VM_ID_ARGUMENT, "the ID of the VM to monitor", vmRequired, true);
        ArgumentSpec hostId = new SimpleArgumentSpec(HOST_ID_ARGUMENT, "the ID of the host to monitor", true, true);
        return Arrays.asList(vmId, hostId);
    }
}
