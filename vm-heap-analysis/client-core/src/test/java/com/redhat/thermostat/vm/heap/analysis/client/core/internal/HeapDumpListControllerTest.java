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

import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import com.redhat.thermostat.common.ActionEvent;
import com.redhat.thermostat.common.ActionListener;
import com.redhat.thermostat.vm.heap.analysis.client.core.HeapDumpListView;
import com.redhat.thermostat.vm.heap.analysis.client.core.HeapDumpListViewProvider;
import com.redhat.thermostat.vm.heap.analysis.common.HeapDump;

public class HeapDumpListControllerTest {

    private HeapDumpController mainController;
    private HeapDumpListViewProvider provider;
    private HeapDumpListView view;
    
    @Before
    public void setUp() throws Exception {
        provider = mock(HeapDumpListViewProvider.class);
        view = mock(HeapDumpListView.class);
        when(provider.createView()).thenReturn(view);
        
        mainController = mock(HeapDumpController.class);
    }
    
    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Test
    public void testAnalyseDump() {        
        
        HeapDump dump = mock(HeapDump.class);
        
        ArgumentCaptor<ActionListener> viewArgumentCaptor = ArgumentCaptor.forClass(ActionListener.class);
        doNothing().when(view).addListListener(viewArgumentCaptor.capture());
        
        HeapDumpListController controller = new HeapDumpListController(provider, mainController);
        
        ActionListener<HeapDumpListView.ListAction> listener = viewArgumentCaptor.getValue();
        assertNotNull(listener);
        
        ActionEvent<HeapDumpListView.ListAction> actionEvent = new ActionEvent<HeapDumpListView.ListAction>(view, HeapDumpListView.ListAction.OPEN_DUMP_DETAILS);
        actionEvent.setPayload(dump);
        listener.actionPerformed(actionEvent);
        
        verify(mainController).analyseDump(dump);
        verifyNoMoreInteractions(mainController);
    }
    
    @Test
    public void testSetDumps() {        
        
        List<HeapDump> dumps = mock(List.class);
        
        HeapDumpListController controller = new HeapDumpListController(provider, mainController);
        
        controller.setDumps(dumps);
        
        verify(view).setDumps(dumps);
    }
}

