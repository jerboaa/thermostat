/*
 * Copyright 2012-2014 Red Hat, Inc.
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

import com.redhat.thermostat.storage.model.Pojo;

public final class StatementDescriptor<T extends Pojo> {
    
    private final Category<T> category;
    private final String desc;
    
    public StatementDescriptor(Category<T> category, String desc) {
        this.category = Objects.requireNonNull(category);
        this.desc = Objects.requireNonNull(desc);
    }

    /**
     * Describes this statement for preparation. For example:
     * 
     * <pre>
     * QUERY host-info WHERE 'agentId' = ?s LIMIT 1
     * </pre>
     * 
     * @return The statement descriptor.
     */
    public String getDescriptor() {
        return desc;
    }
    
    public Category<T> getCategory() {
        return category;
    }
    
    @Override
    public String toString() {
        return desc;
    }
    
    @Override
    public int hashCode() {
        /*
         * Note that category's hash code gets generated on de-facto-immutable
         * values. Hence, it is safe to use it here.
         */
        return Objects.hash(desc, category);
    }
    
    @Override
    public boolean equals(Object other) {
        if (!(other instanceof StatementDescriptor)) {
            return false;
        }
        @SuppressWarnings("rawtypes")
        StatementDescriptor o = (StatementDescriptor)other;
        return desc.equals(o.desc)
                && category.equals(o.category);
    }
    
}

