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

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Calendar;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.redhat.thermostat.client.heap.SaveHeapDumpToFileCommand.FileStreamCreator;
import com.redhat.thermostat.common.appctx.ApplicationContext;
import com.redhat.thermostat.common.appctx.ApplicationContextUtil;
import com.redhat.thermostat.common.cli.Command;
import com.redhat.thermostat.common.cli.CommandException;
import com.redhat.thermostat.common.cli.SimpleArguments;
import com.redhat.thermostat.common.dao.DAOFactory;
import com.redhat.thermostat.common.dao.HeapDAO;
import com.redhat.thermostat.common.dao.HostRef;
import com.redhat.thermostat.common.dao.VmRef;
import com.redhat.thermostat.common.model.HeapInfo;
import com.redhat.thermostat.test.TestCommandContextFactory;

public class SaveHeapDumpToFileCommandTest {

    @Before
    public void setUp() {
        ApplicationContextUtil.resetApplicationContext();
    }

    @After
    public void tearDown() {
        ApplicationContextUtil.resetApplicationContext();
    }

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

        Command command = new SaveHeapDumpToFileCommand();
        command.run(factory.createContext(args));
    }

    @Test (expected=CommandException.class)
    public void verifyMissingFileNameThrowsException() throws CommandException {
        TestCommandContextFactory factory = new TestCommandContextFactory();

        SimpleArguments args = new SimpleArguments();
        args.addArgument("hostId", "host-id");
        args.addArgument("vmId", "1");
        args.addArgument("heapId", "heap-id-1");

        Command command = new SaveHeapDumpToFileCommand();
        command.run(factory.createContext(args));
    }

    @Test
    public void verifyCommandWorks() throws CommandException, FileNotFoundException {
        String heap = "0xCAFEBABE";
        ByteArrayOutputStream heapDumpStream = new ByteArrayOutputStream();

        HostRef hostRef = mock(HostRef.class);
        when(hostRef.getStringID()).thenReturn("host-id");
        VmRef vmRef = mock(VmRef.class);
        when(vmRef.getStringID()).thenReturn("1");

        HeapInfo heapInfo = mock(HeapInfo.class);
        Calendar timestamp = Calendar.getInstance();
        timestamp.set(2012, 5, 7, 15, 32, 0);
        when(heapInfo.getTimestamp()).thenReturn(timestamp.getTimeInMillis());
        when(heapInfo.getHeapDumpId()).thenReturn("heap-id-1");

        HeapDAO heapDao = mock(HeapDAO.class);

        when(heapDao.getAllHeapInfo(isA(VmRef.class))).thenReturn(Arrays.asList(heapInfo));
        when(heapDao.getHeapDump(heapInfo)).thenReturn(new ByteArrayInputStream(heap.getBytes(Charset.forName("UTF-8"))));
        DAOFactory daoFactory = mock(DAOFactory.class);
        when(daoFactory.getHeapDAO()).thenReturn(heapDao);

        ApplicationContext.getInstance().setDAOFactory(daoFactory);

        TestCommandContextFactory factory = new TestCommandContextFactory();

        SimpleArguments args = new SimpleArguments();
        args.addArgument("hostId", "host-id");
        args.addArgument("vmId", "1");
        args.addArgument("heapId", "heap-id-1");
        args.addArgument("file", "some-file-name");

        FileStreamCreator creator = mock(FileStreamCreator.class);
        when(creator.createOutputStream("some-file-name")).thenReturn(heapDumpStream);

        Command command = new SaveHeapDumpToFileCommand(creator);
        command.run(factory.createContext(args));

        assertArrayEquals(heap.getBytes(Charset.forName("UTF-8")), heapDumpStream.toByteArray());
    }

}
