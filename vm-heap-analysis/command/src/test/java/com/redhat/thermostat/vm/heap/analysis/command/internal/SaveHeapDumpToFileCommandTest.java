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

package com.redhat.thermostat.vm.heap.analysis.command.internal;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.nio.charset.Charset;

import org.junit.Test;

import com.redhat.thermostat.common.cli.Command;
import com.redhat.thermostat.common.cli.CommandException;
import com.redhat.thermostat.common.cli.SimpleArguments;
import com.redhat.thermostat.common.utils.OSGIUtils;
import com.redhat.thermostat.storage.model.HeapInfo;
import com.redhat.thermostat.test.TestCommandContextFactory;
import com.redhat.thermostat.vm.heap.analysis.command.internal.SaveHeapDumpToFileCommand;
import com.redhat.thermostat.vm.heap.analysis.command.internal.SaveHeapDumpToFileCommand.FileStreamCreator;
import com.redhat.thermostat.vm.heap.analysis.common.HeapDAO;

public class SaveHeapDumpToFileCommandTest {

    @Test
    public void verifyBasicInformation() {
        Command command = new SaveHeapDumpToFileCommand();
        assertEquals("save-heap-dump-to-file", command.getName());
        assertNotNull(command.getDescription());
        assertNotNull(command.getUsage());
    }

    @Test (expected=CommandException.class)
    public void verifyMissingHeapIdThrowsException() throws CommandException {
        TestCommandContextFactory factory = new TestCommandContextFactory();

        SimpleArguments args = new SimpleArguments();
        args.addArgument("hostId", "host-id");
        args.addArgument("vmId", "1");
        args.addArgument("file", "heap-id-1");

        OSGIUtils serviceProvider = mock(OSGIUtils.class);
        when(serviceProvider.getServiceAllowNull(HeapDAO.class)).thenReturn(mock(HeapDAO.class));

        Command command = new SaveHeapDumpToFileCommand(serviceProvider, mock(FileStreamCreator.class));
        command.run(factory.createContext(args));
    }

    @Test (expected=CommandException.class)
    public void verifyMissingFileNameThrowsException() throws CommandException {
        TestCommandContextFactory factory = new TestCommandContextFactory();

        SimpleArguments args = new SimpleArguments();
        args.addArgument("hostId", "host-id");
        args.addArgument("vmId", "1");
        args.addArgument("heapId", "heap-id-1");

        OSGIUtils serviceProvider = mock(OSGIUtils.class);
        when(serviceProvider.getServiceAllowNull(HeapDAO.class)).thenReturn(mock(HeapDAO.class));

        Command command = new SaveHeapDumpToFileCommand(serviceProvider, mock(FileStreamCreator.class));
        command.run(factory.createContext(args));
    }

    @Test
    public void verifyCommandWorks() throws CommandException, FileNotFoundException {
        final String HEAP_ID = "heap-id-1";
        final String FILE_NAME = "some-file-name";
        final String HEAP_CONTENTS = "0xCAFEBABE";
        final byte[] HEAP_CONTENT_BYTES = HEAP_CONTENTS.getBytes(Charset.forName("UTF-8"));

        ByteArrayOutputStream heapDumpStream = new ByteArrayOutputStream();

        HeapDAO heapDao = mock(HeapDAO.class);

        HeapInfo info = mock(HeapInfo.class);
        when(heapDao.getHeapInfo(HEAP_ID)).thenReturn(info);
        when(heapDao.getHeapDumpData(info)).thenReturn(new ByteArrayInputStream(HEAP_CONTENT_BYTES));

        OSGIUtils serviceProvider = mock(OSGIUtils.class);
        when(serviceProvider.getServiceAllowNull(HeapDAO.class)).thenReturn(heapDao);

        TestCommandContextFactory factory = new TestCommandContextFactory();

        SimpleArguments args = new SimpleArguments();
        args.addArgument("heapId", HEAP_ID);
        args.addArgument("file", FILE_NAME);

        FileStreamCreator creator = mock(FileStreamCreator.class);
        when(creator.createOutputStream(FILE_NAME)).thenReturn(heapDumpStream);

        Command command = new SaveHeapDumpToFileCommand(serviceProvider, creator);
        command.run(factory.createContext(args));

        assertArrayEquals(HEAP_CONTENT_BYTES, heapDumpStream.toByteArray());
    }

}
