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

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.HashSet;

import org.junit.Before;
import org.junit.Test;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.redhat.thermostat.storage.core.Key;
import com.redhat.thermostat.storage.query.BinaryComparisonExpression;
import com.redhat.thermostat.storage.query.BinaryLogicalExpression;
import com.redhat.thermostat.storage.query.BinarySetMembershipExpression;
import com.redhat.thermostat.storage.query.Expression;
import com.redhat.thermostat.storage.query.ExpressionFactory;
import com.redhat.thermostat.storage.query.LiteralSetExpression;
import com.redhat.thermostat.storage.query.LiteralExpression;
import com.redhat.thermostat.storage.query.Operator;
import com.redhat.thermostat.storage.query.UnaryLogicalExpression;

public class ExpressionSerializerTest {

    private static final Key<String> key = new Key<>("hello");
    private Gson gson;

    private static final class TestExpression implements Expression {
        
    }
    
    @Before
    public void setUp() throws Exception {
        gson = new GsonBuilder()
                .registerTypeHierarchyAdapter(Expression.class,
                        new ExpressionSerializer())
                .registerTypeHierarchyAdapter(Operator.class,
                        new OperatorSerializer()).create();
    }

    @Test
    public void testDeserializeLiteralExpression() {
        Expression expr = new LiteralExpression<String>("hello");
        
        JsonObject json = new JsonObject();
        json.addProperty(ExpressionSerializer.PROP_VALUE, "hello");
        json.addProperty(ExpressionSerializer.PROP_VALUE_CLASS, String.class.getCanonicalName());
        json.addProperty(ExpressionSerializer.PROP_CLASS_NAME, LiteralExpression.class.getCanonicalName());
        
        assertEquals(expr, gson.fromJson(json, Expression.class));
    }
    
    @Test
    public void testDeserializeLiteralArrayExpression() {
        Expression expr = new LiteralSetExpression<String>(new HashSet<>(Arrays.asList("hello", "goodbye")), 
                String.class);
        
        JsonObject json = new JsonObject();
        JsonArray arr = new JsonArray();
        arr.add(gson.toJsonTree("hello"));
        arr.add(gson.toJsonTree("goodbye"));
        json.add(ExpressionSerializer.PROP_VALUE, arr);
        json.addProperty(ExpressionSerializer.PROP_VALUE_CLASS, String.class.getCanonicalName());
        json.addProperty(ExpressionSerializer.PROP_CLASS_NAME, LiteralSetExpression.class.getCanonicalName());
        
        assertEquals(expr, gson.fromJson(json, Expression.class));
    }
    
    @Test
    public void testDeserializeBinaryComparisonExpression() {
        ExpressionFactory factory = new ExpressionFactory();
        BinaryComparisonExpression<String> expr = factory.equalTo(key, "world");
        
        JsonObject json = new JsonObject();
        json.add(ExpressionSerializer.PROP_OPERAND_LEFT, gson.toJsonTree(expr.getLeftOperand()));
        json.add(ExpressionSerializer.PROP_OPERATOR, gson.toJsonTree(expr.getOperator()));
        json.add(ExpressionSerializer.PROP_OPERAND_RIGHT, gson.toJsonTree(expr.getRightOperand()));
        json.addProperty(ExpressionSerializer.PROP_CLASS_NAME, BinaryComparisonExpression.class.getCanonicalName());
        
        assertEquals(expr, gson.fromJson(json, Expression.class));
    }
    
    @Test
    public void testDeserializeBinarySetMembershipExpression() {
        ExpressionFactory factory = new ExpressionFactory();
        BinarySetMembershipExpression<String> expr = factory.in(key, 
                new HashSet<>(Arrays.asList("world", "goodbye")), String.class);
        
        JsonObject json = new JsonObject();
        json.add(ExpressionSerializer.PROP_OPERAND_LEFT, gson.toJsonTree(expr.getLeftOperand()));
        json.add(ExpressionSerializer.PROP_OPERATOR, gson.toJsonTree(expr.getOperator()));
        json.add(ExpressionSerializer.PROP_OPERAND_RIGHT, gson.toJsonTree(expr.getRightOperand()));
        json.addProperty(ExpressionSerializer.PROP_CLASS_NAME, BinarySetMembershipExpression.class.getCanonicalName());
        
        assertEquals(expr, gson.fromJson(json, Expression.class));
    }
    
    @Test
    public void testDeserializeBinaryLogicalExpression() {
        ExpressionFactory factory = new ExpressionFactory();
        BinaryLogicalExpression<BinaryComparisonExpression<String>, BinaryComparisonExpression<String>> expr = factory.and(factory.equalTo(key, "world"), factory.greaterThan(key, "goodbye"));
        
        JsonObject json = new JsonObject();
        json.add(ExpressionSerializer.PROP_OPERAND_LEFT, gson.toJsonTree(expr.getLeftOperand()));
        json.add(ExpressionSerializer.PROP_OPERATOR, gson.toJsonTree(expr.getOperator()));
        json.add(ExpressionSerializer.PROP_OPERAND_RIGHT, gson.toJsonTree(expr.getRightOperand()));
        json.addProperty(ExpressionSerializer.PROP_CLASS_NAME, BinaryLogicalExpression.class.getCanonicalName());
        
        assertEquals(expr, gson.fromJson(json, Expression.class));
    }
    
    @Test
    public void testDeserializeUnaryLogicalExpression() {
        ExpressionFactory factory = new ExpressionFactory();
        UnaryLogicalExpression<BinaryComparisonExpression<String>> expr = factory.not(factory.greaterThan(key, "goodbye"));
        
        JsonObject json = new JsonObject();
        json.add(ExpressionSerializer.PROP_OPERAND, gson.toJsonTree(expr.getOperand()));
        json.add(ExpressionSerializer.PROP_OPERATOR, gson.toJsonTree(expr.getOperator()));
        json.addProperty(ExpressionSerializer.PROP_CLASS_NAME, UnaryLogicalExpression.class.getCanonicalName());
        
        assertEquals(expr, gson.fromJson(json, Expression.class));
    }
    
    @Test(expected=JsonParseException.class)
    public void testDeserializeNoClassName() {
        JsonObject json = new JsonObject();
        
        gson.fromJson(json, Expression.class);
    }
    
    @Test(expected=JsonParseException.class)
    public void testDeserializeUnknownExpression() {
        JsonObject json = new JsonObject();
        json.addProperty(ExpressionSerializer.PROP_CLASS_NAME, TestExpression.class.getCanonicalName());
        
        gson.fromJson(json, Expression.class);
    }

    @Test
    public void testSerializeLiteralExpression() {
        Expression expr = new LiteralExpression<String>("hello");
        
        JsonObject json = new JsonObject();
        json.addProperty(ExpressionSerializer.PROP_VALUE, "hello");
        json.addProperty(ExpressionSerializer.PROP_VALUE_CLASS, String.class.getCanonicalName());
        json.addProperty(ExpressionSerializer.PROP_CLASS_NAME, LiteralExpression.class.getCanonicalName());
        
        assertEquals(gson.toJson(json), gson.toJson(expr));
    }
    
    @Test
    public void testSerializeLiteralArrayExpression() {
        Expression expr = new LiteralSetExpression<String>(new HashSet<>(Arrays.asList("hello", "goodbye")),
                String.class);
        
        JsonObject json = new JsonObject();
        JsonArray arr = new JsonArray();
        arr.add(gson.toJsonTree("hello"));
        arr.add(gson.toJsonTree("goodbye"));
        json.add(ExpressionSerializer.PROP_VALUE, arr);
        json.addProperty(ExpressionSerializer.PROP_VALUE_CLASS, String.class.getCanonicalName());
        json.addProperty(ExpressionSerializer.PROP_CLASS_NAME, LiteralSetExpression.class.getCanonicalName());
        
        assertEquals(gson.toJson(json), gson.toJson(expr));
    }
    
    @Test
    public void testSerializeBinaryComparisonExpression() {
        ExpressionFactory factory = new ExpressionFactory();
        BinaryComparisonExpression<String> expr = factory.equalTo(key, "world");
        
        JsonObject json = new JsonObject();
        json.add(ExpressionSerializer.PROP_OPERAND_LEFT, gson.toJsonTree(expr.getLeftOperand()));
        json.add(ExpressionSerializer.PROP_OPERATOR, gson.toJsonTree(expr.getOperator()));
        json.add(ExpressionSerializer.PROP_OPERAND_RIGHT, gson.toJsonTree(expr.getRightOperand()));
        json.addProperty(ExpressionSerializer.PROP_CLASS_NAME, BinaryComparisonExpression.class.getCanonicalName());
        
        assertEquals(gson.toJson(json), gson.toJson(expr));
    }
    
    @Test
    public void testSerializeBinarySetMembershipExpression() {
        ExpressionFactory factory = new ExpressionFactory();
        BinarySetMembershipExpression<String> expr = factory.in(key, new HashSet<>(Arrays.asList("world", "goodbye")),
                String.class);
        
        JsonObject json = new JsonObject();
        json.add(ExpressionSerializer.PROP_OPERAND_LEFT, gson.toJsonTree(expr.getLeftOperand()));
        json.add(ExpressionSerializer.PROP_OPERATOR, gson.toJsonTree(expr.getOperator()));
        json.add(ExpressionSerializer.PROP_OPERAND_RIGHT, gson.toJsonTree(expr.getRightOperand()));
        json.addProperty(ExpressionSerializer.PROP_CLASS_NAME, BinarySetMembershipExpression.class.getCanonicalName());
        
        assertEquals(gson.toJson(json), gson.toJson(expr));
    }
    
    @Test
    public void testSerializeBinaryLogicalExpression() {
        ExpressionFactory factory = new ExpressionFactory();
        BinaryLogicalExpression<BinaryComparisonExpression<String>, BinaryComparisonExpression<String>> expr = factory.and(factory.equalTo(key, "world"), factory.greaterThan(key, "goodbye"));
        
        JsonObject json = new JsonObject();
        json.add(ExpressionSerializer.PROP_OPERAND_LEFT, gson.toJsonTree(expr.getLeftOperand()));
        json.add(ExpressionSerializer.PROP_OPERATOR, gson.toJsonTree(expr.getOperator()));
        json.add(ExpressionSerializer.PROP_OPERAND_RIGHT, gson.toJsonTree(expr.getRightOperand()));
        json.addProperty(ExpressionSerializer.PROP_CLASS_NAME, BinaryLogicalExpression.class.getCanonicalName());
        
        assertEquals(gson.toJson(json), gson.toJson(expr));
    }
    
    @Test
    public void testSerializeUnaryLogicalExpression() {
        ExpressionFactory factory = new ExpressionFactory();
        UnaryLogicalExpression<BinaryComparisonExpression<String>> expr = factory.not(factory.greaterThan(key, "goodbye"));
        
        JsonObject json = new JsonObject();
        json.add(ExpressionSerializer.PROP_OPERAND, gson.toJsonTree(expr.getOperand()));
        json.add(ExpressionSerializer.PROP_OPERATOR, gson.toJsonTree(expr.getOperator()));
        json.addProperty(ExpressionSerializer.PROP_CLASS_NAME, UnaryLogicalExpression.class.getCanonicalName());
        
        assertEquals(gson.toJson(json), gson.toJson(expr));
    }
    
    @Test(expected=JsonParseException.class)
    public void testSerializeUnknownExpression() {
        gson.toJson(new TestExpression());
    }

}
