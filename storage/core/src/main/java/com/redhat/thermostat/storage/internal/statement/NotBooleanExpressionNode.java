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
import com.redhat.thermostat.storage.core.PreparedParameter;
import com.redhat.thermostat.storage.query.ComparisonExpression;
import com.redhat.thermostat.storage.query.UnaryLogicalExpression;
import com.redhat.thermostat.storage.query.UnaryLogicalOperator;

/**
 * A node representing a boolean not expression in the prepared statement's
 * parse tree.
 *
 */
class NotBooleanExpressionNode extends UnaryExpressionNode {

    NotBooleanExpressionNode(Node parent) {
        super(parent);
    }

    @Override
    public UnaryLogicalOperator getOperator() {
        return UnaryLogicalOperator.NOT;
    }
    
    @Override
    public PatchedWhereExpression patch(PreparedParameter[] params) throws IllegalPatchException {
        if (getValue() == null || !(getValue() instanceof Node) ) {
            String msg = getClass().getSimpleName() +
                    " invalid when attempted to patch";
            IllegalStateException cause = new IllegalStateException(msg);
            throw new IllegalPatchException(cause);
        }
        Node node = (Node)getValue();
        PatchedWhereExpression patched = node.patch(params);
        // If this cast fails we are in serious trouble. Mongodb doesn't support
        // something like NOT ( a AND b ). However, the grammar does not support
        // parenthesized expressions, NOT has higher precedence as AND/OR
        // expressions and the LHS and RHS of binary boolean expressions are
        // required to be binary comparison expressions.
        // Hence, we wouldn't parse an expression such as the above anyway.
        ComparisonExpression expr = (ComparisonExpression)patched.getExpression();
        UnaryLogicalExpression<ComparisonExpression> notExpr = new UnaryLogicalExpression<>(
                expr, getOperator());
        return new PatchedWhereExpressionImpl(notExpr);
    }
    
    @Override
    public boolean equals(Object other) {
        if (other == null) {
            return false;
        }
        if (!(other instanceof NotBooleanExpressionNode)) {
            return false;
        }
        NotBooleanExpressionNode o = (NotBooleanExpressionNode)other;
        return Objects.equals(getValue(), o.getValue());
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(getOperator(), getValue());
    }

}

