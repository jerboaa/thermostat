/*
 * Copyright 2012-2015 Red Hat, Inc.
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

import java.util.Objects;

/**
 * Keys are attributes in {@link Category}s. Think of them as
 * column names in a table if you're familiar with SQL.
 */
public class Key<T> {

    // Keys used by most Categories.
    public static final Key<Long> TIMESTAMP = new Key<>("timeStamp");
    public static final Key<String> AGENT_ID = new Key<>("agentId");
    public static final Key<String> VM_ID = new Key<>("vmId");
    public static final Key<String> ID = new Key<>("_id");

    private String name;

    public Key() {
        // This is used only in de-serialization, e.g. using Gson, and therefore
        // we do not check the name for null or empty.
        super();
    }

    public Key(String name) {
        if (name == null || name.length() == 0) {
            throw new IllegalArgumentException("A Key must have a non-null name of length >= 1.");
        }
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
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
        return name.equals(e.getName());
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }

    @Override
    public String toString() {
        return "Key: " + name;
    }
}

