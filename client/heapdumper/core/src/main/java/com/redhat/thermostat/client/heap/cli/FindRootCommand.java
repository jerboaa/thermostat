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

package com.redhat.thermostat.client.heap.cli;

import java.io.PrintStream;
import java.util.Collection;
import java.util.Iterator;

import com.redhat.thermostat.client.heap.LocaleResources;
import com.redhat.thermostat.client.heap.internal.PrintObjectUtils;
import com.redhat.thermostat.common.cli.CommandContext;
import com.redhat.thermostat.common.cli.CommandException;
import com.redhat.thermostat.common.cli.SimpleCommand;
import com.redhat.thermostat.common.dao.HeapDAO;
import com.redhat.thermostat.common.heap.HeapDump;
import com.redhat.thermostat.common.locale.Translate;
import com.redhat.thermostat.common.utils.OSGIUtils;
import com.sun.tools.hat.internal.model.JavaHeapObject;
import com.sun.tools.hat.internal.model.Root;
import com.sun.tools.hat.internal.model.Snapshot;

public class FindRootCommand extends SimpleCommand {

    private static final Translate<LocaleResources> translator = LocaleResources.createLocalizer();

    private static final String ALL_ARG = "all";
    private static final String NAME = "find-root";

    private OSGIUtils serviceProvider;

    public FindRootCommand() {
        this(OSGIUtils.getInstance());
    }

    FindRootCommand(OSGIUtils serviceProvider) {
        this.serviceProvider = serviceProvider;
    }

    @Override
    public void run(CommandContext ctx) throws CommandException {
        HeapDAO heapDao = serviceProvider.getServiceAllowNull(HeapDAO.class);
        if (heapDao == null) {
            throw new CommandException(translator.localize(LocaleResources.HEAP_SERVICE_UNAVAILABLE));
        }

        try {
            run(ctx, heapDao);
        } finally {
            serviceProvider.ungetService(HeapDAO.class, heapDao);
        }
    }

    private void run(CommandContext ctx, HeapDAO heapDao) throws CommandException {
        ObjectCommandHelper objCmdHelper = new ObjectCommandHelper(ctx, heapDao);
        HeapDump heapDump = objCmdHelper.getHeapDump();
        Snapshot snapshot = heapDump.getSnapshot();
        JavaHeapObject obj = objCmdHelper.getJavaHeapObject();
        boolean findAll = ctx.getArguments().hasArgument(ALL_ARG);
        FindRoot findRoot = new FindRoot();
        Collection<HeapPath<JavaHeapObject>> pathsToRoot = findRoot.findShortestPathsToRoot(obj, findAll);
        PrintStream out = ctx.getConsole().getOutput();
        if (pathsToRoot.isEmpty()) {
            out.println(translator.localize(LocaleResources.COMMAND_FIND_ROOT_NO_ROOT_FOUND, PrintObjectUtils.objectToString(obj)));
        } else {
            printPathsToRoot(snapshot, pathsToRoot, out);
        }
    }

    private void printPathsToRoot(Snapshot snapshot, Collection<HeapPath<JavaHeapObject>> pathsToRoot, PrintStream out) {
        for (HeapPath<JavaHeapObject> path : pathsToRoot) {
            printPathToRoot(snapshot, path, out);
            out.println();
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
    public String getName() {
        return NAME;
    }

}
