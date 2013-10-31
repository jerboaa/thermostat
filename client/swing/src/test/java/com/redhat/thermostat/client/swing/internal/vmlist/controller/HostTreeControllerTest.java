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

package com.redhat.thermostat.client.swing.internal.vmlist.controller;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;

import java.util.List;

import javax.swing.RepaintManager;
import javax.swing.SwingUtilities;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.redhat.thermostat.client.core.vmlist.HostFilter;
import com.redhat.thermostat.client.core.vmlist.VMFilter;
import com.redhat.thermostat.client.swing.internal.accordion.Accordion;
import com.redhat.thermostat.client.swing.internal.accordion.AccordionModel;
import com.redhat.thermostat.client.swing.internal.vmlist.HostTreeComponentFactory;
import com.redhat.thermostat.storage.core.HostRef;
import com.redhat.thermostat.storage.core.VmRef;

public class HostTreeControllerTest {

    private Accordion<HostRef, VmRef> accordion;
    private DecoratorManager decoratorManager;
    private HostTreeComponentFactory componentFactory;
    private AccordionModel<HostRef, VmRef> proxyModel;
    private DecoratorProviderExtensionListener hostDecoratorListener;
    private DecoratorProviderExtensionListener vmDecoratorListener;
    
    @BeforeClass
    public static void setUpOnce() {
        // This is needed because some other test may have installed the
        // EDT violation checker repaint manager.
        RepaintManager.setCurrentManager(new RepaintManager());
    }
    
    @Before
    @SuppressWarnings("unchecked")
    public void setup() {
        accordion = mock(Accordion.class);
        decoratorManager = mock(DecoratorManager.class);
        componentFactory = mock(HostTreeComponentFactory.class);
        
        proxyModel = new AccordionModel<>();
        when(accordion.getModel()).thenReturn(proxyModel);
        
        hostDecoratorListener = mock(DecoratorProviderExtensionListener.class);
        vmDecoratorListener = mock(DecoratorProviderExtensionListener.class);
        when(decoratorManager.getVmDecoratorListener()).thenReturn(vmDecoratorListener);
        when(decoratorManager.getHostDecoratorListener()).thenReturn(hostDecoratorListener);
    }
    
    private void waitForSwing() {
        try {
            SwingUtilities.invokeAndWait(new Runnable() {
                @Override
                public void run() {
                    // just wait :)
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private abstract class TestHostFilter extends HostFilter {
        boolean filter;
        @Override
        public final boolean matches(HostRef toMatch) {
            if (!filter) return true;
            
            return matchesImpl(toMatch);
        }
        
        protected abstract boolean matchesImpl(HostRef toMatch);
        
        public void toggle() {
            filter = !this.filter;
            notify(FilterEvent.FILTER_CHANGED);
        }
    }
    
    private abstract class TestVMFilter extends VMFilter {
        boolean filter;
        @Override
        public final boolean matches(VmRef toMatch) {
            if (!filter) return true;
            
            return matchesImpl(toMatch);
        }
        
        protected abstract boolean matchesImpl(VmRef toMatch);
        
        public void toggle() {
            filter = !this.filter;
            notify(FilterEvent.FILTER_CHANGED);
        }
    }
    
    @Test
    public void testController() {
        HostTreeController controller =
                new HostTreeController(accordion, decoratorManager,
                                       componentFactory);
 
        // Add without filters
        
        HostRef host0 = new HostRef("0", "0");
        HostRef host1 = new HostRef("1", "1");
        
        controller.registerHost(host0);
        controller.registerHost(host1);
        
        waitForSwing();
        
        // check if our model contains everything it's supposed to
        List<HostRef> headers = proxyModel.getHeaders();
        assertEquals(2, headers.size());
        assertTrue(headers.contains(host0));
        assertTrue(headers.contains(host1));
        
        // now with filter
        
        TestHostFilter filter1 = new TestHostFilter() {
            @Override
            protected boolean matchesImpl(HostRef toMatch) {
                return (toMatch.getName().equals("0"));
            }
        };
        // enable the filter first
        filter1.toggle();
        
        controller.addHostFilter(filter1);
        
        waitForSwing();
        
        // we need to check that the model is rebuild with the appropriate
        // hosts, the filter should only let one host in
        headers = proxyModel.getHeaders();
        assertEquals(1, headers.size());
        assertTrue(headers.contains(host0));
        assertFalse(headers.contains(host1));
                
        // this should cause the tree to rebuild with all hosts in place again
        filter1.toggle();
        
        waitForSwing();
        
        headers = proxyModel.getHeaders();
        assertEquals(2, headers.size());
        assertTrue(headers.contains(host0));
        assertTrue(headers.contains(host1));
        
        // now on with vms, filter not enabled at first
        TestVMFilter filter2 = new TestVMFilter() {
            @Override
            protected boolean matchesImpl(VmRef toMatch) {
                return (toMatch.getName().equals("vm0"));
            }
        };
        
        controller.addVMFilter(filter2);
        
        waitForSwing();
        
        headers = proxyModel.getHeaders();
        assertEquals(2, headers.size());
        
        VmRef vm0 = new VmRef(host0, "0", 0, "vm0");
        VmRef vm1 = new VmRef(host0, "1", 1, "vm1");
        VmRef vm2 = new VmRef(host1, "2", 2, "vm2");

        controller.registerVM(vm0);
        controller.registerVM(vm1);
        controller.registerVM(vm2);
        
        waitForSwing();
        
        List<VmRef> components  = proxyModel.getComponents(host0);
        assertEquals(2, components.size());
        assertTrue(components.contains(vm0));
        assertTrue(components.contains(vm1));
        
        components  = proxyModel.getComponents(host1);
        assertEquals(1, components.size());
        assertTrue(components.contains(vm2));

        filter2.toggle();

        waitForSwing();

        components  = proxyModel.getComponents(host0);
        assertEquals(1, components.size());
        assertTrue(components.contains(vm0));
        assertFalse(components.contains(vm1));
        
        components  = proxyModel.getComponents(host1);
        assertTrue(components.isEmpty());
        
        // now test if controller reacts to updates
        
        controller.updateVMStatus(vm0);
        
        waitForSwing();

        verify(vmDecoratorListener).decorationChanged();
        
        controller.updateHostStatus(host0);

        waitForSwing();
        
        verify(hostDecoratorListener).decorationChanged();
    }
    
    @Test
    public void testFilteredHostFiltersVMToo() {
        HostTreeController controller =
                new HostTreeController(accordion, decoratorManager,
                                       componentFactory);
        HostRef host0 = new HostRef("0", "0");
        HostRef host1 = new HostRef("1", "1");
        
        VmRef vm0 = new VmRef(host0, "0", 0, "vm0");
        VmRef vm1 = new VmRef(host1, "1", 1, "vm1");
        VmRef vm2 = new VmRef(host1, "2", 2, "vm2");

        TestHostFilter filter = new TestHostFilter() {
            @Override
            protected boolean matchesImpl(HostRef toMatch) {
                return (toMatch.getName().equals("0"));
            }
        };
        
        // enabled the filter
        filter.toggle();

        controller.registerHost(host0);
        controller.registerHost(host1);
        
        waitForSwing();
        
        // filter out host 0, then add the vms
        controller.addHostFilter(filter);
        
        waitForSwing();
        
        controller.registerVM(vm0);
        controller.registerVM(vm1);
        controller.registerVM(vm2);
        
        waitForSwing();

        List<VmRef> components  = proxyModel.getComponents(host0);
        assertEquals(1, components.size());
        assertTrue(components.contains(vm0));
        
        components  = proxyModel.getComponents(host1);
        assertEquals(0, components.size());
    }
}
