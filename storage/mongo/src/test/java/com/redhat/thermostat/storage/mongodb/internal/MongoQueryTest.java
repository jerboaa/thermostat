/*
 * Copyright 2012 Red Hat, Inc.
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
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import com.redhat.thermostat.storage.core.Category;
import com.redhat.thermostat.storage.core.Query;
import com.redhat.thermostat.storage.core.Query.Criteria;
import com.redhat.thermostat.storage.mongodb.internal.MongoQuery;

public class MongoQueryTest {

    @Test
    public void testEmptyQuery() {
        MongoQuery query = new MongoQuery();
        DBObject mongoQuery = query.getGeneratedQuery();
        assertTrue(mongoQuery.keySet().isEmpty());
    }

    @Test
    public void testCollectionName() {
        MongoQuery query = new MongoQuery().from(new Category("some-collection"));
        assertEquals("some-collection", query.getCategory().getName());
    }

    @Test
    public void testWhereEquals() {
        DBObject generatedQuery = generateSimpleWhereQuery("key", Criteria.EQUALS, "value");
        assertEquals("value", generatedQuery.get("key"));
    }

    @Test
    public void testWhereNotEquals() {
        DBObject generatedQuery = generateSimpleWhereQuery("key", Criteria.NOT_EQUAL_TO, "value");
        assertEquals(new BasicDBObject("$ne", "value"), generatedQuery.get("key"));
    }

    @Test
    public void testWhereGreaterThan() {
        DBObject generatedQuery = generateSimpleWhereQuery("key", Criteria.GREATER_THAN, "value");
        assertEquals(new BasicDBObject("$gt", "value"), generatedQuery.get("key"));
    }

    @Test
    public void testWhereGreaterThanOrEqualTo() {
        DBObject generatedQuery = generateSimpleWhereQuery("key", Criteria.GREATER_THAN_OR_EQUAL_TO, "value");
        assertEquals(new BasicDBObject("$gte", "value"), generatedQuery.get("key"));
    }

    @Test
    public void testWhereLessThan() {
        DBObject generatedQuery = generateSimpleWhereQuery("key", Criteria.LESS_THAN, "value");
        assertEquals(new BasicDBObject("$lt", "value"), generatedQuery.get("key"));
    }

    @Test
    public void testWhereLessThanOrEqualTo() {
        DBObject generatedQuery = generateSimpleWhereQuery("key", Criteria.LESS_THAN_OR_EQUAL_TO, "value");
        assertEquals(new BasicDBObject("$lte", "value"), generatedQuery.get("key"));
    }

    private DBObject generateSimpleWhereQuery(String key, Criteria criteria, Object value) {
        MongoQuery query = new MongoQuery().where(key, criteria, value);
        return query.getGeneratedQuery();
    }

}
