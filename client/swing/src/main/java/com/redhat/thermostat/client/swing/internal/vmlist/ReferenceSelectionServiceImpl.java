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

package com.redhat.thermostat.client.swing.internal.vmlist;

import com.redhat.thermostat.client.swing.ReferenceSelectionChangeListener;
import com.redhat.thermostat.client.swing.ReferenceSelectionChangedEvent;
import com.redhat.thermostat.client.swing.ReferenceSelectionService;
import com.redhat.thermostat.storage.core.Ref;

import javax.swing.event.EventListenerList;

/**
 */
public class ReferenceSelectionServiceImpl implements ReferenceSelectionService {

    protected EventListenerList listenerList;
    private static final String __LOCK__ = new String("ReferenceSelectionServiceImpl__LOCK__");

    private Ref reference;

    public ReferenceSelectionServiceImpl() {
        listenerList = new EventListenerList();
    }

    public void setReference(Ref reference) {
        Ref oldRef = null;
        Ref newRef = reference;
        synchronized (__LOCK__) {
            oldRef = this.reference;
            this.reference = reference;
        }

        fireSelectionChangedEvent(oldRef, newRef);
    }

    public Ref getReference() {
        Ref _ref = null;
        synchronized (__LOCK__) {
            _ref = reference;
        }
        return _ref;
    }

    private void fireSelectionChangedEvent(Ref oldReference, Ref newReference) {
        Object[] listeners = listenerList.getListenerList();

        ReferenceSelectionChangedEvent event =
                new ReferenceSelectionChangedEvent(this, oldReference, newReference);

        for (int i = listeners.length - 2; i >= 0; i -= 2) {
            if (listeners[i] == ReferenceSelectionChangeListener.class) {
                ((ReferenceSelectionChangeListener) listeners[i + 1]).referenceChanged(event);
            }
        }
    }

    @Override
    public void addReferenceSelectionChangeListener(ReferenceSelectionChangeListener listener) {
        listenerList.add(ReferenceSelectionChangeListener.class, listener);
    }

    @Override
    public void removeReferenceSelectionChangeListener(ReferenceSelectionChangeListener listener) {
        listenerList.remove(ReferenceSelectionChangeListener.class, listener);
    }
}
