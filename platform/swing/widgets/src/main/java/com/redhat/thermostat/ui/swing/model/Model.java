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

package com.redhat.thermostat.ui.swing.model;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Simple class for generic models that contains data for a Swing
 * Component.
 */
public abstract class Model<T> {

    private final List<ModelListener<T>> listeners = new CopyOnWriteArrayList<>();

    public void addModelListener(ModelListener<T> listener) {
        listeners.add(listener);
    }

    public void removeModelListener(ModelListener<T> listener) {
        listeners.remove(listener);
    }

    protected abstract T getData();

    /**
     * Rebuilds this model underlying representation.
     *
     * <br /><br />
     *
     * In addition to help defining a "safe point" where the model can change
     * its underlaying representation, this method allows subclasses to
     * accumulate a certain number of data probes before signalling to
     * their attached view that the data is ready for visualisation.
     *
     * <br /><br />
     *
     * The method fires a {@link ModelListener#modelRebuilt(Model, Object)}
     * event.
     */
    public void rebuild() {
        rebuildImpl();
        fireModelUpdate(getData());
    }

    /*
    * Called by {@link #rebuild()} before {@link #fireModelUpdate()}, for
    * implementations that want to specialise their {@link #rebuild()}
    * method without the need to reimplement the event logic.
    */
    protected void rebuildImpl() {}

    /**
     * Clears this model underlying representation.
     *
     * <br /><br />
     *
     * In addition to help defining a "safe point" where the model can change
     * its underlaying representation, this method allows subclasses to
     * accumulate a certain number of data probes before signalling to
     * their attached view that the data is ready for visualisation.
     *
     * <br /><br />
     *
     * The method fires a {@link ModelListener#modelCleared(Model)}
     * event.
     */
    public void clear() {
        clearImpl();
        fireModelCleared();
    }

    /**
     * Called by {@link #clear()} before {@link #fireModelCleared()}, for
     * implementations that want to specialise their {@link #clear()}
     * method without the need to reimplement the event logic.
     */
    protected void clearImpl() {}

    protected void fireModelUpdate(final T data) {
        for (ModelListener listener : listeners) {
            listener.modelRebuilt(this, data);
        }
    }

    protected void fireModelCleared() {
        for (ModelListener listener : listeners) {
            listener.modelCleared(this);
        }
    }
}
