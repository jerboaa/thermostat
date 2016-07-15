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

package com.redhat.thermostat.client.swing.internal.vmlist.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.swing.SwingUtilities;

import com.redhat.thermostat.client.swing.internal.accordion.Accordion;
import com.redhat.thermostat.client.swing.internal.accordion.AccordionComponent;
import com.redhat.thermostat.client.swing.internal.accordion.AccordionComponentEvent;
import com.redhat.thermostat.client.swing.internal.accordion.AccordionHeaderEvent;
import com.redhat.thermostat.client.swing.internal.accordion.AccordionItemSelectedChangeListener;
import com.redhat.thermostat.client.swing.internal.accordion.AccordionModel;
import com.redhat.thermostat.client.swing.internal.accordion.AccordionModelChangeListener;
import com.redhat.thermostat.client.swing.internal.accordion.ItemSelectedEvent;
import com.redhat.thermostat.client.swing.internal.vmlist.HostTreeComponentFactory;
import com.redhat.thermostat.client.swing.internal.vmlist.ReferenceProvider;
import com.redhat.thermostat.client.ui.ReferenceFilter;
import com.redhat.thermostat.common.ActionEvent;
import com.redhat.thermostat.common.ActionListener;
import com.redhat.thermostat.common.ActionNotifier;
import com.redhat.thermostat.common.Filter;
import com.redhat.thermostat.common.Filter.FilterEvent;
import com.redhat.thermostat.storage.core.HostRef;
import com.redhat.thermostat.storage.core.Ref;
import com.redhat.thermostat.storage.core.VmRef;

public class HostTreeController {

    public enum ReferenceSelection {
        ITEM_SELECTED,
    }

    private DecoratorManager decoratorManager;
    private HostTreeComponentFactory componentFactory;

    private final ActionNotifier<ReferenceSelection> referenceNotifier;

    /* The list of filters is updated infrequently and irregularly, but the list is iterated over and each filter
     * checked and applied for all Refs in the AccordionModel on every rebuildTree. This is a significant slowdown,
     * particularly at first start up if a large number of hosts and VMs are present. This map is used to cache the
     * filter results, giving a significant performance improvement.
     */
    private final Map<Ref, Boolean> filterMap = new HashMap<>();
    private CopyOnWriteArrayList<ReferenceFilter> filters;

    private FilterListener filterListener;

    private Accordion<HostRef, VmRef> accordion;

    // we keep a private fullModel updated with all the references, while we
    // apply filters and decorations to the accordion original model only 
    private AccordionModel<HostRef, VmRef> proxyModel;
    private AccordionModel<HostRef, VmRef> fullModel;

    private class AccordionModelProxy implements AccordionModelChangeListener<HostRef, VmRef> {
        @Override
        public synchronized void headerAdded(AccordionHeaderEvent<HostRef> e) {
            proxyModel.addHeader(e.getHeader());
        }

        @Override
        public synchronized void headerRemoved(AccordionHeaderEvent<HostRef> e) {
            proxyModel.removeHeader(e.getHeader());
        }

        @Override
        public synchronized void componentAdded(AccordionComponentEvent<HostRef, VmRef> e) {
            proxyModel.addComponent(e.getHeader(), e.getComponent());
        }

        @Override
        public synchronized void componentRemoved(AccordionComponentEvent<HostRef, VmRef> e) {
            proxyModel.removeComponent(e.getHeader(), e.getComponent());
        }
    }

    public HostTreeController(Accordion<HostRef, VmRef> accordion,
                              DecoratorManager decoratorManager,
                              HostTreeComponentFactory componentFactory)
    {
        this.accordion = accordion;
        this.componentFactory = componentFactory;

        filterListener = new FilterListener();

        filters = new CopyOnWriteArrayList<>();

        fullModel = new AccordionModel<>();
        fullModel.addAccordionModelChangeListener(new AccordionModelProxy());

        this.decoratorManager = decoratorManager;
        referenceNotifier = new ActionNotifier<>(this);
        this.proxyModel = accordion.getModel();
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

    public synchronized void registerHost(final HostRef host) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                fullModel.addHeader(host);
                if (filter(host)) {
                    proxyModel.removeHeader(host);
                }
            }
        });
    }

    public synchronized void updateHostStatus(final HostRef host) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                if (filter(host)) {
                    proxyModel.removeHeader(host);
                }
                fireDecoratorChanged();
            }
        });
    }

    private void addHostToProxy(final HostRef host) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                proxyModel.addHeader(host);
            }
        });
    }

    private void setAccordionExpanded(final HostRef ref, final boolean expanded) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                accordion.setExpanded(ref, expanded);
            }
        });
    }

    private void addVmToProxy(final VmRef vm) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                proxyModel.addComponent(vm.getHostRef(), vm);
            }
        });
    }

    private void addVMImpl(final VmRef vm) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                fullModel.addComponent(vm.getHostRef(), vm);

                // adding a vm may add an host, so we need to be sure
                // the host is not filtered before checking the vm itself
                if (filter(vm.getHostRef())) {

                    // this will also remove all the vm, so we can skip the
                    // next filtering step
                    proxyModel.removeHeader(vm.getHostRef());

                } else if (filter(vm)) {
                    proxyModel.removeComponent(vm.getHostRef(), vm);
                }
            }
        });
    }

    private boolean filter(Ref ref) {
        if (filterMap.containsKey(ref)) {
            return filterMap.get(ref);
        }
        boolean filtered = false;
        for (ReferenceFilter filter : filters) {
            if (filter.applies(ref)) {
                if (!filter.matches(ref)) {
                    filtered = true;
                    break;
                }
            }
        }
        filterMap.put(ref, filtered);
        return filtered;
    }

    public synchronized void registerVM(final VmRef vm) {
        addVMImpl(vm);
    }

    private AccordionComponent getHostComponent(HostRef reference) {
        AccordionComponent component = null;
        List<HostRef> hosts = proxyModel.getHeaders();
        if (hosts.contains(reference)) {
            component = componentFactory.getTitledPane(reference);
        }
        return component;
    }

    private void selectComponent(final Ref _selected) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                Ref selected = _selected;
                AccordionComponent component = null;
                if (selected instanceof VmRef) {
                    VmRef reference = (VmRef) selected;
                    List<VmRef> vms = proxyModel.getComponents(reference.getHostRef());
                    if (vms.contains(reference)) {
                        component = componentFactory.getAccordionComponent(reference);
                    } else {
                        // try select the host relative to this vm then, since
                        // the vm has been removed
                        selected = reference.getHostRef();
                    }
                }

                if (selected instanceof HostRef) {
                    component = getHostComponent((HostRef) selected);
                }

                // if this is not the case, let's just not select anything
                if (component != null) {
                    accordion.setSelectedComponent(component);
                }
            }
        });
    }

    public void clearSelection() {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                accordion.setSelectedComponent(null);
            }
        });
    }

    public synchronized void updateVMStatus(final VmRef vm) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                if (filter(vm)) {
                    proxyModel.removeComponent(vm.getHostRef(), vm);
                }
                fireDecoratorChanged();
            }
        });
    }

    private void fireDecoratorChanged() {
        decoratorManager.getInfoLabelDecoratorListener().fireDecorationChanged();
        decoratorManager.getMainLabelDecoratorListener().fireDecorationChanged();
        decoratorManager.getIconDecoratorListener().fireDecorationChanged();
    }

    private synchronized void rebuildTree() {
        Ref selected = null;
        AccordionComponent component = accordion.getSelectedComponent();
        if (component instanceof ReferenceProvider) {
            // we know this is the case, because we gave the factory
            // in the first place, but anyway... 
            selected = ((ReferenceProvider) component).getReference();
        }

        // operate on a copy first since we need to know which of the ones we
        // have now was previously collapsed/expanded, we can't do this
        // if we empty the model first
        AccordionModel<RefPayload<HostRef>, RefPayload<VmRef>> _model = new AccordionModel<>();
        List<HostRef> hosts = fullModel.getHeaders();
        for (HostRef host : hosts) {
            if (!filter(host)) {

                RefPayload<HostRef> hostPayload = new RefPayload<>();
                hostPayload.reference = host;
                hostPayload.expanded = accordion.isExpanded(host);

                _model.addHeader(hostPayload);
                List<VmRef> vms = fullModel.getComponents(host);
                for (VmRef vm : vms) {
                    if (!filter(vm)) {

                        RefPayload<VmRef> vmPayload = new RefPayload<>();
                        vmPayload.reference = vm;
                        _model.addComponent(hostPayload, vmPayload);
                    }
                }
            }
        }

        // clear and refill, then expand as appropriate
        proxyModel.clear();
        List<RefPayload<HostRef>> payloadsHosts = _model.getHeaders();
        for (RefPayload<HostRef> host : payloadsHosts) {
            addHostToProxy(host.reference);

            List<RefPayload<VmRef>> vms = _model.getComponents(host);
            for (RefPayload<VmRef> vm : vms) {
                addVmToProxy(vm.reference);
            }

            // need to do this after we add content to the host
            // or it won't take effect
            setAccordionExpanded(host.reference, host.expanded);
        }

        // now select the entry that was originally selected, or its parent...
        // ... or nothing
        if (selected != null) {
            selectComponent(selected);
        }
    }

    private class FilterListener implements ActionListener<Filter.FilterEvent> {
        @Override
        public void actionPerformed(final ActionEvent<FilterEvent> actionEvent) {
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    Object payload = actionEvent.getPayload();
                    if (payload == null) {
                        filterMap.clear();
                    } else if (payload instanceof FilterRefRemovedPayload) {
                        FilterRefRemovedPayload<?> refRemovedPayload =
                                (FilterRefRemovedPayload) payload;
                        filterMap.remove(refRemovedPayload.getRef());
                    }
                    rebuildTree();
                }
            });
        }
    }

    private class RefPayload<R extends Ref> {
        R reference;
        boolean expanded;
    }

    // decorator accessors

    public DecoratorManager getDecoratorManager() {
        return decoratorManager;
    }

    public void addFilter(ReferenceFilter filter) {
        filterMap.clear();
        filters.add(filter);
        filter.addFilterEventListener(filterListener);
        rebuildTree();
    }

    public void removeFilter(ReferenceFilter filter) {
        filterMap.clear();
        filters.remove(filter);
        filter.removeFilterEventListener(filterListener);
        rebuildTree();
    }

    public void expandNode(final HostRef reference) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                accordion.setExpanded(reference, true);
            }
        });
    }

    protected static abstract class FilterPayload<T extends Ref> {

        private final T ref;

        public FilterPayload(T ref) {
            this.ref = ref;
        }

        public T getRef() {
            return ref;
        }

    }

    public static class FilterRefAddedPayload {}

    public static class FilterRefRemovedPayload<T extends Ref> extends FilterPayload<T> {
        public FilterRefRemovedPayload(T ref) {
            super(ref);
        }
    }

}

