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

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.redhat.thermostat.common.appctx.ApplicationContext;
import com.redhat.thermostat.common.cli.ArgumentSpec;
import com.redhat.thermostat.common.cli.Arguments;
import com.redhat.thermostat.common.cli.Command;
import com.redhat.thermostat.common.cli.CommandContext;
import com.redhat.thermostat.common.cli.CommandException;
import com.redhat.thermostat.common.cli.HostVMArguments;
import com.redhat.thermostat.common.cli.SimpleArgumentSpec;
import com.redhat.thermostat.common.dao.HeapDAO;
import com.redhat.thermostat.common.dao.VmRef;
import com.redhat.thermostat.common.model.HeapInfo;
import com.redhat.thermostat.common.utils.StreamUtils;

public class SaveHeapDumpToFileCommand implements Command {

    private static final String NAME = "save-heap-dump-to-file";
    private static final String DESCRIPTION = "saves a heap dump to a local file";
    private static final String USAGE = DESCRIPTION;

    private static final String HEAP_ID_ARGUMENT = "heapId";
    private static final String FILE_NAME_ARGUMENT = "file";

    private final FileStreamCreator creator;

    public SaveHeapDumpToFileCommand() {
        this(new FileStreamCreator());
    }

    SaveHeapDumpToFileCommand(FileStreamCreator creator) {
        this.creator = creator;
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
        return USAGE;
    }

    @Override
    public Collection<ArgumentSpec> getAcceptedArguments() {
        List<ArgumentSpec> args = new ArrayList<>();
        args.addAll(HostVMArguments.getArgumentSpecs());
        args.add(new SimpleArgumentSpec(HEAP_ID_ARGUMENT, HEAP_ID_ARGUMENT, "the heap id", true, true));
        args.add(new SimpleArgumentSpec(FILE_NAME_ARGUMENT, "f", "the file name to save to", true, true));

        return args;
    }

    @Override
    public boolean isStorageRequired() {
        return true;
    }

    @Override
    public void run(CommandContext ctx) throws CommandException {
        Arguments args = ctx.getArguments();
        VmRef vmRef = new HostVMArguments(args).getVM();
        String heapId = args.getArgument(HEAP_ID_ARGUMENT);
        if (heapId == null) {
            throw new CommandException("heapId required");
        }
        String filename = args.getArgument(FILE_NAME_ARGUMENT);
        if (filename == null) {
            throw new CommandException("file required");
        }

        HeapDAO heapDAO = ApplicationContext.getInstance().getDAOFactory().getHeapDAO();
        Collection<HeapInfo> allHeapInfos = heapDAO.getAllHeapInfo(vmRef);
        for (HeapInfo heapInfo : allHeapInfos) {
            if (heapInfo.getHeapDumpId().equals(heapId)) {
                try {
                    ctx.getConsole().getOutput().print("saving dump to " + filename + "\n");
                    saveHeapDump(heapDAO, heapInfo, filename);
                } catch (IOException ioe) {
                    ctx.getConsole().getOutput().print("error saving to file: " + ioe.getMessage());
                }
            }
        }
    }

    private void saveHeapDump(HeapDAO heapDAO, HeapInfo heapInfo, String filename) throws FileNotFoundException, IOException {
        try (BufferedInputStream bis = new BufferedInputStream(heapDAO.getHeapDump(heapInfo));
             BufferedOutputStream bout = new BufferedOutputStream(creator.createOutputStream(filename))) {
            StreamUtils.copyStream(bis, bout);
        }
    }

    @Override
    public void disable() {
        /* NO-OP */
    }

    static class FileStreamCreator {
        public OutputStream createOutputStream(String filename) throws FileNotFoundException {
            return new FileOutputStream(filename);
        }
    }
}
