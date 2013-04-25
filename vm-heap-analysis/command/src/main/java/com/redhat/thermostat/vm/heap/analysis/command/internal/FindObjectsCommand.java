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

package com.redhat.thermostat.vm.heap.analysis.command.internal;

import java.util.Collection;
import java.util.List;

import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;

import com.redhat.thermostat.common.cli.AbstractCommand;
import com.redhat.thermostat.common.cli.CommandContext;
import com.redhat.thermostat.common.cli.CommandException;
import com.redhat.thermostat.common.cli.TableRenderer;
import com.redhat.thermostat.common.locale.Translate;
import com.redhat.thermostat.vm.heap.analysis.command.locale.LocaleResources;
import com.redhat.thermostat.vm.heap.analysis.common.HeapDAO;
import com.redhat.thermostat.vm.heap.analysis.common.HeapDump;
import com.redhat.thermostat.vm.heap.analysis.common.model.HeapInfo;
import com.sun.tools.hat.internal.model.JavaHeapObject;

public class FindObjectsCommand extends AbstractCommand {

    private static final Translate<LocaleResources> translator = LocaleResources.createLocalizer();

    private static final String HEAP_ID_ARG = "heapId";
    private static final String LIMIT_ARG = "limit";
    private static final String HEADER_OBJECT_ID = translator.localize(LocaleResources.HEADER_OBJECT_ID);
    private static final String HEADER_TYPE = translator.localize(LocaleResources.HEADER_OBJECT_TYPE);
    private static final int DEFAULT_LIMIT = 10;

    private BundleContext context;

    public FindObjectsCommand() {
        this(FrameworkUtil.getBundle(FindObjectsCommand.class).getBundleContext());
    }

    FindObjectsCommand(BundleContext context) {
        this.context = context;
    }

    @Override
    public void run(CommandContext ctx) throws CommandException {
        ServiceReference heapDAORef = context.getServiceReference(HeapDAO.class.getName());
        if (heapDAORef == null) {
            throw new CommandException(translator.localize(LocaleResources.HEAP_SERVICE_UNAVAILABLE));
        }
        HeapDAO heapDAO = (HeapDAO) context.getService(heapDAORef);
        try {
            run(ctx, heapDAO);
        } finally {
            context.ungetService(heapDAORef);
            heapDAO = null;
        }
    }

    private void run(CommandContext ctx, HeapDAO heapDAO) throws CommandException {
        String heapId = ctx.getArguments().getArgument(HEAP_ID_ARG);
        HeapInfo heapInfo = heapDAO.getHeapInfo(heapId);
        if (heapInfo == null) {
            throw new HeapNotFoundException(heapId);
        }
        HeapDump heapDump = heapDAO.getHeapDump(heapInfo);
        if (heapDump == null) {
            throw new HeapNotFoundException(heapId);
        }

        List<String> terms = ctx.getArguments().getNonOptionArguments();
        if (terms.size() == 0) {
            ctx.getConsole().getOutput().println(translator.localize(LocaleResources.SEARCH_TERM_REQUIRED));
            return;
        }
        String searchTerm = terms.get(0);
        if (searchTerm.trim().length() == 0) {
            ctx.getConsole().getOutput().println(translator.localize(LocaleResources.SEARCH_TERM_REQUIRED));
            return;
        }

        String limitArg = ctx.getArguments().getArgument(LIMIT_ARG);
        int limit = parseLimit(limitArg);
        Collection<String> results = heapDump.searchObjects(searchTerm, limit);
        TableRenderer table = new TableRenderer(2);
        table.printLine(HEADER_OBJECT_ID, HEADER_TYPE);
        for (String objectId : results) {
            JavaHeapObject obj = heapDump.findObject(objectId);
            String id = obj.getIdString();
            String className = obj.getClazz().getName();
            table.printLine(id, className);
        }
        table.render(ctx.getConsole().getOutput());
    }

    private int parseLimit(String limitArg) throws CommandException {
        int limit = DEFAULT_LIMIT;
        if (limitArg != null) {
            try {
                limit = Integer.parseInt(limitArg);
            } catch (NumberFormatException ex) {
                throw new CommandException(translator.localize(LocaleResources.INVALID_LIMIT, limitArg));
            }
        }
        return limit;
    }
}

