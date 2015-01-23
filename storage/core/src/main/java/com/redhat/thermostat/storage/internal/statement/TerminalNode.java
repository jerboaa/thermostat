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

package com.redhat.thermostat.storage.internal.statement;

import java.util.Objects;

import com.redhat.thermostat.storage.core.IllegalPatchException;
import com.redhat.thermostat.storage.core.Key;
import com.redhat.thermostat.storage.core.PreparedParameter;
import com.redhat.thermostat.storage.model.Pojo;
import com.redhat.thermostat.storage.query.LiteralExpression;

/**
 * Node representing a leaf node in the prepared statement parse tree.
 *
 */
class TerminalNode extends Node {

    TerminalNode(Node parent) {
        super(parent);
    }
    
    @Override
    public PatchedWhereExpression patch(PreparedParameter[] params) throws IllegalPatchException {
        if (getValue() == null) {
            String msg = TerminalNode.class.getSimpleName() +
                    " invalid when attempted to patch";
            IllegalStateException cause = new IllegalStateException(msg);
            throw new IllegalPatchException(cause);
        }
        Object actualValue;
        if (getValue() instanceof UnfinishedValueNode) {
            // need to patch the value
            UnfinishedValueNode patch = (UnfinishedValueNode)getValue();
            PreparedParameter param = null;
            try {
                param = params[patch.getParameterIndex()];
            } catch (Exception e) {
                throw new IllegalPatchException(e);
            }
            // Do some type sanity checking for free parameters
            ensureTypeCompatibility(patch, param);
            if (patch.isLHS()) {
                // LHS need to get patched to keys
                Key<?> valueKey = new Key<>((String)param.getValue());
                actualValue = valueKey;
            } else {
                actualValue = param.getValue();
            }
        } else {
            actualValue = getValue();
        }
        LiteralExpression<?> literalExp = new LiteralExpression<>(actualValue);
        return new PatchedWhereExpressionImpl(literalExp);
    }
    
    private void ensureTypeCompatibility(UnfinishedValueNode patch,
            PreparedParameter param) throws IllegalPatchException {
        if (patch.getType() == Pojo.class) {
            // handle pojo case
            Object value = param.getValue();
            if (Pojo.class.isAssignableFrom(param.getType()) &&
                    value != null && !value.getClass().isArray()) {
                return; // pojo-type match: OK
            }
            // dead-end
            IllegalArgumentException iae = constructIllegalArgumentException(patch, param);
            throw new IllegalPatchException(iae);
        } else if (patch.getType() == Pojo[].class) {
            // handle pojo list case
            Object value = param.getValue();
            if (Pojo.class.isAssignableFrom(param.getType()) && value != null
                    && value.getClass().isArray()) {
                return; // pojo-list type match: OK
            }
        } else {
            // primitive types or primitive list types
            if (param.getType() != patch.getType()) {
                IllegalArgumentException iae = constructIllegalArgumentException(patch, param);
                throw new IllegalPatchException(iae);
            }
            // passed primitive (array) type check
        }
    }
        
    private IllegalArgumentException constructIllegalArgumentException(
            UnfinishedValueNode patch, PreparedParameter param) {
        Object value = param.getValue();
        String paramArrayPrefix = "";
        if (value != null && value.getClass().isArray()) {
            paramArrayPrefix = "[";
        }
        String msg = TerminalNode.class.getSimpleName()
                + " invalid type when attempting to patch. Expected "
                + patch.getType().getName() + " but was "
                + paramArrayPrefix + param.getType().getName();
        IllegalArgumentException iae = new IllegalArgumentException(msg);
        return iae;
    }

    @Override
    public boolean equals(Object other) {
        if (other == null) {
            return false;
        }
        if (!(other instanceof TerminalNode)) {
            return false;
        }
        TerminalNode o = (TerminalNode)other;
        return Objects.equals(getValue(), o.getValue());
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(getValue());
    }
}

