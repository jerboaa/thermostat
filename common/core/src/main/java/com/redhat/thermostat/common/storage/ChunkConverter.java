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

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import org.bson.types.ObjectId;

import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import com.redhat.thermostat.common.utils.LoggingUtils;

class ChunkConverter {

    private static final Logger logger = LoggingUtils.getLogger(ChunkConverter.class);

    DBObject chunkToDBObject(Chunk chunk) {
        BasicDBObject dbObject = new BasicDBObject();
        Map<String, DBObject> dbObjectMap = null;
        for (Key<?> key : chunk.getKeys()) {
            dbObjectMap = convertChunkKey(chunk, key, dbObject, dbObjectMap);
        }
        return dbObject;
    }

    private Map<String, DBObject> convertChunkKey(Chunk chunk, Key<?> key, DBObject dbObject, Map<String,DBObject> dbObjectMap) {
        String[] keyParts = key.getName().split("\\.");
        String initialName = keyParts[0];
        return convertChunkKeyRecursively(chunk, key, dbObject, keyParts, 0, initialName, dbObjectMap);
    }

    private Map<String, DBObject> convertChunkKeyRecursively(Chunk chunk, Key<?> key, DBObject dbObject, String[] keyParts, int partIndex,
                                            String partialKeyName, Map<String, DBObject> dbObjectMap) {
        if (partIndex == keyParts.length - 1) {
            String dbKey = keyParts[partIndex];
            Object value = chunk.get(key);
            if (dbKey.equals("_id")) {
                value = new ObjectId((String) value);
            }
            dbObject.put(dbKey, value);
        } else {
            dbObjectMap = lazyCreateDBObjectMap(dbObjectMap);
            DBObject nestedDbObject = getOrCreateSubObject(partialKeyName, dbObjectMap);
            dbObject.put(keyParts[partIndex], nestedDbObject);
            partIndex++;
            String nextSubKey = keyParts[partIndex];
            partialKeyName = partialKeyName + "." + nextSubKey;
            convertChunkKeyRecursively(chunk, key, nestedDbObject, keyParts, partIndex, partialKeyName, dbObjectMap);
        }
        return dbObjectMap;
    }

    
    private Map<String, DBObject> lazyCreateDBObjectMap(Map<String, DBObject> dbObjectMap) {
        if (dbObjectMap == null) {
            dbObjectMap = new HashMap<String, DBObject>();
        }
        return dbObjectMap;
    }

    private DBObject getOrCreateSubObject(String partialKeyName,
            Map<String, DBObject> dbObjectMap) {
        DBObject dbObject = dbObjectMap.get(partialKeyName);
        if (dbObject == null) {
            dbObject = new BasicDBObject();
            dbObjectMap.put(partialKeyName, dbObject);
        }
        return dbObject;
    }

    public Chunk dbObjectToChunk(DBObject dbObject, Category category) {
        Chunk chunk = new Chunk(category, false);
        dbObjectToChunkRecurse(chunk, dbObject, category, null);
        return chunk;
    }

    private void dbObjectToChunkRecurse(Chunk chunk, DBObject dbObject, Category category, String fullKey) {
        for (String dbKey : dbObject.keySet()) {
            String newFullKey;
            if (fullKey == null) {
                newFullKey = dbKey;
            } else {
                newFullKey = fullKey + "." + dbKey;
            }
            dbObjectToChunkRecursively(chunk, dbObject, category, dbKey,
                    newFullKey);
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void dbObjectToChunkRecursively(Chunk chunk, DBObject dbObject, Category category, String dbKey, String fullKey) {
        Object value = dbObject.get(dbKey);
        if (value instanceof DBObject) {
            DBObject dbObj = (DBObject) value;
            dbObjectToChunkRecurse(chunk, dbObj, category, fullKey);
        } else if (value instanceof ObjectId) {
            Key key = category.getKey(fullKey);
            chunk.put(key, objectIdToString((ObjectId) value));
        } else {
            Key key = category.getKey(fullKey);
            if (key != null) {
                chunk.put(key, value);
            } else {
                logger.warning("No key matching \"" + fullKey + "\" in category \"" + category + "\"");
            }
        }
    }

    private String objectIdToString(ObjectId value) {
        return value.toString();
    }
}
