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

import java.beans.PropertyDescriptor;
import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.apache.commons.beanutils.PropertyUtils;

import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import com.redhat.thermostat.storage.core.Persist;
import com.redhat.thermostat.storage.core.StorageException;
import com.redhat.thermostat.storage.model.Pojo;

// We have to use raw types and unchecked casts, since we don't have
// the relevant generic type info in this class. Suppress warnings
// in this class.
@SuppressWarnings({"rawtypes", "unchecked"})
class MongoPojoConverter {

    public DBObject convertPojoToMongo(Pojo obj) {
        try {
            return convertPojoToMongoImpl(obj);
        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException ex) {
            throw new StorageException(ex);
        }
    }

    private DBObject convertPojoToMongoImpl(Pojo obj) throws IllegalAccessException, InvocationTargetException, NoSuchMethodException {
        BasicDBObject dbObj = new BasicDBObject();
        PropertyDescriptor[] descs = PropertyUtils.getPropertyDescriptors(obj);
        for (PropertyDescriptor desc : descs) {
            storePropertyToDBObject(obj, dbObj, desc);
        }
        return dbObj;
    }

    private void storePropertyToDBObject(Pojo obj, BasicDBObject dbObj, PropertyDescriptor desc) throws IllegalAccessException, InvocationTargetException, NoSuchMethodException {
        if (hasPersistentAnnotation(desc)) {
            String name = desc.getName();
            Object value = PropertyUtils.getProperty(obj, name);
            if (desc.getPropertyType().isArray()) {
                value = convertIndexedProperty(value);
            }
            if (value instanceof Pojo) {
                value = convertPojoToMongoImpl((Pojo) value);
            }
            dbObj.put(name, value);
        }
    }

    private Object convertIndexedProperty(Object values) throws IllegalAccessException, InvocationTargetException, NoSuchMethodException {
        int length = Array.getLength(values);
        List list = new ArrayList(length);
        for (int i = 0; i < length; i++) {
            Object value = Array.get(values, i);
            if (value instanceof Pojo) {
                value = convertPojoToMongoImpl((Pojo) value);
            }
            list.add(value);
        }
        return list;
    }

    public <T extends Pojo> T convertMongoToPojo(DBObject dbObj, Class<T> pojoClass) {
        try {
            return convertMongoToPojoImpl(dbObj, pojoClass);
        } catch (IllegalAccessException | InstantiationException | InvocationTargetException | NoSuchMethodException ex) {
            throw new StorageException(ex);
        }
    }

    private <T extends Pojo> T convertMongoToPojoImpl(DBObject dbObj, Class pojoClass) throws InstantiationException, IllegalAccessException, InvocationTargetException, NoSuchMethodException {
        if (dbObj == null) {
            return null;
        }
        T pojo = (T) pojoClass.newInstance();
        Set<String> keys = dbObj.keySet();
        for (String name : keys) {
            if (! name.equals("_id")) {
                storePropertyToPojo(dbObj, pojo, name);
            }
        }
        return pojo;
    }

    private <T extends Pojo> void storePropertyToPojo(DBObject dbObj, T pojo, String name)
            throws IllegalAccessException, InvocationTargetException, NoSuchMethodException, InstantiationException {

        PropertyDescriptor desc = PropertyUtils.getPropertyDescriptor(pojo, name);
        if (hasPersistentAnnotation(desc)) {
            Object value = dbObj.get(name);
            if (desc.getPropertyType().isArray()) {
                value = convertIndexedPropertyFromMongo(desc, (List) value);
            }
            if (value instanceof DBObject) {
                value = convertMongoToPojoImpl((DBObject) value, desc.getPropertyType());
            }
            PropertyUtils.setProperty(pojo, name, value);
        } else {
            throw new StorageException("no available mapping for extra property: '" + name + "' in " + pojo.getClass().getName());
        }
    }

    private Object convertIndexedPropertyFromMongo(PropertyDescriptor desc, List values) throws InstantiationException, IllegalAccessException, InvocationTargetException, NoSuchMethodException {
        Class componentType = desc.getPropertyType().getComponentType();
        Object array = Array.newInstance(componentType, values.size());
        int i = 0;
        for (Object value : values) {
            if (value instanceof DBObject) {
                value = convertMongoToPojoImpl((DBObject) value, componentType);
            }
            Array.set(array, i, value);
            i++;
        }
        return array;
    }

    private boolean hasPersistentAnnotation(PropertyDescriptor desc) {
        if (desc == null) {
            return false;
        }
        Method writeMethod = desc.getWriteMethod();
        Method readMethod = desc.getReadMethod();
        return writeMethod != null && writeMethod.isAnnotationPresent(Persist.class)
               && readMethod != null && readMethod.isAnnotationPresent(Persist.class);
    }

}

