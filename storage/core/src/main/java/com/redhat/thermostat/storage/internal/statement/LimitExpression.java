/*
 * Copyright 2012, 2013 Red Hat, Inc.
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

import com.redhat.thermostat.storage.core.IllegalPatchException;
import com.redhat.thermostat.storage.core.PreparedParameter;

/**
 * Represents a limit expression in the prepared statement's parse tree.
 *
 */
class LimitExpression implements Printable, Patchable {

    private Object value;

    public Object getValue() {
        return value;
    }

    public void setValue(Object value) {
        this.value = value;
    }

    @Override
    public void print(int level) {
        System.out.println("LIMIT: " + getValue());
    }

    @Override
    public PatchedLimitExpression patch(PreparedParameter[] params)
            throws IllegalPatchException {
        if (value instanceof Unfinished) {
            Unfinished unfinished = (Unfinished)value;
            try {
                PreparedParameter param = params[unfinished.getParameterIndex()];
                Class<?> typeClass = param.getType();
                if (typeClass != int.class) {
                    String msg = "Invalid parameter type for limit expression. Expected integer!";
                    IllegalArgumentException e = new IllegalArgumentException(msg);
                    throw e;
                }
                int limitVal = (Integer)param.getValue();
                return new PatchedLimitExpressionImpl(limitVal);
            } catch (Exception e) {
                throw new IllegalPatchException(e);
            }
        } else {
            // must have been int, since parsing would have failed otherwise
            int limitVal = (int)getValue();
            return new PatchedLimitExpressionImpl(limitVal);
        }
    }
    
    private static class PatchedLimitExpressionImpl implements PatchedLimitExpression {
        
        private final int val;
        
        PatchedLimitExpressionImpl(int limitVal) {
            this.val = limitVal;
        }

        @Override
        public int getLimitValue() {
            return val;
        }
        
    }
    
}
