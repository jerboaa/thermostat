/*
 * Copyright 2012-2015 Red Hat, Inc.
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

package com.redhat.thermostat.vm.heap.analysis.client.core.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import com.redhat.thermostat.common.ActionEvent;
import com.redhat.thermostat.common.ActionListener;
import com.redhat.thermostat.vm.heap.analysis.client.core.HeapObjectUI;
import com.redhat.thermostat.vm.heap.analysis.client.core.ObjectRootsView;
import com.redhat.thermostat.vm.heap.analysis.client.core.ObjectRootsView.Action;
import com.redhat.thermostat.vm.heap.analysis.client.core.ObjectRootsViewProvider;
import com.redhat.thermostat.vm.heap.analysis.command.FindRoot;
import com.redhat.thermostat.vm.heap.analysis.command.HeapPath;
import com.redhat.thermostat.vm.heap.analysis.common.HeapDump;
import com.redhat.thermostat.vm.heap.analysis.hat.hprof.model.JavaClass;
import com.redhat.thermostat.vm.heap.analysis.hat.hprof.model.JavaHeapObject;

public class ObjectRootsControllerTest {

    ObjectRootsController controller;

    HeapDump heapDump;
    JavaHeapObject heapObject;
    JavaClass clazz;
    FindRoot rootFinder;

    ObjectRootsView view;
    ActionListener<Action> listener;

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Before
    public void setUp() {
        // Set up views
        view = mock(ObjectRootsView.class);

        ObjectRootsViewProvider viewProvider = mock(ObjectRootsViewProvider.class);
        when(viewProvider.createView()).thenReturn(view);
        
        // set up models
        heapObject = mock(JavaHeapObject.class);
        when(heapObject.getIdString()).thenReturn("id-string");

        clazz = mock(JavaClass.class);
        when(clazz.getName()).thenReturn("class");
        when(heapObject.getClazz()).thenReturn(clazz);

        heapDump = mock(HeapDump.class);
        when(heapDump.findObject("test")).thenReturn(heapObject);

        rootFinder = mock(FindRoot.class);

        // create controller
        controller = new ObjectRootsController(heapDump, heapObject, rootFinder, viewProvider);

        ArgumentCaptor<ActionListener> listenerCaptor = ArgumentCaptor.forClass(ActionListener.class);
        verify(view).addActionListener(listenerCaptor.capture());

        listener = listenerCaptor.getValue();
    }


    @Test
    public void verifyAddListenersToView() {
        assertNotNull(listener);
    }

    @Test
    public void verifySetObjectDetailsInView() {
        when(heapObject.getIdString()).thenReturn("object-id");
        when(heapObject.isHeapAllocated()).thenReturn(true);
        when(heapObject.getSize()).thenReturn(10);

        when(clazz.getName()).thenReturn("object-class");

        HeapObjectUI heapObj = new HeapObjectUI("test", "test");
        ActionEvent<Action> event = new ActionEvent<Action>(view, Action.OBJECT_SELECTED);
        event.setPayload(heapObj);
        listener.actionPerformed(event);

        verify(view).setObjectDetails("Object ID: object-id\n" +
                "Type: object-class\n" +
                "Size: 10 bytes\n" +
                "Heap allocated: true\n");
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Test
    public void testShow() {

        HeapPath<JavaHeapObject> path = mock(HeapPath.class);
        when(path.iterator()).thenReturn(Arrays.asList(heapObject).iterator());

        when(rootFinder.findShortestPathsToRoot(heapObject, false)).thenReturn(Arrays.asList(path));

        controller.show();

        ArgumentCaptor<List> captor = ArgumentCaptor.forClass(List.class);

        verify(view).setPathToRoot(captor.capture());

        HeapObjectUI heapObj = (HeapObjectUI) captor.getValue().get(0);
        assertEquals("id-string", heapObj.objectId);
        assertEquals("class@id-string", heapObj.text);

        verify(view).showView();
    }
}

