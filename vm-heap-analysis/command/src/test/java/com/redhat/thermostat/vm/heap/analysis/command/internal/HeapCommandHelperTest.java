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

import com.redhat.thermostat.common.cli.Arguments;
import com.redhat.thermostat.common.cli.CommandContext;
import com.redhat.thermostat.common.cli.CommandException;
import com.redhat.thermostat.common.cli.Console;
import com.redhat.thermostat.vm.heap.analysis.common.HeapDAO;
import com.redhat.thermostat.vm.heap.analysis.common.HeapDump;
import com.redhat.thermostat.vm.heap.analysis.common.model.HeapInfo;
import com.redhat.thermostat.vm.heap.analysis.hat.hprof.model.JavaHeapObject;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

public class HeapCommandHelperTest {

    CommandContext ctx;
    Arguments args;
    HeapDAO dao;
    HeapInfo heapInfo;
    HeapDump heapDump;
    JavaHeapObject heapObject;
    Console console;
    HeapCommandHelper helper;
    public static final String HEAP_ID = "fooId";
    public static final String OBJECT_ID = "objectId";

    @Before
    public void setup() {
        ctx = mock(CommandContext.class);
        args = mock(Arguments.class);
        dao = mock(HeapDAO.class);
        heapInfo = mock(HeapInfo.class);
        heapDump = mock(HeapDump.class);
        heapObject = mock(JavaHeapObject.class);
        console = mock(Console.class);

        when(ctx.getArguments()).thenReturn(args);
        when(ctx.getConsole()).thenReturn(console);

        when(args.hasArgument(HeapCommandHelper.HEAP_ID_ARG)).thenReturn(true);
        when(args.getArgument(HeapCommandHelper.HEAP_ID_ARG)).thenReturn(HEAP_ID);
        when(args.hasArgument(HeapCommandHelper.OBJECT_ID_ARG)).thenReturn(true);
        when(args.getArgument(HeapCommandHelper.OBJECT_ID_ARG)).thenReturn(OBJECT_ID);

        when(dao.getHeapInfo(HEAP_ID)).thenReturn(heapInfo);
        when(dao.getHeapDump(heapInfo)).thenReturn(heapDump);

        when(heapDump.findObject(OBJECT_ID)).thenReturn(heapObject);

        helper = new HeapCommandHelper(new HeapCommandHelper.HeapDumpIdentifier(HEAP_ID, dao));
    }

    @Test
    public void testGetHeapDumpReturnsExpectedDump() throws HeapNotFoundException {
        HeapDump dump = helper.getHeapDump();
        assertThat(dump, is(heapDump));
    }

    @Test
    public void verifyHeapDumpsAreCached() throws HeapNotFoundException {
        HeapDump dump = helper.getHeapDump();
        verify(dao).getHeapInfo(HEAP_ID);
        verify(dao).getHeapDump(heapInfo);
        assertThat(dump, is(heapDump));
        HeapDump dump2 = helper.getHeapDump();
        assertThat(dump2, is(dump));
        // verify no additional DAO accesses due to second getHeapDump call
        verify(dao).getHeapInfo(HEAP_ID);
        verify(dao).getHeapDump(heapInfo);
    }

    @Test
    public void testGetJavaHeapObject() throws CommandException {
        JavaHeapObject obj = helper.getJavaHeapObject(ctx);
        assertThat(obj, is(heapObject));
        verify(heapDump).findObject(OBJECT_ID);
    }

    @Test
    public void verifyGetJavaHeapObjectIsCached() throws CommandException {
        JavaHeapObject obj = helper.getJavaHeapObject(ctx);
        verify(dao).getHeapInfo(HEAP_ID);
        verify(dao).getHeapDump(heapInfo);
        assertThat(obj, is(heapObject));
        JavaHeapObject obj2 = helper.getJavaHeapObject(ctx);
        assertThat(obj2, is(obj));
        // verify no additional DAO accesses due to second getJavaHeapObject call
        verify(dao).getHeapInfo(HEAP_ID);
        verify(dao).getHeapDump(heapInfo);
    }

    @Test(expected = ObjectNotFoundException.class)
    public void assertExceptionWhenGetJavaHeapObjectSuppliedInvalidId() throws CommandException {
        when(args.getArgument(OBJECT_ID)).thenReturn("fluff");
        helper.getJavaHeapObject(ctx);
    }

    @Test
    public void testHelperCaching() throws HeapNotFoundException {
        when(dao.getHeapInfo(any(String.class))).thenReturn(heapInfo);
        when(dao.getHeapDump(any(HeapInfo.class))).thenReturn(heapDump);

        List<HeapCommandHelper> helpers = new ArrayList<>(HeapCommandHelper.MAXIMUM_CACHED_HELPERS);
        for (int i = 0; i < HeapCommandHelper.MAXIMUM_CACHED_HELPERS; i++) {
            String heapId = "heapId-" + i;
            when(args.getArgument(HeapCommandHelper.HEAP_ID_ARG)).thenReturn(heapId);
            HeapCommandHelper helper = HeapCommandHelper.getHelper(ctx, dao);
            helpers.add(helper);
        }

        // verify no duplicate helpers were returned to us
        Set<HeapCommandHelper> helperSet = new HashSet<>(helpers);
        assertThat(helpers.size(), is(equalTo(helperSet.size())));

        // check each helper so far generated no dao accesses (we haven't accessed any dumps or heap objects yet)
        verifyZeroInteractions(dao);

        // use the helpers in order to ensure that they have cached their results and that the 1st is LRU
        int accessCount = 0;
        for (int i = 0; i < HeapCommandHelper.MAXIMUM_CACHED_HELPERS; i++) {
            String heapId = "heapId-" + i;
            when(args.getArgument(HeapCommandHelper.HEAP_ID_ARG)).thenReturn(heapId);
            HeapCommandHelper helper = HeapCommandHelper.getHelper(ctx, dao);
            helper.getHeapDump();
            accessCount++;
        }

        verify(dao, times(accessCount)).getHeapInfo(anyString());
        verify(dao, times(accessCount)).getHeapDump(any(HeapInfo.class));

        // this should evict the old 1st helper which was LRU
        String newHeapId = "heapId-" + HeapCommandHelper.MAXIMUM_CACHED_HELPERS;
        when(args.getArgument(HeapCommandHelper.HEAP_ID_ARG)).thenReturn(newHeapId);
        HeapCommandHelper newHelper = HeapCommandHelper.getHelper(ctx, dao);

        newHelper.getHeapDump();
        accessCount++;
        verify(dao, times(accessCount)).getHeapInfo(anyString());
        verify(dao, times(accessCount)).getHeapDump(any(HeapInfo.class));

        // ask for the evicted helper again and ensure that this produces dao accesses the first time again (only),
        // in other words ensure that it really was evicted but is subsequently re-cached
        String evictedId = "heapId-0";
        when(args.getArgument(HeapCommandHelper.HEAP_ID_ARG)).thenReturn(evictedId);
        HeapCommandHelper evictedHelper = HeapCommandHelper.getHelper(ctx, dao);

        evictedHelper.getHeapDump();
        accessCount++;
        verify(dao, times(accessCount)).getHeapInfo(anyString());
        verify(dao, times(accessCount)).getHeapDump(any(HeapInfo.class));

        assertThat(HeapCommandHelper.getHelper(ctx, dao), is(evictedHelper));
    }

}
