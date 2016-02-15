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

package com.redhat.thermostat.common.command;

import java.util.Collection;

/**
 * Helper class for comparing messages, such as {@link Request} and
 * {@link Response}.
 * 
 */
public class Messages {

    /**
     * Two requests, a and b, are considered equal if and only if they are,
     * not-null, of the same type and all parameters (all keys and values)
     * match. Listeners and targets are ignored.
     * 
     * @return true if a and b are both not-null and equal. false otherwise.
     */
    public static boolean equal(Request a, Request b) {
        if (a == null || b == null) {
            return false;
        }
        // type needs to be the same
        if (a.getType() != b.getType()) {
            return false;
        }
        // all parameters and values need to match
        Collection<String> ourParamValues = a.getParameterNames();
        Collection<String> otherParamValues = b.getParameterNames();
        if (ourParamValues.size() != otherParamValues.size()) {
            return false;
        }
        for (String name: ourParamValues) {
            String otherParamValue = b.getParameter(name);
            if (otherParamValue == null) {
                // other doesn't have param which we have
                return false;
            } else {
                // both requests contain same param name
                String ourParamValue = a.getParameter(name);
                if (!ourParamValue.equals(otherParamValue)) {
                    return false;
                }
            }
        }
        return true;
    }
    
    /**
     * Two responses are equal if and only if they are of the same type.
     * 
     * @return true if a and b are both not-null and are equal. false otherwise.
     */
    public static boolean equal(Response a, Response b) {
        return a.getType() == b.getType();
    }
}

