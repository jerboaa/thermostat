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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.same;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Enumeration;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import com.redhat.thermostat.common.cli.CommandException;
import com.redhat.thermostat.common.cli.SimpleArguments;
import com.redhat.thermostat.common.internal.test.TestCommandContextFactory;
import com.redhat.thermostat.shared.locale.Translate;
import com.redhat.thermostat.testutils.StubBundleContext;
import com.redhat.thermostat.vm.heap.analysis.command.locale.LocaleResources;
import com.redhat.thermostat.vm.heap.analysis.common.HeapDAO;
import com.redhat.thermostat.vm.heap.analysis.common.HeapDump;
import com.redhat.thermostat.vm.heap.analysis.common.model.HeapInfo;
import com.redhat.thermostat.vm.heap.analysis.hat.hprof.model.JavaClass;
import com.redhat.thermostat.vm.heap.analysis.hat.hprof.model.JavaHeapObject;
import com.redhat.thermostat.vm.heap.analysis.hat.hprof.model.JavaHeapObjectVisitor;
import com.redhat.thermostat.vm.heap.analysis.hat.hprof.model.Snapshot;

public class ObjectInfoCommandTest {

    private static final Translate<LocaleResources> translator = LocaleResources.createLocalizer();
    private static final String HEAP_ID = "TEST_HEAP_ID";

    private ObjectInfoCommand cmd;
    private HeapDump heapDump;

    private HeapDAO dao;

    @Before
    public void setUp() {
        setupHeapDump();
        setupDAO();
    }

    @After
    public void tearDown() {
        heapDump = null;
        cmd = null;
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

        dao = mock(HeapDAO.class);
        when(dao.getHeapInfo(HEAP_ID)).thenReturn(heapInfo);
        when(dao.getHeapDump(heapInfo)).thenReturn(heapDump);
    }

    @Test
    public void testStorageRequired() {
        StubBundleContext context = new StubBundleContext();
        cmd = new ObjectInfoCommand(context);
        assertTrue(cmd.isStorageRequired());
    }

    @Test
    public void testSimpleObject() throws CommandException {
        StubBundleContext context = new StubBundleContext();
        context.registerService(HeapDAO.class, dao, null);
        cmd = new ObjectInfoCommand(context);
        
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

    @Test
    public void testHeapNotFound() throws CommandException {
        StubBundleContext context = new StubBundleContext();
        context.registerService(HeapDAO.class, dao, null);
        cmd = new ObjectInfoCommand(context);
        
        TestCommandContextFactory factory = new TestCommandContextFactory();
        SimpleArguments args = new SimpleArguments();
        args.addArgument("heapId", "fluff");
        args.addArgument("objectId", "foo");

        try {
            cmd.run(factory.createContext(args));
            fail();
        } catch (HeapNotFoundException ex) {
            assertEquals("Heap ID not found: fluff", ex.getMessage());
        }
    }

    @Test
    public void testObjectNotFound() throws CommandException {
        StubBundleContext context = new StubBundleContext();
        context.registerService(HeapDAO.class, dao, null);
        cmd = new ObjectInfoCommand(context);
        
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
    
    @Test
    public void testNoHeapDAO() throws CommandException {
        StubBundleContext context = new StubBundleContext();
        cmd = new ObjectInfoCommand(context);
        
        TestCommandContextFactory factory = new TestCommandContextFactory();
        SimpleArguments args = new SimpleArguments();
        args.addArgument("heapId", HEAP_ID);
        args.addArgument("objectId", "foo");

        try {
            cmd.run(factory.createContext(args));
            fail();
        } catch (CommandException e) {
            assertEquals(translator.localize(LocaleResources.HEAP_SERVICE_UNAVAILABLE).getContents(), e.getMessage());
        }
    }
}

