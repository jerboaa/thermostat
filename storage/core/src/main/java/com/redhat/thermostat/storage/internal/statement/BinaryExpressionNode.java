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

import java.util.Objects;

import com.redhat.thermostat.storage.core.IllegalPatchException;
import com.redhat.thermostat.storage.core.Key;
import com.redhat.thermostat.storage.core.PreparedParameter;
import com.redhat.thermostat.storage.query.BinaryComparisonExpression;
import com.redhat.thermostat.storage.query.BinaryComparisonOperator;
import com.redhat.thermostat.storage.query.BinaryLogicalExpression;
import com.redhat.thermostat.storage.query.BinaryLogicalOperator;
import com.redhat.thermostat.storage.query.Expression;
import com.redhat.thermostat.storage.query.LiteralExpression;
import com.redhat.thermostat.storage.query.Operator;

class BinaryExpressionNode extends Node {
    
    private Node leftChild;
    private Node rightChild;
    private Operator operator;

    public Operator getOperator() {
        return operator;
    }

    public void setOperator(Operator operator) {
        this.operator = operator;
    }

    BinaryExpressionNode(Node parent) {
        super(parent);
    }

    public Node getLeftChild() {
        return leftChild;
    }

    public void setLeftChild(Node leftChild) {
        this.leftChild = leftChild;
    }

    public Node getRightChild() {
        return rightChild;
    }

    public void setRightChild(Node rightChild) {
        this.rightChild = rightChild;
    }
    
    @Override
    public PatchedWhereExpression patch(PreparedParameter[] params) throws IllegalPatchException {
        if (leftChild == null || rightChild == null || getOperator() == null) {
            String msg = BinaryExpressionNode.class.getSimpleName() +
                    " invalid when attempted to patch";
            IllegalStateException cause = new IllegalStateException(msg);
            throw new IllegalPatchException(cause);
        }
        PatchedWhereExpression left = leftChild.patch(params);
        PatchedWhereExpression right = rightChild.patch(params);
        
        Expression leftExpression = left.getExpression();
        Expression rightExpression = right.getExpression();
        return createExpression(leftExpression, rightExpression);
    }

    private PatchedWhereExpression createExpression(Expression leftExpression,
            Expression rightExpression) {
        if (operator instanceof BinaryComparisonOperator) {
            return getBinaryComparisonExpression(leftExpression, (BinaryComparisonOperator) operator, rightExpression);
        } else if (operator instanceof BinaryLogicalOperator) {
            return getBinaryLogicalExpression(leftExpression, (BinaryLogicalOperator) operator, rightExpression);
        }
        return null;
    }
    
    private PatchedWhereExpression getBinaryLogicalExpression(Expression a,
            BinaryLogicalOperator op, Expression b) {
        BinaryLogicalExpression<Expression, Expression> impl = new BinaryLogicalExpression<Expression, Expression>(
                a, op, b);
        return new PatchedWhereExpressionImpl(impl);
    }

    @SuppressWarnings("unchecked") // Unchecked casts to LiteralExpression
    private <T> PatchedWhereExpressionImpl getBinaryComparisonExpression(Expression a, BinaryComparisonOperator op, Expression b) {
        LiteralExpression<Key<T>> leftOperand = (LiteralExpression<Key<T>>) a;
        LiteralExpression<T> rightOperand = (LiteralExpression<T>)b;
        BinaryComparisonExpression<T> impl = new BinaryComparisonExpression<>(
                leftOperand, op, rightOperand);
        return new PatchedWhereExpressionImpl(impl);
    }

    @Override
    public void print(int level) {
        for (int i = 0; i < level; i++) {
            System.out.print("-");
        }
        System.out.print("B: " + getOperator());
        System.out.println("");
        int newLevel = level + 1;
        if (leftChild != null) {
            leftChild.print(newLevel);
        }
        if (rightChild != null) {
            rightChild.print(newLevel);
        }
    }
    
    @Override
    public boolean equals(Object other) {
        if (other == null) {
            return false;
        }
        if (!(other instanceof BinaryExpressionNode)) {
            return false;
        }
        BinaryExpressionNode o = (BinaryExpressionNode)other;
        return this.getOperator().equals(o.getOperator()) &&
                Objects.equals(leftChild, o.leftChild) &&
                Objects.equals(rightChild, o.rightChild);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(getOperator(), leftChild, rightChild, getParent());
    }
}
