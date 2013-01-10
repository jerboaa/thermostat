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

package com.redhat.thermostat.storage.core;

/**
 * Updates fields of a database entry. 
 */
public interface Update {

    /**
     * Adds a where clause that denotes the entry to be updated. If more than one
     * where-clause is declared, they are concatenated as an and-query.
     * If a clause with the same key is declared more than once, the latter
     * overrides the former. This is so that an Update object can be reused
     * for multiple requests. If an update is issued for which no entry can
     * be found (i.e. the where-clause yields no results), a
     * <code>StorageException</code> may get thrown.
     *
     * @param key the key of the field of the where clause
     * @param value the value of the field of the where clause
     */
    <T> void where(Key<T> key, T value);

    /**
     * Sets a field in a found document to the specified value. If the same key is
     * set more than once, the latest values overrides the former values.
     *
     * @param key the key of the field
     * @param value the value to set
     */
    <T> void set(Key<T> key, T value);

    /**
     * Applies the update operation.
     */
    void apply();
}
