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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import com.redhat.thermostat.client.heap.HeapDumpDetailsView.Action;
import com.redhat.thermostat.client.heap.HeapDumpDetailsView.HeapObjectUI;
import com.redhat.thermostat.common.ActionEvent;
import com.redhat.thermostat.common.ActionListener;
import com.redhat.thermostat.common.ViewFactory;
import com.redhat.thermostat.common.appctx.ApplicationContext;
import com.redhat.thermostat.common.heap.HeapDump;
import com.redhat.thermostat.common.heap.ObjectHistogram;
import com.sun.tools.hat.internal.model.JavaClass;
import com.sun.tools.hat.internal.model.JavaHeapObject;

public class HeapDumpDetailsControllerTest {

    private HeapDumpDetailsView view;
    private ActionListener<HeapDumpDetailsView.Action> actionListener;

    @Before
    public void setUp() {

        // view factory
        ViewFactory viewFactory = mock(ViewFactory.class);

        view = mock(HeapDumpDetailsView.class);
        when(viewFactory.getView(HeapDumpDetailsView.class)).thenReturn(view);

        ApplicationContext.getInstance().setViewFactory(viewFactory);

    }

    @Test
    public void verifyInitialize() throws IOException {
        ObjectHistogram histogram = mock(ObjectHistogram.class);

        HeapDump dump = mock(HeapDump.class);
        when(dump.getHistogram()).thenReturn(histogram);


        HeapDumpDetailsController controller = new HeapDumpDetailsController();
        controller.setDump(dump);

        verify(dump).searchObjects(isA(String.class), anyInt());
        verify(view).setHeapHistogram(histogram);
    }

    @Test
    public void verifySearchWorks() {
        final String SEARCH_TEXT = "Test";
        final String OBJECT_ID = "0xcafebabe";

        when(view.getSearchText()).thenReturn(SEARCH_TEXT);

        JavaClass heapObjectClass = mock(JavaClass.class);
        when(heapObjectClass.getName()).thenReturn("FOO");

        JavaHeapObject heapObject = mock(JavaHeapObject.class);
        when(heapObject.getIdString()).thenReturn(OBJECT_ID);
        when(heapObject.getClazz()).thenReturn(heapObjectClass);

        HeapDump dump = mock(HeapDump.class);
        when(dump.searchObjects(eq(SEARCH_TEXT), anyInt())).thenReturn(Arrays.asList(OBJECT_ID));
        when(dump.findObject(eq(OBJECT_ID))).thenReturn(heapObject);

        ArgumentCaptor<ActionListener> viewArgumentCaptor1 = ArgumentCaptor.forClass(ActionListener.class);
        doNothing().when(view).addActionListener(viewArgumentCaptor1.capture());

        HeapDumpDetailsController controller = new HeapDumpDetailsController();
        controller.setDump(dump);

        actionListener = viewArgumentCaptor1.getValue();
        assertNotNull(actionListener);
        actionListener.actionPerformed(new ActionEvent<HeapDumpDetailsView.Action>(view, Action.SEARCH));

        ArgumentCaptor<Collection> matchingObjectsCaptor = ArgumentCaptor.forClass(Collection.class);
        verify(view).setMatchingObjects(matchingObjectsCaptor.capture());

        List<HeapObjectUI> matchingObjects = new ArrayList<HeapObjectUI>(matchingObjectsCaptor.getValue());
        assertEquals(1, matchingObjects.size());
        assertEquals(OBJECT_ID, matchingObjects.get(0).objectId);
    }

    @Test
    public void verifyGetitngDetailsWorks() {
        final String OBJECT_ID = "0xcafebabe";

        JavaHeapObject heapObject = mock(JavaHeapObject.class);

        HeapObjectUI heapObjectRepresentation = new HeapObjectUI(OBJECT_ID, "FOO BAR");

        HeapDump dump = mock(HeapDump.class);
        when(dump.findObject(OBJECT_ID)).thenReturn(heapObject);

        ArgumentCaptor<ActionListener> viewArgumentCaptor1 = ArgumentCaptor.forClass(ActionListener.class);
        doNothing().when(view).addActionListener(viewArgumentCaptor1.capture());

        when(view.getSelectedMatchingObject()).thenReturn(heapObjectRepresentation);

        HeapDumpDetailsController controller = new HeapDumpDetailsController();
        controller.setDump(dump);

        actionListener = viewArgumentCaptor1.getValue();
        assertNotNull(actionListener);
        actionListener.actionPerformed(new ActionEvent<HeapDumpDetailsView.Action>(view, Action.GET_OBJECT_DETAIL));

        verify(view).setObjectDetails(heapObject);
    }
}
