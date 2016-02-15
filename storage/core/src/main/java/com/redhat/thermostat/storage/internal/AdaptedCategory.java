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

package com.redhat.thermostat.storage.internal;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.redhat.thermostat.storage.core.Category;
import com.redhat.thermostat.storage.core.Key;
import com.redhat.thermostat.storage.model.AggregateResult;
import com.redhat.thermostat.storage.model.Pojo;

/**
 * An adapted category. This facilitates aggregate queries for which the data
 * class type changes.
 *
 * @param <T> The type to adapt a category to.
 * @param <S> The source type to adapt things from.
 */
public final class AdaptedCategory<T extends Pojo, S extends Pojo> extends Category<T> {

    /**
     * Constructor used by CategoryAdapter which has just
     * performed a registration check. That means only categories
     * constructed via public Category constructors can get adapted.
     *  
     */
    public AdaptedCategory(Category<S> category, Class<T> dataClass) {
        this.name = category.getName();
        Map<String, Key<?>> mappedKeys = new HashMap<>();
        for (Key<?> key: category.getKeys()) {
            mappedKeys.put(key.getName(), key);
        }
        this.keys = Collections.unmodifiableMap(mappedKeys);
        if (!AggregateResult.class.isAssignableFrom(dataClass)) {
            String msg = "Can only adapt to aggregate results!";
            throw new IllegalArgumentException(msg);
        }
        this.indexedKeys = Collections.unmodifiableList(category.getIndexedKeys());
        this.dataClassName = dataClass.getName();
    }
    
}

