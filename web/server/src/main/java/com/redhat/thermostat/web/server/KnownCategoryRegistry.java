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

import java.util.Collections;
import java.util.HashSet;
import java.util.ServiceLoader;
import java.util.Set;

import com.redhat.thermostat.storage.core.Category;
import com.redhat.thermostat.storage.core.auth.CategoryRegistration;

/**
 * Registers trusted category names.
 *
 * @see CategoryRegistration
 * @see Category
 */
final class KnownCategoryRegistry {

    private static final ServiceLoader<CategoryRegistration> TRUSTED_CATEGORIES = ServiceLoader
            .load(CategoryRegistration.class);
    private final Set<String> trustedCategories;
    
    KnownCategoryRegistry() {
        this(TRUSTED_CATEGORIES);
    }
    
    KnownCategoryRegistry(Iterable<CategoryRegistration> registrations) {
        trustedCategories = new HashSet<>();
        for (CategoryRegistration reg: registrations) {
            Set<String> currentRegs = reg.getCategoryNames();
            // Some Set implementations throw NPEs when contains() is called on
            // a null value. Be sure we catch NPE since those impls can't contain
            // null values anyway.
            try {
                if (currentRegs.contains(null)) {
                    String msg = "null name not allowed!";
                    throw new IllegalStateException(msg);
                }
                // Pass: Not containing null values.
            } catch (NullPointerException npe) {
                // Pass: Set impl does not support contains checks on null
                //       values.
            }
            trustedCategories.addAll(currentRegs);
        }
    }
    
    Set<String> getRegisteredCategoryNames() {
        // return a read-only view of registered category names.
        return Collections.unmodifiableSet(trustedCategories);
    }
}

