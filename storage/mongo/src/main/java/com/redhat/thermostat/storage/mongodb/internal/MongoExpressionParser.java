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

package com.redhat.thermostat.storage.mongodb.internal;

import java.util.Set;

import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.BasicDBObjectBuilder;
import com.mongodb.DBObject;
import com.redhat.thermostat.storage.core.Key;
import com.redhat.thermostat.storage.query.BinaryComparisonExpression;
import com.redhat.thermostat.storage.query.BinaryComparisonOperator;
import com.redhat.thermostat.storage.query.BinaryLogicalExpression;
import com.redhat.thermostat.storage.query.BinaryLogicalOperator;
import com.redhat.thermostat.storage.query.BinarySetMembershipExpression;
import com.redhat.thermostat.storage.query.BinarySetMembershipOperator;
import com.redhat.thermostat.storage.query.ComparisonExpression;
import com.redhat.thermostat.storage.query.Expression;
import com.redhat.thermostat.storage.query.LiteralSetExpression;
import com.redhat.thermostat.storage.query.LiteralExpression;
import com.redhat.thermostat.storage.query.UnaryLogicalExpression;
import com.redhat.thermostat.storage.query.UnaryLogicalOperator;

public class MongoExpressionParser {
    
    public DBObject parse(Expression expr) {
        DBObject result;
        if (expr instanceof BinaryComparisonExpression) {
            result = parseBinaryComparisonExpression((BinaryComparisonExpression<?>) expr);
        }
        else if (expr instanceof BinarySetMembershipExpression) {
            result = parseBinarySetComparisonExpression((BinarySetMembershipExpression<?>) expr);
        }
        else if (expr instanceof BinaryLogicalExpression) {
            // LHS OP RHS ==> { OP : [ LHS, RHS ] }
            BinaryLogicalExpression<?, ?> biExpr = (BinaryLogicalExpression<?, ?>) expr;
            BinaryLogicalOperator op = biExpr.getOperator();
            
            DBObject leftResult = parse(biExpr.getLeftOperand());
            DBObject rightResult = parse(biExpr.getRightOperand());
            BasicDBList list = new BasicDBList();
            list.add(leftResult);
            list.add(rightResult);
            
            result = new BasicDBObject(getLogicalOperator(op), list);
        }
        else if (expr instanceof UnaryLogicalExpression) {
            UnaryLogicalExpression<?> uniExpr = (UnaryLogicalExpression<?>) expr;
            ComparisonExpression operand = uniExpr.getOperand();
            UnaryLogicalOperator op = uniExpr.getOperator();
            // Special case
            if (op == UnaryLogicalOperator.NOT && operand instanceof BinaryComparisonExpression) {
                BinaryComparisonExpression<?> binExpr = (BinaryComparisonExpression<?>) operand;
                if (binExpr.getOperator() == BinaryComparisonOperator.EQUALS) {
                    // TODO Convert to $ne?
                    throw new IllegalArgumentException("Cannot use $not with equality");
                }
            }
            DBObject object = parse(operand);
            insertUnaryLogicalOperator(object, op);
            result = object;
        }
        else {
            throw new IllegalArgumentException("Unknown Expression of type " + expr.getClass());
        }
        
        return result;
    }

    private void insertUnaryLogicalOperator(DBObject object, UnaryLogicalOperator op) {
        String strOp = getLogicalOperator(op);
        // OP { LHS : RHS } => { LHS : { OP : RHS }}
        // Apply operator to each key
        Set<String> keySet = object.keySet();
        for (String key : keySet) {
            Object value = object.get(key);
            BasicDBObject newValue = new BasicDBObject(strOp, value);
            object.put(key, newValue);
        }
    }

    private <T> DBObject parseBinaryComparisonExpression(BinaryComparisonExpression<T> expr) {
        BasicDBObjectBuilder builder = BasicDBObjectBuilder.start();
        LiteralExpression<Key<T>> leftExpr = expr.getLeftOperand();
        LiteralExpression<T> rightExpr = expr.getRightOperand();
        BinaryComparisonOperator op = expr.getOperator();
        if (op == BinaryComparisonOperator.EQUALS) {
            // LHS == RHS => { LHS : RHS }
            builder.add(leftExpr.getValue().getName(), rightExpr.getValue());
        }
        else {
            // LHS OP RHS => { LHS : { OP : RHS } }
            builder.push(leftExpr.getValue().getName());

            String mongoOp = getComparisonOperator(op);
            builder.add(mongoOp, rightExpr.getValue());

            builder.pop();
        }
        return builder.get();
    }
    
    private <T> DBObject parseBinarySetComparisonExpression(BinarySetMembershipExpression<T> expr) {
        BasicDBObjectBuilder builder = BasicDBObjectBuilder.start();
        LiteralExpression<Key<T>> leftExpr = expr.getLeftOperand();
        LiteralSetExpression<T> rightExpr = expr.getRightOperand();
        BinarySetMembershipOperator op = expr.getOperator();
        // LHS OP [ RHS ] => { LHS : { OP : [ RHS ] } }
        builder.push(leftExpr.getValue().getName());

        String mongoOp = getSetMembershipOperator(op);
        builder.add(mongoOp, rightExpr.getValues());

        builder.pop();
        return builder.get();
    }
    
    private String getComparisonOperator(BinaryComparisonOperator operator) {
        String result;
        switch (operator) {
        case NOT_EQUAL_TO:
            result = "$ne";
            break;
        case LESS_THAN:
            result = "$lt";
            break;
        case LESS_THAN_OR_EQUAL_TO:
            result = "$lte";
            break;
        case GREATER_THAN:
            result = "$gt";
            break;
        case GREATER_THAN_OR_EQUAL_TO:
            result = "$gte";
            break;
        default:
            throw new IllegalArgumentException("MongoQuery can not handle " + operator);
        }
        return result;
    }
    
    private String getSetMembershipOperator(BinarySetMembershipOperator operator) {
        String result;
        switch (operator) {
        case IN:
            result = "$in";
            break;
        case NOT_IN:
            result = "$nin";
            break;
        default:
            throw new IllegalArgumentException("MongoQuery can not handle " + operator);
        }
        return result;
    }
    
    private String getLogicalOperator(BinaryLogicalOperator operator) {
        String result;
        switch (operator) {
        case AND:
            result = "$and";
            break;
        case OR:
            result = "$or";
            break;
        default:
            throw new IllegalArgumentException("MongoQuery can not handle " + operator);
        }
        return result;
    }
    
    private String getLogicalOperator(UnaryLogicalOperator operator) {
        String result;
        switch (operator) {
        case NOT:
            result = "$not";
            break;
        default:
            throw new IllegalArgumentException("MongoQuery can not handle " + operator);
        }
        return result;
    }
    
}

