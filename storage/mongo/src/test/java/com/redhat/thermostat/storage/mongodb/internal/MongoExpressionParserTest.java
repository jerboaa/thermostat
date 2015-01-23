/*
 * Copyright 2012-2015 Red Hat, Inc.
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

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;

import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObjectBuilder;
import com.mongodb.DBObject;
import com.redhat.thermostat.storage.core.Key;
import com.redhat.thermostat.storage.query.Expression;
import com.redhat.thermostat.storage.query.ExpressionFactory;

public class MongoExpressionParserTest {
    
    private final Key<Integer> KEY_1 = new Key<>("test");
    private final Key<Integer> KEY_2 = new Key<>("test2");
    private final Key<String> KEY_3 = new Key<>("key");
    
    private MongoExpressionParser parser;
    private ExpressionFactory factory;

    @Before
    public void setUp() throws Exception {
        parser = new MongoExpressionParser();
        factory = new ExpressionFactory();
    }

    @Test
    public void testWhereEquals() {
        Expression expr = factory.equalTo(KEY_3, "value");
        DBObject query = BasicDBObjectBuilder.start().add(KEY_3.getName(), "value").get();
        assertEquals(query, parser.parse(expr));
    }

    @Test
    public void testWhereNotEquals() {
        Expression expr = factory.notEqualTo(KEY_3, "value");
        DBObject query = BasicDBObjectBuilder.start().push(KEY_3.getName()).add("$ne", "value").get();
        assertEquals(query, parser.parse(expr));
    }

    @Test
    public void testWhereGreaterThan() {
        Expression expr = factory.greaterThan(KEY_3, "value");
        DBObject query = BasicDBObjectBuilder.start().push(KEY_3.getName()).add("$gt", "value").get();
        assertEquals(query, parser.parse(expr));
    }

    @Test
    public void testWhereGreaterThanOrEqualTo() {
        Expression expr = factory.greaterThanOrEqualTo(KEY_3, "value");
        DBObject query = BasicDBObjectBuilder.start().push(KEY_3.getName()).add("$gte", "value").get();
        assertEquals(query, parser.parse(expr));
    }

    @Test
    public void testWhereLessThan() {
        Expression expr = factory.lessThan(KEY_3, "value");
        DBObject query = BasicDBObjectBuilder.start().push(KEY_3.getName()).add("$lt", "value").get();
        assertEquals(query, parser.parse(expr));
    }

    @Test
    public void testWhereLessThanOrEqualTo() {
        Expression expr = factory.lessThanOrEqualTo(KEY_3, "value");
        DBObject query = BasicDBObjectBuilder.start().push(KEY_3.getName()).add("$lte", "value").get();
        assertEquals(query, parser.parse(expr));
    }
    
    @Test
    public void testWhereIn() {
        Set<String> values = new HashSet<>(Arrays.asList("value", "values"));
        Expression expr = factory.in(KEY_3, values, String.class);
        DBObject query = BasicDBObjectBuilder.start().push(KEY_3.getName()).add("$in", values).get();
        assertEquals(query, parser.parse(expr));
    }
    
    @Test
    public void testWhereNotIn() {
        Set<String> values = new HashSet<>(Arrays.asList("value", "values"));
        Expression expr = factory.notIn(KEY_3, values, String.class);
        DBObject query = BasicDBObjectBuilder.start().push(KEY_3.getName()).add("$nin", values).get();
        assertEquals(query, parser.parse(expr));
    }

    @Test
    public void testMultiWhere() {
        Expression expr = factory.and(factory.lessThanOrEqualTo(KEY_1, 1), factory.greaterThan(KEY_1, 2));
        
        BasicDBList list = new BasicDBList();
        list.add(BasicDBObjectBuilder.start().push("test").add("$lte", 1).get());
        list.add(BasicDBObjectBuilder.start().push("test").add("$gt", 2).get());
        DBObject dbObject = BasicDBObjectBuilder.start().add("$and", list).get();
        assertEquals(dbObject, parser.parse(expr));
    }
    
    @Test
    public void testMultiWhere2() {
        Expression expr = factory.and(factory.lessThanOrEqualTo(KEY_1, 1), factory.greaterThan(KEY_2, 2));

        BasicDBList list = new BasicDBList();
        list.add(BasicDBObjectBuilder.start().push("test").add("$lte", 1).get());
        list.add(BasicDBObjectBuilder.start().push("test2").add("$gt", 2).get());
        DBObject dbObject = BasicDBObjectBuilder.start().add("$and", list).get();
        assertEquals(dbObject, parser.parse(expr));
    }
    
    @Test
    public void testMultiWhere3() {
        Expression expr = factory.and(factory.equalTo(KEY_1, 1), factory.greaterThan(KEY_1, 2));

        BasicDBList list = new BasicDBList();
        list.add(BasicDBObjectBuilder.start().add("test", 1).get());
        list.add(BasicDBObjectBuilder.start().push("test").add("$gt", 2).get());
        DBObject dbObject = BasicDBObjectBuilder.start().add("$and", list).get();
        assertEquals(dbObject, parser.parse(expr));
    }
    
    @Test
    public void testMultiWhere4() {
        Expression expr = factory.and(factory.equalTo(KEY_1, 1), factory.greaterThan(KEY_2, 2));

        BasicDBList list = new BasicDBList();
        list.add(BasicDBObjectBuilder.start().add("test", 1).get());
        list.add(BasicDBObjectBuilder.start().push("test2").add("$gt", 2).get());
        DBObject dbObject = BasicDBObjectBuilder.start().add("$and", list).get();
        assertEquals(dbObject, parser.parse(expr));
    }
    
    @Test
    public void testWhereOr() {
        Expression expr = factory.or(factory.equalTo(KEY_1, 1), factory.greaterThan(KEY_2, 2));

        BasicDBList list = new BasicDBList();
        list.add(BasicDBObjectBuilder.start().add("test", 1).get());
        list.add(BasicDBObjectBuilder.start().push("test2").add("$gt", 2).get());
        DBObject dbObject = BasicDBObjectBuilder.start().add("$or", list).get();
        assertEquals(dbObject, parser.parse(expr));
    }
    
    @Test
    public void testWhereNotCompare() {
        Expression expr = factory.not(factory.greaterThan(KEY_1, 1));

        DBObject dbObject = BasicDBObjectBuilder.start().push("test").push("$not").add("$gt", 1).get();
        assertEquals(dbObject, parser.parse(expr));
    }
    
    @Test
    public void testWhereNotSetCompare() {
        Set<Integer> values = new HashSet<>(Arrays.asList(1, 2));
        Expression expr = factory.not(factory.in(KEY_1, values, Integer.class));

        DBObject dbObject = BasicDBObjectBuilder.start().push("test").push("$not").add("$in", values).get();
        assertEquals(dbObject, parser.parse(expr));
    }
    
    @Test(expected=IllegalArgumentException.class)
    public void testWhereLogicalNotEquals() {
        Expression expr = factory.not(factory.equalTo(KEY_1, 1));
        parser.parse(expr);
    }
    
    @Test
    public void testWhere3() {
        Expression expr = factory.and(
                factory.lessThanOrEqualTo(KEY_3, "value"),
                factory.and(factory.equalTo(KEY_1, 1),
                        factory.greaterThan(KEY_2, 2)));

        BasicDBList list = new BasicDBList();
        list.add(BasicDBObjectBuilder.start().add("test", 1).get());
        list.add(BasicDBObjectBuilder.start().push("test2").add("$gt", 2).get());
        DBObject dbObject = BasicDBObjectBuilder.start().add("$and", list).get();
        list = new BasicDBList();
        list.add(BasicDBObjectBuilder.start().push("key").add("$lte", "value").get());
        list.add(dbObject);
        dbObject = BasicDBObjectBuilder.start().add("$and", list).get();
        assertEquals(dbObject, parser.parse(expr));
    }

}

