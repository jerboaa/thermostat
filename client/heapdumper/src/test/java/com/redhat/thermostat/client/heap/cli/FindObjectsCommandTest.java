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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Arrays;

import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import com.redhat.thermostat.client.heap.cli.FindObjectsCommand;
import com.redhat.thermostat.common.appctx.ApplicationContext;
import com.redhat.thermostat.common.appctx.ApplicationContextUtil;
import com.redhat.thermostat.common.cli.CommandException;
import com.redhat.thermostat.common.cli.SimpleArguments;
import com.redhat.thermostat.common.dao.DAOFactory;
import com.redhat.thermostat.common.dao.HeapDAO;
import com.redhat.thermostat.common.heap.HeapDump;
import com.redhat.thermostat.common.model.HeapInfo;
import com.redhat.thermostat.test.TestCommandContextFactory;
import com.sun.tools.hat.internal.model.JavaClass;
import com.sun.tools.hat.internal.model.JavaHeapObject;

public class FindObjectsCommandTest {

    private static final String HEAP_ID = "TEST_HEAP_ID";

    private FindObjectsCommand cmd;

    private HeapDump heapDump;

    @Before
    public void setUp() {
        ApplicationContextUtil.resetApplicationContext();

        cmd = new FindObjectsCommand();

        setupHeapDump();

        setupDAO();

    }

    private void setupHeapDump() {
        heapDump = mock(HeapDump.class);
        JavaClass fooCls = mock(JavaClass.class);
        when(fooCls.getName()).thenReturn("FooType");
        JavaHeapObject fooObj = mock(JavaHeapObject.class);
        when(fooObj.getIdString()).thenReturn("123");
        when(fooObj.getClazz()).thenReturn(fooCls);
        JavaClass barCls = mock(JavaClass.class);
        when(barCls.getName()).thenReturn("BarType");
        JavaHeapObject barObj = mock(JavaHeapObject.class);
        when(barObj.getIdString()).thenReturn("456");
        when(barObj.getClazz()).thenReturn(barCls);
        JavaClass bazCls = mock(JavaClass.class);
        when(bazCls.getName()).thenReturn("BazType");
        JavaHeapObject bazObj = mock(JavaHeapObject.class);
        when(bazObj.getIdString()).thenReturn("789");
        when(bazObj.getClazz()).thenReturn(bazCls);

        when(heapDump.searchObjects("fluff", 10)).thenReturn(Arrays.asList("foo", "bar", "baz"));
        when(heapDump.searchObjects("fluff", 2)).thenReturn(Arrays.asList("foo", "bar"));
        when(heapDump.findObject("foo")).thenReturn(fooObj);
        when(heapDump.findObject("bar")).thenReturn(barObj);
        when(heapDump.findObject("baz")).thenReturn(bazObj);
    }

    private void setupDAO() {

        HeapInfo heapInfo = mock(HeapInfo.class);

        HeapDAO dao = mock(HeapDAO.class);
        when(dao.getHeapInfo(HEAP_ID)).thenReturn(heapInfo);
        when(dao.getHeapDump(heapInfo)).thenReturn(heapDump);

        DAOFactory daoFactory = mock(DAOFactory.class);
        when(daoFactory.getHeapDAO()).thenReturn(dao);

        ApplicationContext.getInstance().setDAOFactory(daoFactory);
    }

    @After
    public void tearDown() {
        heapDump = null;
        cmd = null;
        ApplicationContextUtil.resetApplicationContext();
    }

    @Test
    public void testName() {
        assertEquals("find-objects", cmd.getName());
    }

    @Test
    public void testDescAndUsage() {
        assertNotNull(cmd.getDescription());
        assertNotNull(cmd.getUsage());
    }

    @Ignore
    @Test
    public void testOptions() {
        Options options = cmd.getOptions();
        assertEquals(2, options.getOptions().size());

        assertTrue(options.hasOption("heapId"));
        Option heapOption = options.getOption("heapId");
        assertEquals("the ID of the heapdump to analyze", heapOption.getDescription());
        assertTrue(heapOption.isRequired());
        assertTrue(heapOption.hasArg());

        assertTrue(options.hasOption("limit"));
        Option limitOption = options.getOption("limit");
        assertEquals("limit search to top N results, defaults to 10", limitOption.getDescription());
        assertFalse(limitOption.isRequired());
        assertTrue(limitOption.hasArg());
    }

    @Test
    public void testStorageRequired() {
        assertTrue(cmd.isStorageRequired());
    }

    @Test
    public void testSimpleSearch() throws CommandException {

        TestCommandContextFactory factory = new TestCommandContextFactory();
        SimpleArguments args = new SimpleArguments();
        args.addArgument("heapId", HEAP_ID);
        args.addNonOptionArgument("fluff");

        cmd.run(factory.createContext(args));

        String expected = "ID  TYPE\n" +
                          "123 FooType\n" +
                          "456 BarType\n" +
                          "789 BazType\n";

        assertEquals(expected, factory.getOutput());

    }

    @Test
    public void testSearchWithLimit() throws CommandException {

        TestCommandContextFactory factory = new TestCommandContextFactory();
        SimpleArguments args = new SimpleArguments();
        args.addArgument("heapId", HEAP_ID);
        args.addArgument("limit", "2");
        args.addNonOptionArgument("fluff");

        cmd.run(factory.createContext(args));

        String expected = "ID  TYPE\n" +
                          "123 FooType\n" +
                          "456 BarType\n";

        assertEquals(expected, factory.getOutput());

    }

    @Test(expected=CommandException.class)
    public void testSearchWithInvalidLimit() throws CommandException {

        TestCommandContextFactory factory = new TestCommandContextFactory();
        SimpleArguments args = new SimpleArguments();
        args.addArgument("heapId", HEAP_ID);
        args.addArgument("limit", "urgs");
        args.addNonOptionArgument("fluff");

        cmd.run(factory.createContext(args));


    }

    @Test
    public void testSearchWithBadHeapId() throws CommandException {
        final String INVALID_HEAP_ID = "foobarbaz";


        HeapDAO dao = mock(HeapDAO.class);
        when(dao.getHeapInfo(INVALID_HEAP_ID)).thenReturn(null);
        when(dao.getHeapDump(isA(HeapInfo.class))).thenReturn(null);

        DAOFactory daoFactory = mock(DAOFactory.class);
        when(daoFactory.getHeapDAO()).thenReturn(dao);

        ApplicationContext.getInstance().setDAOFactory(daoFactory);

        TestCommandContextFactory factory = new TestCommandContextFactory();
        SimpleArguments args = new SimpleArguments();
        args.addArgument("heapId", INVALID_HEAP_ID);

        cmd.run(factory.createContext(args));

        assertEquals("Heap ID not found: " + INVALID_HEAP_ID + "\n", factory.getOutput());
    }
}
