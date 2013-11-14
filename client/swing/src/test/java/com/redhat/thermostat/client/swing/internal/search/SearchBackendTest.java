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

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;

import org.junit.Test;

import com.redhat.thermostat.storage.core.HostRef;
import com.redhat.thermostat.storage.core.VmRef;

/**
 *
 */
public class SearchBackendTest {

    @Test
    public void testSearchUnMatch() {

        SearchBackend backend = new SearchBackend();
        
        HostRef host0 = new HostRef("0", "some weird host");
        
        boolean result = backend.match("some host", host0);
        assertFalse(result);
    }

    @Test
    public void testSearchMatch0() {

        SearchBackend backend = new SearchBackend();
        
        HostRef host0 = new HostRef("0", "some host");
        
        backend.addHost(host0);
        
        boolean result = backend.match("some host", host0);
        assertTrue(result);
    }
    
    @Test
    public void testSearchMatch1() {
        SearchBackend backend = new SearchBackend();
        
        HostRef host0 = new HostRef("host0", "some weird host");
        HostRef host1 = new HostRef("host1", "some host");

        VmRef vm0 = new VmRef(host0, "vm0", 0, "some vm");
        VmRef vm1 = new VmRef(host1, "vm1", 1, "some other vm");
        
        backend.addHost(host0);
        backend.addHost(host1);
        backend.addVM(vm0);
        backend.addVM(vm1);
        
        boolean result = backend.match("some host", host1);
        assertTrue(result);
        
        result = backend.match("some other vm", host1);
        assertTrue(result);
    }
    
    @Test
    public void testSearchUnMatchWithParent() {

        SearchBackend backend = new SearchBackend();
        
        HostRef host0 = new HostRef("host0", "some weird host");
        HostRef host1 = new HostRef("host1", "some host");

        VmRef vm0 = new VmRef(host0, "vm0", 0, "some vm");
        VmRef vm1 = new VmRef(host1, "vm1", 1, "some other vm");
        
        backend.addHost(host0);
        backend.addHost(host1);
        backend.addVM(vm0);
        backend.addVM(vm1);
        
        boolean result = backend.match("somehost", host1);
        assertFalse(result);
        
        result = backend.match("some vm", host1);
        assertFalse(result);
    }
    
    @Test
    public void testSearchMatch2() {
        SearchBackend backend = new SearchBackend();
        
        HostRef host0 = new HostRef("host0", "some weird host");
        HostRef host1 = new HostRef("host1", "some host");
        HostRef host2 = new HostRef("host2", "some host");

        VmRef vm0 = new VmRef(host0, "vm0", 0, "some vm");
        VmRef vm1 = new VmRef(host1, "vm1", 1, "some other vm");
        
        backend.addHost(host0);
        backend.addHost(host1);
        backend.addVM(vm0);
        backend.addVM(vm1);
        
        boolean result = backend.match("some host", host2);
        assertTrue(result);
        
        result = backend.match("some other vm", host2);
        assertFalse(result);
    }
}
