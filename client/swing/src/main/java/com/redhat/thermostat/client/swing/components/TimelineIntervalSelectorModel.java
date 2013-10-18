/*
 * Copyright 2012, 2013 Red Hat, Inc.
 *
 * This file is part of Thermostat.
 *
 * Thermostat is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2, or (at your option) any later version.
 *
 * Thermostat is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * Thermostat; see the file COPYING. If not see <http://www.gnu.org/licenses/>.
 *
 * Linking this code with other modules is making a combined work based on this
 * code. Thus, the terms and conditions of the GNU General Public License cover
 * the whole combination.
 *
 * As a special exception, the copyright holders of this code give you
 * permission to link this code with independent modules to produce an
 * executable, regardless of the license terms of these independent modules, and
 * to copy and distribute the resulting executable under terms of your choice,
 * provided that you also meet, for each linked independent module, the terms
 * and conditions of the license of that module. An independent module is a
 * module which is not derived from or based on this code. If you modify this
 * code, you may extend this exception to your version of the library, but you
 * are not obligated to do so. If you do not wish to do so, delete this
 * exception statement from your version.
 */

package com.redhat.thermostat.client.swing.components;

import java.util.EventListener;

import javax.swing.event.EventListenerList;

public class TimelineIntervalSelectorModel {

    public static interface ChangeListener extends EventListener {
        public void changed();
    }

    private final EventListenerList listeners = new EventListenerList();

    private long totalMinimum = 0;
    private long totalMaximum = 100;

    private long selectedMinimum = 0;
    private long selectedMaximum = 10;

    public long getTotalMinimum() {
        return totalMinimum;
    }

    public void setTotalMinimum(long totalMinimum) {
        if (this.totalMinimum != totalMinimum) {
            this.totalMinimum = totalMinimum;

            if (this.totalMaximum < this.totalMinimum) {
                this.totalMaximum = this.totalMinimum;
            }

            if (this.selectedMaximum < this.totalMinimum){
                this.selectedMaximum = this.totalMinimum;
            }

            if (this.selectedMinimum < this.totalMinimum) {
                this.selectedMinimum = this.totalMinimum;
            }

            fireModelChanged();
        }
    }

    public long getTotalMaximum() {
        return totalMaximum;
    }

    public void setTotalMaximum(long totalMaximum) {
        if (this.totalMaximum != totalMaximum) {
            this.totalMaximum = totalMaximum;

            if (this.totalMinimum > this.totalMaximum) {
                this.totalMinimum = this.totalMaximum;
            }

            if (this.selectedMaximum > this.totalMaximum) {
                this.selectedMaximum = this.totalMaximum;
            }

            if (this.selectedMinimum > this.totalMaximum) {
                this.selectedMinimum = this.totalMaximum;
            }

            fireModelChanged();
        }
    }

    public long getSelectedMinimum() {
        return selectedMinimum;
    }

    public void setSelectedMinimum(long selectedMinimum) {
        if(this.selectedMinimum != selectedMinimum) {
            this.selectedMinimum = selectedMinimum;
            fireModelChanged();
        }
    }

    public long getSelectedMaximum() {
        return selectedMaximum;
    }

    public void setSelectedMaximum(long selectedMaximum) {
        if (this.selectedMaximum != selectedMaximum) {
            this.selectedMaximum = selectedMaximum;
            fireModelChanged();
        }
    }

    public void addChangeListener(ChangeListener l) {
        listeners.add(ChangeListener.class, l);
    }

    public void removeChangeListener(ChangeListener l) {
        listeners.remove(ChangeListener.class, l);
    }

    private void fireModelChanged() {
        Object[] listeners = this.listeners.getListenerList();

        for (int i = listeners.length - 2; i >= 0; i -= 2) {
            if (listeners[i] == ChangeListener.class) {
                ((ChangeListener) listeners[i + 1]).changed();
            }
        }
    }

}
