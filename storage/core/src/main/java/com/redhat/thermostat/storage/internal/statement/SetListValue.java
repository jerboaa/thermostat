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

package com.redhat.thermostat.storage.internal.statement;

import java.util.Objects;

import com.redhat.thermostat.storage.core.IllegalPatchException;
import com.redhat.thermostat.storage.core.Key;
import com.redhat.thermostat.storage.core.PreparedParameter;
import com.redhat.thermostat.storage.query.LiteralExpression;

/**
 * Represents a value pair in a set list of prepared writes.
 *
 */
class SetListValue implements Patchable, Printable {

    private TerminalNode key;
    private TerminalNode value;
    
    TerminalNode getKey() {
        return key;
    }
    void setKey(TerminalNode key) {
        this.key = key;
    }
    TerminalNode getValue() {
        return value;
    }
    void setValue(TerminalNode value) {
        this.value = value;
    }
    
    @Override
    public boolean equals(Object other) {
        if (!(other instanceof SetListValue)) {
            return false;
        }
        SetListValue o = (SetListValue)other;
        return Objects.equals(this.getValue(), o.getValue()) &&
                Objects.equals(this.getKey(), o.getKey());
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(this.getKey(), this.getValue());
    }
    
    @Override
    public String toString() {
        return "{" + getKey() + " = " + getValue() + "}";
    }
    
    @Override
    public void print(int level) {
        for (int i = 0; i < level; i++) {
            System.out.print(" ");
        }
        System.out.println(toString());
        
    }
    
    @Override
    public PatchedSetListMemberExpression patch(PreparedParameter[] params)
            throws IllegalPatchException {
        
        // patch LHS
        PatchedWhereExpression keyExp = key.patch(params);
        @SuppressWarnings("unchecked") // we've generated the key
        LiteralExpression<Key<?>> keyLiteral = (LiteralExpression<Key<?>>)keyExp.getExpression();
        Key<?> keyVal = (Key<?>) keyLiteral.getValue();
        
        // patch RHS
        PatchedWhereExpression valExp = value.patch(params);
        LiteralExpression<?> valLiteral = (LiteralExpression<?>)valExp.getExpression();
        Object val = valLiteral.getValue();
        
        PatchedSetListMember member = new PatchedSetListMember(keyVal, val);
        PatchedSetListMemberExpressionImpl retval = new PatchedSetListMemberExpressionImpl(member);
        return retval;
    }
    
    private static class PatchedSetListMemberExpressionImpl implements PatchedSetListMemberExpression {

        private final PatchedSetListMember member;
        
        private PatchedSetListMemberExpressionImpl(PatchedSetListMember member) {
            this.member = member;
        }
        
        @Override
        public PatchedSetListMember getSetListMember() {
            return member;
        }
        
    }
}

