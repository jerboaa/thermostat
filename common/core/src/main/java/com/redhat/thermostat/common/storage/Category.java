/*
 * Copyright 2012 Red Hat, Inc.
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

package com.redhat.thermostat.common.storage;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class Category {
    private final String name;
    private final Map<String, Key<?>> keys;

    private ConnectionKey connectionKey;

    /**
     * Creates a new Category instance with the specified name.
     *
     * @param name the name of the category
     *
     * @throws IllegalArgumentException if a Category is created with a name that has been used before
     */
    public Category(String name, Key<?>... keys) {
        if (Categories.contains(name)) {
            throw new IllegalStateException();
        }
        this.name = name;
        Map<String, Key<?>> keysMap = new HashMap<String, Key<?>>();
        for (Key<?> key : keys) {
            keysMap.put(key.getName(), key);
        }
        this.keys = Collections.unmodifiableMap(keysMap);
        Categories.add(this);
    }

    public String getName() {
        return name;
    }

    public synchronized Collection<Key<?>> getKeys() {
        return keys.values();
    }

    public void setConnectionKey(ConnectionKey connKey) {
        connectionKey = connKey;
    }

    public ConnectionKey getConnectionKey() {
        return connectionKey;
    }

    public boolean hasBeenRegistered() {
        return getConnectionKey() != null;
    }

    public Key<?> getKey(String name) {
        return keys.get(name);
    }

    @Override
    public String toString() {
        return getName();
    }

    public boolean equals(Object o) {
        if (! (o instanceof Category)) {
            return false;
        }
        Category other = (Category) o;
        return Objects.equals(name, other.name) && keys.equals(other.keys);
    }
}
