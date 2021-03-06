/*
 * Copyright 2012-2017 Red Hat, Inc.
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

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import com.redhat.thermostat.client.cli.FileNameArgument;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;

import com.redhat.thermostat.common.cli.AbstractCommand;
import com.redhat.thermostat.common.cli.Arguments;
import com.redhat.thermostat.common.cli.CommandContext;
import com.redhat.thermostat.common.cli.CommandException;
import com.redhat.thermostat.common.cli.CommandLineArgumentParseException;
import com.redhat.thermostat.common.utils.StreamUtils;
import com.redhat.thermostat.shared.locale.Translate;
import com.redhat.thermostat.vm.heap.analysis.command.locale.LocaleResources;
import com.redhat.thermostat.vm.heap.analysis.common.HeapDAO;
import com.redhat.thermostat.vm.heap.analysis.common.model.HeapInfo;

public class SaveHeapDumpToFileCommand extends AbstractCommand {

    static final String COMMAND_NAME = "save-heap-dump-to-file";

    private static final Translate<LocaleResources> translator = LocaleResources.createLocalizer();

    private static final String HEAP_ID_ARGUMENT = "heapId";

    private final FileStreamCreator creator;
    private final BundleContext context;

    public SaveHeapDumpToFileCommand() {
        this(FrameworkUtil.getBundle(SaveHeapDumpToFileCommand.class).getBundleContext(), new FileStreamCreator());
    }

    SaveHeapDumpToFileCommand(BundleContext context, FileStreamCreator creator) {
        this.context = context;
        this.creator = creator;
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
        String heapId = args.getArgument(HEAP_ID_ARGUMENT);
        if (heapId == null) {
            throw new CommandLineArgumentParseException(translator.localize(LocaleResources.HEAP_ID_REQUIRED));
        }
        FileNameArgument fileNameArgument = FileNameArgument.required(args);
        String filename = fileNameArgument.getFileName();

        HeapInfo heapInfo = heapDAO.getHeapInfo(heapId);
        try (InputStream heapStream = heapDAO.getHeapDumpData(heapInfo)) {
            if (heapStream != null) {
                try {
                    saveHeapDump(heapStream, filename);
                    ctx.getConsole().getOutput().println(translator.localize(LocaleResources.COMMAND_SAVE_HEAP_DUMP_SAVED_TO_FILE, filename).getContents());
                } catch (IOException e) {
                    ctx.getConsole().getOutput().println(translator.localize(LocaleResources.COMMAND_SAVE_HEAP_DUMP_ERROR_SAVING, e.getMessage()).getContents());
                }
            } else {
                throw new HeapNotFoundException(heapId);
            }
        } catch (IOException e) {
            throw new CommandException(translator.localize(LocaleResources.COMMAND_SAVE_HEAP_DUMP_ERROR_CLOSING_STREAM, e.getMessage()));
        }
    }

    private void saveHeapDump(InputStream heapStream, String filename) throws IOException {
        try (BufferedInputStream bis = new BufferedInputStream(heapStream);
             BufferedOutputStream bout = new BufferedOutputStream(creator.createOutputStream(filename))) {
            StreamUtils.copyStream(bis, bout);
        }
    }

    static class FileStreamCreator {
        public OutputStream createOutputStream(String filename) throws FileNotFoundException {
            return new FileOutputStream(filename);
        }
    }
}

