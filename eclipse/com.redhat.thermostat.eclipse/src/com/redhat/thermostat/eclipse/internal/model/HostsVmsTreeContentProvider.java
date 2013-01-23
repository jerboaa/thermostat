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

package com.redhat.thermostat.eclipse.internal.model;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;

import com.redhat.thermostat.common.HostsVMsLoader;
import com.redhat.thermostat.common.dao.HostRef;
import com.redhat.thermostat.common.dao.VmRef;

public class HostsVmsTreeContentProvider implements ITreeContentProvider {

    private static final Object[] EMPTY_LIST = new Object[0];

    private HostsVMsLoader loader;
    private Map<VmRef, HostRef> reverseLookupMap;
    private HostsVmsTreeRoot root;

    public HostsVmsTreeContentProvider(HostsVMsLoader loader) {
        this.loader = loader;
        this.reverseLookupMap = buildReverseLookupMap();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.jface.viewers.IContentProvider#dispose()
     */
    @Override
    public void dispose() {
        // nothing
    }

    @Override
    public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
        if (newInput instanceof HostsVmsTreeRoot) {
            root = (HostsVmsTreeRoot) newInput;
        }
        // refresh reverse look-up
        this.reverseLookupMap = buildReverseLookupMap();
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.eclipse.jface.viewers.ITreeContentProvider#getElements(java.lang.
     * Object)
     */
    @Override
    public Object[] getElements(Object root) {
        return getChildren(root);
    }

    @Override
    public Object[] getChildren(Object parentElement) {
        if (parentElement instanceof HostsVmsTreeRoot) {
            return loader.getHosts().toArray();
        } else if (parentElement instanceof HostRef) {
            HostRef hostRef = (HostRef) parentElement;
            return loader.getVMs(hostRef).toArray();
        } else {
            return EMPTY_LIST;
        }
    }

    @Override
    public Object getParent(Object element) {
        if (element instanceof HostsVmsTreeRoot) {
            return null;
        } else if (element instanceof HostRef) {
            return root;
        } else if (element instanceof VmRef) {
            return this.reverseLookupMap.get(element);
        }
        return null;
    }

    @Override
    public boolean hasChildren(Object element) {
        if (element instanceof HostsVmsTreeRoot) {
            return loader.getHosts().size() > 0;
        } else if (element instanceof HostRef) {
            HostRef host = (HostRef) element;
            return loader.getVMs(host).size() > 0;
        } else {
            // VM refs don't have children
            return false;
        }
    }

    private Map<VmRef, HostRef> buildReverseLookupMap() {
        Map<VmRef, HostRef> lookupMap = new HashMap<>();
        for (HostRef ref : loader.getHosts()) {
            for (VmRef vmRef : loader.getVMs(ref)) {
                lookupMap.put(vmRef, ref);
            }
        }
        return lookupMap;
    }
}

