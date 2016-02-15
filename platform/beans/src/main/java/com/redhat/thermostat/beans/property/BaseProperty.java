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

package com.redhat.thermostat.beans.property;

/**
 */
public class BaseProperty<T> implements NamedProperty<T> {

    private boolean immutable;

    private ObservableValue<? extends T> observable;
    private BindListener bindListener;

    private String name;

    private PropertyChangeHandler<T> propertyChangeHandler;
    protected T value;

    public BaseProperty() {
        this(null);
    }

    public BaseProperty(T value) {
        this(value, "");
    }

    public BaseProperty(T value, String name) {
        this.value = value;
        this.name = name;
        propertyChangeHandler= new PropertyChangeHandler<>(this);
        bindListener = new BindListener();
        immutable = false;
    }

    public boolean isImmutable() {
        return immutable;
    }

    public void setImmutable() {
        immutable = true;
    }

    @Override
    public void addListener(ChangeListener<? super T> listener) {
        propertyChangeHandler.addListener(listener);
    }

    @Override
    public void removeListener(ChangeListener<? super T> listener) {
        propertyChangeHandler.removeListener(listener);
    }

    @Override
    public T getValue() {
        if (isBound()) {
            value = observable.getValue();
        }
        return value;
    }

    @Override
    public void setValue(T value) {

        if (immutable) {
            throw new IllegalArgumentException("This property is immutable");
        }

        if (isBound()) {
            throw new BindException("This property is bound");
        }

        this.value = value;
        firePropertyChangeEvent();
    }

    protected void firePropertyChangeEvent() {
        propertyChangeHandler.firePropertyChangeEvent();
    }

    @Override
    public void addListener(InvalidationListener listener) {
        propertyChangeHandler.addListener(listener);
    }

    @Override
    public void removeListener(InvalidationListener listener) {
        propertyChangeHandler.removeListener(listener);
    }

    @Override
    public void bind(ObservableValue<? extends T> observable) {

        if (immutable) {
            throw new IllegalArgumentException("This property is immutable");
        }

        if (isBound()) {
            throw new BindException("This property is already bound");
        }

        this.observable = observable;
        observable.addListener(bindListener);

        this.value = observable.getValue();
        firePropertyChangeEvent();
    }

    @Override
    public void unbind() {
        observable.removeListener(bindListener);
        observable = null;
    }

    @Override
    public boolean isBound() {
        return observable != null;
    }

    @Override
    public String getName() {
        return name;
    }

    private class BindListener implements ChangeListener<T> {

        @Override
        public void changed(ObservableValue<? extends T> observable, T oldValue, T newValue) {
            firePropertyChangeEvent();
        }
    }
}
