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
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.same;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Collection;
import java.util.Enumeration;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import com.redhat.thermostat.client.heap.cli.HeapNotFoundException;
import com.redhat.thermostat.client.heap.cli.ObjectInfoCommand;
import com.redhat.thermostat.common.appctx.ApplicationContext;
import com.redhat.thermostat.common.appctx.ApplicationContextUtil;
import com.redhat.thermostat.common.cli.ArgumentSpec;
import com.redhat.thermostat.common.cli.CommandException;
import com.redhat.thermostat.common.cli.SimpleArgumentSpec;
import com.redhat.thermostat.common.cli.SimpleArguments;
import com.redhat.thermostat.common.dao.DAOFactory;
import com.redhat.thermostat.common.dao.HeapDAO;
import com.redhat.thermostat.common.heap.HeapDump;
import com.redhat.thermostat.common.model.HeapInfo;
import com.redhat.thermostat.test.TestCommandContextFactory;
import com.sun.tools.hat.internal.model.JavaClass;
import com.sun.tools.hat.internal.model.JavaHeapObject;
import com.sun.tools.hat.internal.model.JavaHeapObjectVisitor;
import com.sun.tools.hat.internal.model.Snapshot;

public class ObjectInfoCommandTest {

    private static final String HEAP_ID = "TEST_HEAP_ID";

    private ObjectInfoCommand cmd;
    private HeapDump heapDump;

    @Before
    public void setUp() {
        ApplicationContextUtil.resetApplicationContext();
        cmd = new ObjectInfoCommand();
        setupHeapDump();
        setupDAO();
    }

    @After
    public void tearDown() {
        heapDump = null;
        cmd = null;
        ApplicationContextUtil.resetApplicationContext();
    }

    private void setupHeapDump() {
        heapDump = mock(HeapDump.class);
        JavaClass barCls = mock(JavaClass.class);
        when(barCls.getName()).thenReturn("BarType");
        final JavaHeapObject barObj = mock(JavaHeapObject.class);
        when(barObj.getIdString()).thenReturn("456");
        when(barObj.getClazz()).thenReturn(barCls);
        JavaClass bazCls = mock(JavaClass.class);
        when(bazCls.getName()).thenReturn("BazType");
        final JavaHeapObject bazObj = mock(JavaHeapObject.class);
        when(bazObj.getIdString()).thenReturn("789");
        when(bazObj.getClazz()).thenReturn(bazCls);

        JavaClass fooCls = mock(JavaClass.class);
        when(fooCls.getName()).thenReturn("FooType");
        JavaHeapObject fooObj = mock(JavaHeapObject.class);
        when(fooObj.getIdString()).thenReturn("123");
        when(fooObj.getClazz()).thenReturn(fooCls);
        doAnswer(new Answer<Void>() {

            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                JavaHeapObjectVisitor v = (JavaHeapObjectVisitor) invocation.getArguments()[0];
                v.visit(barObj);
                v.visit(bazObj);
                return null;
            }
            
        }).when(fooObj).visitReferencedObjects(any(JavaHeapObjectVisitor.class));
        when(fooObj.describeReferenceTo(same(barObj), any(Snapshot.class))).thenReturn("field bar");
        when(fooObj.describeReferenceTo(same(bazObj), any(Snapshot.class))).thenReturn("field baz");
        @SuppressWarnings("rawtypes")
        Enumeration referrerEnum = mock(Enumeration.class);
        when(referrerEnum.hasMoreElements()).thenReturn(true).thenReturn(true).thenReturn(false);
        when(referrerEnum.nextElement()).thenReturn(barObj).thenReturn(bazObj);
        when(fooObj.getReferers()).thenReturn(referrerEnum);
        when(barObj.describeReferenceTo(same(fooObj), any(Snapshot.class))).thenReturn("field foo");
        when(bazObj.describeReferenceTo(same(fooObj), any(Snapshot.class))).thenReturn("field foo");
        when(fooObj.isNew()).thenReturn(true);
        when(fooObj.isHeapAllocated()).thenReturn(false);
        when(fooObj.getSize()).thenReturn(128);
        when(heapDump.findObject("foo")).thenReturn(fooObj);
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

    @Test
    public void testName() {
        assertEquals("object-info", cmd.getName());
    }

    @Test
    public void testDescription() {
        assertEquals("prints information about an object in a heap dump", cmd.getDescription());
    }

    @Test
    public void testUsage() {
        assertEquals("prints information about an object in a heap dump", cmd.getUsage());
    }

    @Test
    public void testAcceptedArguments() {
        Collection<ArgumentSpec> args = cmd.getAcceptedArguments();
        assertEquals(2, args.size());
        assertTrue(args.contains(new SimpleArgumentSpec("heapId", "the ID of the heapdump to analyze", true, true)));
        assertTrue(args.contains(new SimpleArgumentSpec("objectId", "the ID of the object to query", true, true)));
    }

    @Test
    public void testStorageRequired() {
        assertTrue(cmd.isStorageRequired());
    }

    @Test
    public void testDisable() {
        cmd.disable(); // No side effects... hopefully :-)
    }

    @Test
    public void testSimpleObject() throws CommandException {
        TestCommandContextFactory factory = new TestCommandContextFactory();
        SimpleArguments args = new SimpleArguments();
        args.addArgument("heapId", HEAP_ID);
        args.addArgument("objectId", "foo");

        cmd.run(factory.createContext(args));

        String expected = "Object ID:      123\n" +
                          "Type:           FooType\n" +
                          "Size:           128 bytes\n" +
                          "Heap allocated: false\n" +
                          "References:     \n" +
                          "                [field bar] -> BarType@456\n" +
                          "                [field baz] -> BazType@789\n" +
                          "Referrers:      \n" +
                          "                BarType@456 -> [field foo]\n" +
                          "                BazType@789 -> [field foo]\n";

        assertEquals(expected, factory.getOutput());

    }

    @Test(expected=CommandException.class)
    public void testHeapNotFound() throws CommandException {
        TestCommandContextFactory factory = new TestCommandContextFactory();
        SimpleArguments args = new SimpleArguments();
        args.addArgument("heapId", "fluff");
        args.addArgument("objectId", "foo");

        try {
            cmd.run(factory.createContext(args));
            fail();
        } catch (HeapNotFoundException ex) {
            assertEquals("Heap not found: fluff", ex.getMessage());
        }
    }

    public void testObjectNotFound() throws CommandException {
        TestCommandContextFactory factory = new TestCommandContextFactory();
        SimpleArguments args = new SimpleArguments();
        args.addArgument("heapId", HEAP_ID);
        args.addArgument("objectId", "fluff");

        try {
            cmd.run(factory.createContext(args));
            fail();
        } catch (ObjectNotFoundException ex) {
            assertEquals("Object not found: fluff", ex.getMessage());
        }
    }
}
