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
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.Test;

import com.redhat.thermostat.common.cli.Command;
import com.redhat.thermostat.common.cli.CommandException;
import com.redhat.thermostat.common.cli.SimpleArguments;
import com.redhat.thermostat.shared.locale.Translate;
import com.redhat.thermostat.test.TestCommandContextFactory;
import com.redhat.thermostat.testutils.StubBundleContext;
import com.redhat.thermostat.vm.heap.analysis.command.locale.LocaleResources;
import com.redhat.thermostat.vm.heap.analysis.common.HeapDAO;
import com.redhat.thermostat.vm.heap.analysis.common.ObjectHistogram;
import com.redhat.thermostat.vm.heap.analysis.common.model.HeapInfo;
import com.redhat.thermostat.vm.heap.analysis.hat.hprof.model.JavaClass;
import com.redhat.thermostat.vm.heap.analysis.hat.hprof.model.JavaHeapObject;

public class ShowHeapHistogramCommandTest {
    
    private static final Translate<LocaleResources> translator = LocaleResources.createLocalizer();

    @Test
    public void verifyWorks() throws CommandException {
        ObjectHistogram histo = new ObjectHistogram();

        JavaClass cls1 = mock(JavaClass.class);
        JavaHeapObject obj1 = mock(JavaHeapObject.class);
        when(cls1.getName()).thenReturn("class1");
        when(obj1.getClazz()).thenReturn(cls1);
        when(obj1.getSize()).thenReturn(5);
        JavaHeapObject obj2 = mock(JavaHeapObject.class);
        when(obj2.getClazz()).thenReturn(cls1);
        when(obj2.getSize()).thenReturn(3);
        JavaClass cls2 = mock(JavaClass.class);
        JavaHeapObject obj3 = mock(JavaHeapObject.class);
        when(cls2.getName()).thenReturn("verylongclassnameclass2");
        when(obj3.getClazz()).thenReturn(cls2);
        when(obj3.getSize()).thenReturn(10);

        histo.addThing(obj1);
        histo.addThing(obj2);
        histo.addThing(obj3);

        final String HEAP_ID = "heap-id-1";

        HeapDAO heapDao = mock(HeapDAO.class);

        HeapInfo heapInfo = mock(HeapInfo.class);
        when(heapDao.getHeapInfo(HEAP_ID)).thenReturn(heapInfo);
        when(heapDao.getHistogram(heapInfo)).thenReturn(histo);

        StubBundleContext context = new StubBundleContext();
        context.registerService(HeapDAO.class, heapDao, null);

        Command command = new ShowHeapHistogramCommand(context);
        TestCommandContextFactory factory = new TestCommandContextFactory();

        SimpleArguments args = new SimpleArguments();
        args.addArgument("heapId", HEAP_ID);

        command.run(factory.createContext(args));

        assertEquals("Class Name              Instances Total Size\n" +
                     "class1                  2         8\n" +
                     "verylongclassnameclass2 1         10\n", factory.getOutput());
    }

    @Test
    public void verifyWorkWithBadHeapId() throws CommandException {
        final String BAD_HEAP_ID = "invalid-heap-id";

        HeapDAO heapDao = mock(HeapDAO.class);

        when(heapDao.getHeapInfo(BAD_HEAP_ID)).thenReturn(null);
        when(heapDao.getHistogram(any(HeapInfo.class))).thenReturn(null);

        StubBundleContext context = new StubBundleContext();
        context.registerService(HeapDAO.class, heapDao, null);

        Command command = new ShowHeapHistogramCommand(context);
        TestCommandContextFactory factory = new TestCommandContextFactory();

        SimpleArguments args = new SimpleArguments();
        args.addArgument("heapId", BAD_HEAP_ID);

        try {
            command.run(factory.createContext(args));
            fail();
        } catch (CommandException e) {
            assertEquals("Heap ID not found: " + BAD_HEAP_ID, e.getMessage());
        }
    }
    
    @Test
    public void testNoHeapDAO() throws CommandException {
        final String HEAP_ID = "heap-id-1";
        StubBundleContext context = new StubBundleContext();

        Command command = new ShowHeapHistogramCommand(context);
        TestCommandContextFactory factory = new TestCommandContextFactory();

        SimpleArguments args = new SimpleArguments();
        args.addArgument("heapId", HEAP_ID);

        try {
            command.run(factory.createContext(args));
            fail();
        } catch (CommandException e) {
            assertEquals(translator.localize(LocaleResources.HEAP_SERVICE_UNAVAILABLE).getContents(), e.getMessage());
        }
    }
}

