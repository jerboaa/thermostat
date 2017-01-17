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

package com.redhat.thermostat.storage.mongodb.internal;

import java.util.NoSuchElementException;

import org.bson.Document;

import com.mongodb.MongoException;
import com.mongodb.client.FindIterable;
import com.redhat.thermostat.storage.core.BasicBatchCursor;
import com.redhat.thermostat.storage.core.StorageException;
import com.redhat.thermostat.storage.model.Pojo;

class MongoCursor<T extends Pojo> extends BasicBatchCursor<T> {

    private final FindIterable<Document> findIterable;
    private final Class<T> resultClass;
    private final com.mongodb.client.MongoCursor<Document> cursor;

    MongoCursor(FindIterable<Document> iterable, Class<T> resultClass) {
        this.findIterable = iterable;
        this.resultClass = resultClass;
        this.cursor = iterable.iterator();
    }

    @Override
    public boolean hasNext() {
        try {
            return cursor.hasNext();
        } catch (MongoException me) {
            throw new StorageException(me);
        }
    }

    @Override
    public T next() {
        try {
            Document next = cursor.next();
            if (next == null) {
                throw new NoSuchElementException();
            }
            MongoPojoConverter converter = new MongoPojoConverter();
            return converter.convertMongoToPojo(next, resultClass);
        } catch (MongoException me) {
            throw new StorageException(me);
        }
    }

    @Override
    public void setBatchSize(int n) throws IllegalArgumentException {
        super.setBatchSize(n); // validates input
        findIterable.batchSize(n);
    }
    
}

