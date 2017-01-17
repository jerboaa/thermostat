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

package com.redhat.thermostat.client.swing.internal.search;

import java.util.Collection;
import java.util.concurrent.atomic.AtomicReference;

import com.redhat.thermostat.client.swing.internal.vmlist.controller.HostTreeController;
import com.redhat.thermostat.client.ui.ReferenceFilter;
import com.redhat.thermostat.client.ui.SearchProvider;
import com.redhat.thermostat.client.ui.SearchProvider.SearchAction;
import com.redhat.thermostat.common.ActionEvent;
import com.redhat.thermostat.common.ActionListener;
import com.redhat.thermostat.storage.core.HostRef;
import com.redhat.thermostat.storage.core.Ref;
import com.redhat.thermostat.storage.core.VmRef;

/**
 * NOTE: This filter is kept private because the Search API is incomplete at
 * this point, will be a separate plugin when public search mechanism is in
 * place.
 */
public class ReferenceFieldSearchFilter extends ReferenceFilter implements ActionListener<SearchAction> {

    private SearchProvider searchProvider;
    private AtomicReference<String> searchString;
    
    private HostTreeController hostTreeController;

    private SearchBackend backend;
    
    public ReferenceFieldSearchFilter(SearchProvider provider,
                                      HostTreeController hostTreeController)
    {
        this.searchProvider = provider;
        this.searchProvider.addSearchListener(this);
        this.searchString = new AtomicReference<>("");
        
        this.hostTreeController = hostTreeController;
        backend = new SearchBackend();
    }
   
    /**
     * For testing only
     */
    void setBackend(SearchBackend backend) {
        this.backend = backend;
    }
    
    @Override
    public boolean applies(Ref reference) {
        boolean applies = false;
        
        String search = searchString.get();
        if (!search.isEmpty()) {
            applies = true;
        }
        
        return applies;
    }
    
    @Override
    public boolean matches(Ref reference) {
        
        String search = searchString.get();
        if (search.isEmpty()) {
            return true;
        }

        boolean match = backend.match(search, reference);
        if (match && (reference instanceof HostRef)) {
            // ask to expand this node, in case it's not
            hostTreeController.expandNode((HostRef) reference);
        }
        return match;
    }
    
    @Override
    public void actionPerformed(ActionEvent<SearchAction> actionEvent) {
        
        String oldFilter = searchString.get();
        
        String filter = (String) actionEvent.getPayload();
        if (filter != null) {
            
            if (!oldFilter.equals(filter)) {
                searchString.set(filter);
            } else {
                searchString.set("");
            }
            notify(FilterEvent.FILTER_CHANGED);
        }
    }

    public void addHost(HostRef host) {
        backend.addHost(host);
        notify(FilterEvent.FILTER_CHANGED, new HostTreeController.FilterRefAddedPayload());
    }

    public void addHosts(Collection<HostRef> hosts) {
        backend.addHosts(hosts);
        notify(FilterEvent.FILTER_CHANGED, new HostTreeController.FilterRefAddedPayload());
    }

    public void removeHost(HostRef host) {
        notify(FilterEvent.FILTER_CHANGED, new HostTreeController.FilterRefRemovedPayload<>(host));
    }

    public void addVM(VmRef vm) {
        backend.addVM(vm);
        notify(FilterEvent.FILTER_CHANGED, new HostTreeController.FilterRefAddedPayload());
    }

    public void addVMs(HostRef host, Collection<VmRef> vms) {
        backend.addVMs(host, vms);
        notify(FilterEvent.FILTER_CHANGED, new HostTreeController.FilterRefAddedPayload());
    }

    public void removeVM(VmRef vm) {
        notify(FilterEvent.FILTER_CHANGED, new HostTreeController.FilterRefRemovedPayload<>(vm));
    }

}

