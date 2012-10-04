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

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import com.redhat.thermostat.client.heap.LocaleResources;
import com.redhat.thermostat.client.heap.Translate;
import com.redhat.thermostat.common.cli.Arguments;
import com.redhat.thermostat.common.cli.CommandContext;
import com.redhat.thermostat.common.cli.CommandException;
import com.redhat.thermostat.common.cli.SimpleCommand;
import com.redhat.thermostat.common.dao.HeapDAO;
import com.redhat.thermostat.common.model.HeapInfo;
import com.redhat.thermostat.common.utils.OSGIUtils;
import com.redhat.thermostat.common.utils.StreamUtils;

public class SaveHeapDumpToFileCommand extends SimpleCommand {

    private static final String NAME = "save-heap-dump-to-file";

    private static final String HEAP_ID_ARGUMENT = "heapId";
    private static final String FILE_NAME_ARGUMENT = "file";

    private final FileStreamCreator creator;
    private final OSGIUtils serviceProvider;

    public SaveHeapDumpToFileCommand() {
        this(OSGIUtils.getInstance(), new FileStreamCreator());
    }

    SaveHeapDumpToFileCommand(OSGIUtils serviceProvider, FileStreamCreator creator) {
        this.serviceProvider = serviceProvider;
        this.creator = creator;
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override

    public void run(CommandContext ctx) throws CommandException {
        HeapDAO heapDAO = serviceProvider.getServiceAllowNull(HeapDAO.class);
        try {
            run(ctx, heapDAO);
        } finally {
            serviceProvider.ungetService(HeapDAO.class, heapDAO);
            heapDAO = null;
        }
    }

    private void run(CommandContext ctx, HeapDAO heapDAO) throws CommandException {
        Arguments args = ctx.getArguments();
        String heapId = args.getArgument(HEAP_ID_ARGUMENT);
        if (heapId == null) {
            throw new CommandException(Translate.localize(LocaleResources.HEAP_ID_REQUIRED));
        }
        String filename = args.getArgument(FILE_NAME_ARGUMENT);
        if (filename == null) {
            throw new CommandException(Translate.localize(LocaleResources.FILE_REQUIRED));
        }

        HeapInfo heapInfo = heapDAO.getHeapInfo(heapId);
        try (InputStream heapStream = heapDAO.getHeapDumpData(heapInfo)) {
            if (heapStream != null) {
                try {
                    saveHeapDump(heapStream, filename);
                    ctx.getConsole().getOutput().println(Translate.localize(LocaleResources.COMMAND_SAVE_HEAP_DUMP_SAVED_TO_FILE, filename));
                } catch (IOException e) {
                    ctx.getConsole().getOutput().println(Translate.localize(LocaleResources.COMMAND_SAVE_HEAP_DUMP_ERROR_SAVING, e.getMessage()));
                }
            } else {
                ctx.getConsole().getOutput().println(Translate.localize(LocaleResources.HEAP_ID_NOT_FOUND, heapId));
            }
        } catch (IOException e) {
            throw new CommandException(Translate.localize(LocaleResources.COMMAND_SAVE_HEAP_DUMP_ERROR_CLOSING_STREAM, e.getMessage()));
        }
    }

    private void saveHeapDump(InputStream heapStream, String filename) throws FileNotFoundException, IOException {
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
