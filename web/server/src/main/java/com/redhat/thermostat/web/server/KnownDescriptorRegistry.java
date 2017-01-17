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

import com.redhat.thermostat.storage.core.auth.StatementDescriptorRegistration;

/**
 * Registers trusted statement descriptors.
 *
 */
final class KnownDescriptorRegistry {

    private static final ServiceLoader<StatementDescriptorRegistration> TRUSTED_DESCS = ServiceLoader
            .load(StatementDescriptorRegistration.class);
    private final Set<String> trustedSet;
    
    KnownDescriptorRegistry() {
        this(TRUSTED_DESCS);
    }
    
    KnownDescriptorRegistry(Iterable<StatementDescriptorRegistration> trustedDescs) {
        trustedSet = new HashSet<>();
        for (StatementDescriptorRegistration reg: trustedDescs) {
            Set<String> newCandidates = reg.getStatementDescriptors();
            // Some Set implementations throw NPEs when contains() is called on
            // a null value. Be sure we catch NPE since those impls can't contain
            // null values anyway.
            try {
                if (newCandidates.contains(null)) {
                    throw new IllegalStateException("null statement descriptor not acceptable!");
                }
                // Pass: Not containing null values.
            } catch (NullPointerException npe) {
                // Pass: Set impl does not support contains checks on null
                //       values.
            }
            trustedSet.addAll(newCandidates);
        }
    }
    
    final Set<String> getRegisteredDescriptors() {
        // return a read-only set of all descriptors
        return Collections.unmodifiableSet(trustedSet);
    }
    
}

