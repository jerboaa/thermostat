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

import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;

import com.redhat.thermostat.client.heap.LocaleResources;
import com.redhat.thermostat.client.heap.Translate;
import com.redhat.thermostat.common.appctx.ApplicationContext;
import com.redhat.thermostat.common.cli.Arguments;
import com.redhat.thermostat.common.cli.CommandContext;
import com.redhat.thermostat.common.cli.CommandException;
import com.redhat.thermostat.common.cli.SimpleCommand;
import com.redhat.thermostat.common.cli.TableRenderer;
import com.redhat.thermostat.common.dao.HeapDAO;
import com.redhat.thermostat.common.heap.HistogramRecord;
import com.redhat.thermostat.common.heap.ObjectHistogram;
import com.redhat.thermostat.common.model.HeapInfo;

public class ShowHeapHistogramCommand extends SimpleCommand {

    private static final String NAME = "show-heap-histogram";
    private static final String DESCRIPTION = Translate.localize(LocaleResources.COMMAND_SHOW_HEAP_HISTOGRAM_DESCRIPTION);
    private static final String USAGE = DESCRIPTION;

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
        return USAGE;
    }

    @Override
    public Options getOptions() {
        Options options = new Options();

        Option heapOption = new Option("i", "heapId", true, Translate.localize(LocaleResources.ARGUMENT_HEAP_ID_DESCRIPTION));
        heapOption.setRequired(true);
        options.addOption(heapOption);

        return options;
    }

    @Override
    public void run(CommandContext ctx) throws CommandException {
        Arguments args = ctx.getArguments();
        String heapId = args.getArgument("heapId");

        HeapDAO heapDAO = ApplicationContext.getInstance().getDAOFactory().getHeapDAO();

        HeapInfo heapInfo = heapDAO.getHeapInfo(heapId);
        if (heapInfo == null) {
            ctx.getConsole().getOutput().println(Translate.localize(LocaleResources.HEAP_ID_NOT_FOUND, heapId));
            return;
        }

        ObjectHistogram histogram = heapDAO.getHistogram(heapInfo);
        if (histogram == null) {
            ctx.getConsole().getOutput().println(Translate.localize(LocaleResources.HEAP_ID_NOT_FOUND, heapId));
            return;
        } else {
            printHeapHistogram(histogram, ctx.getConsole().getOutput());
        }
    }

    private void printHeapHistogram(ObjectHistogram histogram, PrintStream out) {
        TableRenderer table = new TableRenderer(3);
        for (HistogramRecord rec : histogram.getHistogram()) {
            table.printLine(rec.getClassname(), String.valueOf(rec.getNumberOf()), String.valueOf(rec.getTotalSize()));
        }
        table.render(out);
    }

}
