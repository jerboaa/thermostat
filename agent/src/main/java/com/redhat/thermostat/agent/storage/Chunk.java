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

package com.redhat.thermostat.agent.storage;

import java.util.HashMap;
import java.util.Map;

/**
 * A Chunk is a unit containing a set of data that can be added as a whole to the dataset
 * that exists behind the storage layer.
 */
public class Chunk {
    private final Category category;
    private final boolean replace;

    private Map<Key, String> values = new HashMap<Key, String>();

    /**
     * 
     * @param timestamp The time that should be associated with the data in this nugget.
     * @param category The {@link Category} of this data.  This should be a Category that the {@link Backend}
     * who is producing this Chunk has registered via {@link Storage#registerCategory()}
     * @param add whether this chunk should replace the values based on the keys for this category,
     * or be added to a set of values in this category
     */
    public Chunk(Category category, boolean replace) {
        this.category = category;
        this.replace = replace;
    }

    public Category getCategory() {
        return category;
    }

    public boolean getReplace() {
        return replace;
    }

    public void put(Key entry, String value) {
        values.put(entry, value);
    }

    public String get(Key entry) {
        return values.get(entry);
    }
}
