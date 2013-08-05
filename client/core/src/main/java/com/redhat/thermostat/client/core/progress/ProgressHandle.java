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

package com.redhat.thermostat.client.core.progress;

import com.redhat.thermostat.common.ActionListener;
import com.redhat.thermostat.common.ActionNotifier;
import java.util.Objects;
import java.util.UUID;

/**
 */
public class ProgressHandle {

    public enum Status {
        STARTED,
        STOPPED,
    }
    
    private UUID id;
    
    private final ActionNotifier<ProgressHandle.Status> notifier;
    
    private String name;
    private boolean indeterminate;
    
    public ProgressHandle(String name) {
        id = UUID.randomUUID();
        this.name = name;
        notifier = new ActionNotifier<>(this);
    }

    public void setIndeterminate(boolean indeterminate) {
        this.indeterminate = indeterminate;
    }

    public boolean isIndeterminate() {
        return indeterminate;
    }

    public String getName() {
        return name;
    }

    public void start() {
        notifier.fireAction(Status.STARTED);
    }
    
    public void stop() {
        notifier.fireAction(Status.STOPPED);
    }
        
    public void addProgressListener(ActionListener<ProgressHandle.Status> listener) {
        notifier.addActionListener(listener);
    }
    
    public void removeProgressListener(ActionListener<ProgressHandle.Status> listener) {
        notifier.removeActionListener(listener);
    }

    @Override
    public String toString() {
        return name;
    }

    @Override
    public final int hashCode() {
        int hash = 5;
        hash = 53 * hash + Objects.hashCode(this.id);
        return hash;
    }

    @Override
    public final boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final ProgressHandle other = (ProgressHandle) obj;
        if (!Objects.equals(this.id, other.id)) {
            return false;
        }
        return true;
    }
}
