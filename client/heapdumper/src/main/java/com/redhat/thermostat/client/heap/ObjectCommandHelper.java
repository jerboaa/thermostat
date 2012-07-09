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

package com.redhat.thermostat.client.heap;

import java.util.Arrays;
import java.util.Collection;

import com.redhat.thermostat.common.appctx.ApplicationContext;
import com.redhat.thermostat.common.cli.ArgumentSpec;
import com.redhat.thermostat.common.cli.Arguments;
import com.redhat.thermostat.common.cli.CommandContext;
import com.redhat.thermostat.common.cli.CommandException;
import com.redhat.thermostat.common.cli.SimpleArgumentSpec;
import com.redhat.thermostat.common.dao.HeapDAO;
import com.redhat.thermostat.common.heap.HeapDump;
import com.redhat.thermostat.common.model.HeapInfo;
import com.sun.tools.hat.internal.model.JavaHeapObject;

class ObjectCommandHelper {

    private static final String OBJECT_ID_ARG = "objectId";
    private static final String HEAP_ID_ARG = "heapId";

    private CommandContext ctx;
    private HeapDump heapDump;

    ObjectCommandHelper(CommandContext ctx) {
        this.ctx = ctx;
    }

    HeapDump getHeapDump() throws CommandException {
        if (heapDump == null) {
            loadHeapDump();
        }
        return heapDump;
    }

    private void loadHeapDump() throws CommandException {
        Arguments args = ctx.getArguments();
        String heapId = args.getArgument(HEAP_ID_ARG);
        HeapDAO dao = ApplicationContext.getInstance().getDAOFactory().getHeapDAO();
        HeapInfo heapInfo = dao.getHeapInfo(heapId);
        if (heapInfo == null) {
            throw new CommandException(Translate.localize(LocaleResources.HEAP_ID_NOT_FOUND, heapId));
        }
        heapDump = dao.getHeapDump(heapInfo);
    }

    JavaHeapObject getJavaHeapObject() throws CommandException {
        HeapDump heapDump = getHeapDump();
        Arguments args = ctx.getArguments();
        String objectId = args.getArgument(OBJECT_ID_ARG);
        JavaHeapObject obj = heapDump.findObject(objectId);
        if (obj == null) {
            throw new HeapNotFoundException(objectId);
        }
        return obj;
    }

    static Collection<ArgumentSpec> getArgumentSpecs() {
        ArgumentSpec heapIdArg = new SimpleArgumentSpec(HEAP_ID_ARG, Translate.localize(LocaleResources.ARGUMENT_HEAP_ID_DESCRIPTION), true, true);
        ArgumentSpec objectIdArg = new SimpleArgumentSpec(OBJECT_ID_ARG, Translate.localize(LocaleResources.ARGUMENT_OBJECT_ID_DESCRIPTION), true, true);
        return Arrays.asList(heapIdArg, objectIdArg);
    }
}
