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

package com.redhat.thermostat.storage.query;

/**
 * An {@link Expression} with two operands and one operator.
 * @param <S> - the type of {@link Expression} corresponding to the left operand
 * @param <T> - the type of {@link Expression} corresponding to the right operand
 * @param <U> - the type of {@link BinaryOperator} corresponding to the operator
 */
abstract class BinaryExpression<S extends Expression, T extends Expression, U extends BinaryOperator>
        implements Expression {
    
    private S leftOperand;
    private U operator;
    private T rightOperand;
    
    /**
     * Constructs a {@link BinaryExpression} given two operands and an operator.
     * <p>
     * This constructor exists mainly for JSON serialization, use methods in
     * {@link ExpressionFactory} instead of this constructor.
     * @param leftOperand - left operand for this expression
     * @param operator - the operator for this expression
     * @param rightOperand - right operand for this expression
     */
    BinaryExpression(S leftOperand, U operator, T rightOperand) {
        this.leftOperand = leftOperand;
        this.operator = operator;
        this.rightOperand = rightOperand;
    }
    
    /**
     * @return the left operand of this expression
     */
    public S getLeftOperand() {
        return leftOperand;
    }
    
    /**
     * @return the operator of this expression
     */
    public U getOperator() {
        return operator;
    }
    
    /**
     * @return the right operand of this expression
     */
    public T getRightOperand() {
        return rightOperand;
    }
    
    @Override
    public boolean equals(Object obj) {
        boolean result = false;
        if (obj != null && obj instanceof BinaryExpression) {
            BinaryExpression<?, ?, ?> otherExpr = (BinaryExpression<?, ?, ?>) obj;
            result = leftOperand.equals(otherExpr.leftOperand)
                    && rightOperand.equals(otherExpr.rightOperand)
                    && operator.equals(otherExpr.operator);
        }
        return result;
    }
    
    @Override
    public int hashCode() {
        return leftOperand.hashCode() + operator.hashCode() + rightOperand.hashCode();
    }
    
    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder();
        buf.append("( ");
        buf.append(leftOperand.toString());
        buf.append(" ");
        buf.append(operator.toString());
        buf.append(" ");
        buf.append(rightOperand.toString());
        buf.append(" )");
        return buf.toString();
    }
}

