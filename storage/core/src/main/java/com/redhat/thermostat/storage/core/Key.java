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

package com.redhat.thermostat.storage.core;

/**
 * A Key is used to refer to data in a {@link Chunk}.  It may also be a partial key to the
 * set of data represented by a {@link Chunk} in a category.
 */
public class Key<T> {

    // Keys used by most Categories.
    public static final Key<Long> TIMESTAMP = new Key<>("timeStamp", false);
    public static final Key<String> AGENT_ID = new Key<>("agentId", true);
    public static final Key<Integer> VM_ID = new Key<>("vmId", true);
    public static final Key<String> ID = new Key<>("_id", false);

    private String name;
    private boolean isPartialCategoryKey;

    public Key() {
        // This is used only in de-serialization, e.g. using Gson, and therefore
        // we do not check the name for null or empty.
        super();
    }

    public Key(String name, boolean isPartialCategoryKey) {
        if (name == null || name.length() == 0) {
            throw new IllegalArgumentException("A Key must have a non-null name of length >= 1.");
        }
        this.name = name;
        this.isPartialCategoryKey = isPartialCategoryKey;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isPartialCategoryKey() {
        return isPartialCategoryKey;
    }

    public void setPartialCategoryKey(boolean partialCategoryKey) {
        this.isPartialCategoryKey = partialCategoryKey;

    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if ((o == null) || (o.getClass() != this.getClass())) {
            return false;
        }
        Key<?> e = (Key<?>) o;
        return (isPartialCategoryKey == e.isPartialCategoryKey()) &&
            name.equals(e.getName());
    }

    @Override
    public int hashCode() {
        int hash = 1867;
        hash = hash * 37 + (isPartialCategoryKey ? 0 : 1);
        hash = hash * 37 + (name == null ? 0 : name.hashCode());
        return hash;
    }

    @Override
    public String toString() {
        return "Key: " + name;
    }
}

