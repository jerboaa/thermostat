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

package com.redhat.thermostat.storage.query;

import com.redhat.thermostat.storage.core.Key;

/**
 * A {@link BinaryExpression} that corresponds to an algebraic comparison
 * between a {@link Key} and a corresponding value.
 * @param <T> - the type parameter of this expression's {@link Key}
 */
public final class BinaryComparisonExpression<T>
        extends BinaryExpression<LiteralExpression<Key<T>>, LiteralExpression<T>, BinaryComparisonOperator> 
        implements ComparisonExpression {
    
    /**
     * Constructs a {@link BinaryComparisonExpression} whose operands are
     * a {@link LiteralExpression} for a {@link Key} and a {@link LiteralExpression}
     * for the value to compare against the key.
     * <p>
     * This constructor exists mainly for JSON serialization, use methods in
     * {@link ExpressionFactory} instead of this constructor.
     * @param leftOperand - left operand for this expression
     * @param operator - the operator for this expression
     * @param rightOperand - right operand for this expression
     */
    public BinaryComparisonExpression(LiteralExpression<Key<T>> leftOperand,
            BinaryComparisonOperator operator, LiteralExpression<T> rightOperand) {
        super(leftOperand, operator, rightOperand);
    }
    
}

