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

import java.util.logging.Level;
import java.util.logging.Logger;

import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.MongoException;
import com.redhat.thermostat.common.utils.LoggingUtils;
import com.redhat.thermostat.storage.core.StorageException;
import com.redhat.thermostat.storage.core.experimental.BasicBatchCursor;
import com.redhat.thermostat.storage.model.Pojo;

class MongoCursor<T extends Pojo> extends BasicBatchCursor<T> {

    private static final Logger logger = LoggingUtils.getLogger(MongoCursor.class);
    private DBCursor cursor;
    private Class<T> resultClass;

    MongoCursor(DBCursor cursor, Class<T> resultClass) {
        this.cursor = cursor;
        this.resultClass = resultClass;
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
            DBObject next = cursor.next();
            if (next == null) {
                // FIXME: Thermostat 2.0: Change to throwing NoSuchElementException
                String warning = "No next element but next() is being called. " +
                                 "This will throw NoSuchElementException in the next release!";
                logger.log(Level.WARNING, warning);
                return null;
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
        cursor.batchSize(super.getBatchSize());
    }
    
}

