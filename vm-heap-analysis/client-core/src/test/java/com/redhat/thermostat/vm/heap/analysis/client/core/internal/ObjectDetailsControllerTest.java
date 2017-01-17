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

package com.redhat.thermostat.vm.heap.analysis.client.core.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.contains;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doNothing;
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
        when(dump.wildcardSearch(contains(SEARCH_TEXT))).thenReturn(Arrays.asList(OBJECT_ID));
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

