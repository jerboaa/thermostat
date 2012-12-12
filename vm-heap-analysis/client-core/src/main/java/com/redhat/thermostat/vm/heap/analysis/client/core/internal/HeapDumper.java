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

package com.redhat.thermostat.vm.heap.analysis.client.core.internal;

import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;

import com.redhat.thermostat.common.cli.Command;
import com.redhat.thermostat.common.cli.CommandContext;
import com.redhat.thermostat.common.cli.CommandContextFactory;
import com.redhat.thermostat.common.cli.CommandException;
import com.redhat.thermostat.common.cli.SimpleArguments;
import com.redhat.thermostat.common.dao.VmRef;

public class HeapDumper {
    
    private static final String HEAP_DUMP_COMMAND = "dump-heap";
    private static final String HOST_ID_ARGUMENT = "hostId";
    private static final String VM_ID_ARGUMENT = "vmId";
    
    private Command heapDumpCommand;
    private CommandContext commandCtx;
    private VmRef ref;
    
    public HeapDumper(VmRef ref) {
        this.ref = ref;
        BundleContext context = FrameworkUtil.getBundle(getClass()).getBundleContext();
        CommandContextFactory ctxFactory = new CommandContextFactory(context);
        init(ctxFactory);
    }
    
    HeapDumper(VmRef ref, CommandContextFactory ctxFactory) {
        this.ref = ref;
        init(ctxFactory);
    }
    
    private void init(CommandContextFactory ctxFactory) {
        // Setup heap dump command
        heapDumpCommand = ctxFactory.getCommandRegistry().getCommand(HEAP_DUMP_COMMAND);
        SimpleArguments args = new SimpleArguments();
        args.addArgument(HOST_ID_ARGUMENT, ref.getAgent().getStringID());
        args.addArgument(VM_ID_ARGUMENT, ref.getStringID());
        commandCtx = ctxFactory.createContext(args);
    }
    
    public void dump() throws CommandException {
        heapDumpCommand.run(commandCtx);
    }

}
