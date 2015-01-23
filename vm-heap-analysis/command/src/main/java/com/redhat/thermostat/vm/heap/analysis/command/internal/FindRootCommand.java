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

package com.redhat.thermostat.vm.heap.analysis.command.internal;

import java.io.PrintStream;
import java.util.Collection;
import java.util.Iterator;

import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;

import com.redhat.thermostat.common.cli.AbstractCommand;
import com.redhat.thermostat.common.cli.CommandContext;
import com.redhat.thermostat.common.cli.CommandException;
import com.redhat.thermostat.shared.locale.Translate;
import com.redhat.thermostat.vm.heap.analysis.command.FindRoot;
import com.redhat.thermostat.vm.heap.analysis.command.HeapPath;
import com.redhat.thermostat.vm.heap.analysis.command.locale.LocaleResources;
import com.redhat.thermostat.vm.heap.analysis.common.HeapDAO;
import com.redhat.thermostat.vm.heap.analysis.common.HeapDump;
import com.sun.tools.hat.internal.model.JavaHeapObject;
import com.sun.tools.hat.internal.model.Root;
import com.sun.tools.hat.internal.model.Snapshot;

public class FindRootCommand extends AbstractCommand {

    private static final Translate<LocaleResources> translator = LocaleResources.createLocalizer();

    private static final String ALL_ARG = "all";

    private BundleContext context;

    public FindRootCommand() {
        this(FrameworkUtil.getBundle(FindRootCommand.class).getBundleContext());
    }

    FindRootCommand(BundleContext context) {
        this.context = context;
    }

    @Override
    public void run(CommandContext ctx) throws CommandException {
        ServiceReference heapDaoRef = context.getServiceReference(HeapDAO.class.getName());
        requireNonNull(heapDaoRef, translator.localize(LocaleResources.HEAP_SERVICE_UNAVAILABLE));
        HeapDAO heapDao = (HeapDAO) context.getService(heapDaoRef);

        try {
            run(ctx, heapDao);
        } finally {
            context.ungetService(heapDaoRef);
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
            out.println(translator.localize(LocaleResources.COMMAND_FIND_ROOT_NO_ROOT_FOUND, PrintObjectUtils.objectToString(obj)).getContents());
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

}

