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

package com.redhat.thermostat.vm.heap.analysis.client.core.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.contains;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ExecutorService;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.stubbing.OngoingStubbing;

import com.redhat.thermostat.common.ActionEvent;
import com.redhat.thermostat.common.ActionListener;
import com.redhat.thermostat.common.ApplicationService;
import com.redhat.thermostat.vm.heap.analysis.client.core.HeapObjectUI;
import com.redhat.thermostat.vm.heap.analysis.client.core.ObjectDetailsView;
import com.redhat.thermostat.vm.heap.analysis.client.core.ObjectDetailsView.ObjectAction;
import com.redhat.thermostat.vm.heap.analysis.client.core.ObjectDetailsViewProvider;
import com.redhat.thermostat.vm.heap.analysis.client.core.ObjectRootsView;
import com.redhat.thermostat.vm.heap.analysis.client.core.ObjectRootsViewProvider;
import com.redhat.thermostat.vm.heap.analysis.common.HeapDump;
import com.redhat.thermostat.vm.heap.analysis.hat.hprof.model.JavaClass;
import com.redhat.thermostat.vm.heap.analysis.hat.hprof.model.JavaHeapObject;

public class ObjectDetailsControllerTest {

    private ObjectDetailsView view;
    private ApplicationService appService;
    private ObjectRootsViewProvider objectRootsProvider;
    private ObjectDetailsViewProvider objectDetailsProvider;

    private ArgumentCaptor<Runnable> runnableCaptor;

    @Before
    public void setUp() {
        view = mock(ObjectDetailsView.class);
        objectDetailsProvider = mock(ObjectDetailsViewProvider.class);
        when(objectDetailsProvider.createView()).thenReturn(view);
        
        objectRootsProvider = mock(ObjectRootsViewProvider.class);
        when(objectRootsProvider.createView()).thenReturn(mock(ObjectRootsView.class));
        runnableCaptor = ArgumentCaptor.forClass(Runnable.class);
        ExecutorService executorService = mock(ExecutorService.class);
        doNothing().when(executorService).execute(runnableCaptor.capture());

        appService = mock(ApplicationService.class);
        when(appService.getApplicationExecutor()).thenReturn(executorService);
    }

    @After
    public void tearDown() {
        view = null;
        objectDetailsProvider = null;
        objectRootsProvider = null;
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Test
    public void verifySearchWorks() {
        final String SEARCH_TEXT = "Test";
        final String OBJECT_ID = "0xcafebabe";
        final String OBJECT_CLASS_NAME = "FOO";

        when(view.getSearchText()).thenReturn(SEARCH_TEXT);

        JavaClass heapObjectClass = mock(JavaClass.class);
        when(heapObjectClass.getName()).thenReturn(OBJECT_CLASS_NAME);

        JavaHeapObject heapObject = mock(JavaHeapObject.class);
        when(heapObject.getIdString()).thenReturn(OBJECT_ID);
        when(heapObject.getClazz()).thenReturn(heapObjectClass);

        HeapDump dump = mock(HeapDump.class);
        when(dump.searchObjects(contains(SEARCH_TEXT), anyInt())).thenReturn(Arrays.asList(OBJECT_ID));
        when(dump.findObject(eq(OBJECT_ID))).thenReturn(heapObject);

        ArgumentCaptor<ActionListener> viewArgumentCaptor1 = ArgumentCaptor.forClass(ActionListener.class);
        doNothing().when(view).addObjectActionListener(viewArgumentCaptor1.capture());

        @SuppressWarnings("unused")
        ObjectDetailsController controller = new ObjectDetailsController(appService, dump, this.objectDetailsProvider, this.objectRootsProvider);

        ActionListener<ObjectAction> actionListener = viewArgumentCaptor1.getValue();
        assertNotNull(actionListener);
        actionListener.actionPerformed(new ActionEvent<ObjectAction>(view, ObjectAction.SEARCH));

        runnableCaptor.getValue().run();

        ArgumentCaptor<Collection> matchingObjectsCaptor = ArgumentCaptor.forClass(Collection.class);
        verify(view).setMatchingObjects(matchingObjectsCaptor.capture());

        List<HeapObjectUI> matchingObjects = new ArrayList<HeapObjectUI>(matchingObjectsCaptor.getValue());
        assertEquals(1, matchingObjects.size());
        assertEquals(OBJECT_ID, matchingObjects.get(0).objectId);
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Test
    public void verifyInputConvertedIntoWildcardsIfNeeded() {
        HeapDump heap = mock(HeapDump.class);
        when(view.getSearchText()).thenReturn("a");

        ArgumentCaptor<ActionListener> objectActionListenerCaptor = ArgumentCaptor.forClass(ActionListener.class);
        doNothing().when(view).addObjectActionListener(objectActionListenerCaptor.capture());

        @SuppressWarnings("unused")
        ObjectDetailsController controller = new ObjectDetailsController(appService, heap, this.objectDetailsProvider, this.objectRootsProvider);

        ActionListener<ObjectAction> actionListener = objectActionListenerCaptor.getValue();
        assertNotNull(actionListener);
        actionListener.actionPerformed(new ActionEvent<ObjectAction>(view, ObjectAction.SEARCH));

        runnableCaptor.getValue().run();

        verify(heap).searchObjects("*a*", 100);
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Test
    public void verifyWildcardInputNotConvertedIntoWildcards() {
        HeapDump heap = mock(HeapDump.class);
        when(view.getSearchText()).thenReturn("*a?");

        ArgumentCaptor<ActionListener> objectActionListenerCaptor = ArgumentCaptor.forClass(ActionListener.class);
        doNothing().when(view).addObjectActionListener(objectActionListenerCaptor.capture());

        @SuppressWarnings("unused")
        ObjectDetailsController controller = new ObjectDetailsController(appService, heap, this.objectDetailsProvider, this.objectRootsProvider);

        ActionListener<ObjectAction> actionListener = objectActionListenerCaptor.getValue();
        assertNotNull(actionListener);
        actionListener.actionPerformed(new ActionEvent<ObjectAction>(view, ObjectAction.SEARCH));

        runnableCaptor.getValue().run();

        verify(heap).searchObjects("*a?", 300);
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Test
    public void verifySearchLimits() {

        Object[][] limits = new Object[][] {
            { "a",       100 },
            { "ab",      200 },
            { "abc",     300 },
            { "abcd",    400 },
            { "abcde",   500 },
            { "abcdef",  600},
            { "abcdefg", 700},
            { "java.lang.Class", 1000 },
        };

        HeapDump heap = mock(HeapDump.class);

        OngoingStubbing<String> ongoing = when(view.getSearchText());
        for (int i = 0; i < limits.length; i++) {
            ongoing = ongoing.thenReturn((String)limits[i][0]);
        }

        ArgumentCaptor<ActionListener> objectActionListenerCaptor = ArgumentCaptor.forClass(ActionListener.class);
        doNothing().when(view).addObjectActionListener(objectActionListenerCaptor.capture());

        @SuppressWarnings("unused")
        ObjectDetailsController controller = new ObjectDetailsController(appService, heap, this.objectDetailsProvider, this.objectRootsProvider);

        ActionListener<ObjectAction> actionListener = objectActionListenerCaptor.getValue();
        assertNotNull(actionListener);

        for (int i = 0; i < limits.length; i++) {
            actionListener.actionPerformed(new ActionEvent<ObjectAction>(view, ObjectAction.SEARCH));
            runnableCaptor.getValue().run();
        }

        InOrder inOrder = inOrder(heap);
        for (int i = 0; i < limits.length; i++) {
            String text = (String) limits[i][0];
            int times = (Integer) limits[i][1];
            inOrder.verify(heap).searchObjects("*" + text + "*", times);
        }
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Test
    public void verifyGettingDetailsWorks() {
        final String OBJECT_ID = "0xcafebabe";
        final String OBJECT_ID_VISIBLE = "FOO BAR";

        JavaHeapObject heapObject = mock(JavaHeapObject.class);

        HeapObjectUI heapObjectRepresentation = new HeapObjectUI(OBJECT_ID, OBJECT_ID_VISIBLE);

        HeapDump dump = mock(HeapDump.class);
        when(dump.findObject(OBJECT_ID)).thenReturn(heapObject);

        ArgumentCaptor<ActionListener> viewArgumentCaptor1 = ArgumentCaptor.forClass(ActionListener.class);
        doNothing().when(view).addObjectActionListener(viewArgumentCaptor1.capture());

        when(view.getSelectedMatchingObject()).thenReturn(heapObjectRepresentation);

        @SuppressWarnings("unused")
        ObjectDetailsController controller = new ObjectDetailsController(appService, dump, this.objectDetailsProvider, this.objectRootsProvider);

        ActionListener<ObjectAction> actionListener = viewArgumentCaptor1.getValue();
        assertNotNull(actionListener);
        actionListener.actionPerformed(new ActionEvent<ObjectAction>(view, ObjectAction.GET_OBJECT_DETAIL));

        verify(view).setObjectDetails(heapObject);
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Test
    public void verifyFindRoot() {
        final String OBJECT_ID = "0xcafebabe";
        final String OBJECT_ID_VISIBLE = "FOO BAR";

        JavaHeapObject heapObject = mock(JavaHeapObject.class);

        HeapObjectUI heapObjectRepresentation = new HeapObjectUI(OBJECT_ID, OBJECT_ID_VISIBLE);

        HeapDump dump = mock(HeapDump.class);
        when(dump.findObject(OBJECT_ID)).thenReturn(heapObject);

        ArgumentCaptor<ActionListener> viewArgumentCaptor1 = ArgumentCaptor.forClass(ActionListener.class);
        doNothing().when(view).addObjectActionListener(viewArgumentCaptor1.capture());

        when(view.getSelectedMatchingObject()).thenReturn(heapObjectRepresentation);

        @SuppressWarnings("unused")
        ObjectDetailsController controller = new ObjectDetailsController(appService, dump, this.objectDetailsProvider, this.objectRootsProvider);

        ActionListener<ObjectAction> actionListener = viewArgumentCaptor1.getValue();
        assertNotNull(actionListener);
        actionListener.actionPerformed(new ActionEvent<ObjectAction>(view, ObjectAction.GET_OBJECT_DETAIL));

        verify(view).setObjectDetails(heapObject);
    }
}

