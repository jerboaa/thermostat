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

package com.redhat.thermostat.storage.monitor.internal;

import java.util.ArrayList;
import java.util.Collection;

import com.redhat.thermostat.common.ActionNotifier;
import com.redhat.thermostat.storage.core.Ref;

abstract class MonitorAction<R extends Ref, A extends Enum<?>> implements Runnable {

    private static final String LOCK = new String("MonitorAction_LOCK");

    private ActionNotifier<A> notifier;
    Collection<R> references;
    
    public MonitorAction(ActionNotifier<A> notifier) {
        references = new ArrayList<>();
        this.notifier = notifier;
    }
    
    @Override
    public void run() {
        Collection<R> newReferences = getNewReferences();
        Collection<R> _refs = null;
        
        synchronized (LOCK) {
            _refs = new ArrayList<>(references);
            references = new ArrayList<>(newReferences);
        }
        
        handleRemovedHosts(_refs, newReferences);
        handleAddedReferences(_refs, newReferences);
    }
    
    private void handleAddedReferences(Collection<R> currentReference,
                                       Collection<R> newReference)
    {
        Collection<R> copy = new ArrayList<>(newReference);
        copy.removeAll(currentReference);
        for (R reference : copy) {
            notifier.fireAction(getAddAction(), reference);
        }
    }

    private void handleRemovedHosts(Collection<R> currentReference,
                                    Collection<R> newReference)
    {
        Collection<R> copy = new ArrayList<>(currentReference);
        copy.removeAll(newReference);
        for (R reference : copy) {
            notifier.fireAction(getRemoveAction(), reference);
        }
    }
    
    protected abstract A getAddAction();
    protected abstract A getRemoveAction();

    protected abstract Collection<R> getNewReferences();
}
