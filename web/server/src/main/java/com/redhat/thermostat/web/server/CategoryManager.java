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

package com.redhat.thermostat.web.server;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import com.redhat.thermostat.storage.core.Category;
import com.redhat.thermostat.storage.model.Pojo;
import com.redhat.thermostat.web.common.SharedStateId;

class CategoryManager {
    
    private final Map<CategoryIdentifier, SharedStateId> categoryIds;
    private final Map<SharedStateId, Category<?>> categories;
    private int categoryIdCounter = 0;
    
    CategoryManager() {
        categories = new HashMap<>();
        categoryIds = new HashMap<>();
    }
    
    // Testing only
    CategoryManager(int initialValue) {
        this();
        categoryIdCounter = initialValue;
    }
    
    synchronized <T extends Pojo> SharedStateId putCategory(UUID serverNonce, Category<T> category, CategoryIdentifier catId) {
        if (categoryIds.containsKey(Objects.requireNonNull(catId))) {
            return categoryIds.get(catId);
        } else {
            // add new category
            Objects.requireNonNull(category);
            Objects.requireNonNull(serverNonce);
            SharedStateId newId = new SharedStateId(categoryIdCounter, serverNonce);
            categoryIdCounter++;
            // This really should not happen, but if it does fail early. 
            if (categoryIdCounter == Integer.MAX_VALUE) {
                throw new IllegalStateException("Too many categories!");
            }
            categoryIds.put(catId, newId);
            categories.put(newId, category);
            return newId;
        }
    }
    
    synchronized SharedStateId getCategoryId(CategoryIdentifier key) {
        return categoryIds.get(Objects.requireNonNull(key));
    }
    
    @SuppressWarnings("unchecked")
    synchronized <T extends Pojo> Category<T> getCategory(SharedStateId id) {
        return (Category<T>)categories.get(Objects.requireNonNull(id));
    }
    
    static class CategoryIdentifier {
        
        private final String categoryName;
        private final String dataClassName;
        
        CategoryIdentifier(String categoryName, String dataClassName) {
            this.categoryName = categoryName;
            this.dataClassName = dataClassName;
        }
        
        @Override
        public boolean equals(Object other) {
            if (other == null || CategoryIdentifier.class != other.getClass()) {
                return false;
            }
            CategoryIdentifier o = (CategoryIdentifier)other;
            return Objects.equals(categoryName, o.categoryName) &&
                    Objects.equals(dataClassName, o.dataClassName);
        }
        
        @Override
        public int hashCode() {
            return Objects.hash(categoryName, dataClassName);
        }
    }
}
