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

package com.redhat.thermostat.common.storage;

import java.util.Map;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.redhat.thermostat.common.dao.Converter;
import com.redhat.thermostat.common.model.Pojo;

class MongoCursor<T extends Pojo> implements Cursor<T> {

    private DBCursor cursor;
    private Category category;
    private Class<T> resultClass;
    private Map<Class<?>, Converter<?>> converters;

    MongoCursor(DBCursor cursor, Category category, Class<T> resultClass, Map<Class<?>, Converter<?>> converters) {
        this.cursor = cursor;
        this.category = category;
        this.resultClass = resultClass;
        this.converters = converters;
    }

    @Override
    public boolean hasNext() {
        return cursor.hasNext();
    }

    @Override
    public T next() {
        DBObject next = cursor.next();
        if (next == null) {
            return null;
        }
        ChunkConverter converter = new ChunkConverter();
        Chunk resultChunk = converter.dbObjectToChunk(next, category);
        return ChunkToPojoConverter.convertChunkToPojo(resultChunk, resultClass, converters);
    }

    @Override
    public Cursor<T> sort(Key<?> orderBy, SortDirection direction) {
        if (!category.getKeys().contains(orderBy)) {
            throw new IllegalArgumentException("Key not present in this Cursor's category.");
        }   /* TODO: There are other possible error conditions.  Once there is API to configure
             * indexing/optimization, we may want to prevent or log predictably bad performance
             * sorting requests.
             */
        DBObject dbOrderBy = new BasicDBObject(orderBy.getName(), direction.getValue());
        DBCursor sorted = cursor.sort(dbOrderBy);
        return new MongoCursor<T>(sorted, category, resultClass, converters);
    }

    @Override
    public Cursor<T> limit(int i) {
        DBCursor limited = cursor.limit(i);
        return new MongoCursor<T>(limited, category, resultClass, converters);
    }

}
