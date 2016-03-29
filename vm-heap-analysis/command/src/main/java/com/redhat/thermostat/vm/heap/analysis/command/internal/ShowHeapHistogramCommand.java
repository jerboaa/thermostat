/*
 * Copyright 2012-2016 Red Hat, Inc.
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

import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;

import com.redhat.thermostat.common.cli.AbstractCommand;
import com.redhat.thermostat.common.cli.Arguments;
import com.redhat.thermostat.common.cli.CommandContext;
import com.redhat.thermostat.common.cli.CommandException;
import com.redhat.thermostat.common.cli.TableRenderer;
import com.redhat.thermostat.shared.locale.Translate;
import com.redhat.thermostat.vm.heap.analysis.command.locale.LocaleResources;
import com.redhat.thermostat.vm.heap.analysis.common.HeapDAO;
import com.redhat.thermostat.vm.heap.analysis.common.HistogramRecord;
import com.redhat.thermostat.vm.heap.analysis.common.ObjectHistogram;
import com.redhat.thermostat.vm.heap.analysis.common.model.HeapInfo;

public class ShowHeapHistogramCommand extends AbstractCommand {

    static final String COMMAND_NAME = "show-heap-histogram";

    private static final Translate<LocaleResources> translator = LocaleResources.createLocalizer();

    private final BundleContext context;
    
    public ShowHeapHistogramCommand() {
        this(FrameworkUtil.getBundle(ShowHeapHistogramCommand.class).getBundleContext());
    }

    ShowHeapHistogramCommand(BundleContext context) {
        this.context = context;
    }

    @Override
    public void run(CommandContext ctx) throws CommandException {
        ServiceReference ref = context.getServiceReference(HeapDAO.class.getName());
        requireNonNull(ref, translator.localize(LocaleResources.HEAP_SERVICE_UNAVAILABLE));
        HeapDAO heapDAO = (HeapDAO) context.getService(ref);

        try {
            run(ctx, heapDAO);
        } finally {
            context.ungetService(ref);
        }
    }

    private void run(CommandContext ctx, HeapDAO heapDAO) throws CommandException {
        Arguments args = ctx.getArguments();
        String heapId = args.getArgument("heapId");

        HeapInfo heapInfo = heapDAO.getHeapInfo(heapId);
        if (heapInfo == null) {
            throw new HeapNotFoundException(heapId);
        }

        ObjectHistogram histogram = heapDAO.getHistogram(heapInfo);
        if (histogram == null) {
            ctx.getConsole().getOutput().println(translator.localize(LocaleResources.ERROR_READING_HISTOGRAM_MESSAGE, heapId).getContents());
            return;
        } else {
            printHeapHistogram(histogram, ctx.getConsole().getOutput());
        }
    }

    private void printHeapHistogram(ObjectHistogram histogram, PrintStream out) {
        TableRenderer table = new TableRenderer(3);
        table.printHeader(translator.localize(LocaleResources.TABLE_CLASS_NAME).getContents(),
                translator.localize(LocaleResources.TABLE_NUMBER_INSTANCES).getContents(),
                translator.localize(LocaleResources.TABLE_TOTAL_SIZE).getContents());
        for (HistogramRecord rec : histogram.getHistogram()) {
            table.printLine(rec.getClassname(), String.valueOf(rec.getNumberOf()), String.valueOf(rec.getTotalSize()));
        }
        table.render(out);
    }

}

