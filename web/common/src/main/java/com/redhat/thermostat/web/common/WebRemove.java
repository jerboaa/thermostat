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

package com.redhat.thermostat.web.common;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.redhat.thermostat.storage.core.Category;
import com.redhat.thermostat.storage.core.Key;
import com.redhat.thermostat.storage.core.Remove;
import com.redhat.thermostat.storage.core.Query.Criteria;

public class WebRemove implements Remove {

    private transient Map<Category, Integer> categoryIds;
    private int categoryId;
    private List<Qualifier<?>> qualifiers;

    // NOTE: This is needed for de-serialization!
    public WebRemove() {
        this(null);
    }

    public WebRemove(Map<Category, Integer> categoryIds) {
        qualifiers = new ArrayList<>();
        this.categoryIds = categoryIds;
    }

    @Override
    public WebRemove from(Category category) {
        categoryId = categoryIds.get(category);
        return this;
    }

    @Override
    public <T> WebRemove where(Key<T> key, T value) {
        qualifiers.add(new Qualifier<T>(key, Criteria.EQUALS, value));
        return this;
    }

    public int getCategoryId() {
        return categoryId;
    }

    public List<Qualifier<?>> getQualifiers() {
        return qualifiers;
    }

}
