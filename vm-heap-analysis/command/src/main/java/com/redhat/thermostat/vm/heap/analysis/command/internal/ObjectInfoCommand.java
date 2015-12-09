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
import java.util.Enumeration;

import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;

import com.redhat.thermostat.common.cli.AbstractCommand;
import com.redhat.thermostat.common.cli.CommandContext;
import com.redhat.thermostat.common.cli.CommandException;
import com.redhat.thermostat.common.cli.TableRenderer;
import com.redhat.thermostat.shared.locale.Translate;
import com.redhat.thermostat.vm.heap.analysis.command.locale.LocaleResources;
import com.redhat.thermostat.vm.heap.analysis.common.HeapDAO;
import com.redhat.thermostat.vm.heap.analysis.common.HeapDump;
import com.redhat.thermostat.vm.heap.analysis.hat.hprof.model.JavaClass;
import com.redhat.thermostat.vm.heap.analysis.hat.hprof.model.JavaField;
import com.redhat.thermostat.vm.heap.analysis.hat.hprof.model.JavaHeapObject;
import com.redhat.thermostat.vm.heap.analysis.hat.hprof.model.JavaHeapObjectVisitor;
import com.redhat.thermostat.vm.heap.analysis.hat.hprof.model.Snapshot;

public class ObjectInfoCommand extends AbstractCommand {

    private static final Translate<LocaleResources> translator = LocaleResources.createLocalizer();

    private final BundleContext context;
    private Snapshot snapshot;

    public ObjectInfoCommand() {
        this(FrameworkUtil.getBundle(ObjectInfoCommand.class).getBundleContext());
    }

    ObjectInfoCommand(BundleContext context) {
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
        snapshot = heapDump.getSnapshot();
        JavaHeapObject obj = objCmdHelper.getJavaHeapObject();
        TableRenderer table = new TableRenderer(2);
        table.printLine(translator.localize(LocaleResources.COMMAND_OBJECT_INFO_OBJECT_ID).getContents(), obj.getIdString());
        table.printLine(translator.localize(LocaleResources.COMMAND_OBJECT_INFO_TYPE).getContents(), obj.getClazz().getName());
        table.printLine(translator.localize(LocaleResources.COMMAND_OBJECT_INFO_SIZE).getContents(), String.valueOf(obj.getSize()) + " bytes");
        table.printLine(translator.localize(LocaleResources.COMMAND_OBJECT_INFO_HEAP_ALLOCATED).getContents(), String.valueOf(obj.isHeapAllocated()));
        table.printLine(translator.localize(LocaleResources.COMMAND_OBJECT_INFO_REFERENCES).getContents(), "");
        printReferences(table, obj);
        table.printLine(translator.localize(LocaleResources.COMMAND_OBJECT_INFO_REFERRERS).getContents(), "");
        printReferrers(table, obj);

        PrintStream out = ctx.getConsole().getOutput();
        table.render(out);

    }

    private void printReferences(final TableRenderer table, final JavaHeapObject obj) {
        JavaHeapObjectVisitor v = new JavaHeapObjectVisitor() {
            
            @Override
            public void visit(JavaHeapObject ref) {
                table.printLine("", describeReference(obj, ref) + " -> " + PrintObjectUtils.objectToString(ref));
            }
            
            @Override
            public boolean mightExclude() {
                return false;
            }
            
            @Override
            public boolean exclude(JavaClass arg0, JavaField arg1) {
                return false;
            }
        };
        obj.visitReferencedObjects(v);
    }

    private void printReferrers(TableRenderer table, JavaHeapObject obj) {
        Enumeration<?> referrers = obj.getReferers();
        while (referrers.hasMoreElements()) {
            JavaHeapObject ref = (JavaHeapObject) referrers.nextElement();
            table.printLine("", PrintObjectUtils.objectToString(ref) + " -> " + describeReference(ref, obj));
        }
    }

    private String describeReference(JavaHeapObject from, JavaHeapObject to) {
        return "[" + from.describeReferenceTo(to, snapshot) + "]";
    }

}

