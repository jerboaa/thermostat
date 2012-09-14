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

package com.redhat.thermostat.eclipse.test.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.redhat.thermostat.common.HostsVMsLoader;
import com.redhat.thermostat.common.dao.HostRef;
import com.redhat.thermostat.common.dao.VmRef;
import com.redhat.thermostat.eclipse.model.HostsVmsTreeContentProvider;
import com.redhat.thermostat.eclipse.model.HostsVmsTreeRoot;

public class HostVmsTreeContentProviderTest {

    private HostsVmsTreeContentProvider treeProvider;
    private HostRef hostRef;
    HostsVMsLoader loader;
    
    @Before
    public void setUp() throws Exception {
        loader = mock(HostsVMsLoader.class);
        hostRef = mock(HostRef.class);
        treeProvider = new HostsVmsTreeContentProvider(loader);
    }

    @After
    public void tearDown() throws Exception {
        hostRef = null;
        treeProvider = null;
        loader = null;
    }

    @Test
    public void canGetHosts() {
        List<HostRef> expectedHosts = new ArrayList<>();
        HostRef hostRef1 = mock(HostRef.class);
        HostRef hostRef2 = mock(HostRef.class);
        expectedHosts.add(hostRef1);
        expectedHosts.add(hostRef2);
        
        when(loader.getHosts()).thenReturn(expectedHosts);
        Object[] children = treeProvider.getChildren(new HostsVmsTreeRoot());
        verifySameHosts(children, expectedHosts);
        
        children = treeProvider.getChildren(new String("ignored"));
        assertTrue(children.length == 0);
    }
    
    @Test
    public void canGetVms() {
        List<VmRef> expectedVms = new ArrayList<>();
        expectedVms.add(mock(VmRef.class));
        expectedVms.add(mock(VmRef.class));
        when(loader.getVMs(hostRef)).thenReturn(expectedVms);
        Object[] children = treeProvider.getChildren(hostRef);
        verifySameVms(children, expectedVms);
    }
    
    @Test
    public void canGetElements() {
        List<HostRef> expectedHosts = new ArrayList<>();
        HostRef hostRef1 = mock(HostRef.class);
        HostRef hostRef2 = mock(HostRef.class);
        expectedHosts.add(hostRef1);
        expectedHosts.add(hostRef2);
        
        when(loader.getHosts()).thenReturn(expectedHosts);
        Object[] children = treeProvider.getElements(new HostsVmsTreeRoot());
        verifySameHosts(children, expectedHosts);
    }
    
    @Test
    public void canGetRoot() {
        assertNull(treeProvider.getParent(new HostsVmsTreeRoot()));
    }
    
    @Test
    public void canGetVmParent() {
        List<HostRef> expectedHosts = new ArrayList<>();
        HostRef hostRef1 = mock(HostRef.class);
        HostRef hostRef2 = mock(HostRef.class);
        expectedHosts.add(hostRef1);
        expectedHosts.add(hostRef2);
        List<VmRef> expectedVms = new ArrayList<>();
        VmRef vm = mock(VmRef.class);
        expectedVms.add(mock(VmRef.class));
        expectedVms.add(vm);
        expectedVms.add(mock(VmRef.class));
        when(loader.getHosts()).thenReturn(expectedHosts);
        when(loader.getVMs(hostRef1)).thenReturn(expectedVms);
        treeProvider = new HostsVmsTreeContentProvider(loader);
        assertEquals(hostRef1, treeProvider.getParent(vm));
    }
    
    /**
     * Implicitly tests inputChanged() as well
     */
    @Test
    public void canGetHostParent() {
        List<HostRef> expectedHosts = new ArrayList<>();
        HostRef hostRef1 = mock(HostRef.class);
        HostRef hostRef2 = mock(HostRef.class);
        expectedHosts.add(hostRef1);
        expectedHosts.add(hostRef2);
        
        // need this for reverse look-up map building
        // which is triggered when inputChanged() is called
        List<VmRef> expectedVms = new ArrayList<>();
        VmRef vm = mock(VmRef.class);
        expectedVms.add(mock(VmRef.class));
        expectedVms.add(vm);
        expectedVms.add(mock(VmRef.class));
        when(loader.getVMs(hostRef)).thenReturn(expectedVms);
        when(loader.getHosts()).thenReturn(expectedHosts);
        
        // need to call inputChanged in order to set root
        // this is safe to do since treeViewer's setInput() method
        // triggers the same action.
        HostsVmsTreeRoot root = new HostsVmsTreeRoot();
        treeProvider.inputChanged(null, null, root);
        assertEquals(root, treeProvider.getParent(hostRef1));
    }
    
    @Test
    public void hasChildren() {
        List<HostRef> expectedHosts = new ArrayList<>();
        HostRef hostRef1 = mock(HostRef.class);
        HostRef hostRef2 = mock(HostRef.class);
        expectedHosts.add(hostRef1);
        expectedHosts.add(hostRef2);
        when(loader.getHosts()).thenReturn(expectedHosts);
        assertTrue(treeProvider.hasChildren(new HostsVmsTreeRoot()));
        expectedHosts = new ArrayList<>();
        when(loader.getHosts()).thenReturn(expectedHosts);
        assertFalse(treeProvider.hasChildren(new HostsVmsTreeRoot()));
        
        List<VmRef> expectedVms = new ArrayList<>();
        VmRef vm = mock(VmRef.class);
        expectedVms.add(mock(VmRef.class));
        expectedVms.add(vm);
        expectedVms.add(mock(VmRef.class));
        when(loader.getVMs(hostRef)).thenReturn(expectedVms);
        assertTrue(treeProvider.hasChildren(hostRef));
        expectedVms = new ArrayList<>();
        when(loader.getVMs(hostRef)).thenReturn(expectedVms);
        assertFalse(treeProvider.hasChildren(hostRef));
        
        // VMs don't have children
        assertFalse(treeProvider.hasChildren(mock(VmRef.class)));
    }

    private void verifySameHosts(Object[] children, List<HostRef> expectedHosts) {
        assertEquals(children.length, expectedHosts.size());
        for (int i = 0; i < children.length; i++) {
            assertTrue(children[i] instanceof HostRef);
            assertEquals(children[i], expectedHosts.get(i));
        }
    }
    
    private void verifySameVms(Object[] children, List<VmRef> expectedHosts) {
        assertEquals(children.length, expectedHosts.size());
        for (int i = 0; i < children.length; i++) {
            assertTrue(children[i] instanceof VmRef);
            assertEquals(children[i], expectedHosts.get(i));
        }
    }
}
