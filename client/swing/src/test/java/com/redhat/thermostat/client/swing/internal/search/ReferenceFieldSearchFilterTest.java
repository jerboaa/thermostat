/*
 * Copyright 2012, 2013 Red Hat, Inc.
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

package com.redhat.thermostat.client.swing.internal.search;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;

import org.junit.Before;
import org.junit.Test;

import com.redhat.thermostat.client.swing.internal.vmlist.controller.HostTreeController;
import com.redhat.thermostat.client.ui.SearchProvider;
import com.redhat.thermostat.client.ui.SearchProvider.SearchAction;
import com.redhat.thermostat.common.ActionEvent;
import com.redhat.thermostat.storage.core.HostRef;
import com.redhat.thermostat.storage.core.Ref;
import com.redhat.thermostat.storage.core.VmRef;

/**
 *
 */
public class ReferenceFieldSearchFilterTest {

    private SearchProvider provider;
    private HostTreeController hostTreeController;
    
    @Before
    public void setUp() {
        provider = mock(SearchProvider.class);
        hostTreeController = mock(HostTreeController.class);
    }
    
    @Test
    public void testFilterApplies() {

        ReferenceFieldSearchFilter filter = new ReferenceFieldSearchFilter(provider, hostTreeController);
        assertFalse(filter.applies(mock(Ref.class)));
        
        ActionEvent<SearchAction> actionEvent =
                new ActionEvent<SearchProvider.SearchAction>(this, SearchAction.PERFORM_SEARCH);
        actionEvent.setPayload("some search string");
        
        filter.actionPerformed(actionEvent);
        
        assertTrue(filter.applies(mock(Ref.class)));
    }
    
    @Test
    public void testSearchMatchFilterInactive() {
        
        HostRef host0 = new HostRef("h0", "host#0");
        VmRef vm0 = new VmRef(host0, "v0", 0, "vm#0");
        
        ReferenceFieldSearchFilter filter = new ReferenceFieldSearchFilter(provider, hostTreeController);
        
        assertTrue(filter.matches(host0));
        assertTrue(filter.matches(vm0));
    }

    @Test
    public void testSearchHandledCorrectly() {
        
        HostRef host0 = new HostRef("h0", "host#0");
        VmRef vm0 = new VmRef(host0, "v0", 0, "vm#0");
        
        SearchBackend backend = mock(SearchBackend.class);
        
        ReferenceFieldSearchFilter filter = new ReferenceFieldSearchFilter(provider, hostTreeController);
        filter.setBackend(backend);
        
        ActionEvent<SearchAction> actionEvent =
                new ActionEvent<SearchProvider.SearchAction>(this, SearchAction.PERFORM_SEARCH);
        actionEvent.setPayload("some search string");
        
        filter.actionPerformed(actionEvent);
        
        filter.matches(host0);
        
        verify(backend).match("some search string", host0);
        
        filter.matches(vm0);
        
        verify(backend).match("some search string", vm0);
    }
    
    @Test
    public void testTreeExpandedForHostOnly() {
        
        HostRef host0 = new HostRef("h0", "host#0");
        VmRef vm0 = new VmRef(host0, "v0", 0, "vm#0");
        
        SearchBackend backend = mock(SearchBackend.class);
        when(backend.match(any(String.class), any(HostRef.class))).thenReturn(true);
        when(backend.match(any(String.class), any(VmRef.class))).thenReturn(true);

        ReferenceFieldSearchFilter filter = new ReferenceFieldSearchFilter(provider, hostTreeController);
        filter.setBackend(backend);
        
        ActionEvent<SearchAction> actionEvent =
                new ActionEvent<SearchProvider.SearchAction>(this, SearchAction.PERFORM_SEARCH);
        actionEvent.setPayload("some search string");
        
        filter.actionPerformed(actionEvent);
        
        boolean result = filter.matches(host0);
        assertTrue(result);
        
        verify(backend).match("some search string", host0);
        verify(hostTreeController).expandNode(host0);
        
        result = filter.matches(vm0);
        assertTrue(result);

        verify(backend).match("some search string", vm0);
        verifyNoMoreInteractions(hostTreeController);
    }
    
    @Test
    public void testAddReferenceProxiedToBackend() {
        
        HostRef host0 = new HostRef("h0", "host#0");
        VmRef vm0 = new VmRef(host0, "v0", 0, "vm#0");
        
        SearchBackend backend = mock(SearchBackend.class);

        ReferenceFieldSearchFilter filter = new ReferenceFieldSearchFilter(provider, hostTreeController);
        filter.setBackend(backend);
        
        filter.addHost(host0);
        verify(backend).addHost(host0);
        
        filter.addVM(vm0);
        verify(backend).addVM(vm0);
    }
}
