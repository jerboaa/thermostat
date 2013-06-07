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

import org.junit.Before;
import org.junit.Test;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.redhat.thermostat.storage.query.BinaryComparisonOperator;
import com.redhat.thermostat.storage.query.BinaryLogicalOperator;
import com.redhat.thermostat.storage.query.Operator;
import com.redhat.thermostat.storage.query.UnaryLogicalOperator;

public class OperatorSerializerTest {

    private Gson gson;

    private static final class TestOperator implements Operator {
        
    }
    
    @Before
    public void setUp() throws Exception {
        gson = new GsonBuilder()
                .registerTypeHierarchyAdapter(Operator.class,
                        new OperatorSerializer()).create();
    }

    @Test
    public void testDeserializeBinaryComparisonOperator() {
        Operator op = BinaryComparisonOperator.EQUALS;
        
        JsonObject json = new JsonObject();
        json.addProperty(OperatorSerializer.PROP_CONST, BinaryComparisonOperator.EQUALS.name());
        json.addProperty(OperatorSerializer.PROP_CLASS_NAME, BinaryComparisonOperator.class.getCanonicalName());
        
        assertEquals(op, gson.fromJson(json, Operator.class));
    }
    
    @Test
    public void testDeserializeBinaryLogicalOperator() {
        Operator op = BinaryLogicalOperator.AND;
        
        JsonObject json = new JsonObject();
        json.addProperty(OperatorSerializer.PROP_CONST, BinaryLogicalOperator.AND.name());
        json.addProperty(OperatorSerializer.PROP_CLASS_NAME, BinaryLogicalOperator.class.getCanonicalName());
        
        assertEquals(op, gson.fromJson(json, Operator.class));
    }
    
    @Test
    public void testDeserializeUnaryLogicalOperator() {
        Operator op = UnaryLogicalOperator.NOT;
        
        JsonObject json = new JsonObject();
        json.addProperty(OperatorSerializer.PROP_CONST, UnaryLogicalOperator.NOT.name());
        json.addProperty(OperatorSerializer.PROP_CLASS_NAME, UnaryLogicalOperator.class.getCanonicalName());
        
        assertEquals(op, gson.fromJson(json, Operator.class));
    }
    
    @Test(expected=JsonParseException.class)
    public void testDeserializeNoClassName() {
        JsonObject json = new JsonObject();
        
        gson.fromJson(json, Operator.class);
    }
    
    @Test(expected=JsonParseException.class)
    public void testDeserializeUnknownOperator() {
        JsonObject json = new JsonObject();
        json.addProperty(OperatorSerializer.PROP_CLASS_NAME, TestOperator.class.getCanonicalName());
        
        gson.fromJson(json, Operator.class);
    }

    @Test
    public void testSerializeBinaryComparisonOperator() {
        Operator op = BinaryComparisonOperator.EQUALS;
        
        JsonObject json = new JsonObject();
        json.addProperty(OperatorSerializer.PROP_CONST, BinaryComparisonOperator.EQUALS.name());
        json.addProperty(OperatorSerializer.PROP_CLASS_NAME, BinaryComparisonOperator.class.getCanonicalName());
        
        assertEquals(gson.toJson(json), gson.toJson(op));
    }
    
    @Test
    public void testSerializeBinaryLogicalOperator() {
        Operator op = BinaryLogicalOperator.AND;
        
        JsonObject json = new JsonObject();
        json.addProperty(OperatorSerializer.PROP_CONST, BinaryLogicalOperator.AND.name());
        json.addProperty(OperatorSerializer.PROP_CLASS_NAME, BinaryLogicalOperator.class.getCanonicalName());
        
        assertEquals(gson.toJson(json), gson.toJson(op));
    }
    
    @Test
    public void testSerializeUnaryLogicalOperator() {
        Operator op = UnaryLogicalOperator.NOT;
        
        JsonObject json = new JsonObject();
        json.addProperty(OperatorSerializer.PROP_CONST, UnaryLogicalOperator.NOT.name());
        json.addProperty(OperatorSerializer.PROP_CLASS_NAME, UnaryLogicalOperator.class.getCanonicalName());
        
        assertEquals(gson.toJson(json), gson.toJson(op));
    }
    
    @Test(expected=JsonParseException.class)
    public void testSerializeUnknownOperator() {
        gson.toJson(new TestOperator());
    }

}
