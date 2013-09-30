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

import javax.swing.SwingUtilities;

import com.redhat.thermostat.client.swing.internal.accordion.Accordion;
import com.redhat.thermostat.client.swing.internal.accordion.AccordionItemSelectedChangeListener;
import com.redhat.thermostat.client.swing.internal.accordion.AccordionModel;
import com.redhat.thermostat.client.swing.internal.accordion.ItemSelectedEvent;
import com.redhat.thermostat.client.swing.internal.vmlist.ReferenceProvider;
import com.redhat.thermostat.common.ActionListener;
import com.redhat.thermostat.common.ActionNotifier;
import com.redhat.thermostat.storage.core.HostRef;
import com.redhat.thermostat.storage.core.VmRef;

public class HostTreeController {

    public static enum ReferenceSelection {
        ITEM_SELECTED;
    }

    private DecoratorManager decoratorManager;

    private final ActionNotifier<ReferenceSelection> referenceNotifier;

    private AccordionModel<HostRef, VmRef> model;
    
    public HostTreeController(Accordion<HostRef, VmRef> accordion, DecoratorManager decoratorManager) {
        this.decoratorManager = decoratorManager;
        referenceNotifier = new ActionNotifier<>(this);
        this.model = accordion.getModel();
        accordion.addAccordionItemSelectedChangeListener(new AccordionItemSelectedChangeListener() {
            @Override
            public void itemSelected(ItemSelectedEvent event) {
                ReferenceProvider provider = (ReferenceProvider) event.getSelected();
                referenceNotifier.fireAction(ReferenceSelection.ITEM_SELECTED, provider.getReference());
            }
        });
    }
    
    public void addReferenceSelectionChangeListener(ActionListener<ReferenceSelection> listener) {
        referenceNotifier.addActionListener(listener);
    }
    
    public void removeReferenceSelectionChangeListener(ActionListener<ReferenceSelection> listener) {
        referenceNotifier.removeActionListener(listener);
    }
    
    public void addHost(final HostRef host) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                model.addHeader(host);
            }
        });
    }
    
    public void removeHost(final HostRef host) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                model.removeHeader(host);
            }
        });
    }
    
    public void addVM(final VmRef vm) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                model.addComponent(vm.getHostRef(), vm);
            }
        });
    }

    public void removeVM(final VmRef vm) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                model.removeComponent(vm.getHostRef(), vm);
            }
        });
    }

    public DecoratorProviderExtensionListener<HostRef> getHostDecoratorListener() {
        return decoratorManager.getHostDecoratorListener();
    }
    
    public DecoratorProviderExtensionListener<VmRef> getVmDecoratorListener() {
        return decoratorManager.getVmDecoratorListener();
    }
}