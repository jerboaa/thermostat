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

package com.redhat.thermostat.storage.internal.statement;

import com.redhat.thermostat.storage.core.IllegalPatchException;
import com.redhat.thermostat.storage.core.Key;
import com.redhat.thermostat.storage.core.PreparedParameter;
import com.redhat.thermostat.storage.core.Query.SortDirection;

/**
 * Represents sort members.
 * 
 * @see SortExpression
 *
 */
class SortMember implements Printable, Patchable {

    private SortDirection direction;
    private Object sortKey;

    public SortDirection getDirection() {
        return direction;
    }

    public void setDirection(SortDirection direction) {
        this.direction = direction;
    }

    public Object getSortKey() {
        return sortKey;
    }

    public void setSortKey(Object sortKey) {
        this.sortKey = sortKey;
    }

    @Override
    public void print(int level) {
        System.out.print(getSortKey() + " " + getDirection().name());
    }

    @Override
    public PatchedSortMemberExpression patch(PreparedParameter[] params)
            throws IllegalPatchException {
        try {
            String keyVal = null;
            if (getSortKey() instanceof Unfinished) {
                Unfinished unfinished = (Unfinished)getSortKey();
                PreparedParameter p = params[unfinished.getParameterIndex()];
                // Should only allow patching of ?s type NOT ?s[
                if (p.getType() != String.class) {
                    String msg = "Illegal parameter type for index "
                            + unfinished.getParameterIndex()
                            + ". Expected String!";
                    IllegalArgumentException iae = new IllegalArgumentException(msg);
                    throw iae;
                }
                keyVal = (String)p.getValue();
            } else {
                keyVal = (String)getSortKey();
            }
            Key<?> sortKey = new Key<>(keyVal);
            PatchedSortMember m = new PatchedSortMember(sortKey, getDirection());
            return new PatchedSortMemberExpressionImpl(m);
        } catch (Exception e) {
            throw new IllegalPatchException(e);
        }
    }
    
    private static class PatchedSortMemberExpressionImpl implements PatchedSortMemberExpression {
        
        private final PatchedSortMember member;
        private PatchedSortMemberExpressionImpl(PatchedSortMember member) {
            this.member = member;
        }
        
        @Override
        public PatchedSortMember getSortMember() {
            return member;
        }
        
    }
    
}

