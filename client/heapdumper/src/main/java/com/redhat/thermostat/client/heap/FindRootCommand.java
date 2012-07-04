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

import java.io.PrintStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;

import com.redhat.thermostat.common.appctx.ApplicationContext;
import com.redhat.thermostat.common.cli.ArgumentSpec;
import com.redhat.thermostat.common.cli.Arguments;
import com.redhat.thermostat.common.cli.Command;
import com.redhat.thermostat.common.cli.CommandContext;
import com.redhat.thermostat.common.cli.CommandException;
import com.redhat.thermostat.common.cli.SimpleArgumentSpec;
import com.redhat.thermostat.common.dao.HeapDAO;
import com.redhat.thermostat.common.heap.HeapDump;
import com.redhat.thermostat.common.model.HeapInfo;
import com.sun.tools.hat.internal.model.JavaHeapObject;
import com.sun.tools.hat.internal.model.Root;
import com.sun.tools.hat.internal.model.Snapshot;

public class FindRootCommand implements Command {

    private static final String OBJECT_ID_ARG = "objectId";
    private static final String HEAP_ID_ARG = "heapId";
    private static final String DESCRIPTION = "finds the shortest path from an object to a GC root";
    private static final String NAME = "find-root";

    @Override
    public void run(CommandContext ctx) throws CommandException {
        Arguments args = ctx.getArguments();
        String heapId = args.getArgument(HEAP_ID_ARG);
        String objectId = args.getArgument(OBJECT_ID_ARG);
        HeapDAO dao = ApplicationContext.getInstance().getDAOFactory().getHeapDAO();
        HeapInfo heapInfo = dao.getHeapInfo(heapId);
        if (heapInfo == null) {
            throw new CommandException("Heap ID not found: " + heapId);
        }
        HeapDump heapDump = dao.getHeapDump(heapInfo);
        Snapshot snapshot = heapDump.getSnapshot();
        JavaHeapObject obj = heapDump.findObject(objectId);
        if (obj == null) {
            throw new CommandException("Object not found: " + objectId);
        }
        FindRoot findRoot = new FindRoot();
        HeapPath<JavaHeapObject> pathToRoot = findRoot.findShortestPathToRoot(obj);
        PrintStream out = ctx.getConsole().getOutput();
        if (pathToRoot == null) {
            out.println("No root found for: " + obj.getClazz().getName() + "@" + obj.getIdString());
        } else {
            printPathToRoot(snapshot, pathToRoot, out);
        }
    }

    private void printPathToRoot(Snapshot snapshot, HeapPath<JavaHeapObject> pathToRoot, PrintStream out) {
        // Print root.
        Iterator<JavaHeapObject> i = pathToRoot.iterator();
        JavaHeapObject last = i.next();
        Root root = last.getRoot();
        out.println(root.getDescription() + " -> " + PrintObjectUtils.objectToString(last));
        // Print reference 'tree'.
        int indentation = 0;
        while (i.hasNext()) {
            JavaHeapObject next = i.next();
            printIndentation(out, indentation);
            out.print("\u2514");
            out.print(last.describeReferenceTo(next, snapshot));
            out.print(" in ");
            out.print(PrintObjectUtils.objectToString(last));
            out.print(" -> ");
            out.println(PrintObjectUtils.objectToString(next));
            last = next;
            indentation++;
        }
    }

    private void printIndentation(PrintStream out, int indentation) {
        for (int i = 0; i < indentation; i++) {
            out.print(" ");
        }
    }

    @Override
    public void disable() {
        // TODO Auto-generated method stub
        
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public String getDescription() {
        return DESCRIPTION;
    }

    @Override
    public String getUsage() {
        return DESCRIPTION;
    }

    @Override
    public Collection<ArgumentSpec> getAcceptedArguments() {
        ArgumentSpec heapIdArg = new SimpleArgumentSpec(HEAP_ID_ARG, "the ID of the heap dump of the object", true, true);
        ArgumentSpec objectIdArg = new SimpleArgumentSpec(OBJECT_ID_ARG, "the ID of the object to query", true, true);
        return Arrays.asList(heapIdArg, objectIdArg);
    }

    @Override
    public boolean isStorageRequired() {
        return true;
    }

}
