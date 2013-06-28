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

package com.redhat.thermostat.web.common;

import java.lang.reflect.Type;
import java.util.HashSet;
import java.util.Set;

import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.redhat.thermostat.storage.core.Key;
import com.redhat.thermostat.storage.query.BinaryComparisonExpression;
import com.redhat.thermostat.storage.query.BinaryComparisonOperator;
import com.redhat.thermostat.storage.query.BinaryLogicalExpression;
import com.redhat.thermostat.storage.query.BinaryLogicalOperator;
import com.redhat.thermostat.storage.query.BinarySetMembershipExpression;
import com.redhat.thermostat.storage.query.BinarySetMembershipOperator;
import com.redhat.thermostat.storage.query.ComparisonExpression;
import com.redhat.thermostat.storage.query.Expression;
import com.redhat.thermostat.storage.query.LiteralExpression;
import com.redhat.thermostat.storage.query.LiteralSetExpression;
import com.redhat.thermostat.storage.query.Operator;
import com.redhat.thermostat.storage.query.UnaryLogicalExpression;
import com.redhat.thermostat.storage.query.UnaryLogicalOperator;

public class ExpressionSerializer implements JsonSerializer<Expression>,
        JsonDeserializer<Expression> {
    /* The concrete Expression fully-qualified class name */
    static final String PROP_CLASS_NAME = "PROP_CLASS_NAME";
    /* Serialized operand for a UnaryExpression */
    static final String PROP_OPERAND = "PROP_OPERAND";
    /* Serialized left operand for a BinaryExpression */
    static final String PROP_OPERAND_LEFT = "PROP_OPERAND_LEFT";
    /* Serialized right operand for a BinaryExpression */
    static final String PROP_OPERAND_RIGHT = "PROP_OPERAND_RIGHT";
    /* Serialized operator for an Expression */
    static final String PROP_OPERATOR = "PROP_OPERATOR";
    /* Serialized value for a LiteralExpression */
    static final String PROP_VALUE = "PROP_VALUE";
    /* Fully-qualified class name of a LiteralExpression's value */
    static final String PROP_VALUE_CLASS = "PROP_VALUE_CLASS";
    
    @Override
    public Expression deserialize(JsonElement json, Type typeOfT,
            JsonDeserializationContext context) throws JsonParseException {
        JsonElement jsonClassName = json.getAsJsonObject().get(PROP_CLASS_NAME);
        if (jsonClassName == null) {
            throw new JsonParseException("No class name property provided");
        }
        String className = jsonClassName.getAsString();
        Expression result;
        try {
            Class<?> clazz = (Class<?>) Class.forName(className);
            // Deserialize using concrete implementations to avoid reflection
            // and unchecked casts
            if (BinaryComparisonExpression.class.isAssignableFrom(clazz)) {
                result = deserializeBinaryComparisonExpression(json, context);
            }
            else if (BinarySetMembershipExpression.class.isAssignableFrom(clazz)) {
                result = deserializeBinarySetMembershipExpression(json, context);
            }
            else if (BinaryLogicalExpression.class.isAssignableFrom(clazz)) {
                result = deserializeBinaryLogicalExpression(json, context);
            }
            else if (UnaryLogicalExpression.class.isAssignableFrom(clazz)) {
                result = deserializeUnaryLogicalExpression(json, context);
            }
            else if (LiteralExpression.class.isAssignableFrom(clazz)) {
                result = deserializeLiteralExpression(json, context);
            }
            else if (LiteralSetExpression.class.isAssignableFrom(clazz)) {
                result = deserializeLiteralArrayExpression(json, context);
            }
            else {
                throw new JsonParseException("Unknown Expression of type " + className);
            }
        } catch (ClassNotFoundException e) {
            throw new JsonParseException("Unable to deserialize Expression", e);
        }
        return result;
    }

    private <T> Expression deserializeBinaryComparisonExpression(JsonElement json,
            JsonDeserializationContext context) {
        JsonElement jsonLeft = json.getAsJsonObject().get(PROP_OPERAND_LEFT);
        JsonElement jsonRight = json.getAsJsonObject().get(PROP_OPERAND_RIGHT);
        JsonElement jsonOp = json.getAsJsonObject().get(PROP_OPERATOR);
        
        LiteralExpression<Key<T>> left = context.deserialize(jsonLeft, Expression.class);
        LiteralExpression<T> right = context.deserialize(jsonRight, Expression.class);
        BinaryComparisonOperator op = context.deserialize(jsonOp, Operator.class);
        return new BinaryComparisonExpression<>(left, op, right);
    }
    
    private <T> Expression deserializeBinarySetMembershipExpression(JsonElement json,
            JsonDeserializationContext context) {
        JsonElement jsonLeft = json.getAsJsonObject().get(PROP_OPERAND_LEFT);
        JsonElement jsonRight = json.getAsJsonObject().get(PROP_OPERAND_RIGHT);
        JsonElement jsonOp = json.getAsJsonObject().get(PROP_OPERATOR);
        
        LiteralExpression<Key<T>> left = context.deserialize(jsonLeft, Expression.class);
        LiteralSetExpression<T> right = context.deserialize(jsonRight, Expression.class);
        BinarySetMembershipOperator op = context.deserialize(jsonOp, Operator.class);
        return new BinarySetMembershipExpression<>(left, op, right);
    }

    private <S extends Expression, T extends Expression> Expression deserializeBinaryLogicalExpression(JsonElement json,
            JsonDeserializationContext context) {
        JsonElement jsonLeft = json.getAsJsonObject().get(PROP_OPERAND_LEFT);
        JsonElement jsonRight = json.getAsJsonObject().get(PROP_OPERAND_RIGHT);
        JsonElement jsonOp = json.getAsJsonObject().get(PROP_OPERATOR);
        
        S left = context.deserialize(jsonLeft, Expression.class);
        T right = context.deserialize(jsonRight, Expression.class);
        BinaryLogicalOperator op = context.deserialize(jsonOp, Operator.class);
        return new BinaryLogicalExpression<>(left, op, right);
    }

    private <T extends ComparisonExpression> Expression deserializeUnaryLogicalExpression(JsonElement json,
            JsonDeserializationContext context) {
        JsonElement jsonOperand = json.getAsJsonObject().get(PROP_OPERAND);
        JsonElement jsonOp = json.getAsJsonObject().get(PROP_OPERATOR);
        
        T operand = context.deserialize(jsonOperand, Expression.class);
        UnaryLogicalOperator operator = context.deserialize(jsonOp, Operator.class);
        return new UnaryLogicalExpression<>(operand, operator);
    }

    private Expression deserializeLiteralExpression(JsonElement json,
            JsonDeserializationContext context) throws ClassNotFoundException {
        JsonElement jsonValue = json.getAsJsonObject().get(PROP_VALUE);
        JsonElement jsonValueClass = json.getAsJsonObject().get(PROP_VALUE_CLASS);
        String valueClassName = jsonValueClass.getAsString();
        Class<?> valueClass = Class.forName(valueClassName);
        return makeLiteralExpression(context, jsonValue, valueClass);
    }
    
    private <T> Expression makeLiteralExpression(JsonDeserializationContext context, 
            JsonElement jsonValue, Class<T> valueClass) throws ClassNotFoundException {
        T value = context.deserialize(jsonValue, valueClass);
        return new LiteralExpression<>(value);
    }

    private Expression deserializeLiteralArrayExpression(JsonElement json,
            JsonDeserializationContext context) throws ClassNotFoundException {
        JsonElement jsonValue = json.getAsJsonObject().get(PROP_VALUE);
        if (jsonValue instanceof JsonArray) {
            JsonElement jsonValueClass = json.getAsJsonObject().get(PROP_VALUE_CLASS);
            String valueClassName = jsonValueClass.getAsString();
            Class<?> type = Class.forName(valueClassName);
            JsonArray jsonArr = (JsonArray) jsonValue;
            return makeLiteralArrayExpression(context, jsonArr, type);
        }
        else {
            throw new JsonParseException("No JsonArray supplied for " + PROP_VALUE);
        }
    }

    private <T> Expression makeLiteralArrayExpression(JsonDeserializationContext context,
            JsonArray jsonArr, Class<T> valueClass) {
        Set<T> values = new HashSet<>();
        for (JsonElement element : jsonArr) {
            T value = context.deserialize(element, valueClass);
            values.add(value);
        }
        return new LiteralSetExpression<>(values, valueClass);
    }

    @Override
    public JsonElement serialize(Expression src, Type typeOfSrc,
            JsonSerializationContext context) {
        JsonObject result;
        // Only concrete implementations are public
        if (src instanceof BinaryLogicalExpression) {
            BinaryLogicalExpression<?, ?> binExpr = (BinaryLogicalExpression<?, ?>) src;
            JsonElement left = context.serialize(binExpr.getLeftOperand());
            JsonElement op = context.serialize(binExpr.getOperator());
            JsonElement right = context.serialize(binExpr.getRightOperand());
            result = new JsonObject();
            result.add(PROP_OPERAND_LEFT, left);
            result.add(PROP_OPERATOR, op);
            result.add(PROP_OPERAND_RIGHT, right);
        }
        else if (src instanceof BinaryComparisonExpression) {
            BinaryComparisonExpression<?> binExpr = (BinaryComparisonExpression<?>) src;
            JsonElement left = context.serialize(binExpr.getLeftOperand());
            JsonElement op = context.serialize(binExpr.getOperator());
            JsonElement right = context.serialize(binExpr.getRightOperand());
            result = new JsonObject();
            result.add(PROP_OPERAND_LEFT, left);
            result.add(PROP_OPERATOR, op);
            result.add(PROP_OPERAND_RIGHT, right);
        }
        else if (src instanceof BinarySetMembershipExpression) {
            BinarySetMembershipExpression<?> binExpr = (BinarySetMembershipExpression<?>) src;
            JsonElement left = context.serialize(binExpr.getLeftOperand());
            JsonElement op = context.serialize(binExpr.getOperator());
            JsonElement right = context.serialize(binExpr.getRightOperand());
            result = new JsonObject();
            result.add(PROP_OPERAND_LEFT, left);
            result.add(PROP_OPERATOR, op);
            result.add(PROP_OPERAND_RIGHT, right);
        }
        else if (src instanceof UnaryLogicalExpression) {
            UnaryLogicalExpression<?> unaryExpr = (UnaryLogicalExpression<?>) src;
            JsonElement operand = context.serialize(unaryExpr.getOperand());
            JsonElement operator = context.serialize(unaryExpr.getOperator());
            result = new JsonObject();
            result.add(PROP_OPERAND, operand);
            result.add(PROP_OPERATOR, operator);
        }
        else if (src instanceof LiteralExpression) {
            LiteralExpression<?> litExpr = (LiteralExpression<?>) src;
            JsonElement value = context.serialize(litExpr.getValue());
            result = new JsonObject();
            result.add(PROP_VALUE, value);
            // Store the type of value to properly deserialize it later
            result.addProperty(PROP_VALUE_CLASS, litExpr.getValue().getClass().getCanonicalName());
        }
        else if (src instanceof LiteralSetExpression) {
            LiteralSetExpression<?> litArrExpr = (LiteralSetExpression<?>) src;
            result = serializeLiteralArrayExpression(litArrExpr, context);
        }
        else {
            throw new JsonParseException("Unknown expression of type " + src.getClass());
        }
        result.addProperty(PROP_CLASS_NAME, src.getClass().getCanonicalName());
        return result;
    }
    
    private <T> JsonObject serializeLiteralArrayExpression(LiteralSetExpression<T> expr,
            JsonSerializationContext context) {
        JsonObject result = new JsonObject();
        Set<T> list = expr.getValues();
        Class<T> type = expr.getType();
        JsonArray arr = new JsonArray();
        for (T element : list) {
            arr.add(context.serialize(element, type));
        }
        result.add(PROP_VALUE, arr);
        // Store type of list elements
        result.addProperty(PROP_VALUE_CLASS, type.getCanonicalName());
        return result;
    }

}
